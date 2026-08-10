package uk.gov.dwp.health.pip.document.submission.manager.model.application;

public class ApplicationCoordinatorNotFoundResultFailure extends ResultFailure {

  public ApplicationCoordinatorNotFoundResultFailure(String applicationId) {
    super("Data for application with ID: %s not found in Application Coordinator.".formatted(
            applicationId));
  }
}