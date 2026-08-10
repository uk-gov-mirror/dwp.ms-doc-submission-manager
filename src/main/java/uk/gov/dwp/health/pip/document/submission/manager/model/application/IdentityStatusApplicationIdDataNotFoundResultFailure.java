package uk.gov.dwp.health.pip.document.submission.manager.model.application;

public class IdentityStatusApplicationIdDataNotFoundResultFailure extends ResultFailure {
  public IdentityStatusApplicationIdDataNotFoundResultFailure(String applicationId) {
    super("Identity Status with application ID: %s not found.".formatted(
            applicationId));
  }
}
