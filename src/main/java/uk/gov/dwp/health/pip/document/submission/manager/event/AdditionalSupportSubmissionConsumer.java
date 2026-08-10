package uk.gov.dwp.health.pip.document.submission.manager.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Component;
import uk.gov.dwp.health.integration.message.consumers.HealthMessageConsumer;
import uk.gov.dwp.health.pip.application.coordinator.generated.event.AdditionalSupportApplicationAutoSubmittedEventV1Payload;
import uk.gov.dwp.health.pip.document.submission.manager.service.AdditionalSupportSubmissionService;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(
    prefix = "event_inbound_additional_support_submission_queue",
    name = "enabled",
    havingValue = "true")
public class AdditionalSupportSubmissionConsumer
    implements HealthMessageConsumer<AdditionalSupportApplicationAutoSubmittedEventV1Payload> {

  private final AdditionalSupportSubmissionService additionalSupportSubmissionService;
  private final EventInboundConfigurationProperties properties;

  @Override
  public String getQueueName() {
    return properties.getAdditionalSupportSubmissionQueueName();
  }

  @Override
  public void handleMessage(
      MessageHeaders messageHeaders,
      AdditionalSupportApplicationAutoSubmittedEventV1Payload payload) {
    String applicationId = payload.getApplicationId();
    String claimantId = payload.getClaimantId();

    log.info("Handling additional support submission message for applicationId: {}", applicationId);

    additionalSupportSubmissionService.submitAdditionalSupportApplication(
        applicationId, claimantId);

    log.info("Handled additional support submission message for applicationId: {}", applicationId);
  }
}
