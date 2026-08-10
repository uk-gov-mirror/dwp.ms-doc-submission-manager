package uk.gov.dwp.health.pip.document.submission.manager.service.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import uk.gov.dwp.health.integration.message.consumers.HealthMessageConsumer;
import uk.gov.dwp.health.pip.document.submission.manager.config.properties.EventConfigProperties;
import uk.gov.dwp.health.pip.document.submission.manager.entity.DrsUpload;
import uk.gov.dwp.health.pip.document.submission.manager.event.response.DrsUploadResponse;
import uk.gov.dwp.health.pip.document.submission.manager.model.DrsStatusEnum;
import uk.gov.dwp.health.pip.document.submission.manager.service.CloudWatchMetricsService;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EventListenerService implements HealthMessageConsumer<Map<String, Object>> {

  private static Logger log = LoggerFactory.getLogger(EventListenerService.class);

  private final EventConfigProperties configProperties;
  private final DataServiceImpl dataService;
  private final ObjectMapper objectMapper;
  private final CloudWatchMetricsService cloudWatchMetricsService;

  @Override
  public String getQueueName() {
    return configProperties.getQueueName();
  }

  @Override
  public void handleMessage(MessageHeaders messageHeaders, final Map<String, Object> payload) {
    final DrsUploadResponse drsUploadResponse =
        objectMapper.convertValue(payload, DrsUploadResponse.class);
    DrsUpload drsRequest = findDrsRequestAudit(drsUploadResponse.getRequestId());
    if (drsRequest == null) {
      log.warn(
          "Request ID {} did not find corresponding submission", drsUploadResponse.getRequestId());
    } else {
      updateDrsRequestAudit(drsUploadResponse, drsRequest);
    }
  }

  private DrsUpload findDrsRequestAudit(String requestId) {
    return dataService.findDrsRequestByRequestId(requestId);
  }

  private void updateDrsRequestAudit(DrsUploadResponse response, DrsUpload drsRequest) {
    if (response.isSuccess()) {
      log.info("Successful Doc Batch request for {}", response.getRequestId());
    }
    drsRequest.setStatus(
        response.isSuccess() ? DrsStatusEnum.SUCCESS.status : DrsStatusEnum.FAIL.status);
    if (!response.isSuccess()) {
      drsRequest.setErrors(response.getErrorMessage());
      Optional.ofNullable(response.getAdditionalErrorDetails())
          .ifPresent(
              d -> {
                try {
                  drsRequest.setAdditionalErrorDetails(objectMapper.writeValueAsString(d));
                } catch (JacksonException e) {
                  log.error("Fail to marshal additional error to JSON string");
                }
              });
      log.error(
          "Unsuccessful Doc Batch request {} - error details are in mongo in drs_upload",
          response.getRequestId()
      );
      cloudWatchMetricsService.incrementSubmissionFailureMetric();
    }
    drsRequest.setCompletedAt(LocalDateTime.now());
    dataService.createUpdateDrsRequestAudit(drsRequest);
  }
}
