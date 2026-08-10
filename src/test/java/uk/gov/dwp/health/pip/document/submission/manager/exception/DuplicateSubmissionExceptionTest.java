package uk.gov.dwp.health.pip.document.submission.manager.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DuplicateSubmissionExceptionTest {

  @Test
  void testCreateDuplicateException() {
    DuplicateSubmissionException cut = new DuplicateSubmissionException("submission already exist");
    assertThat(cut.getMessage()).isEqualTo("submission already exist");
  }
}
