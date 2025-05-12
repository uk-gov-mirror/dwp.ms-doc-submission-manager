package uk.gov.dwp.health.pip.document.submission.manager.event;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import uk.gov.dwp.health.integration.message.events.Event;
import uk.gov.dwp.health.pip.document.submission.manager.model.application.HealthCaptureApplicationDto;


public class SubmittedApplicationEvent extends Event {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  static {
    OBJECT_MAPPER.registerModule(new JavaTimeModule());
  }

  public SubmittedApplicationEvent(
          HealthCaptureApplicationDto submittedHealthCaptureApplication, String topic,
          String routingKey) {
    setTopic(topic);
    setRoutingKey(routingKey);
    setPayload(OBJECT_MAPPER.convertValue(submittedHealthCaptureApplication, new TypeReference<>() {
    }));
  }
}