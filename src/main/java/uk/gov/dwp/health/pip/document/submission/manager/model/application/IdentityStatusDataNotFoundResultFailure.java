package uk.gov.dwp.health.pip.document.submission.manager.model.application;

public class IdentityStatusDataNotFoundResultFailure extends ResultFailure {

  public IdentityStatusDataNotFoundResultFailure(String applicationId) {
    super("Identity Status for application with ID: %s not found.".formatted(
        applicationId));
  }
}
