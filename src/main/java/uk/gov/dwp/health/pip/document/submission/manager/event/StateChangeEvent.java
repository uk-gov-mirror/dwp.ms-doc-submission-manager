package uk.gov.dwp.health.pip.document.submission.manager.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.NoArgsConstructor;
import uk.gov.dwp.health.integration.message.events.Event;

import java.util.Map;
import uk.gov.dwp.health.pip.application.coordinator.openapi.coordinator.dto.ApplicationCoordinatorDto;

@NoArgsConstructor
public class StateChangeEvent extends Event {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  static {
    OBJECT_MAPPER.registerModule(new JavaTimeModule());
  }

  public StateChangeEvent(
      ApplicationCoordinatorDto payload, String topic, String routingKey) {
    this.setTopic(topic);
    this.setPayload(OBJECT_MAPPER.convertValue(payload, Map.class));
    this.setRoutingKey(routingKey);
  }
}
