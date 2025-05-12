package uk.gov.dwp.health.pip.document.submission.manager.exception;

public class PdfGenerationException extends RuntimeException {
  public PdfGenerationException(String msg) {
    super(msg);
  }

  public PdfGenerationException(String message, Throwable cause) {
    super(message, cause);
  }
}
