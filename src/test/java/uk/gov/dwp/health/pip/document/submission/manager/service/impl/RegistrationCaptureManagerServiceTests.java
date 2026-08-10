package uk.gov.dwp.health.pip.document.submission.manager.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.ObjectMapper;
import uk.gov.dwp.health.pip.document.submission.manager.config.MsRegistrationCaptureManagerConfig;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.registration.v4.dto.RegistrationDto;
import uk.gov.dwp.health.pip.document.submission.manager.utils.JsonUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

@ExtendWith(MockitoExtension.class)
class RegistrationCaptureManagerServiceTests {

  private RestTemplate restTemplate;

  private RegistrationCaptureManagerService registrationCaptureManagerService;

  private MockRestServiceServer mockServer;

  @Mock private MsRegistrationCaptureManagerConfig msRegistrationCaptureManagerConfig;

  @BeforeEach
  void setUp() {
    restTemplate = new RestTemplate();
    mockServer = MockRestServiceServer.createServer(restTemplate);
  }

  @Test
  void applicationExistsInRCM_getRegistrationData_returnsRegistrationData()
      throws IOException, URISyntaxException {

    var response =
        JsonUtils.readJsonFromFileAndMap(
            "src/test/resources/entity/dto/registrationData.json", RegistrationDto.class);

    when(msRegistrationCaptureManagerConfig.getRegistrationDataUri())
        .thenReturn("http://www.teststring.com/v4/application/{applicationId}/registration");

    registrationCaptureManagerService = new RegistrationCaptureManagerService(msRegistrationCaptureManagerConfig, restTemplate, new ObjectMapper());

    mockServer
        .expect(
            ExpectedCount.once(),
            requestTo(new URI("http://www.teststring.com/v4/application/123456789/registration")))
        .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
        .andRespond(withStatus(HttpStatus.OK).body(JsonUtils.mapToJson(response)));

    var result = registrationCaptureManagerService.getRegistrationData("123456789");
    assertEquals(0, result.getFailures().size());
    assertEquals(
        "1SD6YL153OmQWxuzdoskGVLCToeRamaAzme",
        result.getValue().getPersonalDetails().getFirstName());
  }

  @Test
  void applicationDoesNotExistInRCM_getRegistrationData_returnsNotFound()
      throws URISyntaxException {

    when(msRegistrationCaptureManagerConfig.getRegistrationDataUri())
        .thenReturn("http://www.teststring.com/v4/application/{applicationId}/registration");

    registrationCaptureManagerService = new RegistrationCaptureManagerService(msRegistrationCaptureManagerConfig, restTemplate, new ObjectMapper());

    mockServer
        .expect(
            ExpectedCount.once(),
            requestTo(new URI("http://www.teststring.com/v4/application/123/registration")))
        .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
        .andRespond(withStatus(HttpStatus.NOT_FOUND));

    var result = registrationCaptureManagerService.getRegistrationData("123");
    assertEquals(1, result.getFailures().size());
    assertEquals(
        "Data for application with ID: 123 not found in Registration Capture Manager.",
        result.getFailures().get(0).getFailureReason());
  }

  @Test
  void applicationDoesNotExistInRCM_getRegistrationData_returnsFailure()
      throws URISyntaxException {

    when(msRegistrationCaptureManagerConfig.getRegistrationDataUri())
        .thenReturn("http://www.teststring.com/v4/application/{applicationId}/registration");

    registrationCaptureManagerService = new RegistrationCaptureManagerService(msRegistrationCaptureManagerConfig, restTemplate, new ObjectMapper());

    mockServer
        .expect(
            ExpectedCount.once(),
            requestTo(new URI("http://www.teststring.com/v4/application/123/registration")))
        .andExpect(MockRestRequestMatchers.method(HttpMethod.GET))
        .andRespond(withStatus(HttpStatus.BAD_REQUEST));

    var result = registrationCaptureManagerService.getRegistrationData("123");
    assertEquals(1, result.getFailures().size());
  }
}
