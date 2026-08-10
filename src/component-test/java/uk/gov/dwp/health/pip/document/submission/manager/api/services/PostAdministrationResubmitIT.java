package uk.gov.dwp.health.pip.document.submission.manager.api.services;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.dwp.health.pip.document.submission.manager.utils.UrlBuilderUtil.postAdministrationResubmitUrl;

import io.restassured.response.Response;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.HttpStatus;
import uk.gov.dwp.health.pip.document.submission.manager.api.ApiTest;
import uk.gov.dwp.health.pip.document.submission.manager.config.MongoClientConnection;
import uk.gov.dwp.health.pip.document.submission.manager.dto.requests.services.ResubmissionRequest;
import uk.gov.dwp.health.pip.document.submission.manager.dto.requests.submission.DrsMetadata;
import uk.gov.dwp.health.pip.document.submission.manager.dto.responses.ErrorResponse;
import uk.gov.dwp.health.pip.document.submission.manager.dto.responses.submission.RequestIdResponse;
import uk.gov.dwp.health.pip.document.submission.manager.entity.DocumentId;
import uk.gov.dwp.health.pip.document.submission.manager.entity.Documentation;
import uk.gov.dwp.health.pip.document.submission.manager.entity.DrsUpload;
import uk.gov.dwp.health.pip.document.submission.manager.entity.Storage;
import uk.gov.dwp.health.pip.document.submission.manager.entity.Submission;

@Slf4j
class PostAdministrationResubmitIT extends ApiTest {

  @BeforeEach
  public void testSetup() {
    MongoClientConnection.emptyMongoCollections();
    setupData();
  }
  
  @Test
  void shouldReturn202StatusCode() {
    RequestIdResponse requestIdResponse = new RequestIdResponse();
    requestIdResponse.setRequestId("68d534b231afc4f728c92142");
    ResubmissionRequest resubmissionRequest =
        ResubmissionRequest.builder()
            .region("GB")
            .drsRequestIdResponses(List.of(requestIdResponse))
            .drsMetadata(
                DrsMetadata.builder()
                    .surname("Khanzzz")
                    .forename("Hamzazzz")
                    .dob("1990-01-20")
                    .nino("AA370773A")
                    .postcode("LS2 7UA")
                    .build())
            .build();

    Response response = postRequest(postAdministrationResubmitUrl(), resubmissionRequest);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED.value());
  }

  @Test
  void shouldReturn400StatusCodeForInvalidJson() {
    Response response = postRequest(postAdministrationResubmitUrl(), "}");
    ErrorResponse errorResponse = response.as(ErrorResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    assertThat(errorResponse.getMessage()).contains("JSON parse error");
  }

  @Test
  void shouldReturn404StatusCodeForInvalidDRSRequestId() {
    RequestIdResponse requestIdResponse = new RequestIdResponse();
    requestIdResponse.setRequestId("abc");
    List<RequestIdResponse> requestIdResponses = new ArrayList<>();
    requestIdResponses.add(requestIdResponse);

    ResubmissionRequest resubmissionRequest =
        ResubmissionRequest.builder().drsRequestIdResponses(requestIdResponses).build();
    Response response = postRequest(postAdministrationResubmitUrl(), resubmissionRequest);
    ErrorResponse errorResponse = response.as(ErrorResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND.value());
    assertThat(errorResponse.getMessage())
        .contains("Failed DRS request audit does not exist - abc");
  }

  private void setupData() {
    Documentation documentation1 =
        Documentation.builder()
            .id("68d534b231afc4f728c92140")
            .applicationId("102373758488641693069310")
            .claimantId("851e592e7183a47c0db3108d")
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
    Documentation documentation2 =
        Documentation.builder()
            .id("68d534b231afc4f728c92143")
            .applicationId("102373758488641693069310")
            .claimantId("851e592e7183a47c0db3108d")
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

    DrsUpload drsUpload1 =
        DrsUpload.builder()
            .id("68d534b231afc4f728c92142")
            .submissionId("68d534b231afc4f728c92141")
            .documentIdIds(
                List.of(DocumentId.builder().documentId("68d534b231afc4f728c92140").build()))
            .status("PUBLISHED")
            .build();
    DrsUpload drsUpload2 =
        DrsUpload.builder()
            .id("68d534b231afc4f728c92144")
            .submissionId("68d534b231afc4f728c92141")
            .documentIdIds(
                List.of(DocumentId.builder().documentId("68d534b231afc4f728c92143").build()))
            .status("PUBLISHED")
            .build();

    Submission submission =
        Submission.builder()
            .id("68d534b231afc4f728c92141")
            .claimantId("851e592e7183a47c0db3108d")
            .applicationId("102373758488641693069310")
            .documentIdIds(
                List.of(
                    DocumentId.builder().documentId("68d534b231afc4f728c92140").build(),
                    DocumentId.builder().documentId("68d534b231afc4f728c92143").build()))
            .build();

    MongoTemplate mongoTemplate = MongoClientConnection.getMongoTemplate();
    mongoTemplate.save(documentation1, "document");
    mongoTemplate.save(documentation2, "document");
    mongoTemplate.save(drsUpload1, "drs_upload");
    mongoTemplate.save(drsUpload2, "drs_upload");
    mongoTemplate.save(submission, "submission");
  }
}
