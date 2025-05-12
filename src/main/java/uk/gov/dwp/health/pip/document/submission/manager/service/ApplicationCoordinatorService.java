package uk.gov.dwp.health.pip.document.submission.manager.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import uk.gov.dwp.health.integration.message.events.EventManager;
import uk.gov.dwp.health.pip.application.coordinator.openapi.coordinator.dto.ApplicationCoordinatorDto;
import uk.gov.dwp.health.pip.application.coordinator.openapi.coordinator.dto.StateDto;
import uk.gov.dwp.health.pip.document.submission.manager.config.MsApplicationCoordinatorConfig;
import uk.gov.dwp.health.pip.document.submission.manager.event.StateChangeEvent;

@Service
@Slf4j
@RequiredArgsConstructor
public class ApplicationCoordinatorService {

  private EventManager eventManager;
  @Value("${app.coordinator.submitted.topic.name}")
  private String topicName;
  @Value("${app.coordinator.submitted.routing.key}")
  private String routingKey;
  private RestTemplate restTemplate;
  private MsApplicationCoordinatorConfig msApplicationCoordinatorConfig;

  @Autowired
  public ApplicationCoordinatorService(EventManager eventManager, RestTemplate restTemplate,
      MsApplicationCoordinatorConfig msApplicationCoordinatorConfig) {
    this.eventManager = eventManager;
    this.restTemplate = restTemplate;
    this.msApplicationCoordinatorConfig = msApplicationCoordinatorConfig;
  }

  public void submit(String applicationId, String submissionId) {
    log.info(String.format("Attempting to update state of application "
        + "with application ID %s to SUBMITTED in Coordinator with submission ID %s",
        applicationId, submissionId));
    ApplicationCoordinatorDto eventPayload = new ApplicationCoordinatorDto();
    eventPayload.setState(new StateDto(StateDto.CurrentStateEnum.SUBMITTED));
    eventPayload.setApplicationId(applicationId);
    eventPayload.setSubmissionId(submissionId);
    eventManager.send(new StateChangeEvent(eventPayload, topicName, routingKey));
    log.info(String.format("Triggered state update of application "
        + "with application ID %s to SUBMITTED in Coordinator with submission ID %s",
        applicationId, submissionId));
  }

  public boolean isApplicationInApplicationCoordinator(String applicationId) {
    String getByApplicationIdUri = UriComponentsBuilder.fromUriString(
            msApplicationCoordinatorConfig.getApplicationByApplicationIdUrl())
        .queryParam("application_id", applicationId).build().toUriString();

    try {
      ResponseEntity<String> response = restTemplate.getForEntity(getByApplicationIdUri,
          String.class);
      return response.getStatusCode().value() == 200;
    } catch (HttpClientErrorException e) {
      if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
        return false;
      }
      throw new HttpClientErrorException(HttpStatusCode.valueOf(e.getStatusCode().value()),
          "Received the following response body when calling application coordinator: "
              + e.getResponseBodyAsString());
    }
  }
}
