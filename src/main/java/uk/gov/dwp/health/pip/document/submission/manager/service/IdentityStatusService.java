package uk.gov.dwp.health.pip.document.submission.manager.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import uk.gov.dwp.health.pip.document.submission.manager.config.MsIdentityStatusConfig;
import uk.gov.dwp.health.pip.document.submission.manager.config.restclient.model.IdentityDto;
import uk.gov.dwp.health.pip.document.submission.manager.model.application.ExceptionOccurredResultFailure;
import uk.gov.dwp.health.pip.document.submission.manager.model.application.IdentityStatusDataNotFoundResultFailure;
import uk.gov.dwp.health.pip.document.submission.manager.model.application.ResultWrapper;

@Service
@Slf4j
@RequiredArgsConstructor
public class IdentityStatusService {

  private final RestTemplate restTemplate;

  private final ObjectMapper objectMapper;

  private final MsIdentityStatusConfig msApplicationConfig;

  public ResultWrapper<IdentityDto> getIdentityStatus(
      String applicationId)
      throws JsonProcessingException {

    log.info("Attempting to call Identity Server with application Id %s".formatted(
        applicationId));
    
    var uri = msApplicationConfig.getUri();

    try {
      ResponseEntity<String> result = restTemplate
          .getForEntity(uri, String.class, applicationId);

      var data = objectMapper.readValue(result.getBody(),
          IdentityDto.class);

      return ResultWrapper
          .<IdentityDto>builder()
          .value(data)
          .build();

    } catch (HttpClientErrorException e) {
      log.error(e.getMessage());
      if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
        return ResultWrapper
            .<IdentityDto>builder()
            .failure(new IdentityStatusDataNotFoundResultFailure(applicationId))
            .build();
      }
      return ResultWrapper
          .<IdentityDto>builder()
          .failure(new ExceptionOccurredResultFailure(e.getMessage(), e.getStackTrace()))
          .build();
    }
  }
}