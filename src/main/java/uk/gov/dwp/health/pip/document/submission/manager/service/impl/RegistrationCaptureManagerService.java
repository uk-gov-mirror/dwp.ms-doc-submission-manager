package uk.gov.dwp.health.pip.document.submission.manager.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.ObjectMapper;
import uk.gov.dwp.health.pip.document.submission.manager.config.MsRegistrationCaptureManagerConfig;
import uk.gov.dwp.health.pip.document.submission.manager.model.application.ExceptionOccurredResultFailure;
import uk.gov.dwp.health.pip.document.submission.manager.model.application.RegistrationCaptureMgrNotFoundResultFailure;
import uk.gov.dwp.health.pip.document.submission.manager.model.application.ResultWrapper;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.registration.v4.dto.RegistrationDto;

@Service
@Slf4j
@RequiredArgsConstructor
class RegistrationCaptureManagerService {

  private final MsRegistrationCaptureManagerConfig msRegistrationCaptureManagerConfig;
  private final RestTemplate restTemplate;
  private final ObjectMapper objectMapper;

  /**
   * Deprecated.
   *
   * @deprecated (when: 2025-11-24; why: could standardise on using a generated client; refactoring
   *     advice: use the {@link MsRegistrationCaptureManagerConfig#registrationApiClientV4()}
   *     generated client)
   */
  @Deprecated(since = "2025-11-24")
  ResultWrapper<RegistrationDto> getRegistrationData(String applicationId) {
    var registrationDataUri = msRegistrationCaptureManagerConfig.getRegistrationDataUri();
    log.info(
        "Attempting to call Registration Capture Manager with application Id {} on {}",
        applicationId,
        registrationDataUri);
    try {
      ResponseEntity<String> result =
          restTemplate.getForEntity(registrationDataUri, String.class, applicationId);

      var registrationMgrData = objectMapper.readValue(result.getBody(), RegistrationDto.class);

      return ResultWrapper.<RegistrationDto>builder().value(registrationMgrData).build();
    } catch (HttpClientErrorException e) {
      if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
        log.info(e.getMessage());
        return getRegistrationDataNotFoundResult(applicationId);
      }
      log.error(e.getMessage());
      return ResultWrapper.<RegistrationDto>builder()
          .failure(new ExceptionOccurredResultFailure(e.getMessage(), e.getStackTrace()))
          .build();
    }
  }

  private static ResultWrapper<RegistrationDto> getRegistrationDataNotFoundResult(
      final String applicationId) {
    return ResultWrapper.<RegistrationDto>builder()
        .failure(new RegistrationCaptureMgrNotFoundResultFailure(applicationId))
        .build();
  }
}
