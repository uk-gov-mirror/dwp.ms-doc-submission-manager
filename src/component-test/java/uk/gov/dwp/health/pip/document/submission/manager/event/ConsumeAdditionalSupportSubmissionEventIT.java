package uk.gov.dwp.health.pip.document.submission.manager.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import software.amazon.awssdk.services.sqs.model.Message;
import uk.gov.dwp.health.pip.document.submission.manager.api.ApiTest;
import uk.gov.dwp.health.pip.document.submission.manager.entity.Submission;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static uk.gov.dwp.health.pip.document.submission.manager.config.MongoClientConnection.emptyMongoCollections;
import static uk.gov.dwp.health.pip.document.submission.manager.config.MongoClientConnection.getMongoTemplate;

class ConsumeAdditionalSupportSubmissionEventIT extends ApiTest {
  private final Logger logger = LoggerFactory.getLogger(getClass());
  private final MongoTemplate mongoTemplate = getMongoTemplate();
  private final ObjectMapper objectMapper = new ObjectMapper();
  private static final String INBOUND_QUEUE_URL =
      getEnv(
          "EVENT_INBOUND_ADDITIONAL_SUPPORT_SUBMISSION_QUEUE_URL",
          "http://localhost:4566/000000000000/ms-doc-sub-additional-support-submission-queue");
  private static final String INBOUND_QUEUE_DLQ_URL =
      getEnv(
          "EVENT_INBOUND_ADDITIONAL_SUPPORT_SUBMISSION_QUEUE_DLQ_URL",
          "http://localhost:4566/000000000000/ms-doc-sub-additional-support-submission-queue-dlq");
  private static final String OUTBOUND_QUEUE_URL =
      getEnv(
          "EVENT_OUTBOUND_ADDITIONAL_SUPPORT_SUBMISSION_QUEUE_URL",
          "http://localhost:4566/000000000000/ms-app-co-additional-support-submission-queue");

  @BeforeEach
  void beforeEach() {
    emptyMongoCollections();
    messageUtil.clearQueue(INBOUND_QUEUE_URL);
    messageUtil.clearQueue(INBOUND_QUEUE_DLQ_URL);
    messageUtil.clearQueue(OUTBOUND_QUEUE_URL);
  }

  @Test
  void when_event_processed_successfully() throws JsonProcessingException, JSONException {
    Map<String, Object> map = new HashMap<>();
    map.put("application_id", "000000000000000000000200");
    map.put("claimant_id", "000000000000000000000001");
    String messageBody = objectMapper.writeValueAsString(map);

    messageUtil.sendMessageToQueue(INBOUND_QUEUE_URL, messageBody);

    await()
        .atMost(1, TimeUnit.MINUTES)
        .until(() -> messageUtil.getMessageCount(INBOUND_QUEUE_URL).equals("0"));
    await()
        .atMost(1, TimeUnit.MINUTES)
        .until(() -> messageUtil.getMessageCount(OUTBOUND_QUEUE_URL).equals("1"));
    assertThat(messageUtil.getMessageCount(INBOUND_QUEUE_DLQ_URL)).isEqualTo("0");

    Message message = messageUtil.getMessage(OUTBOUND_QUEUE_URL);
    logger.info(message.body());
    JSONObject body = new JSONObject(message.body());
    assertThat(body.get("application_id")).isEqualTo("000000000000000000000200");
    assertThat(body.get("submission_id")).isNotNull();

    List<Submission> submissions = mongoTemplate.findAll(Submission.class);
    assertThat(submissions).hasSize(1);
    Submission submission = submissions.get(0);
    assertThat(submission.getApplicationId()).isEqualTo("000000000000000000000200");
    assertThat(submission.getClaimantId()).isEqualTo("000000000000000000000001");
    assertThat(submission.getStarted()).isEqualTo("2023-05-09");
    assertThat(submission.getCompleted()).isEqualTo(LocalDate.now());
  }

  @Test
  void when_event_processed_with_existing_submission() throws JsonProcessingException {
    Submission submission =
        Submission.builder()
            .applicationId("000000000000000000000200")
            .claimantId("000000000000000000000001")
            .build();
    mongoTemplate.save(submission);

    Map<String, Object> map = new HashMap<>();
    map.put("application_id", "000000000000000000000200");
    map.put("claimant_id", "000000000000000000000001");
    String messageBody = objectMapper.writeValueAsString(map);

    messageUtil.sendMessageToQueue(INBOUND_QUEUE_URL, messageBody);

    await()
        .atMost(1, TimeUnit.MINUTES)
        .until(() -> messageUtil.getMessageCount(INBOUND_QUEUE_URL).equals("0"));
    await()
        .atMost(1, TimeUnit.MINUTES)
        .until(() -> messageUtil.getMessageCount(INBOUND_QUEUE_DLQ_URL).equals("1"));
    assertThat(messageUtil.getMessageCount(OUTBOUND_QUEUE_URL)).isEqualTo("0");
  }

  @Test
  void when_event_processed_with_rest_client_exception() throws JsonProcessingException {
    Map<String, Object> map = new HashMap<>();
    map.put("application_id", "000000000000000000000400");
    map.put("claimant_id", "000000000000000000000001");
    String messageBody = objectMapper.writeValueAsString(map);

    messageUtil.sendMessageToQueue(INBOUND_QUEUE_URL, messageBody);

    await()
        .atMost(1, TimeUnit.MINUTES)
        .until(() -> messageUtil.getMessageCount(INBOUND_QUEUE_URL).equals("0"));
    await()
        .atMost(1, TimeUnit.MINUTES)
        .until(() -> messageUtil.getMessageCount(INBOUND_QUEUE_DLQ_URL).equals("1"));
    assertThat(messageUtil.getMessageCount(OUTBOUND_QUEUE_URL)).isEqualTo("0");
  }
}
