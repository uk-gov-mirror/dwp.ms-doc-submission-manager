package uk.gov.dwp.health.pip.document.submission.manager.api.submission;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.Dimension;
import software.amazon.awssdk.services.cloudwatch.model.DimensionFilter;
import software.amazon.awssdk.services.cloudwatch.model.ListMetricsRequest;
import software.amazon.awssdk.services.cloudwatch.model.ListMetricsResponse;
import software.amazon.awssdk.services.cloudwatch.model.Metric;
import uk.gov.dwp.health.pip.document.submission.manager.api.ApiTest;
import uk.gov.dwp.health.pip.document.submission.manager.config.MongoClientConnection;
import uk.gov.dwp.health.pip.document.submission.manager.entity.DocumentId;
import uk.gov.dwp.health.pip.document.submission.manager.entity.Documentation;
import uk.gov.dwp.health.pip.document.submission.manager.entity.DrsUpload;
import uk.gov.dwp.health.pip.document.submission.manager.entity.Storage;
import uk.gov.dwp.health.pip.document.submission.manager.entity.Submission;
import uk.gov.dwp.health.pip.document.submission.manager.event.response.DrsUploadResponse;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DocBatchResponseIT extends ApiTest {

  private static final String HOST_NAME =
      System.getenv().getOrDefault("AWS_SERVICE_HOSTNAME", "localhost");
  private static final String METRIC_NAME =
      System.getenv()
          .getOrDefault("AWS_CLOUD_WATCH_SUBMISSION_FAILURE_METRIC_NAME", "submission-failed");
  private static final ObjectMapper objectMapper = new ObjectMapper();
  private static CloudWatchClient cloudWatchClient;

  @BeforeAll
  static void setupAll() {
    cloudWatchClient =
        CloudWatchClient.builder()
            .endpointOverride(URI.create("http://" + HOST_NAME + ":4566"))
            .region(Region.EU_WEST_2)
            .build();

    objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
  }

  @BeforeEach
  void testSetup() {
    MongoClientConnection.emptyMongoCollections();
  }

  @Test
  void updateDrsRequestAudit() throws JsonProcessingException {
    setupData();

    final int countBefore = getMetrics().metrics().size();
    final DrsUploadResponse drsUploadResponse = new DrsUploadResponse();
    drsUploadResponse.setRequestId("68d5408131afc4f728c92154");
    drsUploadResponse.setSuccess(false);
    drsUploadResponse.setErrorMessage("Document failed validation at DRS");
    final String messageBody = objectMapper.writeValueAsString(drsUploadResponse);
    final String queueUrl = "http://" + HOST_NAME + ":4566/000000000000/docbatch-batch-response";

    LoggerFactory.getLogger(getClass())
        .info("messageBody = [{}], queueUrl = [{}]", messageBody, queueUrl);

    messageUtil.sendMessageToQueue(queueUrl, messageBody);

    final long startOfTimer = System.currentTimeMillis();
    int countAfter = countBefore;
    Metric metric = null;
    while (System.currentTimeMillis() - 10000 < startOfTimer && countAfter <= countBefore) {
      final List<Metric> metrics = getMetrics().metrics();
      countAfter = metrics.size();
      if (countAfter > 0) {
        metric = metrics.get(metrics.size() - 1);
      }
    }
    assertTrue(
        countAfter == countBefore + 1,
        "Expected metrics count to increment by one - went from "
            + countBefore
            + " to "
            + countAfter);
    boolean foundAppVersion = false;
    for (final Dimension dimension : metric.dimensions()) {
      if (dimension.name().equals("AppVersion")) {
        assertTrue(dimension.value().matches("[0-9]*\\.[0-9]*\\.[0-9]*.*"));
        foundAppVersion = true;
        break;
      }
    }
    assertTrue(foundAppVersion, "Expected AppVersion dimension on metric");
  }

  private ListMetricsResponse getMetrics() {
    ListMetricsRequest request =
        ListMetricsRequest.builder()
            .dimensions(DimensionFilter.builder().name("channel").value("strategic").build())
            .metricName(METRIC_NAME)
            .namespace("test")
            .build();

    return cloudWatchClient.listMetrics(request);
  }

  private void setupData() {
    Documentation documentation =
        Documentation.builder()
            .id("68d5408131afc4f728c92152")
            .applicationId("6ab2e541827710233ad6b5c5")
            .claimantId("89276856bdbb8f83b5fd2474")
            .filename("medical-evidence.jpg")
            .sizeKb(5000)
            .timestamp(LocalDateTime.of(2020, 9, 8, 14, 30))
            .documentType("1274")
            .storage(
                List.of(
                    Storage.builder()
                        .type("S3")
                        .uniqueId("123_TEST.jpg.2020.08.06")
                        .url("http://localstack:4566/pip_bucket/123_TEST.jpg.2020.08.06")
                        .build()))
            .build();

    DrsUpload drsUpload =
        DrsUpload.builder()
            .id("68d5408131afc4f728c92154")
            .submissionId("68d5408131afc4f728c92153")
            .documentIdIds(
                List.of(DocumentId.builder().documentId("68d5408131afc4f728c92152").build()))
            .status("PUBLISHED")
            .submittedAt(LocalDateTime.now())
            .build();

    Submission submission =
        Submission.builder()
            .id("68d5408131afc4f728c92153")
            .claimantId("89276856bdbb8f83b5fd2474")
            .applicationId("6ab2e541827710233ad6b5c5")
            .documentIdIds(
                List.of(DocumentId.builder().documentId("68d5408131afc4f728c92152").build()))
            .build();

    MongoTemplate mongoTemplate = MongoClientConnection.getMongoTemplate();
    mongoTemplate.save(documentation, "document");
    mongoTemplate.save(drsUpload, "drs_upload");
    mongoTemplate.save(submission, "submission");
  }
}
