package uk.gov.dwp.health.pip.document.submission.manager.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.ObjectMapper;
import uk.gov.dwp.health.pip.application.coordinator.openapi.coordinator.dto.ApplicationDetails;
import uk.gov.dwp.health.pip.application.coordinator.openapi.v1.api.HealthAndDisabilityApiClientV1;
import uk.gov.dwp.health.pip.document.submission.manager.config.MsApplicationCoordinatorConfig;
import uk.gov.dwp.health.pip.document.submission.manager.model.application.ApplicationCoordinatorNotFoundResultFailure;
import uk.gov.dwp.health.pip.document.submission.manager.model.application.ExceptionOccurredResultFailure;
import uk.gov.dwp.health.pip.document.submission.manager.model.application.ResultWrapper;
import uk.gov.dwp.health.pip.document.submission.manager.utils.JsonUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

@ExtendWith(MockitoExtension.class)
class ApplicationCoordinatorServiceTest {
  private static final String getApplicationUrl =
      "http://application-coordinator:8080/v1/application";
  private static final String submitApplicationUrl =
      "http://application-coordinator:8080/v1/applications/{application_id}/healthdisability/submission/{submission_id}";
  private static final String queryString = "?application_id=";

  private RestTemplate restTemplate;

  @Mock private HealthAndDisabilityApiClientV1 coordinatorApiClientV1;
  @Mock private MsApplicationCoordinatorConfig msApplicationCoordinatorConfig;

  private ApplicationCoordinatorService applicationCoordinatorService;

  private MockRestServiceServer mockServer;

  @BeforeEach
  void setUp() {
    restTemplate = new RestTemplate();
    mockServer = MockRestServiceServer.createServer(restTemplate);
  }

  @Nested
  class testSubmitApplication {
    @BeforeEach
    void setUp() {
      applicationCoordinatorService =
          new ApplicationCoordinatorService(coordinatorApiClientV1, null, null, null);
    }

    @Test
    void testSubmitApplicationSuccess() {
      doNothing()
          .when(coordinatorApiClientV1)
          .submitHealthDisabilityData("application-id-1", "submission-id-1");

      applicationCoordinatorService.submitApplication("application-id-1", "submission-id-1");

      verify(coordinatorApiClientV1, times(1))
          .submitHealthDisabilityData("application-id-1", "submission-id-1");
    }

    @Test
    void testSubmitApplicationFailure() {
      doThrow(HttpClientErrorException.class)
          .when(coordinatorApiClientV1)
          .submitHealthDisabilityData("application-id-1", "submission-id-1");

      assertThatThrownBy(
              () ->
                  applicationCoordinatorService.submitApplication(
                      "application-id-1", "submission-id-1"))
          .isInstanceOf(HttpClientErrorException.class);
    }
  }

  @Test
  @DisplayName(
      "test that the correct value is returned when 400 is returned by application coordinator")
  void testForBadRequest() throws URISyntaxException {
    when(msApplicationCoordinatorConfig.getApplicationByApplicationIdUrl())
        .thenReturn(getApplicationUrl);

    applicationCoordinatorService =
        new ApplicationCoordinatorService(
            coordinatorApiClientV1,
            msApplicationCoordinatorConfig,
            new ObjectMapper(),
            restTemplate);

    String applicationId = UUID.randomUUID().toString();

    mockServer
        .expect(
            ExpectedCount.once(),
            requestTo(new URI(getApplicationUrl + queryString + applicationId)))
        .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
        .andRespond(withStatus(HttpStatus.BAD_REQUEST));

    HttpClientErrorException exception =
        assertThrows(
            HttpClientErrorException.class,
            () ->
                applicationCoordinatorService.isApplicationInApplicationCoordinator(applicationId));
    assertEquals(
        "400 Received the following response body when calling application coordinator: ",
        exception.getMessage());
  }

  @Test
  @DisplayName(
      "test that the correct value is returned when 404 is returned by application coordinator")
  void testForNotFound() throws URISyntaxException {
    when(msApplicationCoordinatorConfig.getApplicationByApplicationIdUrl())
        .thenReturn(getApplicationUrl);

    applicationCoordinatorService =
        new ApplicationCoordinatorService(
            coordinatorApiClientV1,
            msApplicationCoordinatorConfig,
            new ObjectMapper(),
            restTemplate);

    String applicationId = UUID.randomUUID().toString();
    mockServer
        .expect(
            ExpectedCount.once(),
            requestTo(new URI(getApplicationUrl + queryString + applicationId)))
        .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
        .andRespond(withStatus(HttpStatus.NOT_FOUND));

    boolean actualResult =
        applicationCoordinatorService.isApplicationInApplicationCoordinator(applicationId);
    assertFalse(actualResult);
  }

