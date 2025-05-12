package uk.gov.dwp.health.pip.document.submission.manager.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import uk.gov.dwp.health.pip.document.submission.manager.config.MsAccountConfig;
import uk.gov.dwp.health.pip.document.submission.manager.config.restclient.model.V7AccountDetails;
import uk.gov.dwp.health.pip.document.submission.manager.model.application.AccountMgrDataNotFoundResultFailure;
import uk.gov.dwp.health.pip.document.submission.manager.model.application.ExceptionOccurredResultFailure;
import uk.gov.dwp.health.pip.document.submission.manager.model.application.ResultWrapper;
import uk.gov.dwp.health.pip.document.submission.manager.utils.JsonUtils;

@Service
@Slf4j
@RequiredArgsConstructor
public class AccountManagerService {

  private final MsAccountConfig msAccountConfig;
  private final RestTemplate restTemplate;
  private final ObjectMapper objectMapper;

  public ResultWrapper<V7AccountDetails> getAccountMgrData(String claimantId)
      throws JsonProcessingException {

    var accountMgrUri = msAccountConfig.getAccountMgrDataUri();

    try {
      log.info(
          "Attempting to call Account Manager with claimant Id {} on {}",
          claimantId, accountMgrUri
      );
      ResponseEntity<String> result = restTemplate
          .getForEntity(accountMgrUri, String.class, claimantId);

      objectMapper.registerModule(new JavaTimeModule());

      final Object[] responseBodyObject = objectMapper.readValue(result.getBody(), Object[].class);
      if (responseBodyObject == null || responseBodyObject.length == 0) {
        return getAccountDataNotFoundResult(claimantId);
      }
      var response = JsonUtils.mapToJson(responseBodyObject[0]);

      var accountMgrData = objectMapper.readValue(response,
          V7AccountDetails.class);

      return ResultWrapper
          .<V7AccountDetails>builder()
          .value(accountMgrData)
          .build();
    } catch (HttpClientErrorException e) {
      log.error(e.getMessage());
      if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
        return getAccountDataNotFoundResult(claimantId);
      }
      return ResultWrapper
          .<V7AccountDetails>builder()
          .failure(new ExceptionOccurredResultFailure(e.getMessage(), e.getStackTrace()))
          .build();
    }
  }

  private static ResultWrapper<V7AccountDetails> getAccountDataNotFoundResult(
      final String claimantId
  ) {
    return ResultWrapper
        .<V7AccountDetails>builder()
        .failure(new AccountMgrDataNotFoundResultFailure(claimantId))
        .build();
  }
}
