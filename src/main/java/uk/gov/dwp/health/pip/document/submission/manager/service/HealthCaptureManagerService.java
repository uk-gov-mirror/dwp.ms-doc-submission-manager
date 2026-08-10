package uk.gov.dwp.health.pip.document.submission.manager.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import uk.gov.dwp.health.pip.document.submission.manager.config.HealthCaptureManagerConfig;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.healthinformation.v2.dto.HealthCaptureApplicationDtoV2;

@Service
@Slf4j
@RequiredArgsConstructor
public class HealthCaptureManagerService {

  private final RestTemplate restTemplate;

  private final HealthCaptureManagerConfig healthCaptureManagerConfig;

  public HealthCaptureApplicationDtoV2 getApplicationDtoV2FromHealthCaptureManager(
      String applicationId) {
    String getApplicationByIdUri = healthCaptureManagerConfig.getHcmApplicationByIdV2Uri();
    getApplicationByIdUri = getApplicationByIdUri.replace("{application_id}", applicationId);

    ResponseEntity<HealthCaptureApplicationDtoV2> response =
        restTemplate.getForEntity(getApplicationByIdUri, HealthCaptureApplicationDtoV2.class);

    return response.getBody();
  }
}
