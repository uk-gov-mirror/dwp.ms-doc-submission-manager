package uk.gov.dwp.health.pip.document.submission.manager.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.Charset;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import uk.gov.dwp.health.integration.message.events.EventManager;
import uk.gov.dwp.health.pip.application.coordinator.openapi.coordinator.dto.StateDto.CurrentStateEnum;
import uk.gov.dwp.health.pip.document.submission.manager.config.MsApplicationCoordinatorConfig;
import uk.gov.dwp.health.pip.document.submission.manager.event.StateChangeEvent;

@ExtendWith(MockitoExtension.class)
class ApplicationCoordinatorServiceTest {
  private static String topicName = "state-change-topic";
  private static String routingKey = "state.change";
  private static final String getApplicationUrl = "http://application-coordinator:8080/v1/application";
  private static final String queryString = "?application_id=";
  @Mock
  private static EventManager eventManager;
  @Mock
  private RestTemplate restTemplate;
  @Mock
  private MsApplicationCoordinatorConfig msApplicationCoordinatorConfig;
  @InjectMocks
  private static ApplicationCoordinatorService applicationCoordinatorService;
  @Captor
  private ArgumentCaptor<StateChangeEvent> stateChangeEventCaptor;

  @BeforeEach
  public void setUp() {
    ReflectionTestUtils.setField(applicationCoordinatorService, "topicName", topicName);
    ReflectionTestUtils.setField(applicationCoordinatorService, "routingKey", routingKey);
  }

  @Test
  @DisplayName("test event manager is called with the correct data")
  void testWithCorrectData() {
    String applicationId = "12345678";
    String submissionId = "12345";

    applicationCoordinatorService.submit(applicationId, submissionId);

    verify(eventManager).send(stateChangeEventCaptor.capture());
    assertEquals(routingKey,
            stateChangeEventCaptor.getValue().getRoutingKey());
    assertEquals(topicName, stateChangeEventCaptor.getValue().getTopic());
    assertEquals(applicationId, stateChangeEventCaptor.getValue().getPayload().get("application_id"));
    assertEquals(submissionId, stateChangeEventCaptor.getValue().getPayload().get("submission_id"));
    assertEquals(CurrentStateEnum.SUBMITTED.toString(),
        ((Map<String, Object>)stateChangeEventCaptor.getValue().getPayload().get("state")).get("currentState"));
  }

  @Test
  @DisplayName("test that the correct value is returned when 400 is returned by application coordinator")
  void testForBadRequest() {
    when(msApplicationCoordinatorConfig.getApplicationByApplicationIdUrl()).thenReturn(getApplicationUrl);
    String applicationId = UUID.randomUUID().toString();
    String errorBodyMessage = "ERROR!";
    when(restTemplate.getForEntity(getApplicationUrl + queryString + applicationId, String.class))
            .thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Bad Request", errorBodyMessage.getBytes(), Charset.defaultCharset()));
    HttpClientErrorException exception = assertThrows(HttpClientErrorException.class, () ->
            applicationCoordinatorService.isApplicationInApplicationCoordinator(applicationId));
    assertEquals("400 Received the following response body when calling application coordinator: "
                 + errorBodyMessage, exception.getMessage());
  }

  @Test
  @DisplayName("test that the correct value is returned when 404 is returned by application coordinator")
  void testForNotFound() {
    when(msApplicationCoordinatorConfig.getApplicationByApplicationIdUrl()).thenReturn(getApplicationUrl);
    String applicationId = UUID.randomUUID().toString();
    when(restTemplate.getForEntity(getApplicationUrl + queryString + applicationId, String.class))
            .thenReturn(ResponseEntity.notFound().build());
    boolean actualResult = applicationCoordinatorService.isApplicationInApplicationCoordinator(applicationId);
    assertEquals(false, actualResult);
  }

  @Test
  @DisplayName("test that the correct value is returned when 200 is returned by application coordinator")
  void testForSuccessfulResponse() {
    when(msApplicationCoordinatorConfig.getApplicationByApplicationIdUrl()).thenReturn(getApplicationUrl);
    String applicationId = UUID.randomUUID().toString();
    when(restTemplate.getForEntity(getApplicationUrl + queryString + applicationId, String.class))
            .thenReturn(ResponseEntity.ok().build());
    boolean actualResult = applicationCoordinatorService.isApplicationInApplicationCoordinator(applicationId);
    assertEquals(true, actualResult);
  }
}
