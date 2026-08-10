package uk.gov.dwp.health.pip.document.submission.manager.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import tools.jackson.core.JacksonException;
import uk.gov.dwp.health.pip.application.coordinator.openapi.coordinator.dto.ApplicationDetails;
import uk.gov.dwp.health.pip.document.submission.manager.config.properties.ApplicationTimeframeProperties;
import uk.gov.dwp.health.pip.document.submission.manager.config.restclient.model.V7AccountDetails;
import uk.gov.dwp.health.pip.document.submission.manager.config.restclient.model.V7AccountDetails.LanguageEnum;
import uk.gov.dwp.health.pip.document.submission.manager.config.restclient.model.V7AccountDetails.RegionEnum;
import uk.gov.dwp.health.pip.document.submission.manager.config.restclient.model.V7AccountDetails.UserJourneyEnum;
import uk.gov.dwp.health.pip.document.submission.manager.exception.V3SubmissionServiceException;
import uk.gov.dwp.health.pip.document.submission.manager.messaging.WorkflowMessagePublisher;
import uk.gov.dwp.health.pip.document.submission.manager.model.application.ResultWrapper;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.healthinformation.v2.dto.ApplicationTimeframeDto;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.healthinformation.v2.dto.FileUploadDto;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.healthinformation.v2.dto.HealthCaptureApplicationDtoV2;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.healthinformation.v2.dto.PersonalDetailsDto;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.healthinformation.v2.dto.RegistrationDetailsDto;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.model.ApplicationMeta;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.model.DrsMetadata;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.model.PipApplicationV1;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.model.S3RequestDocumentObject;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.model.S3RequestDocumentObject.DrsDocTypeEnum;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.model.SubmissionResponseObjectV1;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.pdfgenerator.v4.dto.S3PdfReturn;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.registration.v4.dto.RegistrationDto;
import uk.gov.dwp.health.pip.document.submission.manager.service.AccountManagerService;
import uk.gov.dwp.health.pip.document.submission.manager.service.ApplicationCoordinatorService;
import uk.gov.dwp.health.pip.document.submission.manager.service.HealthCaptureManagerService;
import uk.gov.dwp.health.pip.document.submission.manager.service.PdfGeneratorService;
import uk.gov.dwp.health.pip.document.submission.manager.utils.DrsMetadataValidationUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static uk.gov.dwp.health.pip.document.submission.manager.utils.DateTimeUtils.instantToString;
import static uk.gov.dwp.health.pip.document.submission.manager.utils.DateTimeUtils.stringToInstant;

@RequiredArgsConstructor
@Slf4j
@Service
public class V3SubmissionServiceImpl {

  private static final int MILLIS_PER_HOUR = 60 * 60 * 1000;
  private static final int MILLIS_PER_DAY = 24 * MILLIS_PER_HOUR;

  private final ApplicationCoordinatorService applicationCoordinatorService;
  private final WorkflowMessagePublisher workflowMessagePublisher;
  private final AccountManagerService accountManagerService;
  private final RegistrationCaptureManagerService registrationCaptureManagerService;
  private final PdfGeneratorService pdfGeneratorService;
  private final ApplicationTimeframeProperties applicationTimeframeProperties;
  private final HealthCaptureManagerService healthCaptureManagerService;
  private final EmailNotificationServiceImpl emailNotificationService;
  private final GetClaimantEmailService getClaimantEmailService;
  private final V1SubmissionServiceImpl v1SubmissionService;
  private final Clock clock;

