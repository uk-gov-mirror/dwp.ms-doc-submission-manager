package uk.gov.dwp.health.pip.document.submission.manager.exception;

import uk.gov.dwp.health.pip.document.submission.manager.model.application.ResultFailure;

public class ApplicationNotFoundFailure extends ResultFailure {

  public ApplicationNotFoundFailure(String healthCaptureApplicationId) {
    super("Health capture application with ID: %s not found.".formatted(
        healthCaptureApplicationId));
  }
}
