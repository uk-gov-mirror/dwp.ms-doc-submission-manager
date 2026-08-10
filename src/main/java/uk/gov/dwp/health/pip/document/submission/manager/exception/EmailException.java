package uk.gov.dwp.health.pip.document.submission.manager.exception;

public class EmailException extends RuntimeException {

  public EmailException(String message) {
    super(message);
  }

  public EmailException(String message, Throwable cause) {
    super(message, cause);
  }
}
