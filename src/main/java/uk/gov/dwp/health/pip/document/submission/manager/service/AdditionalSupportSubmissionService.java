package uk.gov.dwp.health.pip.document.submission.manager.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import uk.gov.dwp.health.pip.document.submission.manager.entity.Submission;
import uk.gov.dwp.health.pip.document.submission.manager.event.AdditionalSupportSubmissionProducer;
import uk.gov.dwp.health.pip.document.submission.manager.exception.DuplicateSubmissionException;
import uk.gov.dwp.health.pip.document.submission.manager.repository.SubmissionRepository;
import uk.gov.dwp.health.pip.registration.capture.openapi.v4.api.RegistrationApiClientV4;
import uk.gov.dwp.health.pip.registration.capture.openapi.v4.model.RegistrationDto;

import java.time.LocalDate;
import java.util.Optional;

import static java.lang.String.format;

@RequiredArgsConstructor
@Service
@Slf4j
public class AdditionalSupportSubmissionService {

  private final AdditionalSupportSubmissionProducer additionalSupportSubmissionProducer;
  private final RegistrationApiClientV4 registrationApiClientV4;
  private final SubmissionRepository submissionRepository;

  public void submitAdditionalSupportApplication(String applicationId, String claimantId) {
    log.info("Submitting additional support application for applicationId: {}", applicationId);

    checkForExistingSubmission(applicationId, claimantId);
    RegistrationDto registrationDto = getRegistrationData(applicationId);
    Submission submission = buildSubmission(applicationId, claimantId, registrationDto);
    Submission savedSubmission = submissionRepository.save(submission);
    additionalSupportSubmissionProducer.sendEvent(applicationId, savedSubmission.getId());

    log.info("Submitted additional support application for applicationId: {}", applicationId);
  }

  private void checkForExistingSubmission(String applicationId, String claimantId) {
    Optional<Submission> submission =
        submissionRepository.findByClaimantIdAndApplicationId(claimantId, applicationId);

    if (submission.isPresent()) {
      String message =
          format(
              "Submission already exists for claimant id [%s] and application id [%s]",
              claimantId, applicationId);
      log.warn(message);
      throw new DuplicateSubmissionException(message);
    }
  }

  private RegistrationDto getRegistrationData(String applicationId) {
    try {
      return registrationApiClientV4.getRegistrationDataByApplicationId(applicationId);
    } catch (RestClientException exception) {
      log.error(
          "Failed to get registration data for applicationId: {}. Exception: {}",
          applicationId,
          exception.getMessage());
      throw exception;
    }
  }

  private Submission buildSubmission(
      String applicationId, String claimantId, RegistrationDto registrationDto) {
    LocalDate effectiveFrom =
        registrationDto.getEffectiveFrom() != null
            ? LocalDate.parse(registrationDto.getEffectiveFrom())
            : null;

    return Submission.builder()
        .applicationId(applicationId)
        .claimantId(claimantId)
        .started(effectiveFrom)
        .completed(LocalDate.now())
        .build();
  }
}
