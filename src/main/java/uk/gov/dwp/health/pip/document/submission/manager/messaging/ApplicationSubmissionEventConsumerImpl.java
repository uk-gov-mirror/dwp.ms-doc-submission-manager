package uk.gov.dwp.health.pip.document.submission.manager.messaging;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Component;
import uk.gov.dwp.health.integration.message.consumers.HealthMessageConsumer;
import uk.gov.dwp.health.pip.document.submission.manager.config.properties.ApplicationSubmissionProperties;
import uk.gov.dwp.health.pip.document.submission.manager.model.ApplicationSubmittedV1;
import uk.gov.dwp.health.pip.document.submission.manager.service.SubmitApplicationService;

@Slf4j
@RequiredArgsConstructor
@Component
public class ApplicationSubmissionEventConsumerImpl
    implements HealthMessageConsumer<ApplicationSubmittedV1> {

  private final SubmitApplicationService submitApplicationService;
  private final ApplicationSubmissionProperties applicationSubmissionProperties;

  @Override
  public String getQueueName() {
    return applicationSubmissionProperties.getSubmissionQueueName();
  }

  @Override
  @SneakyThrows
  public void handleMessage(MessageHeaders messageHeaders, ApplicationSubmittedV1 receivedEvent) {
    try {
      submitApplicationService.submitApplication(receivedEvent);
    } catch (Exception e) {
      log.error(e.getMessage());
      throw e;
    }
  }
}
