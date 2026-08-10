package uk.gov.dwp.health.pip.document.submission.manager.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.ObjectMapper;
import uk.gov.dwp.health.pip.document.submission.manager.config.MsIdentityStatusConfig;
import uk.gov.dwp.health.pip.document.submission.manager.config.restclient.model.IdentityDto;
import uk.gov.dwp.health.pip.document.submission.manager.model.application.ResultWrapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdentityStatusServiceTest {

  private static final String USER_ID = "user123";
  private static final String BASE_URI = "http://identity-service/v1/";
  private static final String FULL_GET_BY_ID_URI = BASE_URI + "get-identity-by-id/" + USER_ID;
  private static final String RESPONSE_JSON =
      """
      {
        "subjectId": "test@example.com",
        "nino": "AB123456C",
        "applicationId": "app123",
        "idvStatus": "VERIFIED"
      }
      """;

  @Mock private RestTemplate restTemplate;
  @Mock private ObjectMapper objectMapper;
  @Mock private MsIdentityStatusConfig msIdentityStatusConfig;
  @InjectMocks private IdentityStatusService identityStatusService;

  @Test
  @DisplayName("Should return IdentityDto when successful response from get identity by id")
  void successfulResponse() {
    IdentityDto expectedIdentityDto = new IdentityDto();
    expectedIdentityDto.setSubjectId("test@example.com");
    expectedIdentityDto.setNino("AB123456C");
    expectedIdentityDto.setApplicationId("app123");

    ResponseEntity<String> responseEntity = new ResponseEntity<>(RESPONSE_JSON, HttpStatus.OK);

    when(msIdentityStatusConfig.getIdentityByUserIdUrl()).thenReturn(BASE_URI + "get-identity-by-id/");
    when(restTemplate.exchange(
            eq(FULL_GET_BY_ID_URI), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
        .thenReturn(responseEntity);
    when(objectMapper.readValue(RESPONSE_JSON, IdentityDto.class)).thenReturn(expectedIdentityDto);

    ResultWrapper<IdentityDto> result = identityStatusService.getIdentityByUserId(USER_ID);

    assertNotNull(result);
    assertNotNull(result.getValue());
    assertTrue(result.getFailures().isEmpty());
    assertEquals(expectedIdentityDto.getSubjectId(), result.getValue().getSubjectId());
    assertEquals(expectedIdentityDto.getNino(), result.getValue().getNino());
    assertEquals(expectedIdentityDto.getApplicationId(), result.getValue().getApplicationId());
  }
}
