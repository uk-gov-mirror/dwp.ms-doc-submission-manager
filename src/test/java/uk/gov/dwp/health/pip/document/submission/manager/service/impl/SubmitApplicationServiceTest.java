package uk.gov.dwp.health.pip.document.submission.manager.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.dwp.health.pip.document.submission.manager.config.properties.ApplicationTimeframeProperties;
import uk.gov.dwp.health.pip.document.submission.manager.config.restclient.model.IdentityDto;
import uk.gov.dwp.health.pip.document.submission.manager.config.restclient.model.V7AccountDetails;
import uk.gov.dwp.health.pip.document.submission.manager.exception.DataRequestException;
import uk.gov.dwp.health.pip.document.submission.manager.messaging.SubmittedApplicationPublisher;
import uk.gov.dwp.health.pip.document.submission.manager.messaging.WorkflowMessagePublisher;
import uk.gov.dwp.health.pip.document.submission.manager.model.ApplicationSubmittedV1;
import uk.gov.dwp.health.pip.document.submission.manager.model.application.AccountMgrDataNotFoundResultFailure;
import uk.gov.dwp.health.pip.document.submission.manager.model.application.AuditableFormSpecificationDto;
import uk.gov.dwp.health.pip.document.submission.manager.model.application.HealthCaptureApplicationDto;
import uk.gov.dwp.health.pip.document.submission.manager.model.application.IdentityStatusDataNotFoundResultFailure;
import uk.gov.dwp.health.pip.document.submission.manager.model.application.ResultWrapper;
import uk.gov.dwp.health.pip.document.submission.manager.model.application.state.ApplicationTimeframe;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.model.PipApplicationV1;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.model.RequestId;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.model.S3PdfReturn;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.model.SubmissionResponseObjectV1;
import uk.gov.dwp.health.pip.document.submission.manager.service.AccountManagerService;
import uk.gov.dwp.health.pip.document.submission.manager.service.ApplicationCoordinatorService;
import uk.gov.dwp.health.pip.document.submission.manager.service.HealthCaptureManagerService;
import uk.gov.dwp.health.pip.document.submission.manager.service.IdentityStatusService;
import uk.gov.dwp.health.pip.document.submission.manager.service.PdfGeneratorService;
import uk.gov.dwp.health.pip.document.submission.manager.service.SubmitApplicationService;
import uk.gov.dwp.health.pip.document.submission.manager.utils.JsonUtils;

@ExtendWith(SpringExtension.class)
class SubmitApplicationServiceTest {

  private String APPLICATION_ID = "123456789";
  private String CLAIMANT_ID = "123456789";
  private String SUBMISSION_ID = "123456789123456";

  @Mock
  private ApplicationCoordinatorService applicationCoordinatorService;
  @Mock
  private WorkflowMessagePublisher workflowMessagePublisher;
  @Mock
  private SubmittedApplicationPublisher submittedApplicationPublisher;
  @Mock
  private AccountManagerService accountManagerService;
  @Mock
  private PdfGeneratorService pdfGeneratorService;
  @Mock
  private V1SubmissionServiceImpl v1SubmissionService;
  @Mock
  IdentityStatusService identityStatusService;
  @Mock
  HealthCaptureManagerService healthCaptureManagerService;

  private S3PdfReturn s3pdfReturn;
  private SubmissionResponseObjectV1 submissionResponseResult;
  private ApplicationTimeframeProperties applicationTimeframeProperties;

  private SubmitApplicationService sut;

  @BeforeEach
  void setUp() {
    applicationTimeframeProperties = new ApplicationTimeframeProperties();
    applicationTimeframeProperties.setActiveDuration(93);
    sut = new SubmitApplicationService(applicationCoordinatorService, workflowMessagePublisher,
            submittedApplicationPublisher,
            identityStatusService, accountManagerService, pdfGeneratorService,
            applicationTimeframeProperties, v1SubmissionService, healthCaptureManagerService);
    s3pdfReturn = new S3PdfReturn();
    s3pdfReturn.setBucket("PIP_BUCKET");
    s3pdfReturn.setFileSizeKb(123456);
    s3pdfReturn.setS3Ref("S3_REF");
    submissionResponseResult = new SubmissionResponseObjectV1();
    submissionResponseResult.setSubmissionId("123456789123456");
  }

