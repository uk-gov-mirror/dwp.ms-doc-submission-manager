package uk.gov.dwp.health.pip.document.submission.manager.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.HttpClientErrorException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import uk.gov.dwp.health.pip.application.coordinator.openapi.coordinator.dto.ApplicationDetails;
import uk.gov.dwp.health.pip.document.submission.manager.config.properties.ApplicationTimeframeProperties;
import uk.gov.dwp.health.pip.document.submission.manager.config.restclient.model.V7AccountDetails;
import uk.gov.dwp.health.pip.document.submission.manager.exception.DataRequestException;
import uk.gov.dwp.health.pip.document.submission.manager.exception.V3SubmissionServiceException;
import uk.gov.dwp.health.pip.document.submission.manager.messaging.WorkflowMessagePublisher;
import uk.gov.dwp.health.pip.document.submission.manager.model.application.AccountMgrDataNotFoundResultFailure;
import uk.gov.dwp.health.pip.document.submission.manager.model.application.RegistrationCaptureMgrNotFoundResultFailure;
import uk.gov.dwp.health.pip.document.submission.manager.model.application.ResultWrapper;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.healthinformation.v2.dto.ApplicationTimeframeDto;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.healthinformation.v2.dto.FileUploadDto;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.healthinformation.v2.dto.HealthCaptureApplicationDtoV2;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.healthinformation.v2.dto.PersonalDetailsDto;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.healthinformation.v2.dto.RegistrationDetailsDto;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.healthinformation.v2.dto.StateDto;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.model.PipApplicationV1;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.model.RequestId;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.model.SubmissionResponseObjectV1;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.pdfgenerator.v4.dto.S3PdfReturn;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.registration.v4.dto.RegistrationDto;
import uk.gov.dwp.health.pip.document.submission.manager.service.AccountManagerService;
import uk.gov.dwp.health.pip.document.submission.manager.service.ApplicationCoordinatorService;
import uk.gov.dwp.health.pip.document.submission.manager.service.HealthCaptureManagerService;
import uk.gov.dwp.health.pip.document.submission.manager.service.PdfGeneratorService;
import uk.gov.dwp.health.pip.document.submission.manager.utils.JsonUtils;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.dwp.health.pip.document.submission.manager.utils.DateTimeUtils.instantToString;

@ExtendWith(MockitoExtension.class)
@Slf4j
class V3SubmissionServiceImplTest {

  private static final String APPLICATION_ID = "123456789";
  private static final String CLAIMANT_ID = "123456789";
  private static final String SUBMISSION_ID = "123456789123456";
  private static final String USER_ID = "66f51d95d15f2c7ce0b9dd17";

  @Mock private ApplicationCoordinatorService applicationCoordinatorService;
  @Mock private GetClaimantEmailService getClaimantEmailService;
  @Mock private WorkflowMessagePublisher workflowMessagePublisher;
  @Mock private AccountManagerService accountManagerService;
  @Mock private RegistrationCaptureManagerService registrationCaptureManagerService;
  @Mock private PdfGeneratorService pdfGeneratorService;
  @Mock private V1SubmissionServiceImpl v1SubmissionService;
  @Mock private HealthCaptureManagerService healthCaptureManagerService;
  @Mock private EmailNotificationServiceImpl emailNotificationService;
  @Mock private EventPublisherImpl eventPublisher;
  @Mock private DataServiceImpl dataService;
  @Mock Clock clock;

  private S3PdfReturn s3pdfReturn;
  private SubmissionResponseObjectV1 submissionResponseResult;

  private V3SubmissionServiceImpl v3SubmissionServiceImpl;
  private Instant now;