  public HealthCaptureApplicationDtoV2 createNewSubmission(String applicationId, String userId) {
    log.info("Begin createNewSubmission");

    HealthCaptureApplicationDtoV2 healthCaptureApplicationDtoV2 =
        healthCaptureManagerService.getApplicationDtoV2FromHealthCaptureManager(applicationId);

    String submissionDate = instantToString(clock.instant());
    log.info("Set submission date to {}", submissionDate);
    healthCaptureApplicationDtoV2.setSubmissionDate(submissionDate);

    // Default to GB/English/Strategic application if not otherwise specified.
    RegionEnum region = RegionEnum.GB;
    LanguageEnum language = LanguageEnum.EN;
    UserJourneyEnum journeyType = UserJourneyEnum.STRATEGIC;

    final ResultWrapper<RegistrationDto> registration;
    try {
      registration = registrationCaptureManagerService.getRegistrationData(applicationId);
      if (registration.isSuccess()) {
        ResultWrapper<ApplicationDetails> applicationDetailsResultWrapper =
            applicationCoordinatorService.getApplication(applicationId);

        if (applicationDetailsResultWrapper.isSuccess()) {
          log.info("Successfully received application data from coordinator");
          ApplicationDetails applicationDetails = applicationDetailsResultWrapper.getValue();
          language = LanguageEnum.valueOf(applicationDetails.getLanguage().getValue());
          region = RegionEnum.valueOf(applicationDetails.getRegion().getValue());
          if (applicationDetails.getJourneyType() != null) {
            journeyType = UserJourneyEnum.valueOf(applicationDetails.getJourneyType().getValue());
          }
        }

        handleSubmissionPreparation(healthCaptureApplicationDtoV2, registration.getValue());
        processWorkflowEvent(applicationId, healthCaptureApplicationDtoV2);
      }
    } catch (JacksonException exception) {
      throw new V3SubmissionServiceException(
          "Exception occurred while parsing registration capture manager or application "
              + "coordinator JSON object for application id "
              + applicationId
              + " with message "
              + exception.getMessage(),
          exception);
    }

    if (!registration.isSuccess()) {
      log.info("Getting registration data was not successful");
      try {
        final ResultWrapper<V7AccountDetails> account =
            accountManagerService.getAccountMgrData(healthCaptureApplicationDtoV2.getClaimantId());

        if (account.isSuccess()) {
          log.info("Successfully received account data");
          handleSubmissionPreparation(healthCaptureApplicationDtoV2, account.getValue());
          region = account.getValue().getRegion();
          journeyType = account.getValue().getUserJourney();
          if (CheckToDispatchWorkflowEventService.shouldDispatchEvent(account.getValue())) {
            dispatchWorkflowEvent(healthCaptureApplicationDtoV2);
          }
        }
      } catch (JacksonException exception) {
        throw new V3SubmissionServiceException(
            "Exception occurred while parsing account manager JSON object for application id "
                + applicationId
                + " with message "
                + exception.getMessage(),
            exception);
      }
    }

    if (healthCaptureApplicationDtoV2.getRegistrationDetails() == null
        || healthCaptureApplicationDtoV2.getApplicationTimeframe() == null) {
      throw new V3SubmissionServiceException(
          "Registration details or application timeframe is null for application " + applicationId);
    }

    sendSubmission(healthCaptureApplicationDtoV2);

    String email = getEmailFromIdentity(userId);

    emailNotificationService.sendSubmissionEmailNotification(
        email, region, journeyType, applicationId, language, !registration.isSuccess());

    log.info("End createNewSubmission");

    return healthCaptureApplicationDtoV2;
  }

  private void handleSubmissionPreparation(
      HealthCaptureApplicationDtoV2 healthCaptureApplicationDtoV2, V7AccountDetails account) {
    if (healthCaptureApplicationDtoV2.getRegistrationDetails() == null) {
      log.info("Registration details == null");
      RegistrationDetailsDto registrationDetails = new RegistrationDetailsDto();
      registrationDetails.setPersonalDetails(
          HealthCaptureApplicationMapper.mapAccountDetailsToPersonalDetailsDtoV2(account));
      healthCaptureApplicationDtoV2.setRegistrationDetails(registrationDetails);
    }

    if (healthCaptureApplicationDtoV2.getApplicationTimeframe() == null) {
      log.info("Application timeframe == null");
      final ObjectId objectId = new ObjectId(account.getRef());
      final Date objectIdDate = objectId.getDate();
      final long epochDay = objectIdDate.getTime() / MILLIS_PER_DAY;
      final LocalDate startDate = LocalDate.ofEpochDay(epochDay);
      createApplicationTimeFrame(healthCaptureApplicationDtoV2, startDate);
    }

    populateClaimantEmail(healthCaptureApplicationDtoV2, account);
  }

  private void handleSubmissionPreparation(
      HealthCaptureApplicationDtoV2 healthCaptureApplicationDtoV2,
      RegistrationDto registrationDto) {

    if (healthCaptureApplicationDtoV2.getRegistrationDetails() == null) {
      log.info("Registration details == null");
      RegistrationDetailsDto registrationDetailsDto = new RegistrationDetailsDto();
      registrationDetailsDto.setPersonalDetails(
          HealthCaptureApplicationMapper.mapRegistrationDetailsToPersonalDetailsDtoV2(
              registrationDto));
      healthCaptureApplicationDtoV2.setRegistrationDetails(registrationDetailsDto);
    }

    if (healthCaptureApplicationDtoV2.getApplicationTimeframe() == null) {
      log.info("Application timeframe == null");
      createApplicationTimeFrame(
          healthCaptureApplicationDtoV2, LocalDate.parse(registrationDto.getSubmissionDate()));
    }

    populateClaimantEmail(healthCaptureApplicationDtoV2, null);
  }

