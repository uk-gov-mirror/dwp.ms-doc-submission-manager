package uk.gov.dwp.health.pip.document.submission.manager.event;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NoArgsConstructor;
import uk.gov.dwp.health.integration.message.events.QueueEvent;
import uk.gov.dwp.health.pip.document.submission.manager.generated.event.AdditionalSupportApplicationAutoSubmittedEventV1Payload;

@NoArgsConstructor
class AdditionalSupportApplicationAutoSubmittedEvent extends QueueEvent {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  AdditionalSupportApplicationAutoSubmittedEvent(
      String queue, AdditionalSupportApplicationAutoSubmittedEventV1Payload payload) {
    setOutboundQueue(queue);
    setPayload(OBJECT_MAPPER.convertValue(payload, new TypeReference<>() {}));
    setVersion("1.0.0");
  }
}