  @BeforeEach
  void setUp() {
    ApplicationTimeframeProperties applicationTimeframeProperties =
        new ApplicationTimeframeProperties();
    applicationTimeframeProperties.setActiveDuration(93);
    v3SubmissionServiceImpl =
        new V3SubmissionServiceImpl(
            applicationCoordinatorService,
            workflowMessagePublisher,
            accountManagerService,
            registrationCaptureManagerService,
            pdfGeneratorService,
            applicationTimeframeProperties,
            healthCaptureManagerService,
            emailNotificationService,
            getClaimantEmailService,
            v1SubmissionService,
            clock);
    s3pdfReturn = new S3PdfReturn();
    s3pdfReturn.setBucket("PIP_BUCKET");
    s3pdfReturn.setFileSizeKb(123456);
    s3pdfReturn.setS3Ref("S3_REF");
    submissionResponseResult = new SubmissionResponseObjectV1();
    submissionResponseResult.setSubmissionId("123456789123456");
  }

  @Test
  void whenSubmitTriggered_thenHcmEndpointAndServicesCalledCorrectly() throws IOException {
    HealthCaptureApplicationDtoV2 existingApplication =
        JsonUtils.readJsonFromFileAndMap(
            "src/test/resources/entity/dto/getHealthDataResponseBody_submission.json",
            HealthCaptureApplicationDtoV2.class);
    when(healthCaptureManagerService.getApplicationDtoV2FromHealthCaptureManager(APPLICATION_ID))
        .thenReturn(existingApplication);
    when(registrationCaptureManagerService.getRegistrationData(APPLICATION_ID))
        .thenReturn(
            ResultWrapper.<RegistrationDto>builder()
                .failure(new RegistrationCaptureMgrNotFoundResultFailure(APPLICATION_ID))
                .build());
    now = Instant.now();
    when(clock.instant()).thenReturn(now);

    var accountMgrResponse =
        JsonUtils.readJsonFromFile("src/test/resources/entity/dto/accountMgrResponse.json");
    ObjectMapper objectMapper = new ObjectMapper();
    var accountMgrDataEdited = accountMgrResponse.substring(1, accountMgrResponse.length() - 1);
    var accountMgrData = objectMapper.readValue(accountMgrDataEdited, V7AccountDetails.class);

    when(accountManagerService.getAccountMgrData(CLAIMANT_ID))
        .thenReturn(ResultWrapper.<V7AccountDetails>builder().value(accountMgrData).build());
    when(getClaimantEmailService.getClaimantEmail(accountMgrData, existingApplication))
        .thenReturn("testing@test.com");
    when(pdfGeneratorService.generateS3Pdf(existingApplication)).thenReturn(s3pdfReturn);

    var drsSubmissionResponse = new SubmissionResponseObjectV1();
    drsSubmissionResponse.setSubmissionId(SUBMISSION_ID);
    drsSubmissionResponse.setDrsRequestIds(List.of(new RequestId().requestId("drs_request_id")));

    when(v1SubmissionService.createNewSubmission(any(PipApplicationV1.class)))
        .thenReturn(drsSubmissionResponse);
    doNothing()
        .when(applicationCoordinatorService)
        .submitApplication(APPLICATION_ID, SUBMISSION_ID);
    when(applicationCoordinatorService.isApplicationInApplicationCoordinator(any()))
        .thenReturn(true);

    HealthCaptureApplicationDtoV2 submittedHealthCaptureApplicationDto =
        v3SubmissionServiceImpl.createNewSubmission(APPLICATION_ID, USER_ID);

    verify(healthCaptureManagerService, times(1))
        .getApplicationDtoV2FromHealthCaptureManager(APPLICATION_ID);
    verify(accountManagerService, times(1)).getAccountMgrData(CLAIMANT_ID);
    verify(pdfGeneratorService, times(1)).generateS3Pdf(existingApplication);
    verify(v1SubmissionService, times(1)).createNewSubmission(any(PipApplicationV1.class));
    verify(applicationCoordinatorService, times(1))
        .submitApplication(APPLICATION_ID, SUBMISSION_ID);

    assertThat(submittedHealthCaptureApplicationDto.getState().getCurrent())
        .isEqualTo("HEALTH_AND_DISABILITY");
    assertThat(
        submittedHealthCaptureApplicationDto
            .getRegistrationDetails()
            .getPersonalDetails()
            .getPostcode())
        .isEqualTo("AB123CD");
    assertThat(
        submittedHealthCaptureApplicationDto
            .getRegistrationDetails()
            .getPersonalDetails()
            .getEmail())
        .isEqualTo("testing@test.com");
    assertThat(submittedHealthCaptureApplicationDto.getSubmissionDate())
        .isEqualTo(instantToString(now));
    assertEquals(
        FileUploadDto.DrsDocTypeEnum.PIP2_FORM,
        submittedHealthCaptureApplicationDto.getFiles().get(0).getDrsDocType());
  }