  @Test
  void whenSubmitTriggered_thenHcmEndpointAndServicesCalledCorrectly() throws IOException {
    ApplicationSubmittedV1 applicationSubmittedV1 = new ApplicationSubmittedV1();
    applicationSubmittedV1.setApplicationId(APPLICATION_ID);

    var existingApplication = JsonUtils.readJsonFromFileAndMap(
        "src/test/resources/entity/healthCaptureApplication_submission.json",
        HealthCaptureApplicationDto.class);
    when(healthCaptureManagerService.getApplicationDtoFromHealthCaptureManager(
        APPLICATION_ID)).thenReturn(existingApplication);

    var formSpecification = JsonUtils.readJsonFromFileAndMap(
        "src/test/resources/entity/formSpecification_submission.json",
        AuditableFormSpecificationDto.class);
    when(healthCaptureManagerService.getFormSpecificationFromHealthCaptureManager(
        existingApplication.getFormSpecificationId())).thenReturn(formSpecification);

    var accountMgrResponse = JsonUtils.readJsonFromFile(
        "src/test/resources/entity/dto/accountMgrResponse.json");
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    var accountMgrDataEdited = accountMgrResponse.substring(1, accountMgrResponse.length() - 1);
    var accountMgrData = objectMapper.readValue(accountMgrDataEdited, V7AccountDetails.class);

    when(accountManagerService.getAccountMgrData(CLAIMANT_ID)).thenReturn(
        ResultWrapper.<V7AccountDetails>builder().value(accountMgrData).build());

    when(pdfGeneratorService.generateS3Pdf(existingApplication, formSpecification)).thenReturn(
        s3pdfReturn);

    var drsSubmissionResponse = new SubmissionResponseObjectV1();
    drsSubmissionResponse.setSubmissionId(SUBMISSION_ID);
    drsSubmissionResponse.setDrsRequestIds(List.of(new RequestId().requestId("drs_request_id")));

    when(v1SubmissionService.createNewSubmission(any(PipApplicationV1.class))).thenReturn(
        drsSubmissionResponse);

    doNothing().when(applicationCoordinatorService).submit(APPLICATION_ID, SUBMISSION_ID);
    when(applicationCoordinatorService.isApplicationInApplicationCoordinator(any()))
        .thenReturn(true);

    when(identityStatusService.getIdentityStatus(APPLICATION_ID)).thenReturn(
        ResultWrapper.<IdentityDto>builder()
            .failure(new IdentityStatusDataNotFoundResultFailure(APPLICATION_ID)).build());

    sut.submitApplication(applicationSubmittedV1);

    var argumentCaptor = ArgumentCaptor.forClass(HealthCaptureApplicationDto.class);
    verify(submittedApplicationPublisher, times(1)).publishMessage(argumentCaptor.capture());
    verify(healthCaptureManagerService, times(1)).getApplicationDtoFromHealthCaptureManager(
        APPLICATION_ID);
    verify(accountManagerService, times(1)).getAccountMgrData(CLAIMANT_ID);
    verify(pdfGeneratorService, times(1)).generateS3Pdf(existingApplication, formSpecification);
    verify(v1SubmissionService, times(1)).createNewSubmission(any(PipApplicationV1.class));
    verify(applicationCoordinatorService, times(1)).submit(APPLICATION_ID, SUBMISSION_ID);

    var submittedApplication = argumentCaptor.getValue();

    assertEquals("SUBMITTED", submittedApplication.getState().getCurrent());
    assertEquals("AB123CD",
            submittedApplication.getRegistrationDetails().getPersonalDetails().getPostcode());
    assertEquals("testing@test.com",
            submittedApplication.getRegistrationDetails().getPersonalDetails().getEmail());
  }

