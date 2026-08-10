package uk.gov.dwp.health.pip.document.submission.manager.api.submission;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.dwp.health.pip.document.submission.manager.utils.UrlBuilderUtil.postAttachUrl;

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
import uk.gov.dwp.health.pip.document.submission.manager.dto.requests.submission.Document;
import uk.gov.dwp.health.pip.document.submission.manager.dto.requests.submission.DocumentSubmissionRequest;
import uk.gov.dwp.health.pip.document.submission.manager.dto.requests.submission.DrsMetadata;
import uk.gov.dwp.health.pip.document.submission.manager.dto.responses.ErrorResponse;
import uk.gov.dwp.health.pip.document.submission.manager.dto.responses.submission.DocumentSubmissionResponse;
import uk.gov.dwp.health.pip.document.submission.manager.entity.DocumentId;
import uk.gov.dwp.health.pip.document.submission.manager.entity.Documentation;
import uk.gov.dwp.health.pip.document.submission.manager.entity.DrsUpload;
import uk.gov.dwp.health.pip.document.submission.manager.entity.Storage;
import uk.gov.dwp.health.pip.document.submission.manager.entity.Submission;

@Slf4j
class PostAttachIT extends ApiTest {

  @BeforeEach
  public void testSetup() {
    MongoClientConnection.emptyMongoCollections();
    setupData();
  }

  @Test
  void shouldReturn202StatusCodeAndCorrectResponseBody() {
    DocumentSubmissionRequest documentSubmissionRequest =
        DocumentSubmissionRequest.builder()
            .submissionId("68d5408131afc4f728c92153")
            .build();
    Response response =
        postRequestWithHeader(
            postAttachUrl(), documentSubmissionRequest, "x-user-id", "971532410455890287207953");
    DocumentSubmissionResponse documentSubmissionResponse =
        response.as(DocumentSubmissionResponse.class);

    int statusCode = response.getStatusCode();

    assertThat(statusCode).isEqualTo(HttpStatus.ACCEPTED.value());
    assertThat(documentSubmissionResponse).isInstanceOf(DocumentSubmissionResponse.class);
  }

  @Test
  void shouldReturn404StatusCodeForDocumentNotFound() {
    DocumentSubmissionRequest documentSubmissionRequest =
        DocumentSubmissionRequest.builder()
            .submissionId("b0a0d4fb-e6c8-419e-8cb9-af45914bd1a6")
            .build();
    Response response =
        postRequestWithHeader(
            postAttachUrl(), documentSubmissionRequest, "x-user-id", "971532410455890287207953");
    ErrorResponse errorResponse = response.as(ErrorResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND.value());
    assertThat(errorResponse.getMessage())
        .isEqualTo("Submission [b0a0d4fb-e6c8-419e-8cb9-af45914bd1a6] not found");
  }