  @Test
  void whenSubmitTriggeredNoState_thenHcmEndpointAndServicesCalledCorrectly() throws IOException {
    HealthCaptureApplicationDtoV2 existingApplication =
        JsonUtils.readJsonFromFileAndMap(
            "src/test/resources/entity/dto/getHealthDataResponseBodyNoState_submission.json",
            HealthCaptureApplicationDtoV2.class);
    when(healthCaptureManagerService.getApplicationDtoV2FromHealthCaptureManager(APPLICATION_ID))
        .thenReturn(existingApplication);
    when(registrationCaptureManagerService.getRegistrationData(APPLICATION_ID))
        .thenReturn(
            ResultWrapper.<RegistrationDto>builder()
                .failure(new RegistrationCaptureMgrNotFoundResultFailure(APPLICATION_ID))
                .build());
    now = Instant.now();
    when(clock.instant()).thenReturn(now);

    String accountMgrResponse =
        JsonUtils.readJsonFromFile("src/test/resources/entity/dto/accountMgrResponse.json");
    ObjectMapper objectMapper = new ObjectMapper();
    var accountMgrDataEdited = accountMgrResponse.substring(1, accountMgrResponse.length() - 1);
    var accountMgrData = objectMapper.readValue(accountMgrDataEdited, V7AccountDetails.class);

    when(accountManagerService.getAccountMgrData(CLAIMANT_ID))
        .thenReturn(ResultWrapper.<V7AccountDetails>builder().value(accountMgrData).build());
    when(pdfGeneratorService.generateS3Pdf(existingApplication)).thenReturn(s3pdfReturn);

    var drsSubmissionResponse = new SubmissionResponseObjectV1();
    drsSubmissionResponse.setSubmissionId(SUBMISSION_ID);
    drsSubmissionResponse.setDrsRequestIds(List.of(new RequestId().requestId("drs_request_id")));

    when(v1SubmissionService.createNewSubmission(any(PipApplicationV1.class)))
        .thenReturn(drsSubmissionResponse);
    doNothing()
        .when(applicationCoordinatorService)
        .submitApplication(APPLICATION_ID, SUBMISSION_ID);
    when(applicationCoordinatorService.isApplicationInApplicationCoordinator(any()))
        .thenReturn(true);
    when(getClaimantEmailService.getClaimantEmail(accountMgrData, existingApplication))
        .thenReturn("testing@test.com");

    HealthCaptureApplicationDtoV2 submittedHealthCaptureApplicationDto =
        v3SubmissionServiceImpl.createNewSubmission(APPLICATION_ID, USER_ID);

    verify(healthCaptureManagerService, times(1))
        .getApplicationDtoV2FromHealthCaptureManager(APPLICATION_ID);
    verify(accountManagerService, times(1)).getAccountMgrData(CLAIMANT_ID);
    verify(pdfGeneratorService, times(1)).generateS3Pdf(existingApplication);
    verify(v1SubmissionService, times(1)).createNewSubmission(any(PipApplicationV1.class));
    verify(applicationCoordinatorService, times(1))
        .submitApplication(APPLICATION_ID, SUBMISSION_ID);

    assertThat(submittedHealthCaptureApplicationDto.getState().getCurrent()).isNull();
    assertThat(submittedHealthCaptureApplicationDto.getState().getHistory()).isEmpty();
    assertThat(
        submittedHealthCaptureApplicationDto
            .getRegistrationDetails()
            .getPersonalDetails()
            .getPostcode())
        .isEqualTo("AB123CD");
    assertThat(
        submittedHealthCaptureApplicationDto
            .getRegistrationDetails()
            .getPersonalDetails()
            .getEmail())
        .isEqualTo("testing@test.com");
    assertThat(submittedHealthCaptureApplicationDto.getSubmissionDate())
        .isEqualTo(instantToString(now));
    assertEquals(
        FileUploadDto.DrsDocTypeEnum.PIP2_FORM,
        submittedHealthCaptureApplicationDto.getFiles().get(0).getDrsDocType());
  }