  @Test
  void whenSubmitTriggeredAndNoRegistrationData_thenHcmEndpointAndServicesCalledCorrectly()
      throws IOException {
    ApplicationSubmittedV1 applicationSubmittedV1 = new ApplicationSubmittedV1();
    applicationSubmittedV1.setApplicationId(APPLICATION_ID);

    var existingApplication = JsonUtils.readJsonFromFileAndMap(
            "src/test/resources/entity/healthCaptureApplication_submission_no_registration_data.json",
            HealthCaptureApplicationDto.class);
    when(healthCaptureManagerService.getApplicationDtoFromHealthCaptureManager(
            APPLICATION_ID)).thenReturn(existingApplication);

    var formSpecification = JsonUtils.readJsonFromFileAndMap(
        "src/test/resources/entity/formSpecification_submission.json",
        AuditableFormSpecificationDto.class);
    when(healthCaptureManagerService.getFormSpecificationFromHealthCaptureManager(
        existingApplication.getFormSpecificationId())).thenReturn(formSpecification);

    var accountMgrResponse = JsonUtils.readJsonFromFile(
        "src/test/resources/entity/dto/accountDetailsResponse.json");
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    var accountMgrData = objectMapper.readValue(accountMgrResponse, V7AccountDetails.class);
    accountMgrData.setRef("6634951944e0fded91fae36b");

    when(accountManagerService.getAccountMgrData(CLAIMANT_ID)).thenReturn(
            ResultWrapper.<V7AccountDetails>builder().value(accountMgrData).build());

    when(pdfGeneratorService.generateS3Pdf(existingApplication, formSpecification)).thenReturn(s3pdfReturn);

    var drsSubmissionResponse = new SubmissionResponseObjectV1();
    drsSubmissionResponse.setSubmissionId(SUBMISSION_ID);
    drsSubmissionResponse.setDrsRequestIds(List.of(new RequestId().requestId("drs_request_id")));
    when(v1SubmissionService.createNewSubmission(any(PipApplicationV1.class))).thenReturn(
            drsSubmissionResponse);

    doNothing().when(applicationCoordinatorService).submit(APPLICATION_ID, SUBMISSION_ID);
    when(applicationCoordinatorService.isApplicationInApplicationCoordinator(any()))
        .thenReturn(true);

    when(identityStatusService.getIdentityStatus(APPLICATION_ID)).thenReturn(
            ResultWrapper.<IdentityDto>builder()
                    .failure(new IdentityStatusDataNotFoundResultFailure(APPLICATION_ID)).build());

    sut.submitApplication(applicationSubmittedV1);

    var argumentCaptor = ArgumentCaptor.forClass(HealthCaptureApplicationDto.class);
    verify(submittedApplicationPublisher, times(1)).publishMessage(argumentCaptor.capture());
    verify(healthCaptureManagerService, times(1)).getApplicationDtoFromHealthCaptureManager(
            APPLICATION_ID);
    verify(accountManagerService, times(1)).getAccountMgrData(CLAIMANT_ID);
    verify(pdfGeneratorService, times(1)).generateS3Pdf(existingApplication, formSpecification);
    verify(v1SubmissionService, times(1)).createNewSubmission(any(PipApplicationV1.class));
    verify(applicationCoordinatorService, times(1)).submit(APPLICATION_ID, SUBMISSION_ID);

    var submittedApplication = argumentCaptor.getValue();

    assertNotNull(submittedApplication);
    assertEquals("SUBMITTED", submittedApplication.getState().getCurrent());
    final ApplicationTimeframe applicationTimeframe = submittedApplication.getApplicationTimeframe();
    assertNotNull(applicationTimeframe);
    final LocalDate effectiveFrom = applicationTimeframe.getEffectiveFrom();
    final LocalDate effectiveTo = applicationTimeframe.getEffectiveTo();

    final String msg = "Want 3/5/2024 to 4/8/2024 but got " + effectiveFrom.format(
            DateTimeFormatter.ISO_LOCAL_DATE) + " to " + effectiveTo.format(
            DateTimeFormatter.ISO_LOCAL_DATE);
    assertEquals(3, effectiveFrom.getDayOfMonth(), msg);
    assertEquals(5, effectiveFrom.getMonthValue(), msg);
    assertEquals(2024, effectiveFrom.getYear(), msg);
    assertEquals(4, effectiveTo.getDayOfMonth(), msg);
    assertEquals(8, effectiveTo.getMonthValue(), msg);
    assertEquals(2024, effectiveTo.getYear(), msg);
  }

