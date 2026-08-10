package uk.gov.dwp.health.pip.document.submission.manager.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.ObjectMapper;
import uk.gov.dwp.health.pip.document.submission.manager.config.MsIdentityStatusConfig;
import uk.gov.dwp.health.pip.document.submission.manager.config.restclient.model.IdentityDto;
import uk.gov.dwp.health.pip.document.submission.manager.config.restclient.model.IdentityResponse2;
import uk.gov.dwp.health.pip.document.submission.manager.model.application.ExceptionOccurredResultFailure;
import uk.gov.dwp.health.pip.document.submission.manager.model.application.IdentityStatusDataNotFoundResultFailure;
import uk.gov.dwp.health.pip.document.submission.manager.model.application.ResultWrapper;

@Service
@Slf4j
@RequiredArgsConstructor
public class IdentityStatusService {

  private final RestTemplate restTemplate;

  private final ObjectMapper objectMapper;

  private final MsIdentityStatusConfig msIdentityStatusConfig;

  public static final String HEADER_NINO_KEY = "nino";

  public ResultWrapper<IdentityResponse2> getIdentityStatus(
      String nino, String applicationId) {

    log.info("Attempting to call Identity Server with nino");

    var uri = msIdentityStatusConfig.getUri();
    try {
      HttpEntity<String> headers = createNinoHeader(nino);

      ResponseEntity<String> result = restTemplate
          .exchange(uri, HttpMethod.GET, headers,  String.class, nino);

      var data = objectMapper.readValue(result.getBody(),
              IdentityResponse2.class);

      return ResultWrapper
          .<IdentityResponse2>builder()
          .value(data)
          .build();

    } catch (HttpClientErrorException e) {
      log.error(e.getMessage());
      if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
        return ResultWrapper
            .<IdentityResponse2>builder()
            .failure(new IdentityStatusDataNotFoundResultFailure(applicationId))
            .build();
      }
      return ResultWrapper
          .<IdentityResponse2>builder()
          .failure(new ExceptionOccurredResultFailure(e.getMessage(), e.getStackTrace()))
          .build();
    }
  }

  public ResultWrapper<IdentityDto> getIdentityByUserId(String userId) {
    log.info("Attempting to call Identity Server with userId: {}", userId);

    String uri = getByUserIdUri(userId);
    try {
      HttpEntity<String> headers = new HttpEntity<>(new HttpHeaders());

      ResponseEntity<String> result =
          restTemplate.exchange(uri, HttpMethod.GET, headers, String.class);

      var data = objectMapper.readValue(result.getBody(), IdentityDto.class);

      return ResultWrapper.<IdentityDto>builder().value(data).build();

    } catch (HttpClientErrorException e) {
      log.error(e.getMessage());
      if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
        return ResultWrapper.<IdentityDto>builder()
            .failure(new IdentityStatusDataNotFoundResultFailure(userId))
            .build();
      }
      return ResultWrapper.<IdentityDto>builder()
          .failure(new ExceptionOccurredResultFailure(e.getMessage(), e.getStackTrace()))
          .build();
    }
  }

  private HttpEntity<String> createNinoHeader(String nino) {
    HttpHeaders ninoRequestHeaders = new HttpHeaders();
    ninoRequestHeaders.add(HEADER_NINO_KEY, nino);
    return new HttpEntity<>(ninoRequestHeaders);
  }

  private String getByUserIdUri(String userId) {
    return UriComponentsBuilder.fromUriString(msIdentityStatusConfig.getIdentityByUserIdUrl())
        .path(userId)
        .build()
        .toUriString();
  }
}
