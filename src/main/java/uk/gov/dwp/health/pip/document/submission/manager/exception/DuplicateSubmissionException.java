package uk.gov.dwp.health.pip.document.submission.manager.exception;

public class DuplicateSubmissionException extends RuntimeException {

  public DuplicateSubmissionException(String msg) {
    super(msg);
  }
}
