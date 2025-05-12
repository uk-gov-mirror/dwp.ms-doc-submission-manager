package uk.gov.dwp.health.pip.document.submission.manager.event;

import uk.gov.dwp.health.integration.message.events.Event;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class WorkflowEvent extends Event {

  public WorkflowEvent(
      String topic,
      String applicationId,
      String name,
      String nino,
      Date submissionDate,
      String routingKey) {
    Map<String, Object> map = new HashMap<>();
    map.put("applicationId", applicationId);
    map.put("name", name);
    map.put("nino", nino);
    map.put("submissionDate", submissionDate);

    setPayload(map);
    setTopic(topic);
    setRoutingKey(routingKey);
  }
}