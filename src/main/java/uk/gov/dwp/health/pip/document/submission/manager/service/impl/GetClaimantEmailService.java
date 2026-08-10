package uk.gov.dwp.health.pip.document.submission.manager.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.dwp.health.pip.document.submission.manager.config.restclient.model.V7AccountDetails;
import uk.gov.dwp.health.pip.document.submission.manager.openapi.healthinformation.v2.dto.HealthCaptureApplicationDtoV2;
import uk.gov.dwp.health.pip.document.submission.manager.service.IdentityStatusService;

import static org.springframework.util.StringUtils.hasLength;

@Component
@RequiredArgsConstructor
@Slf4j
class GetClaimantEmailService {

  private final IdentityStatusService identityStatusService;

  String getClaimantEmail(
      final V7AccountDetails account,
      final HealthCaptureApplicationDtoV2 healthCaptureApplicationDtoV2) {
    String applicationId = healthCaptureApplicationDtoV2.getApplicationId();

    var registrationDetailsDto = healthCaptureApplicationDtoV2.getRegistrationDetails();
    if (registrationDetailsDto == null) {
      log.error("Failed to get an email for application {}", applicationId);
      return null;
    }

    var personDetailsDto = registrationDetailsDto.getPersonalDetails();
    if (personDetailsDto == null) {
      log.error("Failed to get an email for application {}", applicationId);
      return null;
    }

    String nino = registrationDetailsDto.getPersonalDetails().getNationalInsuranceNumber();

    //this is the line I have to change, once this is done with identityId then all good!
    // get by userId
    String email = getEmailFromIdentityService(nino, applicationId);
    if (email == null && account != null) {
      email =
          getEmailFromAccount(
              applicationId, healthCaptureApplicationDtoV2.getClaimantId(), account);
    }

    if (email != null) {
      log.info("Successfully found an email for application {}", applicationId);
      return email;
    }

    log.error("Failed to get an email for application {}", applicationId);
    return null;
  }

  private String getEmailFromIdentityService(String nino, String applicationId) {
    log.info("Attempting to call Identity Server with applicationId: {}", applicationId);

    if (nino == null) {
      log.info("Nino not provided from health application for application {}",
              applicationId);
      return null;
    }

    var identity = identityStatusService.getIdentityStatus(nino, applicationId);

    if (identity.isSuccess()) {
      log.info("Successful call to Identity Server for applicationId: {}", applicationId);
      return identity.getValue().getSubjectId();
    } else {
      log.info("Call to, Identity Server failed for applicationId: {}. Failure reason: {}",
              applicationId, identity.getFailures().get(0).getFailureReason());
      return null;
    }
  }

  String getEmailFromIdentityByUserId(String userId) {
    log.info("Attempting to call Identity Server with userId: {}", userId);

    var identity = identityStatusService.getIdentityByUserId(userId);

    if (identity.isSuccess()) {
      log.info("Successful call to Identity Server for userId: {}", userId);
      return identity.getValue().getSubjectId();
    } else {
      log.info(
          "Call to, Identity Server failed for userId: {}. Failure reason: {}",
          userId,
          identity.getFailures().get(0).getFailureReason());
      return null;
    }
  }

  private String getEmailFromAccount(String applicationId, String claimantId,
      V7AccountDetails account) {

    log.info("Attempting to retrieved email from Account Manager account response"
        + " for application {} with claimant Id {}", applicationId, claimantId);

    String email = account.getEmail();

    if (hasLength(email)) {
      log.info(
          "Successfully retrieved email from Account Manager account response for application {}",
          applicationId);
      return email;
    }

    return null;
  }
}