  @Test
  void shouldReturn400StatusCodeForInvalidJson() {
    Response response =
        postRequestWithHeader(postAttachUrl(), "}", "x-user-id", "971532410455890287207953");
    ErrorResponse errorResponse = response.as(ErrorResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    assertThat(errorResponse.getMessage()).contains("JSON parse error");
  }

  @Test
  void shouldReturn400StatusCodeIfNoRegion() {
    DocumentSubmissionRequest documentSubmissionRequest =
        DocumentSubmissionRequest.builder().region("").build();
    Response response =
        postRequestWithHeader(
            postAttachUrl(), documentSubmissionRequest, "x-user-id", "971532410455890287207953");
    ErrorResponse errorResponse = response.as(ErrorResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    assertThat(errorResponse.getMessage()).contains("JSON parse error");
  }

  @Test
  void shouldReturn400StatusCodeIfInvalidRegion() {
    DocumentSubmissionRequest documentSubmissionRequest =
        DocumentSubmissionRequest.builder().region("AA").build();
    Response response =
        postRequestWithHeader(
            postAttachUrl(), documentSubmissionRequest, "x-user-id", "971532410455890287207953");
    ErrorResponse errorResponse = response.as(ErrorResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    assertThat(errorResponse.getMessage()).contains("JSON parse error");
  }

  @Test
  void shouldReturn400StatusCodeIfNoSurname() {
    DocumentSubmissionRequest documentSubmissionRequest =
        DocumentSubmissionRequest.builder()
            .drsMetadata(DrsMetadata.builder().surname("").build())
            .build();
    Response response =
        postRequestWithHeader(
            postAttachUrl(), documentSubmissionRequest, "x-user-id", "971532410455890287207953");
    ErrorResponse errorResponse = response.as(ErrorResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    assertThat(errorResponse.getMessage()).contains("Validation failed");
  }

  @Test
  void shouldReturn400StatusCodeIfInvalidSurname() {
    DocumentSubmissionRequest documentSubmissionRequest =
        DocumentSubmissionRequest.builder()
            .drsMetadata(DrsMetadata.builder().surname("!@£$%^&@%").build())
            .build();
    Response response =
        postRequestWithHeader(
            postAttachUrl(), documentSubmissionRequest, "x-user-id", "971532410455890287207953");
    ErrorResponse errorResponse = response.as(ErrorResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    assertThat(errorResponse.getMessage()).contains("Validation failed");
  }

  @Test
  void shouldReturn400StatusCodeIfNoForename() {
    DocumentSubmissionRequest documentSubmissionRequest =
        DocumentSubmissionRequest.builder()
            .drsMetadata(DrsMetadata.builder().forename("").build())
            .build();
    Response response =
        postRequestWithHeader(
            postAttachUrl(), documentSubmissionRequest, "x-user-id", "971532410455890287207953");
    ErrorResponse errorResponse = response.as(ErrorResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    assertThat(errorResponse.getMessage()).contains("Validation failed");
  }

  @Test
  void shouldReturn400StatusCodeIfInvalidForename() {
    DocumentSubmissionRequest documentSubmissionRequest =
        DocumentSubmissionRequest.builder()
            .drsMetadata(DrsMetadata.builder().forename("!@£$%^&@%").build())
            .build();
    Response response =
        postRequestWithHeader(
            postAttachUrl(), documentSubmissionRequest, "x-user-id", "971532410455890287207953");
    ErrorResponse errorResponse = response.as(ErrorResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    assertThat(errorResponse.getMessage()).contains("Validation failed");
  }

  @Test
  void shouldReturn400StatusCodeIfNoDob() {
    DocumentSubmissionRequest documentSubmissionRequest =
        DocumentSubmissionRequest.builder()
            .drsMetadata(DrsMetadata.builder().dob("").build())
            .build();
    Response response =
        postRequestWithHeader(
            postAttachUrl(), documentSubmissionRequest, "x-user-id", "971532410455890287207953");
    ErrorResponse errorResponse = response.as(ErrorResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    assertThat(errorResponse.getMessage()).contains("Validation failed");
  }

  @Test
  void shouldReturn400StatusCodeIfInvalidDob() {
    DocumentSubmissionRequest documentSubmissionRequest =
        DocumentSubmissionRequest.builder()
            .drsMetadata(DrsMetadata.builder().dob("!@£$%^&@%").build())
            .build();
    Response response =
        postRequestWithHeader(
            postAttachUrl(), documentSubmissionRequest, "x-user-id", "971532410455890287207953");
    ErrorResponse errorResponse = response.as(ErrorResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    assertThat(errorResponse.getMessage()).contains("JSON parse error");
  }

  @Test
  void shouldReturn400StatusCodeIfDobWrongFormat() {
    DocumentSubmissionRequest documentSubmissionRequest =
        DocumentSubmissionRequest.builder()
            .drsMetadata(DrsMetadata.builder().dob("1-1-2000").build())
            .build();
    Response response =
        postRequestWithHeader(
            postAttachUrl(), documentSubmissionRequest, "x-user-id", "971532410455890287207953");
    ErrorResponse errorResponse = response.as(ErrorResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    assertThat(errorResponse.getMessage()).contains("JSON parse error");
  }

  @Test
  void shouldReturn400StatusCodeIfNoNino() {
    DocumentSubmissionRequest documentSubmissionRequest =
        DocumentSubmissionRequest.builder()
            .drsMetadata(DrsMetadata.builder().nino("").build())
            .build();
    Response response =
        postRequestWithHeader(
            postAttachUrl(), documentSubmissionRequest, "x-user-id", "971532410455890287207953");
    ErrorResponse errorResponse = response.as(ErrorResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    assertThat(errorResponse.getMessage()).contains("Validation failed");
  }

  @Test
  void shouldReturn400StatusCodeIfInvalidNino() {
    DocumentSubmissionRequest documentSubmissionRequest =
        DocumentSubmissionRequest.builder()
            .drsMetadata(DrsMetadata.builder().nino("!@£$%^&@%").build())
            .build();
    Response response =
        postRequestWithHeader(
            postAttachUrl(), documentSubmissionRequest, "x-user-id", "971532410455890287207953");
    ErrorResponse errorResponse = response.as(ErrorResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    assertThat(errorResponse.getMessage()).contains("Validation failed");
  }

  @Test
  void shouldReturn400StatusCodeIfNinoWrongFormat() {
    DocumentSubmissionRequest documentSubmissionRequest =
        DocumentSubmissionRequest.builder()
            .drsMetadata(DrsMetadata.builder().nino("ZZ123456Z").build())
            .build();
    Response response =
        postRequestWithHeader(
            postAttachUrl(), documentSubmissionRequest, "x-user-id", "971532410455890287207953");
    ErrorResponse errorResponse = response.as(ErrorResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    assertThat(errorResponse.getMessage()).contains("Validation failed");
  }

  @Test
  void shouldReturn400StatusCodeIfNoPostCode() {
    DocumentSubmissionRequest documentSubmissionRequest =
        DocumentSubmissionRequest.builder()
            .drsMetadata(DrsMetadata.builder().postcode("").build())
            .build();
    Response response =
        postRequestWithHeader(
            postAttachUrl(), documentSubmissionRequest, "x-user-id", "971532410455890287207953");
    ErrorResponse errorResponse = response.as(ErrorResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    assertThat(errorResponse.getMessage()).contains("Validation failed");
  }

  @Test
  void shouldReturn400StatusCodeIfNoBucket() {
    DocumentSubmissionRequest documentSubmissionRequest =
        DocumentSubmissionRequest.builder()
            .documents(List.of(Document.builder().bucket("").build()))
            .build();
    Response response =
        postRequestWithHeader(
            postAttachUrl(), documentSubmissionRequest, "x-user-id", "971532410455890287207953");
    ErrorResponse errorResponse = response.as(ErrorResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    assertThat(errorResponse.getMessage()).contains("Validation failed");
  }

  @Test
  void shouldReturn400StatusCodeIfNoS3Ref() {
    DocumentSubmissionRequest documentSubmissionRequest =
        DocumentSubmissionRequest.builder()
            .documents(List.of(Document.builder().s3Ref("").build()))
            .build();
    Response response =
        postRequestWithHeader(
            postAttachUrl(), documentSubmissionRequest, "x-user-id", "971532410455890287207953");
    ErrorResponse errorResponse = response.as(ErrorResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    assertThat(errorResponse.getMessage()).contains("Validation failed");
  }

  @Test
  void shouldReturn400StatusCodeIfNoContentType() {
    DocumentSubmissionRequest documentSubmissionRequest =
        DocumentSubmissionRequest.builder()
            .documents(List.of(Document.builder().contentType("").build()))
            .build();
    Response response =
        postRequestWithHeader(
            postAttachUrl(), documentSubmissionRequest, "x-user-id", "971532410455890287207953");
    ErrorResponse errorResponse = response.as(ErrorResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    assertThat(errorResponse.getMessage()).contains("Validation failed");
  }

  @Test
  void shouldReturn400StatusCodeIfNoName() {
    DocumentSubmissionRequest documentSubmissionRequest =
        DocumentSubmissionRequest.builder()
            .documents(List.of(Document.builder().name("").build()))
            .build();
    Response response =
        postRequestWithHeader(
            postAttachUrl(), documentSubmissionRequest, "x-user-id", "971532410455890287207953");
    ErrorResponse errorResponse = response.as(ErrorResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    assertThat(errorResponse.getMessage()).contains("Validation failed");
  }

  @Test
  void shouldReturn400StatusCodeIfInvalidSize() {
    DocumentSubmissionRequest documentSubmissionRequest =
        DocumentSubmissionRequest.builder()
            .documents(List.of(Document.builder().size(-1).build()))
            .build();
    Response response =
        postRequestWithHeader(
            postAttachUrl(), documentSubmissionRequest, "x-user-id", "971532410455890287207953");
    ErrorResponse errorResponse = response.as(ErrorResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    assertThat(errorResponse.getMessage()).contains("Validation failed");
  }

  @Test
  void shouldReturn400StatusCodeIfNoDateTime() {
    DocumentSubmissionRequest documentSubmissionRequest =
        DocumentSubmissionRequest.builder()
            .documents(List.of(Document.builder().dateTime("").build()))
            .build();
    Response response =
        postRequestWithHeader(
            postAttachUrl(), documentSubmissionRequest, "x-user-id", "971532410455890287207953");
    ErrorResponse errorResponse = response.as(ErrorResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    assertThat(errorResponse.getMessage()).contains("Validation failed");
  }

  @Test
  void shouldReturn400StatusCodeIfInvalidDateTime() {
    DocumentSubmissionRequest documentSubmissionRequest =
        DocumentSubmissionRequest.builder()
            .documents(List.of(Document.builder().dateTime("!@£$)KDFDS").build()))
            .build();
    Response response =
        postRequestWithHeader(
            postAttachUrl(), documentSubmissionRequest, "x-user-id", "971532410455890287207953");
    ErrorResponse errorResponse = response.as(ErrorResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    assertThat(errorResponse.getMessage()).contains("JSON parse error");
  }

  @Test
  void shouldReturn400StatusCodeIfInvalidDateTimeFormat() {
    DocumentSubmissionRequest documentSubmissionRequest =
        DocumentSubmissionRequest.builder()
            .documents(List.of(Document.builder().dateTime("09-09-2008T14:30").build()))
            .build();
    Response response =
        postRequestWithHeader(
            postAttachUrl(), documentSubmissionRequest, "x-user-id", "971532410455890287207953");
    ErrorResponse errorResponse = response.as(ErrorResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    assertThat(errorResponse.getMessage()).contains("JSON parse error");
  }

  @Test
  void shouldReturn400StatusCodeIfNoDrsDocType() {
    DocumentSubmissionRequest documentSubmissionRequest =
        DocumentSubmissionRequest.builder()
            .documents(List.of(Document.builder().drsDocType("").build()))
            .build();
    Response response =
        postRequestWithHeader(
            postAttachUrl(), documentSubmissionRequest, "x-user-id", "971532410455890287207953");
    ErrorResponse errorResponse = response.as(ErrorResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    assertThat(errorResponse.getMessage()).contains("JSON parse error");
  }

  @Test
  void shouldReturn400StatusCodeIfInvalidDrsDocType() {
    DocumentSubmissionRequest documentSubmissionRequest =
        DocumentSubmissionRequest.builder()
            .documents(List.of(Document.builder().drsDocType("!@£@!DFADFDF:{}").build()))
            .build();
    Response response =
        postRequestWithHeader(
            postAttachUrl(), documentSubmissionRequest, "x-user-id", "971532410455890287207953");
    ErrorResponse errorResponse = response.as(ErrorResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    assertThat(errorResponse.getMessage()).contains("JSON parse error");
  }

  private void setupData() {
    Documentation documentation =
        Documentation.builder()
            .id("68d5408131afc4f728c92152")
            .applicationId("102373758488641693069310")
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
            .applicationId("102373758488641693069310")
            .documentIdIds(
                List.of(DocumentId.builder().documentId("68d5408131afc4f728c92152").build()))
            .build();

    MongoTemplate mongoTemplate = MongoClientConnection.getMongoTemplate();
    mongoTemplate.save(documentation, "document");
    mongoTemplate.save(drsUpload, "drs_upload");
    mongoTemplate.save(submission, "submission");
  }
}