  @Test
  void whenSubmitTriggeredAndNoRegistrationData_thenHcmEndpointAndServicesCalledCorrectly()
      throws IOException {
    HealthCaptureApplicationDtoV2 existingApplication =
        JsonUtils.readJsonFromFileAndMap(
            "src/test/resources/entity/dto/getV2HealthDataResponseBody_no_registration_data.json",
            HealthCaptureApplicationDtoV2.class);
    when(healthCaptureManagerService.getApplicationDtoV2FromHealthCaptureManager(APPLICATION_ID))
        .thenReturn(existingApplication);
    when(registrationCaptureManagerService.getRegistrationData(APPLICATION_ID))
        .thenReturn(
            ResultWrapper.<RegistrationDto>builder()
                .failure(new RegistrationCaptureMgrNotFoundResultFailure(APPLICATION_ID))
                .build());
    now = Instant.now();
    when(clock.instant()).thenReturn(now);

    var accountMgrResponse =
        JsonUtils.readJsonFromFile("src/test/resources/entity/dto/accountDetailsResponse.json");
    ObjectMapper objectMapper = new ObjectMapper();
    var accountMgrData = objectMapper.readValue(accountMgrResponse, V7AccountDetails.class);
    accountMgrData.setRef("6634951944e0fded91fae36b");

    when(accountManagerService.getAccountMgrData(CLAIMANT_ID))
        .thenReturn(ResultWrapper.<V7AccountDetails>builder().value(accountMgrData).build());

    when(pdfGeneratorService.generateS3Pdf(existingApplication)).thenReturn(s3pdfReturn);

    var drsSubmissionResponse = new SubmissionResponseObjectV1();
    drsSubmissionResponse.setSubmissionId(SUBMISSION_ID);
    drsSubmissionResponse.setDrsRequestIds(List.of(new RequestId().requestId("drs_request_id")));
    when(v1SubmissionService.createNewSubmission(any(PipApplicationV1.class)))
        .thenReturn(drsSubmissionResponse);

    doNothing()
        .when(applicationCoordinatorService)
        .submitApplication(APPLICATION_ID, SUBMISSION_ID);
    when(applicationCoordinatorService.isApplicationInApplicationCoordinator(any()))
        .thenReturn(true);

    when(getClaimantEmailService.getClaimantEmail(accountMgrData, existingApplication))
        .thenReturn("testing@test.com");

    HealthCaptureApplicationDtoV2 submittedHealthCaptureApplicationDto =
        v3SubmissionServiceImpl.createNewSubmission(APPLICATION_ID, USER_ID);

    verify(healthCaptureManagerService, times(1))
        .getApplicationDtoV2FromHealthCaptureManager(APPLICATION_ID);
    verify(accountManagerService, times(1)).getAccountMgrData(CLAIMANT_ID);
    verify(pdfGeneratorService, times(1)).generateS3Pdf(existingApplication);
    verify(v1SubmissionService, times(1)).createNewSubmission(any(PipApplicationV1.class));
    verify(applicationCoordinatorService, times(1))
        .submitApplication(APPLICATION_ID, SUBMISSION_ID);

    assertNotNull(submittedHealthCaptureApplicationDto);
    assertThat(submittedHealthCaptureApplicationDto.getState().getCurrent())
        .isEqualTo("HEALTH_AND_DISABILITY");
    final ApplicationTimeframeDto applicationTimeframe =
        submittedHealthCaptureApplicationDto.getApplicationTimeframe();
    assertNotNull(applicationTimeframe);
    assertThat(applicationTimeframe.getEffectiveFrom()).isEqualTo("2024-05-03");
    assertThat(applicationTimeframe.getEffectiveTo()).isEqualTo("2024-08-04");
    assertEquals(
        FileUploadDto.DrsDocTypeEnum.PIP2_FORM,
        submittedHealthCaptureApplicationDto.getFiles().get(0).getDrsDocType());
  }

