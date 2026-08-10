package uk.gov.dwp.health.pip.document.submission.manager.api.query;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.dwp.health.pip.document.submission.manager.utils.UrlBuilderUtil.getStatusUrl;

import io.restassured.response.Response;
import java.time.LocalDateTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.HttpStatus;
import uk.gov.dwp.health.pip.document.submission.manager.api.ApiTest;
import uk.gov.dwp.health.pip.document.submission.manager.config.MongoClientConnection;
import uk.gov.dwp.health.pip.document.submission.manager.dto.responses.ErrorResponse;
import uk.gov.dwp.health.pip.document.submission.manager.dto.responses.query.StatusResponse;
import uk.gov.dwp.health.pip.document.submission.manager.entity.DocumentId;
import uk.gov.dwp.health.pip.document.submission.manager.entity.Documentation;
import uk.gov.dwp.health.pip.document.submission.manager.entity.DrsUpload;
import uk.gov.dwp.health.pip.document.submission.manager.entity.Storage;
import uk.gov.dwp.health.pip.document.submission.manager.entity.Submission;

@Slf4j
class GetStatusIT extends ApiTest {

  @BeforeEach
  public void testSetup() {
    MongoClientConnection.emptyMongoCollections();
  }

  @Test
  void shouldReturn200StatusCodeAndCorrectResponseBody() {
    setupData();

    Response response = getRequest(getStatusUrl("68d5408131afc4f728c92154"));
    StatusResponse statusResponse = response.as(StatusResponse.class);
    StatusResponse.Documents document = statusResponse.getDocuments().get(0);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK.value());
    assertThat(statusResponse.getRequestId()).isEqualTo("68d5408131afc4f728c92154");
    assertThat(statusResponse.getDrsUploadStatus()).isEqualTo("PUBLISHED");
    assertThat(document.getSubmissionId()).isEqualTo("68d5408131afc4f728c92153");
    assertThat(document.getDocumentId()).isEqualTo("68d5408131afc4f728c92152");
    assertThat(document.getContentType()).isEqualTo("1274");
    assertThat(document.getName()).isEqualTo("medical-evidence.jpg");
    assertThat(document.getSize()).isEqualTo(5000);
  }

  @Test
  void shouldReturn400StatusCodeForInvalidIdFormat() {
    Response response = getRequest(getStatusUrl("/"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
  }

  @Test
  void shouldReturn404StatusCodeForIdNotFound() {
    Response response = getRequest(getStatusUrl("abc"));
    ErrorResponse errorResponse = response.as(ErrorResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND.value());
    assertThat(errorResponse.getMessage()).isEqualTo("DRS request abc not found");
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
