package uk.gov.dwp.health.pip.document.submission.manager.cdc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.gov.dwp.health.pip.document.submission.manager.config.MongoClientConnection.getMongoTemplate;
import static uk.gov.dwp.health.pip.document.submission.manager.utils.UrlBuilderUtil.postApplyUrlV3;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.response.Response;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.PurgeQueueRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import uk.gov.dwp.health.pip.document.submission.manager.api.ApiTest;
import uk.gov.dwp.health.pip.document.submission.manager.config.MongoClientConnection;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.healthinformation.v2.dto.HealthCaptureApplicationDtoV2;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.v3.model.ApplicationIdDto;

@Slf4j
public class CDCPluginTestIT extends ApiTest {
  private static final String USER_ID = "102957750860208822019290";
  private static SqsClient sqsClient;
  private static String cdcQueueUrl;

  private static List<Message> pollQueueForMessages() {
    log.info("Polling queue...");
    ReceiveMessageRequest receiveMessageRequest =
        ReceiveMessageRequest.builder().queueUrl(cdcQueueUrl).maxNumberOfMessages(10).build();
    List<Message> receivedMessages = sqsClient.receiveMessage(receiveMessageRequest).messages();
    log.info("Messages received {}", receivedMessages.size());
    return receivedMessages;
  }

  @BeforeAll
  public static void setup() {
    cdcQueueUrl =
        getEnv(
            "CDC_QUEUE_URL",
            "http://localhost:4566/queue/eu-west-2/000000000000/pip-docsub-analytics");
    try {
      sqsClient =
          SqsClient.builder()
              .region(Region.of(getEnv("AWS_REGION", "eu-west-2")))
              .endpointOverride(new URI(getEnv("AWS_ENDPOINT_OVERRIDE", "http://localhost:4566")))
              .build();
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  private static void assertMessageAttributes(Message message) throws JsonProcessingException {
    log.debug(message.body());
    ObjectMapper objectMapper = new ObjectMapper();
    JsonNode jsonNode = objectMapper.readTree(message.body());
    JsonNode messageNode = objectMapper.readTree(jsonNode.path("Message").asText());
    JsonNode namespaceNode = objectMapper.readTree(messageNode.path("namespace").asText());
    assertEquals(
        "pip.docsub.mgr.stream",
        jsonNode.get("MessageAttributes").get("x-dwp-routing-key").get("Value").asText());
    assertEquals("doc-sub-mgr-db", namespaceNode.path("databaseName").asText());
    org.hamcrest.MatcherAssert.assertThat(
        namespaceNode.path("collectionName").asText(),
        anyOf(is("drs_upload"), is("submission"), is("document")));
  }

  @BeforeEach
  public void beforeEach() {
    MongoClientConnection.emptyMongoCollections();
  }

  @AfterEach
  public void afterEach() {
    sqsClient.purgeQueue(PurgeQueueRequest.builder().queueUrl(cdcQueueUrl).build());
  }

  @Test
  public void shouldHaveStreamedMongoDataWhenSubmissionRequestReceived() {
    String applicationId = "4bce57c491efc3ac3bc3e6f5";
    ApplicationIdDto applicationIdDto = new ApplicationIdDto().applicationId(applicationId);

    Response response =
        postRequestWithHeader(postApplyUrlV3(), applicationIdDto, "x-user-id", USER_ID);

    HealthCaptureApplicationDtoV2 healthCaptureApplicationDtoResponse =
        response.as(HealthCaptureApplicationDtoV2.class);
    assertEquals(response.getStatusCode(), HttpStatus.ACCEPTED.value());
    assertThat(healthCaptureApplicationDtoResponse.getSubmissionId()).matches("^[a-zA-Z0-9]{24}$");
    assertEquals(1, getMongoDbCountByApplicationId(applicationId));
    await()
        .atMost(Duration.ofSeconds(10L))
        .untilAsserted(
            () -> {
              List<Message> messages = pollQueueForMessages();
              messages.forEach(
                  message -> {
                    try {
                      assertMessageAttributes(message);
                    } catch (JsonProcessingException e) {
                      throw new RuntimeException(e);
                    }
                  });
              assertThat(messages.size()).isGreaterThan(0);
            });
  }

  private int getMongoDbCountByApplicationId(String applicationId) {
    Query query = new Query();
    query.addCriteria(Criteria.where("applicationId").is(applicationId));
    return (int) getMongoTemplate().count(query, getEnv("SUBMISSION_COLLECTION", "submission"));
  }
}
