package uk.gov.dwp.health.pip.document.submission.manager.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import uk.gov.dwp.health.pip.document.submission.manager.config.HealthCaptureManagerConfig;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.healthinformation.v2.dto.HealthCaptureApplicationDtoV2;

@ExtendWith(MockitoExtension.class)
class HealthCaptureManagerServiceTest {

  @Mock private RestTemplate restTemplate;
  @Mock private HealthCaptureManagerConfig healthCaptureManagerConfig;
  @InjectMocks private HealthCaptureManagerService healthCaptureManagerService;

  @Test
  void getApplicationDtoV2FromHealthCaptureManager() {
    when(healthCaptureManagerConfig.getHcmApplicationByIdV2Uri())
        .thenReturn("host/endpoint/{application_id}");
    when(restTemplate.getForEntity(
            "host/endpoint/application-id-1", HealthCaptureApplicationDtoV2.class))
        .thenReturn(
            new ResponseEntity<>(
                new HealthCaptureApplicationDtoV2().applicationId("application-id-1"),
                HttpStatus.OK));

    HealthCaptureApplicationDtoV2 applicationDtoV2FromHealthCaptureManager =
        healthCaptureManagerService.getApplicationDtoV2FromHealthCaptureManager("application-id-1");

    assertThat(applicationDtoV2FromHealthCaptureManager.getApplicationId())
        .isEqualTo("application-id-1");
  }
}
