package uk.gov.dwp.health.pip.document.submission.manager.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.dwp.health.integration.message.events.EventManager;
import uk.gov.dwp.health.pip.document.submission.manager.config.properties.OutboundWorkflowEventProperties;
import uk.gov.dwp.health.pip.document.submission.manager.event.WorkflowEvent;
import uk.gov.dwp.health.pip.document.submission.manager.exception.MessagingEventException;

import java.util.Date;

@Component
@RequiredArgsConstructor
@Slf4j
public class WorkflowMessagePublisher {

  private final EventManager eventManager;
  private final OutboundWorkflowEventProperties properties;

  public void publishMessage(String applicationId, String name, String nino, Date submissionDate) {
    log.info("About to publish workflow event");

    try {
      eventManager.send(
          new WorkflowEvent(
              properties.getTopicExchange(),
              applicationId,
              name,
              nino,
              submissionDate,
              properties.getRoutingKey()));

    } catch (Exception ex) {
      log.info("Error publishing workflow event: {}", ex.getMessage());
      throw new MessagingEventException(ex.getMessage());
    }

    log.info("Published workflow event");
  }
}