  @Test
  void applicationExists_submitApplication_callsToAccountManager_returnsSuccessful()
      throws Exception {
    HealthCaptureApplicationDtoV2 existingApplication =
        JsonUtils.readJsonFromFileAndMap(
            "src/test/resources/entity/dto/getHealthDataResponseBody_submission.json",
            HealthCaptureApplicationDtoV2.class);

    when(registrationCaptureManagerService.getRegistrationData(APPLICATION_ID))
        .thenReturn(
            ResultWrapper.<RegistrationDto>builder()
                .failure(new RegistrationCaptureMgrNotFoundResultFailure(APPLICATION_ID))
                .build());
    now = Instant.now();
    when(clock.instant()).thenReturn(now);

    var accountMgrResponse =
        JsonUtils.readJsonFromFile("src/test/resources/entity/dto/accountMgrResponse.json");
    ObjectMapper objectMapper = new ObjectMapper();
    var accountMgrDataEdited = accountMgrResponse.substring(1, accountMgrResponse.length() - 1);
    var accountMgrData = objectMapper.readValue(accountMgrDataEdited, V7AccountDetails.class);

    when(healthCaptureManagerService.getApplicationDtoV2FromHealthCaptureManager(APPLICATION_ID))
        .thenReturn(existingApplication);
    when(getClaimantEmailService.getClaimantEmail(accountMgrData, existingApplication))
        .thenReturn("testing@test.com");
    when(accountManagerService.getAccountMgrData(CLAIMANT_ID))
        .thenReturn(ResultWrapper.<V7AccountDetails>builder().value(accountMgrData).build());
    when(pdfGeneratorService.generateS3Pdf(existingApplication)).thenReturn(s3pdfReturn);
    when(v1SubmissionService.createNewSubmission(Mockito.any()))
        .thenReturn(submissionResponseResult);

    HealthCaptureApplicationDtoV2 submittedHealthCaptureApplicationDto =
        v3SubmissionServiceImpl.createNewSubmission(APPLICATION_ID, USER_ID);

    assertNotNull(submittedHealthCaptureApplicationDto);
    assertThat(submittedHealthCaptureApplicationDto.getState().getCurrent())
        .isEqualTo("HEALTH_AND_DISABILITY");
    assertEquals(
        "AB123CD",
        submittedHealthCaptureApplicationDto
            .getRegistrationDetails()
            .getPersonalDetails()
            .getPostcode());
    assertEquals(
        "testing@test.com",
        submittedHealthCaptureApplicationDto
            .getRegistrationDetails()
            .getPersonalDetails()
            .getEmail());
  }

  @Test
  void applicationExists_submitApplication_noRegistrationData_returnsSuccessful() throws Exception {
    HealthCaptureApplicationDtoV2 existingApplication =
        JsonUtils.readJsonFromFileAndMap(
            "src/test/resources/entity/dto/getV2HealthDataResponseBody_no_registration_data.json",
            HealthCaptureApplicationDtoV2.class);

    when(registrationCaptureManagerService.getRegistrationData(APPLICATION_ID))
        .thenReturn(
            ResultWrapper.<RegistrationDto>builder()
                .failure(new RegistrationCaptureMgrNotFoundResultFailure(APPLICATION_ID))
                .build());
    now = Instant.now();
    when(clock.instant()).thenReturn(now);

    var accountMgrResponse =
        JsonUtils.readJsonFromFile("src/test/resources/entity/dto/accountDetailsResponse.json");
    ObjectMapper objectMapper = new ObjectMapper();
    var accountMgrData = objectMapper.readValue(accountMgrResponse, V7AccountDetails.class);
    accountMgrData.setRef("6634951944e0fded91fae36b");

    when(healthCaptureManagerService.getApplicationDtoV2FromHealthCaptureManager(APPLICATION_ID))
        .thenReturn(existingApplication);
    when(getClaimantEmailService.getClaimantEmail(accountMgrData, existingApplication))
        .thenReturn("testing@test.com");
    when(accountManagerService.getAccountMgrData(anyString()))
        .thenReturn(ResultWrapper.<V7AccountDetails>builder().value(accountMgrData).build());
    when(pdfGeneratorService.generateS3Pdf(existingApplication)).thenReturn(s3pdfReturn);
    when(v1SubmissionService.createNewSubmission(Mockito.any()))
        .thenReturn(submissionResponseResult);

    HealthCaptureApplicationDtoV2 submittedHealthCaptureApplicationDto =
        v3SubmissionServiceImpl.createNewSubmission(APPLICATION_ID, USER_ID);

    assertNotNull(submittedHealthCaptureApplicationDto);
    assertThat(submittedHealthCaptureApplicationDto.getState().getCurrent())
        .isEqualTo("HEALTH_AND_DISABILITY");
    final ApplicationTimeframeDto applicationTimeframe =
        submittedHealthCaptureApplicationDto.getApplicationTimeframe();
    assertNotNull(applicationTimeframe);
    assertThat(applicationTimeframe.getEffectiveFrom()).isEqualTo("2024-05-03");
    assertThat(applicationTimeframe.getEffectiveTo()).isEqualTo("2024-08-04");
  }

