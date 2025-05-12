package uk.gov.dwp.health.pip.document.submission.manager.service;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
import static uk.gov.dwp.health.pip.document.submission.manager.config.restclient.model.V7AccountDetails.UserJourneyEnum.STRATEGIC;
import static uk.gov.dwp.health.pip.document.submission.manager.model.application.fileuploads.DrsDocTypeEnum.PIP2_FORM;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import uk.gov.dwp.health.pip.document.submission.manager.config.properties.ApplicationTimeframeProperties;
import uk.gov.dwp.health.pip.document.submission.manager.config.restclient.model.V7AccountDetails;
import uk.gov.dwp.health.pip.document.submission.manager.messaging.SubmittedApplicationPublisher;
import uk.gov.dwp.health.pip.document.submission.manager.messaging.WorkflowMessagePublisher;
import uk.gov.dwp.health.pip.document.submission.manager.model.ApplicationSubmittedV1;
import uk.gov.dwp.health.pip.document.submission.manager.model.application.ApplicationState;
import uk.gov.dwp.health.pip.document.submission.manager.model.application.AuditableFormSpecificationDto;
import uk.gov.dwp.health.pip.document.submission.manager.model.application.HealthCaptureApplicationDto;
import uk.gov.dwp.health.pip.document.submission.manager.model.application.ResultWrapper;
import uk.gov.dwp.health.pip.document.submission.manager.model.application.fileuploads.FileUpload;
import uk.gov.dwp.health.pip.document.submission.manager.model.application.state.ApplicationTimeframe;
import uk.gov.dwp.health.pip.document.submission.manager.model.application.state.History;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.model.ApplicationMeta;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.model.DrsMetadata;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.model.PersonalDetailsDto;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.model.PipApplicationV1;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.model.PipApplicationV1.RegionEnum;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.model.RegistrationDetailsDto;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.model.S3PdfReturn;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.model.S3RequestDocumentObject;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.model.S3RequestDocumentObject.DrsDocTypeEnum;
import uk.gov.dwp.health.pip.document.submission.manager.service.impl.V1SubmissionServiceImpl;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubmitApplicationService {

  private static final int millisPerHour = 60 * 60 * 1000;
  private static final int millisPerDay = 24 * millisPerHour;

  private final ApplicationCoordinatorService applicationCoordinatorService;
  private final WorkflowMessagePublisher workflowMessagePublisher;
  private final SubmittedApplicationPublisher submittedApplicationPublisher;
  private final IdentityStatusService identityStatusService;
  private final AccountManagerService accountManagerService;
  private final PdfGeneratorService pdfGeneratorService;
  private final ApplicationTimeframeProperties applicationTimeframeProperties;
  private final V1SubmissionServiceImpl v1SubmissionService;
  private final HealthCaptureManagerService healthCaptureManagerService;

  public void submitApplication(ApplicationSubmittedV1 submittedApplication)
          throws JsonProcessingException {

    HealthCaptureApplicationDto healthCaptureApplication = healthCaptureManagerService
        .getApplicationDtoFromHealthCaptureManager(submittedApplication.getApplicationId());

    AuditableFormSpecificationDto formSpecification =
        healthCaptureManagerService.getFormSpecificationFromHealthCaptureManager(
        healthCaptureApplication.getFormSpecificationId());

    final ResultWrapper<V7AccountDetails> account = getAccountDetailsFromAccountManager(
        healthCaptureApplication);

    healthCaptureApplication.setSubmissionDate(Date.from(Instant.now()));

    var pdfS3Response = pdfGeneratorService.generateS3Pdf(healthCaptureApplication,
        formSpecification);

    createSubmissionAndSendToDrs(healthCaptureApplication, pdfS3Response);

    if (applicationCoordinatorService.isApplicationInApplicationCoordinator(
        healthCaptureApplication.getApplicationId())) {
      applicationCoordinatorService.submit(healthCaptureApplication.getApplicationId(),
          healthCaptureApplication.getSubmissionId());
    }

    if (!account.isSuccess() || STRATEGIC.equals(account.getValue().getUserJourney())) {
      dispatchWorkflowEvent(healthCaptureApplication);
    }

    retrieveClaimantEmail(account, healthCaptureApplication);

    if (!account.isSuccess()) {
      log.info(String.format("""
              Call to Account Manager failed for application %s with claimant id %s. \
              Failure reason: %s\
              """, healthCaptureApplication.getApplicationId(),
          healthCaptureApplication.getClaimantId(),
          account.getFailures().get(0).getFailureReason()));
    }

    dispatchSubmittedApplicationEvent(healthCaptureApplication);
  }

  private void createSubmissionAndSendToDrs(HealthCaptureApplicationDto healthCaptureApplication,
      S3PdfReturn pdfS3Response) {
    PipApplicationV1 pipApplication = createRequestBody(healthCaptureApplication, pdfS3Response);

    var dsmResult = v1SubmissionService.createNewSubmission(pipApplication);

    healthCaptureApplication.addFile(mapPdfResponseToFileUpload(pdfS3Response));
    setApplicationState(healthCaptureApplication);
    healthCaptureApplication.setSubmissionId(dsmResult.getSubmissionId());
  }

  private ResultWrapper<V7AccountDetails> getAccountDetailsFromAccountManager(
      HealthCaptureApplicationDto healthCaptureApplication) throws JsonProcessingException {
    final ResultWrapper<V7AccountDetails> account = accountManagerService.getAccountMgrData(
        healthCaptureApplication.getClaimantId());

    if (account.isSuccess()) {
      final V7AccountDetails accountDetails = account.getValue();
      if (healthCaptureApplication.getRegistrationDetails() == null) {
        mapAccountDetailsToPersonal(healthCaptureApplication, accountDetails);
      }
      if (healthCaptureApplication.getApplicationTimeframe() == null) {
        final String accountId = accountDetails.getRef();
        final ObjectId objectId = new ObjectId(accountId);
        final Date objectIdDate = objectId.getDate();
        final long epochDay = objectIdDate.getTime() / millisPerDay;
        final LocalDate startDate = LocalDate.ofEpochDay(epochDay);
        final int activeDuration = applicationTimeframeProperties.getActiveDuration();
        final ApplicationTimeframe applicationTimeframe = ApplicationTimeframe.builder()
            .effectiveFrom(startDate).effectiveTo(startDate.plus(activeDuration, ChronoUnit.DAYS))
            .build();
        healthCaptureApplication.setApplicationTimeframe(applicationTimeframe);
      }
    }
    return account;
  }

  private PipApplicationV1 createRequestBody(HealthCaptureApplicationDto application,
      S3PdfReturn s3PdfReturn) {
    var requestBody = new PipApplicationV1();
    requestBody.setClaimantId(application.getClaimantId());
    requestBody.applicationId(application.getApplicationId());
    requestBody.setDocuments(createDocuments(s3PdfReturn, application.getFiles()));
    requestBody.setDrsMetadata(createDrsMetaData(application));
    requestBody.setApplicationMeta(createApplicationMetadata(application));
    requestBody.setRegion(RegionEnum.GB);
    return requestBody;
  }


  private ApplicationMeta createApplicationMetadata(HealthCaptureApplicationDto application) {
    var applicationMeta = new ApplicationMeta();
    var applicationTimeFrame = application.getApplicationTimeframe();
    var completedDate = application.getSubmissionDate().toInstant()
        .atZone(ZoneId.of("Europe/Paris")).toLocalDate();
    applicationMeta.setStartDate(applicationTimeFrame.getEffectiveFrom());
    applicationMeta.setCompletedDate(completedDate);

    return applicationMeta;
  }

  private DrsMetadata createDrsMetaData(HealthCaptureApplicationDto application) {
    var personalDetails = application.getRegistrationDetails().getPersonalDetails();
    var formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    return new DrsMetadata(personalDetails.getSurname(), personalDetails.getFirstName(),
        LocalDate.parse(personalDetails.getDateOfBirth(), formatter),
        personalDetails.getNationalInsuranceNumber(), personalDetails.getPostcode());
  }

  private List<S3RequestDocumentObject> createDocuments(S3PdfReturn s3PdfReturn,
      List<FileUpload> files) {
    var filesMetadata = files.stream().map(this::createFileMetaData).toList();
    var pdfMetadata = createPdfDocumentMetaData(s3PdfReturn);

    var documentList = new ArrayList<S3RequestDocumentObject>();
    documentList.add(pdfMetadata);
    documentList.addAll(filesMetadata);
    return documentList;
  }

  private S3RequestDocumentObject createFileMetaData(FileUpload fileUpload) {
    return new S3RequestDocumentObject(fileUpload.getBucket(), fileUpload.getS3Ref(),
        fileUpload.getMimetype(), fileUpload.getS3Ref(), Math.toIntExact(fileUpload.getSize()),
        LocalDateTime.now(), DrsDocTypeEnum._1241);
  }

  private S3RequestDocumentObject createPdfDocumentMetaData(S3PdfReturn s3PdfReturn) {
    return new S3RequestDocumentObject(s3PdfReturn.getBucket(), s3PdfReturn.getS3Ref(),
        MediaType.APPLICATION_PDF_VALUE, s3PdfReturn.getS3Ref(), s3PdfReturn.getFileSizeKb(),
        LocalDateTime.now(),
        // PDF doc type enum
        DrsDocTypeEnum._1274);
  }

  private static void mapAccountDetailsToPersonal(
      final HealthCaptureApplicationDto healthCaptureApplication,
      final V7AccountDetails accountDetails) {
    healthCaptureApplication.setRegistrationDetails(new RegistrationDetailsDto());
    final PersonalDetailsDto personalDetails = new PersonalDetailsDto();
    personalDetails.setDateOfBirth(accountDetails.getDob().format(ISO_LOCAL_DATE));
    personalDetails.setFirstName(accountDetails.getForename());
    personalDetails.setSurname(accountDetails.getSurname());
    personalDetails.setPostcode(accountDetails.getPostcode());
    personalDetails.setNationalInsuranceNumber(accountDetails.getNino());
    personalDetails.setEmail(accountDetails.getEmail());
    healthCaptureApplication.getRegistrationDetails().setPersonalDetails(personalDetails);
  }

  private FileUpload mapPdfResponseToFileUpload(S3PdfReturn pdfS3Response) {
    var fileupload = new FileUpload();
    fileupload.setSanitisedName(pdfS3Response.getS3Ref());
    fileupload.setSize(Long.valueOf(pdfS3Response.getFileSizeKb()));
    fileupload.setS3Ref(pdfS3Response.getS3Ref());
    fileupload.setBucket(pdfS3Response.getBucket());
    fileupload.setMimetype(MediaType.APPLICATION_PDF_VALUE);
    fileupload.setDisplaySize(
        fileupload.getDisplaySize(Long.valueOf(pdfS3Response.getFileSizeKb())));
    fileupload.setDateTime(LocalDateTime.now().toString());
    fileupload.setDrsDocType(PIP2_FORM);
    fileupload.setId(new ObjectId().toString());

    return fileupload;
  }

  private void dispatchWorkflowEvent(HealthCaptureApplicationDto healthCaptureApplication) {

    var personalDetails = getPersonalDetails(healthCaptureApplication);
    var name = personalDetails.getFirstName() + " " + personalDetails.getSurname();

    log.info(String.format("Attempting to dispatch Workflow Event for application %s",
        healthCaptureApplication.getApplicationId()));

    workflowMessagePublisher.publishMessage(healthCaptureApplication.getApplicationId(), name,
        personalDetails.getNationalInsuranceNumber(), healthCaptureApplication.getSubmissionDate());
  }

  private void dispatchSubmittedApplicationEvent(
          HealthCaptureApplicationDto healthCaptureApplication
  ) {
    log.info(String.format("Attempting to dispatch Submitted Application Event for application %s",
            healthCaptureApplication.getApplicationId()));

    submittedApplicationPublisher.publishMessage(healthCaptureApplication);
  }

  private void retrieveClaimantEmail(final ResultWrapper<V7AccountDetails> account,
      final HealthCaptureApplicationDto healthCaptureApplication) throws JsonProcessingException {
    var applicationId = healthCaptureApplication.getApplicationId();

    String email = null;

    log.info(
        String.format("Attempting to call Identity Server with application Id %s", applicationId));

    var identity = identityStatusService.getIdentityStatus(applicationId);

    if (identity.isSuccess()) {
      log.info(
          String.format("Successful call to Identity Server for application %s", applicationId));

      email = identity.getValue().getSubjectId();
    }

    if (!identity.isSuccess()) {
      log.info(
          String.format("Call to Identity Server failed for application %s. Failure reason: %s",
              applicationId, identity.getFailures().get(0).getFailureReason()));

      String msg = "Attempting to call Account Manager for application %s with claimant Id %s";
      log.info(String.format(msg, applicationId, healthCaptureApplication.getClaimantId()));

      if (account.isSuccess()) {
        log.info(
            String.format("Successful call to Account Manager for application %s", applicationId));
        email = account.getValue().getEmail();
      }

      if (!account.isSuccess()) {
        log.info(String.format("""
                Call to Account Manager failed for application %s with claimant id %s. \
                Failure reason: %s\
                """, applicationId, healthCaptureApplication.getClaimantId(),
            account.getFailures().get(0).getFailureReason()));
      }
    }

    if (email != null) {
      getPersonalDetails(healthCaptureApplication).setEmail(email);
      log.info(String.format("Attaching email to application %s", applicationId));
    }

    if (email == null) {
      log.info(String.format("Failed to get an email for application %s", applicationId));
    }
  }

  private static PersonalDetailsDto getPersonalDetails(
      final HealthCaptureApplicationDto application) {
    if (application.getRegistrationDetails() == null) {
      application.setRegistrationDetails(new RegistrationDetailsDto());
    }
    final RegistrationDetailsDto registrationDetails = application.getRegistrationDetails();
    if (registrationDetails.getPersonalDetails() == null) {
      registrationDetails.setPersonalDetails(new PersonalDetailsDto());
    }
    return registrationDetails.getPersonalDetails();
  }

  private void setApplicationState(HealthCaptureApplicationDto healthCaptureApplication) {
    var history = History.builder().state(ApplicationState.SUBMITTED.name())
        .timeStamp(healthCaptureApplication.getSubmissionDate().toInstant()).build();

    var state = healthCaptureApplication.getState();
    state.setCurrent(ApplicationState.SUBMITTED.name());
    state.addHistory(history);
  }
}
