package uk.gov.dwp.health.pip.document.submission.manager.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.ObjectMapper;
import uk.gov.dwp.health.pip.application.coordinator.openapi.coordinator.dto.ApplicationDetails;
import uk.gov.dwp.health.pip.application.coordinator.openapi.v1.api.HealthAndDisabilityApiClientV1;
import uk.gov.dwp.health.pip.document.submission.manager.config.MsApplicationCoordinatorConfig;
import uk.gov.dwp.health.pip.document.submission.manager.model.application.ApplicationCoordinatorNotFoundResultFailure;
import uk.gov.dwp.health.pip.document.submission.manager.model.application.ExceptionOccurredResultFailure;
import uk.gov.dwp.health.pip.document.submission.manager.model.application.ResultWrapper;

@Service
@Slf4j
@RequiredArgsConstructor
public class ApplicationCoordinatorService {

  private final HealthAndDisabilityApiClientV1 coordinatorApiClientV1;
  private final MsApplicationCoordinatorConfig msApplicationCoordinatorConfig;
  private final ObjectMapper objectMapper;
  private final RestTemplate restTemplate;

  @Retryable(
      retryFor = {
        HttpServerErrorException.BadGateway.class,
        HttpServerErrorException.GatewayTimeout.class
      })
  public void submitApplication(String applicationId, String submissionId) {
    log.info("Attempting to submit application with id {} to coordinator", applicationId);

    try {
      coordinatorApiClientV1.submitHealthDisabilityData(applicationId, submissionId);
      log.info("Application with id {} successfully submitted to coordinator", applicationId);
    } catch (HttpClientErrorException exception) {
      log.error(
          "Failed to submit application with id {} to coordinator. Exception: {}",
          applicationId,
          exception.getMessage());
      throw exception;
    }
  }

  public boolean isApplicationInApplicationCoordinator(String applicationId) {
    String applicationIdUri = getByApplicationIdUri(applicationId);

    try {
      ResponseEntity<String> response = restTemplate.getForEntity(applicationIdUri, String.class);
      return response.getStatusCode().value() == 200;
    } catch (HttpClientErrorException e) {
      if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
        return false;
      }
      throw new HttpClientErrorException(
          HttpStatusCode.valueOf(e.getStatusCode().value()),
          "Received the following response body when calling application coordinator: "
              + e.getResponseBodyAsString());
    }
  }

  public ResultWrapper<ApplicationDetails> getApplication(String applicationId) {
    log.info(
        String.format(
            """
                Attempting to get application with application ID: %s \
                from Application Coordinator""",
            applicationId));

    String applicationIdUri = getByApplicationIdUri(applicationId);

    try {
      ResponseEntity<String> result = restTemplate.getForEntity(applicationIdUri, String.class);

      ApplicationDetails applicationData =
          objectMapper.readValue(result.getBody(), ApplicationDetails.class);

      log.info(
          String.format(
              """
                Successfully received application with application ID: %s \
                from Application Coordinator""",
              applicationId));

      return ResultWrapper.<ApplicationDetails>builder().value(applicationData).build();
    } catch (HttpClientErrorException e) {
      log.error(e.getMessage());
      if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
        return getApplicationNotFoundResult(applicationId);
      }
      return ResultWrapper.<ApplicationDetails>builder()
          .failure(new ExceptionOccurredResultFailure(e.getMessage(), e.getStackTrace()))
          .build();
    }
  }

  private String getByApplicationIdUri(String applicationId) {
    return UriComponentsBuilder.fromUriString(
            msApplicationCoordinatorConfig.getApplicationByApplicationIdUrl())
        .queryParam("application_id", applicationId)
        .build()
        .toUriString();
  }

  private ResultWrapper<ApplicationDetails> getApplicationNotFoundResult(
      final String applicationId) {
    return ResultWrapper.<ApplicationDetails>builder()
        .failure(new ApplicationCoordinatorNotFoundResultFailure(applicationId))
        .build();
  }
}