  @Test
  void applicationExists_submitApplication_callsToAccountManager_returnsSuccessful()
          throws Exception {
    ApplicationSubmittedV1 applicationSubmittedV1 = new ApplicationSubmittedV1();
    applicationSubmittedV1.setApplicationId(APPLICATION_ID);

    var existingApplication = JsonUtils.readJsonFromFileAndMap(
            "src/test/resources/entity/healthCaptureApplication_submission.json",
            HealthCaptureApplicationDto.class);

    var formSpecification = JsonUtils.readJsonFromFileAndMap(
        "src/test/resources/entity/formSpecification_submission.json",
        AuditableFormSpecificationDto.class);

    var accountMgrResponse = JsonUtils.readJsonFromFile(
            "src/test/resources/entity/dto/accountMgrResponse.json");
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    var accountMgrDataEdited = accountMgrResponse.substring(1, accountMgrResponse.length() - 1);
    var accountMgrData = objectMapper.readValue(accountMgrDataEdited, V7AccountDetails.class);

    when(healthCaptureManagerService.getApplicationDtoFromHealthCaptureManager(
        APPLICATION_ID)).thenReturn(existingApplication);
    when(healthCaptureManagerService.getFormSpecificationFromHealthCaptureManager(
        existingApplication.getFormSpecificationId())).thenReturn(formSpecification);
    doNothing().when(applicationCoordinatorService).submit(APPLICATION_ID, SUBMISSION_ID);
    when(identityStatusService.getIdentityStatus(APPLICATION_ID)).thenReturn(
            ResultWrapper.<IdentityDto>builder()
                    .failure(new IdentityStatusDataNotFoundResultFailure(APPLICATION_ID)).build());
    when(accountManagerService.getAccountMgrData(CLAIMANT_ID)).thenReturn(
        ResultWrapper.<V7AccountDetails>builder().value(accountMgrData).build());
    when(pdfGeneratorService.generateS3Pdf(existingApplication, formSpecification)).thenReturn(s3pdfReturn);
    when(v1SubmissionService.createNewSubmission(Mockito.any())).thenReturn(
            submissionResponseResult);

    sut.submitApplication(applicationSubmittedV1);

    var argumentCaptor = ArgumentCaptor.forClass(HealthCaptureApplicationDto.class);
    verify(submittedApplicationPublisher, times(1)).publishMessage(argumentCaptor.capture());

    var submittedApplication = argumentCaptor.getValue();

    assertNotNull(submittedApplication);
    assertEquals("SUBMITTED", submittedApplication.getState().getCurrent());
    assertEquals("AB123CD", submittedApplication.getRegistrationDetails().getPersonalDetails().getPostcode());
    assertEquals("testing@test.com",
            submittedApplication.getRegistrationDetails().getPersonalDetails().getEmail());
  }

  @Test
  void applicationExists_submitApplication_noRegistrationData_returnsSuccessful() throws Exception {
    ApplicationSubmittedV1 applicationSubmittedV1 = new ApplicationSubmittedV1();
    applicationSubmittedV1.setApplicationId(APPLICATION_ID);

    var existingApplication = JsonUtils.readJsonFromFileAndMap(
            "src/test/resources/entity/healthCaptureApplication_submission_no_registration_data.json",
            HealthCaptureApplicationDto.class);

    var formSpecification = JsonUtils.readJsonFromFileAndMap(
        "src/test/resources/entity/formSpecification_submission.json",
        AuditableFormSpecificationDto.class);

    var accountMgrResponse = JsonUtils.readJsonFromFile(
            "src/test/resources/entity/dto/accountDetailsResponse.json");
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    var accountMgrData = objectMapper.readValue(accountMgrResponse, V7AccountDetails.class);
    accountMgrData.setRef("6634951944e0fded91fae36b");

    when(healthCaptureManagerService.getApplicationDtoFromHealthCaptureManager(
        APPLICATION_ID)).thenReturn(existingApplication);
    when(healthCaptureManagerService.getFormSpecificationFromHealthCaptureManager(
        existingApplication.getFormSpecificationId())).thenReturn(formSpecification);
    doNothing().when(applicationCoordinatorService).submit(APPLICATION_ID, SUBMISSION_ID);
    when(identityStatusService.getIdentityStatus(anyString())).thenReturn(
        ResultWrapper.<IdentityDto>builder()
            .failure(new IdentityStatusDataNotFoundResultFailure(APPLICATION_ID)).build());
    when(accountManagerService.getAccountMgrData(anyString())).thenReturn(
        ResultWrapper.<V7AccountDetails>builder().value(accountMgrData).build());
    when(pdfGeneratorService.generateS3Pdf(existingApplication, formSpecification))
        .thenReturn(s3pdfReturn);
    when(v1SubmissionService.createNewSubmission(Mockito.any())).thenReturn(
            submissionResponseResult);

    sut.submitApplication(applicationSubmittedV1);

    var argumentCaptor = ArgumentCaptor.forClass(HealthCaptureApplicationDto.class);
    verify(submittedApplicationPublisher, times(1)).publishMessage(argumentCaptor.capture());

    var submittedApplication = argumentCaptor.getValue();

    assertNotNull(submittedApplication);
    assertEquals("SUBMITTED", submittedApplication.getState().getCurrent());
    final ApplicationTimeframe applicationTimeframe = submittedApplication.getApplicationTimeframe();
    assertNotNull(applicationTimeframe);
    final LocalDate effectiveFrom = applicationTimeframe.getEffectiveFrom();
    final LocalDate effectiveTo = applicationTimeframe.getEffectiveTo();
    final String msg = "Want 3/5/2024 to 3/8/2024 but got " + effectiveFrom.format(
            DateTimeFormatter.ISO_LOCAL_DATE) + " to " + effectiveTo.format(
            DateTimeFormatter.ISO_LOCAL_DATE);
    assertEquals(3, effectiveFrom.getDayOfMonth(), msg);
    assertEquals(5, effectiveFrom.getMonthValue(), msg);
    assertEquals(2024, effectiveFrom.getYear(), msg);
  }

