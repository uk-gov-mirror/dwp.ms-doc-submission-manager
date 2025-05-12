package uk.gov.dwp.health.pip.document.submission.manager.messaging;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.dwp.health.integration.message.events.EventManager;
import uk.gov.dwp.health.pip.document.submission.manager.config.properties.OutboundWorkflowEventProperties;
import uk.gov.dwp.health.pip.document.submission.manager.event.WorkflowEvent;
import uk.gov.dwp.health.pip.document.submission.manager.exception.MessagingEventException;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkflowMessagePublisherTests {
  @Mock
  private EventManager eventManager;
  @Mock
  private OutboundWorkflowEventProperties properties;

  private WorkflowMessagePublisher workflowMessagePublisher;

  @BeforeEach
  void beforeEach() {
    workflowMessagePublisher = new WorkflowMessagePublisher(eventManager, properties);
  }

  @Test
  void validParameters_willSuccessfullySendNotification() throws ParseException {
    when(properties.getTopicExchange()).thenReturn("test-topic");
    when(properties.getRoutingKey()).thenReturn("test-routing-key");

    var simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
    workflowMessagePublisher.publishMessage(
        "application-id-1", "Azzzam Azzzle", "test-nino", simpleDateFormat.parse("2020-11-11"));

    var argumentCaptor = ArgumentCaptor.forClass(WorkflowEvent.class);
    verify(eventManager, times(1)).send(argumentCaptor.capture());

    var workflowEvent = argumentCaptor.getValue();
    Map<String, Object> payload = workflowEvent.getPayload();

    assertThat(payload)
        .containsEntry("applicationId", "application-id-1")
        .containsEntry("name", "Azzzam Azzzle")
        .containsEntry("nino", "test-nino")
        .containsEntry("submissionDate", simpleDateFormat.parse("2020-11-11"));
    assertThat(workflowEvent.getTopic()).isEqualTo("test-topic");
    assertThat(workflowEvent.getRoutingKey()).isEqualTo("test-routing-key");
  }

  @Test
  void invalidParameters_WillThrowMessagingEventException() {
    doThrow(NullPointerException.class).when(eventManager).send(any());

    assertThatThrownBy(() -> workflowMessagePublisher.publishMessage(null, null, null, null))
        .isInstanceOf(MessagingEventException.class)
        .hasMessage(null);
  }
}