  @Test
  void applicationExists_submitApplication_callsToAccountManager_emailNotFound() throws Exception {
    HealthCaptureApplicationDtoV2 existingApplication =
        JsonUtils.readJsonFromFileAndMap(
            "src/test/resources/entity/dto/getHealthDataResponseBody_submission.json",
            HealthCaptureApplicationDtoV2.class);

    when(registrationCaptureManagerService.getRegistrationData(APPLICATION_ID))
        .thenReturn(
            ResultWrapper.<RegistrationDto>builder()
                .failure(new RegistrationCaptureMgrNotFoundResultFailure(APPLICATION_ID))
                .build());
    now = Instant.now();
    when(clock.instant()).thenReturn(now);
    when(healthCaptureManagerService.getApplicationDtoV2FromHealthCaptureManager(APPLICATION_ID))
        .thenReturn(existingApplication);
    when(accountManagerService.getAccountMgrData(CLAIMANT_ID))
        .thenReturn(
            ResultWrapper.<V7AccountDetails>builder()
                .failure(new AccountMgrDataNotFoundResultFailure(CLAIMANT_ID))
                .build());
    when(pdfGeneratorService.generateS3Pdf(existingApplication)).thenReturn(s3pdfReturn);
    when(v1SubmissionService.createNewSubmission(Mockito.any()))
        .thenReturn(submissionResponseResult);

    HealthCaptureApplicationDtoV2 submittedHealthCaptureApplicationDto =
        v3SubmissionServiceImpl.createNewSubmission(APPLICATION_ID, USER_ID);

    assertNotNull(submittedHealthCaptureApplicationDto);
    assertThat(submittedHealthCaptureApplicationDto.getState().getCurrent())
        .isEqualTo("HEALTH_AND_DISABILITY");
    assertEquals(
        "AB123CD",
        submittedHealthCaptureApplicationDto
            .getRegistrationDetails()
            .getPersonalDetails()
            .getPostcode());
    assertThat(submittedHealthCaptureApplicationDto.getSubmissionDate())
        .isEqualTo(instantToString(now));
  }

  @Test
  void applicationDoesNotExist_submitApplication_returnsFailure() {
    when(healthCaptureManagerService.getApplicationDtoV2FromHealthCaptureManager(APPLICATION_ID))
        .thenThrow(
            new DataRequestException(
                "Empty response from Health Capture Manager when getting application DTO"));

    assertThrows(
        DataRequestException.class,
        () -> v3SubmissionServiceImpl.createNewSubmission(APPLICATION_ID, USER_ID));
  }

