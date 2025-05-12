package uk.gov.dwp.health.pip.document.submission.manager.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.dwp.health.integration.message.events.EventManager;
import uk.gov.dwp.health.pip.document.submission.manager.config.properties.SubmittedApplicationEventProperties;
import uk.gov.dwp.health.pip.document.submission.manager.event.SubmittedApplicationEvent;
import uk.gov.dwp.health.pip.document.submission.manager.exception.MessagingEventException;
import uk.gov.dwp.health.pip.document.submission.manager.model.application.HealthCaptureApplicationDto;
import uk.gov.dwp.health.pip.document.submission.manager.utils.JsonUtils;

import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubmittedApplicationPublisherTests {
  @Mock
  private EventManager eventManager;
  @Mock
  private SubmittedApplicationEventProperties publisherProperties;

  private final ObjectMapper objectMapper = new ObjectMapper();

  private SubmittedApplicationPublisher submittedApplicationPublisher;

  @BeforeEach
  void beforeEach() {
    submittedApplicationPublisher = new SubmittedApplicationPublisher(eventManager, publisherProperties);
  }

  @Test
  void validParameters_willSuccessfullySendNotification() throws IOException {
    when(publisherProperties.getTopicName()).thenReturn("test-topic");
    when(publisherProperties.getRoutingKey()).thenReturn("test-routing-key");

    var submittedHealthCaptureApplication = JsonUtils.readJsonFromFileAndMap(
            "src/test/resources/entity/healthCaptureApplication_submission.json",
            HealthCaptureApplicationDto.class);

    submittedApplicationPublisher.publishMessage(
            submittedHealthCaptureApplication);

    var argumentCaptor = ArgumentCaptor.forClass(SubmittedApplicationEvent.class);
    verify(eventManager, times(1)).send(argumentCaptor.capture());

    var submittedApplicationEvent = argumentCaptor.getValue();
    Map<String, Object> payload = submittedApplicationEvent.getPayload();

    objectMapper.registerModule(new JavaTimeModule());

    //cast payload back to HealthCaptureApplicationDto to ensure data has persisted.
    HealthCaptureApplicationDto healthCaptureApplicationPayload = objectMapper.convertValue(payload, HealthCaptureApplicationDto.class);

    assertThat(healthCaptureApplicationPayload).usingRecursiveComparison().isEqualTo(submittedHealthCaptureApplication);

    assertThat(submittedApplicationEvent.getTopic()).isEqualTo("test-topic");
    assertThat(submittedApplicationEvent.getRoutingKey()).isEqualTo("test-routing-key");
  }

  @Test
  void invalidParameters_WillThrowMessagingEventException() {
    doThrow(NullPointerException.class).when(eventManager).send(any());

    assertThatThrownBy(() -> submittedApplicationPublisher.publishMessage(null))
            .isInstanceOf(MessagingEventException.class)
            .hasMessage(null);
  }
}