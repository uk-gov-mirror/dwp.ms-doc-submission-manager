package uk.gov.dwp.health.pip.document.submission.manager.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.dwp.health.pip.document.submission.manager.config.restclient.model.IdentityDto;
import uk.gov.dwp.health.pip.document.submission.manager.config.restclient.model.IdentityResponse2;
import uk.gov.dwp.health.pip.document.submission.manager.config.restclient.model.V7AccountDetails;
import uk.gov.dwp.health.pip.document.submission.manager.model.application.IdentityStatusDataNotFoundResultFailure;
import uk.gov.dwp.health.pip.document.submission.manager.model.application.ResultWrapper;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.healthinformation.v2.dto.HealthCaptureApplicationDtoV2;
import uk.gov.dwp.health.pip.document.submission.manager.service.IdentityStatusService;
import uk.gov.dwp.health.pip.document.submission.manager.utils.JsonUtils;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetClaimantEmailServiceTest {

  private final String NINO = "AB123456C";
  private final String APPLICATION_ID = "123456789";
  private final String USER_ID = "user123";

  @Mock private IdentityStatusService identityStatusService;

  private GetClaimantEmailService getClaimantEmailService;

  @BeforeEach
  void beforeEach() {
    getClaimantEmailService = new GetClaimantEmailService(identityStatusService);
  }

  @Test
  void getClaimantEmailV2_getEmailFromAccountManager_successfullySetsEmail() throws IOException {
    V7AccountDetails account =
        JsonUtils.readJsonFromFileAndMap(
            "src/test/resources/entity/dto/accountDetailsResponse.json", V7AccountDetails.class);
    HealthCaptureApplicationDtoV2 healthCaptureApplicationDtoV2 =
        JsonUtils.readJsonFromFileAndMap(
            "src/test/resources/entity/dto/getV2HealthDataResponseBody.json",
            HealthCaptureApplicationDtoV2.class);

    when(identityStatusService.getIdentityStatus(NINO, APPLICATION_ID))
        .thenReturn(
            ResultWrapper.<IdentityResponse2>builder()
                .failure(new IdentityStatusDataNotFoundResultFailure(APPLICATION_ID))
                .build());

    String emailResult =
        getClaimantEmailService.getClaimantEmail(account, healthCaptureApplicationDtoV2);

    assertEquals(emailResult, account.getEmail());
  }

  @Test
  void getClaimantEmailV2_getEmailFromAccountManager_failsToSetEmail() throws IOException {
    V7AccountDetails account = null;
    HealthCaptureApplicationDtoV2 healthCaptureApplicationDtoV2 =
        JsonUtils.readJsonFromFileAndMap(
            "src/test/resources/entity/dto/getV2HealthDataResponseBody.json",
            HealthCaptureApplicationDtoV2.class);

    when(identityStatusService.getIdentityStatus(NINO, APPLICATION_ID))
        .thenReturn(
            ResultWrapper.<IdentityResponse2>builder()
                .failure(new IdentityStatusDataNotFoundResultFailure(APPLICATION_ID))
                .build());

    String emailResult =
        getClaimantEmailService.getClaimantEmail(account, healthCaptureApplicationDtoV2);

    assertNull(emailResult);
  }

  @Test
  void getClaimantEmailV2_getEmailFromIdentityService_successfullySetsEmail() throws IOException {
    V7AccountDetails account = null;
    HealthCaptureApplicationDtoV2 healthCaptureApplicationDtoV2 =
        JsonUtils.readJsonFromFileAndMap(
            "src/test/resources/entity/dto/getV2HealthDataResponseBody.json",
            HealthCaptureApplicationDtoV2.class);

    IdentityResponse2 identityDto =
        JsonUtils.readJsonFromFileAndMap(
            "src/test/resources/entity/dto/identityStatusResponse.json", IdentityResponse2.class);

    when(identityStatusService.getIdentityStatus(NINO, APPLICATION_ID))
        .thenReturn(ResultWrapper.<IdentityResponse2>builder().value(identityDto).build());

    String emailResult =
        getClaimantEmailService.getClaimantEmail(account, healthCaptureApplicationDtoV2);

    assertEquals(emailResult, identityDto.getSubjectId());
  }

  @Test
  void getEmailFromIdentityByUserId_successfullyReturnsEmail() throws IOException {
    String expectedEmail = "test@example.com";
    IdentityDto identityDto = new IdentityDto();
    identityDto.setSubjectId(expectedEmail);

    when(identityStatusService.getIdentityByUserId(USER_ID))
        .thenReturn(ResultWrapper.<IdentityDto>builder().value(identityDto).build());

    String result = getClaimantEmailService.getEmailFromIdentityByUserId(USER_ID);

    assertEquals(expectedEmail, result);
  }

  @Test
  void getEmailFromIdentityByUserId_identityNotFound_returnsNull() throws IOException {
    when(identityStatusService.getIdentityByUserId(USER_ID))
        .thenReturn(
            ResultWrapper.<IdentityDto>builder()
                .failure(new IdentityStatusDataNotFoundResultFailure(USER_ID))
                .build());

    String result = getClaimantEmailService.getEmailFromIdentityByUserId(USER_ID);

    assertNull(result);
  }
}
