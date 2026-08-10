package uk.gov.dwp.health.pip.document.submission.manager.model.application;

public class RegistrationCaptureMgrNotFoundResultFailure extends ResultFailure {

  public RegistrationCaptureMgrNotFoundResultFailure(String applicationId) {
    super("Data for application with ID: %s not found in Registration Capture Manager.".formatted(
        applicationId));
  }
}