package uk.gov.dwp.health.pip.document.submission.manager.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.dwp.health.integration.message.events.EventManager;
import uk.gov.dwp.health.pip.document.submission.manager.config.properties.SubmittedApplicationEventProperties;
import uk.gov.dwp.health.pip.document.submission.manager.event.SubmittedApplicationEvent;
import uk.gov.dwp.health.pip.document.submission.manager.exception.MessagingEventException;
import uk.gov.dwp.health.pip.document.submission.manager.model.application.HealthCaptureApplicationDto;

@Component
@RequiredArgsConstructor
@Slf4j
public class SubmittedApplicationPublisher {

  private final EventManager eventManager;
  private final SubmittedApplicationEventProperties publisherProperties;

  public void publishMessage(HealthCaptureApplicationDto submittedHealthCaptureApplication) {
    log.info("About to publish submitted application event");

    try {
      eventManager.send(
              new SubmittedApplicationEvent(
                      submittedHealthCaptureApplication,
                      publisherProperties.getTopicName(),
                      publisherProperties.getRoutingKey()));

    } catch (Exception ex) {
      log.info("Error publishing Submitted Application Event: {}", ex.getMessage());
      throw new MessagingEventException(ex.getMessage());
    }

    log.info("Published Submitted Application Event");
  }
}