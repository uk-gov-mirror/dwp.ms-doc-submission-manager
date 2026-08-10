package uk.gov.dwp.health.pip.document.submission.manager.model.application;

public class IdentityStatusDataNotFoundResultFailure extends ResultFailure {
  public IdentityStatusDataNotFoundResultFailure(String userId) {
    super("Identity Status with userId: %s not found.".formatted(
            userId));
  }
}