  @Test
  void applicationExists_submitApplication_callsToAccountManager_emailNotFound() throws Exception {
    ApplicationSubmittedV1 applicationSubmittedV1 = new ApplicationSubmittedV1();
    applicationSubmittedV1.setApplicationId(APPLICATION_ID);

    var existingApplication = JsonUtils.readJsonFromFileAndMap(
            "src/test/resources/entity/healthCaptureApplication_submission.json",
            HealthCaptureApplicationDto.class);

    var formSpecification = JsonUtils.readJsonFromFileAndMap(
        "src/test/resources/entity/formSpecification_submission.json",
        AuditableFormSpecificationDto.class);

    when(healthCaptureManagerService.getApplicationDtoFromHealthCaptureManager(
        APPLICATION_ID)).thenReturn(existingApplication);
    when(healthCaptureManagerService.getFormSpecificationFromHealthCaptureManager(
        existingApplication.getFormSpecificationId())).thenReturn(formSpecification);
    doNothing().when(applicationCoordinatorService).submit(APPLICATION_ID, SUBMISSION_ID);
    when(identityStatusService.getIdentityStatus(CLAIMANT_ID)).thenReturn(
            ResultWrapper.<IdentityDto>builder()
                    .failure(new IdentityStatusDataNotFoundResultFailure(APPLICATION_ID)).build());
    when(accountManagerService.getAccountMgrData(CLAIMANT_ID)).thenReturn(
        ResultWrapper.<V7AccountDetails>builder()
            .failure(new AccountMgrDataNotFoundResultFailure(CLAIMANT_ID)).build());
    when(pdfGeneratorService.generateS3Pdf(existingApplication, formSpecification))
        .thenReturn(s3pdfReturn);
    when(v1SubmissionService.createNewSubmission(Mockito.any())).thenReturn(
            submissionResponseResult);

    sut.submitApplication(applicationSubmittedV1);

    var argumentCaptor = ArgumentCaptor.forClass(HealthCaptureApplicationDto.class);
    verify(submittedApplicationPublisher, times(1)).publishMessage(argumentCaptor.capture());

    var submittedApplication = argumentCaptor.getValue();

    assertNotNull(submittedApplication);
    assertEquals("SUBMITTED", submittedApplication.getState().getCurrent());
    assertEquals("AB123CD", submittedApplication.getRegistrationDetails().getPersonalDetails().getPostcode());
  }

  @Test
  void ApplicationDoesNotExist_submitApplication_returnsFailure() {
    ApplicationSubmittedV1 applicationSubmittedV1 = new ApplicationSubmittedV1();
    applicationSubmittedV1.setApplicationId(APPLICATION_ID);

    when(healthCaptureManagerService.getApplicationDtoFromHealthCaptureManager(
            APPLICATION_ID)).thenThrow(new DataRequestException(
            "Empty response from Health Capture Manager when getting application DTO"));

    assertThrows(DataRequestException.class, () -> sut.submitApplication(applicationSubmittedV1));
  }
}