  private void populateClaimantEmail(
      HealthCaptureApplicationDtoV2 healthCaptureApplicationDtoV2, V7AccountDetails account) {
    try {
      String email =
          getClaimantEmailService.getClaimantEmail(account, healthCaptureApplicationDtoV2);

      if (email != null) {
        log.info(
            "Attaching email to application {}", healthCaptureApplicationDtoV2.getApplicationId());
        healthCaptureApplicationDtoV2.getRegistrationDetails().getPersonalDetails().setEmail(email);
      }
    } catch (JacksonException exception) {
      throw new V3SubmissionServiceException(
          "Exception occurred while parsing identity service JSON object for application id "
              + healthCaptureApplicationDtoV2.getApplicationId()
              + " with message "
              + exception.getMessage(),
          exception);
    }
  }

  private void sendSubmission(HealthCaptureApplicationDtoV2 healthCaptureApplicationDtoV2) {
    S3PdfReturn pdfS3Response = pdfGeneratorService.generateS3Pdf(healthCaptureApplicationDtoV2);

    createSubmissionAndSendToDrs(healthCaptureApplicationDtoV2, pdfS3Response);

    if (applicationCoordinatorService.isApplicationInApplicationCoordinator(
        healthCaptureApplicationDtoV2.getApplicationId())) {

      try {
        applicationCoordinatorService.submitApplication(
            healthCaptureApplicationDtoV2.getApplicationId(),
            healthCaptureApplicationDtoV2.getSubmissionId());
      } catch (HttpClientErrorException e) {
        throw new V3SubmissionServiceException(
            "Exception occurred while trying to submit application to application coordinator "
                + "for application id "
                + healthCaptureApplicationDtoV2.getApplicationId());
      }
    }
  }

  private void processWorkflowEvent(
      String applicationId, HealthCaptureApplicationDtoV2 healthCaptureApplicationDtoV2) {
    log.info("Begin processWorkflowEvent");

    final ResultWrapper<ApplicationDetails> applicationCoordinatorResult;

    try {
      applicationCoordinatorResult = applicationCoordinatorService.getApplication(applicationId);
    } catch (JacksonException exception) {
      throw new V3SubmissionServiceException(
          "Exception occurred while parsing application coordinator JSON object for application id "
              + applicationId
              + " with message "
              + exception.getMessage(),
          exception);
    }

    if (!applicationCoordinatorResult.isSuccess()) {
      return;
    }

    if (CheckToDispatchWorkflowEventService.shouldDispatchEvent(
        applicationCoordinatorResult.getValue())) {
      dispatchWorkflowEvent(healthCaptureApplicationDtoV2);
    }

    log.info("End processWorkflowEvent");
  }

  private void createApplicationTimeFrame(
      HealthCaptureApplicationDtoV2 healthCaptureApplicationDtoV2, LocalDate startDate) {
    final int activeDuration = applicationTimeframeProperties.getActiveDuration();
    final ApplicationTimeframeDto applicationTimeframeDto =
        new ApplicationTimeframeDto()
            .effectiveFrom(String.valueOf(startDate))
            .effectiveTo(String.valueOf(startDate.plusDays(activeDuration)));
    healthCaptureApplicationDtoV2.setApplicationTimeframe(applicationTimeframeDto);
  }

  private void createSubmissionAndSendToDrs(
      HealthCaptureApplicationDtoV2 healthCaptureApplicationDtoV2, S3PdfReturn pdfS3Response) {
    PipApplicationV1 submission = createRequestBody(healthCaptureApplicationDtoV2, pdfS3Response);
    SubmissionResponseObjectV1 subRespObjV2 = v1SubmissionService.createNewSubmission(submission);

    healthCaptureApplicationDtoV2.getFiles().add(mapPdfResponseToFileUpload(pdfS3Response));
    healthCaptureApplicationDtoV2.setSubmissionId(subRespObjV2.getSubmissionId());
  }

  private PipApplicationV1 createRequestBody(
      HealthCaptureApplicationDtoV2 application, S3PdfReturn s3PdfReturn) {
    var requestBody = new PipApplicationV1();
    requestBody.setClaimantId(application.getClaimantId());
    requestBody.applicationId(application.getApplicationId());
    requestBody.setDocuments(createDocuments(s3PdfReturn, application.getFiles()));
    requestBody.setDrsMetadata(createDrsMetaData(application));
    requestBody.setApplicationMeta(createApplicationMetadata(application));
    requestBody.setRegion(PipApplicationV1.RegionEnum.GB);
    return requestBody;
  }

