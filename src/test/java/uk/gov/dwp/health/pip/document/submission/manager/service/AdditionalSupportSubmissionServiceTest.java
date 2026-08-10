package uk.gov.dwp.health.pip.document.submission.manager.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClientException;
import uk.gov.dwp.health.pip.document.submission.manager.entity.Submission;
import uk.gov.dwp.health.pip.document.submission.manager.event.AdditionalSupportSubmissionProducer;
import uk.gov.dwp.health.pip.document.submission.manager.exception.DuplicateSubmissionException;
import uk.gov.dwp.health.pip.document.submission.manager.repository.SubmissionRepository;
import uk.gov.dwp.health.pip.registration.capture.openapi.v4.api.RegistrationApiClientV4;
import uk.gov.dwp.health.pip.registration.capture.openapi.v4.model.RegistrationDto;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdditionalSupportSubmissionServiceTest {

  @Mock private AdditionalSupportSubmissionProducer additionalSupportSubmissionProducer;
  @Mock private RegistrationApiClientV4 registrationApiClientV4;
  @Mock private SubmissionRepository submissionRepository;
  @InjectMocks private AdditionalSupportSubmissionService additionalSupportSubmissionService;

  @Test
  void submitAdditionalSupportApplication() {
    RegistrationDto registrationDto = new RegistrationDto().effectiveFrom("2025-11-14");

    when(registrationApiClientV4.getRegistrationDataByApplicationId("application-id-1"))
        .thenReturn(registrationDto);
    when(submissionRepository.save(any(Submission.class)))
        .thenReturn(
            Submission.builder()
                .id("submission-id-1")
                .applicationId("application-id-1")
                .claimantId("claimant-id-1")
                .started(LocalDate.of(2025, 11, 14))
                .completed(LocalDate.now())
                .build());

    additionalSupportSubmissionService.submitAdditionalSupportApplication(
        "application-id-1", "claimant-id-1");

    ArgumentCaptor<Submission> submissionArgumentCaptor = ArgumentCaptor.forClass(Submission.class);
    verify(submissionRepository, times(1)).save(submissionArgumentCaptor.capture());
    Submission submission = submissionArgumentCaptor.getValue();
    assertThat(submission.getApplicationId()).isEqualTo("application-id-1");
    assertThat(submission.getClaimantId()).isEqualTo("claimant-id-1");
    assertThat(submission.getStarted()).isEqualTo("2025-11-14");
    assertThat(submission.getCompleted()).isEqualTo(LocalDate.now());

    verify(additionalSupportSubmissionProducer, times(1))
        .sendEvent("application-id-1", "submission-id-1");
  }

  @Test
  void submitAdditionalSupportApplication_whenEffectiveFromNull_shouldSaveSubmission() {
    when(registrationApiClientV4.getRegistrationDataByApplicationId("application-id-1"))
        .thenReturn(new RegistrationDto());
    when(submissionRepository.save(any(Submission.class)))
        .thenReturn(
            Submission.builder()
                .id("submission-id-1")
                .applicationId("application-id-1")
                .claimantId("claimant-id-1")
                .completed(LocalDate.now())
                .build());

    additionalSupportSubmissionService.submitAdditionalSupportApplication(
        "application-id-1", "claimant-id-1");

    ArgumentCaptor<Submission> submissionArgumentCaptor = ArgumentCaptor.forClass(Submission.class);
    verify(submissionRepository, times(1)).save(submissionArgumentCaptor.capture());
    Submission submission = submissionArgumentCaptor.getValue();
    assertThat(submission.getApplicationId()).isEqualTo("application-id-1");
    assertThat(submission.getClaimantId()).isEqualTo("claimant-id-1");
    assertThat(submission.getStarted()).isNull();
    assertThat(submission.getCompleted()).isEqualTo(LocalDate.now());

    verify(additionalSupportSubmissionProducer, times(1))
        .sendEvent("application-id-1", "submission-id-1");
  }

  @Test
  void submitAdditionalSupportApplication_whenSubmissionAlreadyExists_shouldThrowException() {
    when(submissionRepository.findByClaimantIdAndApplicationId("claimant-id-1", "application-id-1"))
        .thenReturn(Optional.ofNullable(Submission.builder().build()));

    assertThatThrownBy(
            () ->
                additionalSupportSubmissionService.submitAdditionalSupportApplication(
                    "application-id-1", "claimant-id-1"))
        .isInstanceOf(DuplicateSubmissionException.class)
        .hasMessage(
            "Submission already exists for claimant id [claimant-id-1] and application id [application-id-1]");
  }

  @Test
  void
      submitAdditionalSupportApplication_whenRegistrationApiThrowsException_shouldThrowException() {
    doThrow(RestClientException.class)
        .when(registrationApiClientV4)
        .getRegistrationDataByApplicationId("application-id-1");

    assertThatThrownBy(
            () ->
                additionalSupportSubmissionService.submitAdditionalSupportApplication(
                    "application-id-1", "claimant-id-1"))
        .isInstanceOf(RestClientException.class);
  }
}
