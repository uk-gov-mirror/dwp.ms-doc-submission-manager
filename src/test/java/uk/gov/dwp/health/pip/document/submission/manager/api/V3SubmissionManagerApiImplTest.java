package uk.gov.dwp.health.pip.document.submission.manager.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.healthinformation.v2.dto.HealthCaptureApplicationDtoV2;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.v3.model.ApplicationIdDto;
import uk.gov.dwp.health.pip.document.submission.manager.service.impl.V3SubmissionServiceImpl;

@ExtendWith(MockitoExtension.class)
class V3SubmissionManagerApiImplTest {

  @Mock private V3SubmissionServiceImpl v3SubmissionService;
  @InjectMocks private V3SubmissionManagerApiImpl v3SubmissionManagerApi;

  private static final String USER_ID = "66f51d95d15f2c7ce0b9dd17";

  @Test
  void applyPIP() {
    when(v3SubmissionService.createNewSubmission("application-id-1", USER_ID))
        .thenReturn(new HealthCaptureApplicationDtoV2());

    ApplicationIdDto applicationIdDto = new ApplicationIdDto().applicationId("application-id-1");

    ResponseEntity<Object> responseEntity =
        v3SubmissionManagerApi.applyPIP(USER_ID, applicationIdDto);

    assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
    assertThat(responseEntity.getBody()).isNotNull();
  }
}
