package uk.gov.dwp.health.pip.document.submission.manager.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.dwp.health.integration.message.events.EventManager;
import uk.gov.dwp.health.pip.document.submission.manager.exception.MessagingEventException;
import uk.gov.dwp.health.pip.document.submission.manager.generated.event.AdditionalSupportApplicationAutoSubmittedEventV1Payload;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdditionalSupportSubmissionProducer {

  private final EventManager eventManager;
  private final EventOutboundConfigurationProperties properties;

  public void sendEvent(String applicationId, String submissionId) {
    log.info(
        "About to send additional support submission event for applicationId: {}", applicationId);

    AdditionalSupportApplicationAutoSubmittedEventV1Payload payload =
        new AdditionalSupportApplicationAutoSubmittedEventV1Payload()
            .withApplicationId(applicationId)
            .withSubmissionId(submissionId);

    try {
      AdditionalSupportApplicationAutoSubmittedEvent event =
          new AdditionalSupportApplicationAutoSubmittedEvent(
              properties.getApplicationCoordinatorAdditionalSupportSubmissionQueueName(), payload);
      eventManager.sendToQueue(event);
    } catch (Exception ex) {
      log.error(
          "Error sending additional support submission event for applicationId: {}, {}",
          applicationId,
          ex.getMessage());
      throw new MessagingEventException(ex.getMessage());
    }

    log.info("Sent additional support submission event for applicationId: {}", applicationId);
  }
}