  @Test
  void testDispatchWorkflowEvent() {
    HealthCaptureApplicationDtoV2 healthCaptureApplicationDtoV2 =
        new HealthCaptureApplicationDtoV2()
            .applicationId(APPLICATION_ID)
            .registrationDetails(
                new RegistrationDetailsDto()
                    .personalDetails(
                        new PersonalDetailsDto()
                            .firstName("first-name")
                            .surname("surname")
                            .nationalInsuranceNumber("nino")))
            .submissionDate("2025-07-03T09:04:38.206729Z");

    v3SubmissionServiceImpl.dispatchWorkflowEvent(healthCaptureApplicationDtoV2);

    ArgumentCaptor<String> applicationIdArgumentCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> nameArgumentCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> ninoArgumentCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<Date> submissionDateArgumentCaptor = ArgumentCaptor.forClass(Date.class);

    verify(workflowMessagePublisher, times(1))
        .publishMessage(
            applicationIdArgumentCaptor.capture(),
            nameArgumentCaptor.capture(),
            ninoArgumentCaptor.capture(),
            submissionDateArgumentCaptor.capture());

    assertThat(applicationIdArgumentCaptor.getValue()).isEqualTo(APPLICATION_ID);
    assertThat(nameArgumentCaptor.getValue()).isEqualTo("first-name surname");
    assertThat(ninoArgumentCaptor.getValue()).isEqualTo("nino");
    assertThat(submissionDateArgumentCaptor.getValue().toString()).startsWith("Thu Jul 03");
  }

  @Test
  void when_registration_capture_manager_returns_unparsable_response() {
    HealthCaptureApplicationDtoV2 existingApplication = new HealthCaptureApplicationDtoV2();
    when(healthCaptureManagerService.getApplicationDtoV2FromHealthCaptureManager(APPLICATION_ID))
        .thenReturn(existingApplication);
    doThrow(JacksonException.class)
        .when(registrationCaptureManagerService)
        .getRegistrationData(APPLICATION_ID);

    assertThatThrownBy(() -> v3SubmissionServiceImpl.createNewSubmission(APPLICATION_ID, USER_ID))
        .isInstanceOf(V3SubmissionServiceException.class)
        .hasMessage(
            "Exception occurred while parsing registration capture manager or application "
                + "coordinator JSON object for application id "
                + APPLICATION_ID
                + " with message N/A");
  }

  @Test
  void when_account_manager_returns_unparsable_response() {
    HealthCaptureApplicationDtoV2 existingApplication =
        new HealthCaptureApplicationDtoV2().claimantId(CLAIMANT_ID);
    when(healthCaptureManagerService.getApplicationDtoV2FromHealthCaptureManager(APPLICATION_ID))
        .thenReturn(existingApplication);
    when(registrationCaptureManagerService.getRegistrationData(APPLICATION_ID))
        .thenReturn(
            ResultWrapper.<RegistrationDto>builder()
                .failure(new RegistrationCaptureMgrNotFoundResultFailure(APPLICATION_ID))
                .build());
    doThrow(JacksonException.class)
        .when(accountManagerService)
        .getAccountMgrData(CLAIMANT_ID);

    assertThatThrownBy(() -> v3SubmissionServiceImpl.createNewSubmission(APPLICATION_ID, USER_ID))
        .isInstanceOf(V3SubmissionServiceException.class)
        .hasMessage(
            "Exception occurred while parsing account manager JSON object for application id "
                + APPLICATION_ID
                + " with message N/A");
  }

  @Test
  void when_registration_details_or_application_timeframe_null() {
    HealthCaptureApplicationDtoV2 existingApplication =
        new HealthCaptureApplicationDtoV2().claimantId(CLAIMANT_ID);
    when(healthCaptureManagerService.getApplicationDtoV2FromHealthCaptureManager(APPLICATION_ID))
        .thenReturn(existingApplication);
    when(registrationCaptureManagerService.getRegistrationData(APPLICATION_ID))
        .thenReturn(
            ResultWrapper.<RegistrationDto>builder()
                .failure(new RegistrationCaptureMgrNotFoundResultFailure(APPLICATION_ID))
                .build());
    when(accountManagerService.getAccountMgrData(CLAIMANT_ID))
        .thenReturn(
            ResultWrapper.<V7AccountDetails>builder()
                .failure(new AccountMgrDataNotFoundResultFailure(CLAIMANT_ID))
                .build());

    assertThatThrownBy(() -> v3SubmissionServiceImpl.createNewSubmission(APPLICATION_ID, USER_ID))
        .isInstanceOf(V3SubmissionServiceException.class)
        .hasMessage(
            "Registration details or application timeframe is null for application "
                + APPLICATION_ID);
  }

