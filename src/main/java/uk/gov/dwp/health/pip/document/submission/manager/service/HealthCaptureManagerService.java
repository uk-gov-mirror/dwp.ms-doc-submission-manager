package uk.gov.dwp.health.pip.document.submission.manager.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import uk.gov.dwp.health.pip.document.submission.manager.config.HealthCaptureManagerConfig;
import uk.gov.dwp.health.pip.document.submission.manager.exception.DataRequestException;
import uk.gov.dwp.health.pip.document.submission.manager.model.application.AuditableFormSpecificationDto;
import uk.gov.dwp.health.pip.document.submission.manager.model.application.HealthCaptureApplicationDto;

@Service
@Slf4j
@RequiredArgsConstructor
public class HealthCaptureManagerService {

  private final RestTemplate restTemplate;

  private final HealthCaptureManagerConfig healthCaptureManagerConfig;

  public HealthCaptureApplicationDto getApplicationDtoFromHealthCaptureManager(
      String applicationId) {
    String getApplicationByIdUri = healthCaptureManagerConfig.getHcmApplicationByIdUri();
    getApplicationByIdUri = getApplicationByIdUri.replace("{applicationId}", applicationId);

    ResponseEntity<HealthCaptureApplicationDto> response = restTemplate.getForEntity(
        getApplicationByIdUri, HealthCaptureApplicationDto.class);

    if (response.getBody() == null) {
      throw new DataRequestException(
          "Empty response from Health Capture Manager when getting application DTO");
    }
    return response.getBody();
  }

  public AuditableFormSpecificationDto getFormSpecificationFromHealthCaptureManager(
      String formSpecificationId) {
    String getFormSpecificationByIdUri =
        healthCaptureManagerConfig.getHcmFormSpecificationByIdUri();
    getFormSpecificationByIdUri =
        getFormSpecificationByIdUri.replace("{formSpecificationId}", formSpecificationId);

    ResponseEntity<AuditableFormSpecificationDto> response = restTemplate.getForEntity(
        getFormSpecificationByIdUri, AuditableFormSpecificationDto.class);

    if (response.getBody() == null) {
      throw new DataRequestException(
          "Empty response from Health Capture Manager when getting form specification DTO");
    }
    return response.getBody();
  }
}