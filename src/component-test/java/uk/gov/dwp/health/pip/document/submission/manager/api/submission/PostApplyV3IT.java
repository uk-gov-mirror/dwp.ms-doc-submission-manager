package uk.gov.dwp.health.pip.document.submission.manager.api.submission;

import io.restassured.response.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import uk.gov.dwp.health.pip.document.submission.manager.api.ApiTest;
import uk.gov.dwp.health.pip.document.submission.manager.config.MongoClientConnection;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.healthinformation.v2.dto.HealthCaptureApplicationDtoV2;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.healthinformation.v2.dto.PersonalDetailsDto;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.v3.model.ApplicationIdDto;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.dwp.health.pip.document.submission.manager.utils.UrlBuilderUtil.postApplyUrlV3;

class PostApplyV3IT extends ApiTest {

  private static final String USER_ID = "102957750860208822019290";

  private static final String BATCH_DOC_QUEUE_URL =
      getEnv("BATCH_DOC_QUEUE_URL", "http://localhost:4566/000000000000/docbatch-batch-upload");

  @BeforeEach
  void beforeEach() {
    MongoClientConnection.emptyMongoCollections();
    messageUtil.clearQueue(BATCH_DOC_QUEUE_URL);
  }

  @Test
  void shouldHaveSubmissionInTheDbWhenSubmissionRequestReceived() {
    String applicationId = "000000000000000000000001";
    ApplicationIdDto applicationIdDto = new ApplicationIdDto().applicationId(applicationId);

    Response response =
        postRequestWithHeader(postApplyUrlV3(), applicationIdDto, "x-user-id", USER_ID);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED.value());

    HealthCaptureApplicationDtoV2 healthCaptureApplicationDtoResponse =
        response.as(HealthCaptureApplicationDtoV2.class);
    assertThat(healthCaptureApplicationDtoResponse.getSubmissionId()).matches("^[a-zA-Z0-9]{24}$");

    Assertions.assertEquals(1, getMongoDbCountByApplicationId(applicationId));
    Assertions.assertEquals("1", messageUtil.getMessageCount(BATCH_DOC_QUEUE_URL));
  }

  @Test
  void when_registration_capture_manager_returns_unparsable_response() {
    String applicationId = "rcmresponseunparsable123";
    ApplicationIdDto applicationIdDto = new ApplicationIdDto().applicationId(applicationId);

    Response response =
        postRequestWithHeader(postApplyUrlV3(), applicationIdDto, "x-user-id", USER_ID);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
  }

  @Test
  void
      givenHealthCaptureManagerApplicationExistsWithPersonalDetailsPopulated_whenSubmissionFlowIsTriggered_thenHealthCaptureApplicationIsReturnedWithPersonalDetailsPopulated() {
    String applicationId = "707f1f77bcf86cd659439021";
    ApplicationIdDto applicationIdDto = new ApplicationIdDto().applicationId(applicationId);

    Response response =
        postRequestWithHeader(postApplyUrlV3(), applicationIdDto, "x-user-id", USER_ID);
    HealthCaptureApplicationDtoV2 healthCaptureApplicationDtoResponse =
        response.as(HealthCaptureApplicationDtoV2.class);
    PersonalDetailsDto personalDetails =
        healthCaptureApplicationDtoResponse.getRegistrationDetails().getPersonalDetails();

    Assertions.assertEquals("citizen@email.com", personalDetails.getEmail());
    Assertions.assertEquals("AB123456C", personalDetails.getNationalInsuranceNumber());
    Assertions.assertEquals("2000-01-01", personalDetails.getDateOfBirth());
    Assertions.assertEquals("Smith", personalDetails.getSurname());
    Assertions.assertEquals("Ruth", personalDetails.getFirstName());
    Assertions.assertEquals("AB123CD", personalDetails.getPostcode());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED.value());
    assertThat(healthCaptureApplicationDtoResponse.getSubmissionId()).matches("^[a-zA-Z0-9]{24}$");
  }

  @Test
  void
      givenThereIsAnAccountInRegistrationCaptureManager_whenSubmissionFlowIsTriggered_thenHealthCaptureApplicationIsReturnedWithPersonalDetailsPopulated() {
    String applicationId = "88e3e1d04f99b152240e72e0";
    ApplicationIdDto applicationIdDto = new ApplicationIdDto().applicationId(applicationId);

    Response response =
        postRequestWithHeader(postApplyUrlV3(), applicationIdDto, "x-user-id", USER_ID);
    HealthCaptureApplicationDtoV2 healthCaptureApplicationDtoResponse =
        response.as(HealthCaptureApplicationDtoV2.class);
    PersonalDetailsDto personalDetails =
        healthCaptureApplicationDtoResponse.getRegistrationDetails().getPersonalDetails();

    Assertions.assertEquals("RN000020D", personalDetails.getNationalInsuranceNumber());
    Assertions.assertEquals("2000-01-01", personalDetails.getDateOfBirth());
    Assertions.assertEquals("Smith", personalDetails.getSurname());
    Assertions.assertEquals("Jill", personalDetails.getFirstName());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED.value());
    assertThat(healthCaptureApplicationDtoResponse.getSubmissionId()).matches("^[a-zA-Z0-9]{24}$");
  }

  @Test
  void when_application_submit_to_coordinator_returns_502_504_response_and_retry_fails() {
    String applicationId = "000000000000000000000002";
    ApplicationIdDto applicationIdDto = new ApplicationIdDto().applicationId(applicationId);

    Response response =
        postRequestWithHeader(postApplyUrlV3(), applicationIdDto, "x-user-id", USER_ID);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
  }

  private int getMongoDbCountByApplicationId(String applicationId) {
    Query query = new Query();
    query.addCriteria(Criteria.where("applicationId").is(applicationId));
    return (int)
        MongoClientConnection.getMongoTemplate()
            .count(query, getEnv("SUBMISSION_COLLECTION", "submission"));
  }
}
