package uk.gov.dwp.health.pip.document.submission.manager.exception;

public class V3SubmissionServiceException extends RuntimeException {

  public V3SubmissionServiceException(String message, Throwable cause) {
    super(message, cause);
  }

  public V3SubmissionServiceException(String message) {
    super(message);
  }
}
