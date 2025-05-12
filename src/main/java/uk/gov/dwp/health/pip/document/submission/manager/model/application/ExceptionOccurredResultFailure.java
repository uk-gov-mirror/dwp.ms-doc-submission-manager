package uk.gov.dwp.health.pip.document.submission.manager.model.application;

import lombok.Getter;

public class ExceptionOccurredResultFailure extends ResultFailure {

  public ExceptionOccurredResultFailure(String failureReason, StackTraceElement[] stackTrace) {
    super(failureReason);
    this.stackTrace = stackTrace;
  }

  @Getter
  private final StackTraceElement[] stackTrace;
}