  @Test
  @DisplayName(
      "test that the correct value is returned when 200 is returned by application coordinator")
  void testForSuccessfulResponse() throws URISyntaxException {
    when(msApplicationCoordinatorConfig.getApplicationByApplicationIdUrl())
        .thenReturn(getApplicationUrl);

    applicationCoordinatorService =
        new ApplicationCoordinatorService(
            coordinatorApiClientV1,
            msApplicationCoordinatorConfig,
            new ObjectMapper(),
            restTemplate);

    String applicationId = UUID.randomUUID().toString();
    mockServer
        .expect(
            ExpectedCount.once(),
            requestTo(new URI(getApplicationUrl + queryString + applicationId)))
        .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
        .andRespond(withStatus(HttpStatus.OK));

    boolean actualResult =
        applicationCoordinatorService.isApplicationInApplicationCoordinator(applicationId);
    assertTrue(actualResult);
  }

  @Test
  void givenValidId_whenGetApplication_thenReturnSuccess() throws IOException, URISyntaxException {
    when(msApplicationCoordinatorConfig.getApplicationByApplicationIdUrl())
        .thenReturn(getApplicationUrl);

    applicationCoordinatorService =
        new ApplicationCoordinatorService(
            coordinatorApiClientV1,
            msApplicationCoordinatorConfig,
            new ObjectMapper(),
            restTemplate);

    String jsonFromFile =
        JsonUtils.readJsonFromFile(
            "src/test/resources/entity/dto/applicationCoordinatorResponse.json");

    String applicationId = UUID.randomUUID().toString();
    mockServer
        .expect(
            ExpectedCount.once(),
            requestTo(new URI(getApplicationUrl + queryString + applicationId)))
        .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
        .andRespond(withStatus(HttpStatus.OK).body(jsonFromFile));

    ResultWrapper<ApplicationDetails> result =
        applicationCoordinatorService.getApplication(applicationId);
    assertTrue(result.isSuccess());
    assertTrue(result.getFailures().isEmpty());
  }

  @Test
  void givenInvalidId_whenGetApplication_thenReturnNotFoundResultFailure()
      throws URISyntaxException {
    when(msApplicationCoordinatorConfig.getApplicationByApplicationIdUrl())
        .thenReturn(getApplicationUrl);

    applicationCoordinatorService =
        new ApplicationCoordinatorService(
            coordinatorApiClientV1,
            msApplicationCoordinatorConfig,
            new ObjectMapper(),
            restTemplate);

    String applicationId = UUID.randomUUID().toString();
    mockServer
        .expect(
            ExpectedCount.once(),
            requestTo(new URI(getApplicationUrl + queryString + applicationId)))
        .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
        .andRespond(withStatus(HttpStatus.NOT_FOUND));

    ResultWrapper<ApplicationDetails> result =
        applicationCoordinatorService.getApplication(applicationId);
    assertEquals(
        ApplicationCoordinatorNotFoundResultFailure.class, result.getFailures().get(0).getClass());
  }

  @Test
  void givenId_whenServerError_thenReturnResultFailure()
      throws URISyntaxException {
    when(msApplicationCoordinatorConfig.getApplicationByApplicationIdUrl())
        .thenReturn(getApplicationUrl);

    applicationCoordinatorService =
        new ApplicationCoordinatorService(
            coordinatorApiClientV1,
            msApplicationCoordinatorConfig,
            new ObjectMapper(),
            restTemplate);

    String applicationId = UUID.randomUUID().toString();
    mockServer
        .expect(
            ExpectedCount.once(),
            requestTo(new URI(getApplicationUrl + queryString + applicationId)))
        .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
        .andRespond(withStatus(HttpStatus.BAD_REQUEST));

    ResultWrapper<ApplicationDetails> result =
        applicationCoordinatorService.getApplication(applicationId);
    assertEquals(ExceptionOccurredResultFailure.class, result.getFailures().get(0).getClass());
  }
}
