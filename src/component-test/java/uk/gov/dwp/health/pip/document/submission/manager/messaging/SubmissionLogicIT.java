package uk.gov.dwp.health.pip.document.submission.manager.messaging;

import com.amazonaws.services.sqs.model.Message;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Duration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import uk.gov.dwp.health.pip.document.submission.manager.api.ApiTest;
import uk.gov.dwp.health.pip.document.submission.manager.config.MongoClientConnection;
import uk.gov.dwp.health.pip.document.submission.manager.model.application.HealthCaptureApplicationDto;
import uk.gov.dwp.health.pip.document.submission.manager.utils.MessageUtil;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static uk.gov.dwp.health.pip.document.submission.manager.utils.EnvironmentUtil.getEnv;


public class SubmissionLogicIT extends ApiTest {
  @BeforeEach
  public void setupForTests() {
    MongoClientConnection.emptyMongoCollections();
    applicationSubmittedUtils.clearQueue();
    stateChangeUtils.clearQueue();
    batchDocUtils.clearQueue();
    submittedApplicationUtils.clearQueue();
  }

  @Test
  public void shouldHaveSubmissionInTheDbWhenMessageSentOnInboundQueue() {
    String applicationId = "507f1f77bcf86cd799439011";
    applicationSubmittedUtils.sendMessageToQueue(MessageUtil.createSubmissionTestMessage(applicationId));

    await()
            .atMost(Duration.ofMinutes(1))
            .until(
                    () -> getMongoDbCountByApplicationId(applicationId) != 0);

    assertEquals(1, getMongoDbCountByApplicationId(applicationId));
    await()
        .atMost(Duration.ofMinutes(1))
        .until(() -> batchDocUtils.getMessageCount().equals("1"));
  }

  @Test
  public void shouldHaveMessageOnStateChangeQueueWhenMessageSentOnInboundQueue() {
    String applicationId = "507f1f77bcf86cd799439011";

    applicationSubmittedUtils.sendMessageToQueue(MessageUtil.createSubmissionTestMessage(applicationId));

    await()
            .atMost(Duration.ofMinutes(1))
            .until(
                    () -> !stateChangeUtils.getMessageCount().equals("0"));

    assertEquals("1", stateChangeUtils.getMessageCount());
  }

  @Test
  public void whenDSMSubmissionFlowIsTriggered_thenMessageIsSentOnQueue()
      throws JsonProcessingException {
    String applicationId = "123456789";

    applicationSubmittedUtils.sendMessageToQueue(
        MessageUtil.createSubmissionTestMessage(applicationId));

    await().atMost(Duration.ofSeconds(30))
        .until(() -> submittedApplicationUtils.getMessageCount().equals("1"));

    for (Message message : submittedApplicationUtils.receiveMessage().getMessages()) {
      ObjectMapper objectMapper = new ObjectMapper();
      objectMapper.registerModule(new JavaTimeModule());
      HealthCaptureApplicationDto healthCaptureData = objectMapper.readValue(
          mapResponseToMessage(message.getBody()),
          HealthCaptureApplicationDto.class);

      Assertions.assertNotNull(healthCaptureData.getApplicationId());
      Assertions.assertNotNull(healthCaptureData.getRegistrationDetails().getPersonalDetails());
    }
  }

  public int getMongoDbCountByApplicationId(String applicationId) {
    Query query = new Query();
    query.addCriteria(Criteria.where("applicationId").is(applicationId));
    return (int) MongoClientConnection.getMongoTemplate()
            .count(query, getEnv("SUBMISSION_COLLECTION", "submission"));
  }

  private String mapResponseToMessage(String queueMessageBody) throws JsonProcessingException {
    ObjectMapper objectMapper = new ObjectMapper();
    JsonNode rootNode = objectMapper.readTree(queueMessageBody);
    JsonNode messageNode = rootNode.path("Message");
    return messageNode.textValue();
  }
}
