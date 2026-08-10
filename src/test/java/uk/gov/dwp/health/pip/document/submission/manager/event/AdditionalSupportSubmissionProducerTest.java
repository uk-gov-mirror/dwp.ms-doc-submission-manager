package uk.gov.dwp.health.pip.document.submission.manager.event;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.dwp.health.integration.message.events.EventManager;
import uk.gov.dwp.health.pip.document.submission.manager.exception.MessagingEventException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdditionalSupportSubmissionProducerTest {

  @Mock private EventManager eventManager;
  @Mock private EventOutboundConfigurationProperties properties;
  @InjectMocks private AdditionalSupportSubmissionProducer additionalSupportSubmissionProducer;

  @Test
  void testSendEvent() {

    when(properties.getApplicationCoordinatorAdditionalSupportSubmissionQueueName())
        .thenReturn("queue-name");

    additionalSupportSubmissionProducer.sendEvent("application-id-1", "submission-id-1");

    ArgumentCaptor<AdditionalSupportApplicationAutoSubmittedEvent> argumentCaptor =
        ArgumentCaptor.forClass(AdditionalSupportApplicationAutoSubmittedEvent.class);
    verify(eventManager, times(1)).sendToQueue(argumentCaptor.capture());
    AdditionalSupportApplicationAutoSubmittedEvent event = argumentCaptor.getValue();
    String applicationId = event.getPayload().get("application_id").toString();
    String submissionId = event.getPayload().get("submission_id").toString();
    assertThat(event.getOutboundQueue()).isEqualTo("queue-name");
    assertThat(applicationId).isEqualTo("application-id-1");
    assertThat(submissionId).isEqualTo("submission-id-1");
  }

  @Test
  void testSendEventWhenExceptionIsThrown() {
    doThrow(NullPointerException.class)
        .when(eventManager)
        .sendToQueue(any(AdditionalSupportApplicationAutoSubmittedEvent.class));

    assertThatThrownBy(
            () ->
                additionalSupportSubmissionProducer.sendEvent(
                    "application-id-1", "submission-id-1"))
        .isInstanceOf(MessagingEventException.class);
  }
}