  @Test
  void when_get_claimant_email_service_returns_unparsable_response()
      throws JacksonException {
    HealthCaptureApplicationDtoV2 existingApplication =
        new HealthCaptureApplicationDtoV2()
            .applicationId(APPLICATION_ID)
            .claimantId(CLAIMANT_ID)
            .registrationDetails(new RegistrationDetailsDto())
            .applicationTimeframe(new ApplicationTimeframeDto());
    when(healthCaptureManagerService.getApplicationDtoV2FromHealthCaptureManager(APPLICATION_ID))
        .thenReturn(existingApplication);
    when(registrationCaptureManagerService.getRegistrationData(APPLICATION_ID))
        .thenReturn(ResultWrapper.<RegistrationDto>builder().build());
    when(applicationCoordinatorService.getApplication(APPLICATION_ID))
        .thenReturn(
            ResultWrapper.<ApplicationDetails>builder()
                .value(
                    new ApplicationDetails()
                        .language(ApplicationDetails.LanguageEnum.EN)
                        .region(ApplicationDetails.RegionEnum.GB))
                .build());
    doThrow(JacksonException.class)
        .when(getClaimantEmailService)
        .getClaimantEmail(null, existingApplication);

    assertThatThrownBy(() -> v3SubmissionServiceImpl.createNewSubmission(APPLICATION_ID, USER_ID))
        .isInstanceOf(V3SubmissionServiceException.class)
        .hasMessage(
            "Exception occurred while parsing identity service JSON object for application id "
                + "123456789 with message N/A");
  }

  @Test
  void when_application_coordinator_submit_unsuccessful() {
    HealthCaptureApplicationDtoV2 existingApplication =
        new HealthCaptureApplicationDtoV2()
            .applicationId(APPLICATION_ID)
            .claimantId(CLAIMANT_ID)
            .registrationDetails(
                new RegistrationDetailsDto()
                    .personalDetails(new PersonalDetailsDto().dateOfBirth("2000-01-01")))
            .applicationTimeframe(new ApplicationTimeframeDto().effectiveFrom("2025-01-01"))
            .state(new StateDto());
    when(healthCaptureManagerService.getApplicationDtoV2FromHealthCaptureManager(APPLICATION_ID))
        .thenReturn(existingApplication);
    when(clock.instant()).thenReturn(Instant.now());
    when(registrationCaptureManagerService.getRegistrationData(APPLICATION_ID))
        .thenReturn(ResultWrapper.<RegistrationDto>builder().build());
    when(applicationCoordinatorService.getApplication(APPLICATION_ID))
        .thenReturn(
            ResultWrapper.<ApplicationDetails>builder()
                .value(
                    new ApplicationDetails()
                        .language(ApplicationDetails.LanguageEnum.EN)
                        .region(ApplicationDetails.RegionEnum.GB))
                .build());
    when(getClaimantEmailService.getClaimantEmail(null, existingApplication))
        .thenReturn("testing@test.com");
    when(pdfGeneratorService.generateS3Pdf(existingApplication)).thenReturn(s3pdfReturn);
    when(v1SubmissionService.createNewSubmission(any(PipApplicationV1.class)))
        .thenReturn(submissionResponseResult);
    when(applicationCoordinatorService.isApplicationInApplicationCoordinator(APPLICATION_ID))
        .thenReturn(true);
    doThrow(HttpClientErrorException.class)
        .when(applicationCoordinatorService)
        .submitApplication(APPLICATION_ID, SUBMISSION_ID);

    assertThatThrownBy(() -> v3SubmissionServiceImpl.createNewSubmission(APPLICATION_ID, USER_ID))
        .isInstanceOf(V3SubmissionServiceException.class)
        .hasMessage(
            "Exception occurred while trying to submit application to application coordinator for application id "
                + APPLICATION_ID);
  }
}