  private ApplicationMeta createApplicationMetadata(HealthCaptureApplicationDtoV2 application) {
    var applicationMeta = new ApplicationMeta();

    LocalDate effectiveFrom =
        LocalDate.parse(application.getApplicationTimeframe().getEffectiveFrom());
    applicationMeta.setStartDate(effectiveFrom);

    LocalDate completedDate =
        LocalDate.parse(application.getSubmissionDate(), DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    applicationMeta.setCompletedDate(completedDate);

    return applicationMeta;
  }

  private DrsMetadata createDrsMetaData(HealthCaptureApplicationDtoV2 application) {
    var personalDetails = application.getRegistrationDetails().getPersonalDetails();
    var formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    return new DrsMetadata(
        DrsMetadataValidationUtils.sanitiseName(personalDetails.getSurname()),
        DrsMetadataValidationUtils.sanitiseName(personalDetails.getFirstName()),
        LocalDate.parse(personalDetails.getDateOfBirth(), formatter),
        DrsMetadataValidationUtils.sanitiseNino(personalDetails.getNationalInsuranceNumber()),
        DrsMetadataValidationUtils.sanitisePostcode(personalDetails.getPostcode()));
  }

  private List<S3RequestDocumentObject> createDocuments(
      S3PdfReturn s3PdfReturn, List<FileUploadDto> files) {
    var filesMetadata = files.stream().map(this::createFileMetaData).toList();
    var pdfMetadata = createPdfDocumentMetaData(s3PdfReturn);

    var documentList = new ArrayList<S3RequestDocumentObject>();
    documentList.add(pdfMetadata);
    documentList.addAll(filesMetadata);
    return documentList;
  }

  private S3RequestDocumentObject createFileMetaData(FileUploadDto fileUpload) {
    return new S3RequestDocumentObject(
        fileUpload.getBucket(),
        fileUpload.getS3Ref(),
        fileUpload.getMimeType(),
        fileUpload.getS3Ref(),
        Math.toIntExact(fileUpload.getSize()),
        LocalDateTime.now(),
        DrsDocTypeEnum._1241);
  }

  private S3RequestDocumentObject createPdfDocumentMetaData(S3PdfReturn s3PdfReturn) {
    return new S3RequestDocumentObject(
        s3PdfReturn.getBucket(),
        s3PdfReturn.getS3Ref(),
        MediaType.APPLICATION_PDF_VALUE,
        s3PdfReturn.getS3Ref(),
        s3PdfReturn.getFileSizeKb(),
        LocalDateTime.now(),
        // PDF doc type enum
        DrsDocTypeEnum._1274);
  }

  private FileUploadDto mapPdfResponseToFileUpload(S3PdfReturn pdfS3Response) {
    Long fileSizeKb = Long.valueOf(pdfS3Response.getFileSizeKb());
    String displaySize = getDisplaySize(fileSizeKb);

    FileUploadDto fileUploadDto = new FileUploadDto();
    fileUploadDto.setSanitisedName(pdfS3Response.getS3Ref());
    fileUploadDto.setSize(fileSizeKb);
    fileUploadDto.setS3Ref(pdfS3Response.getS3Ref());
    fileUploadDto.setBucket(pdfS3Response.getBucket());
    fileUploadDto.setMimeType(MediaType.APPLICATION_PDF_VALUE);
    fileUploadDto.setDisplaySize(displaySize);
    fileUploadDto.setDateTime(LocalDateTime.now().toString());
    fileUploadDto.setDrsDocType(FileUploadDto.DrsDocTypeEnum.PIP2_FORM);
    fileUploadDto.setId(new ObjectId().toString());

    return fileUploadDto;
  }

  private String getDisplaySize(Long size) {
    if (size > 1000) {
      Long mbSize = size / 1000;
      return mbSize + " MB";
    }
    return size + " KB";
  }

  void dispatchWorkflowEvent(HealthCaptureApplicationDtoV2 healthCaptureApplicationDtoV2) {
    log.info("Begin dispatchWorkflowEvent()");

    PersonalDetailsDto personalDetails =
        healthCaptureApplicationDtoV2.getRegistrationDetails().getPersonalDetails();
    String name = personalDetails.getFirstName() + " " + personalDetails.getSurname();

    String submissionDateString = healthCaptureApplicationDtoV2.getSubmissionDate();
    Instant instant = stringToInstant(submissionDateString);
    Date submissionDate = Date.from(instant);

    log.info(
        String.format(
            "Attempting to dispatch Workflow Event for application %s",
            healthCaptureApplicationDtoV2.getApplicationId()));

    workflowMessagePublisher.publishMessage(
        healthCaptureApplicationDtoV2.getApplicationId(),
        name,
        personalDetails.getNationalInsuranceNumber(),
        submissionDate);
  }

  private String getEmailFromIdentity(String userId) {
    try {
      return getClaimantEmailService.getEmailFromIdentityByUserId(userId);
    } catch (JacksonException e) {
      throw new RuntimeException("Failed to get email from identity service for user id " + userId);
    }
  }
}
