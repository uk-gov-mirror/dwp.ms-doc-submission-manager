package uk.gov.dwp.health.pip.document.submission.manager.model.application;

public class AccountMgrDataNotFoundResultFailure extends ResultFailure {

  public AccountMgrDataNotFoundResultFailure(String claimantId) {
    super("Data for claimant with ID: %s not found in Account Manager.".formatted(
        claimantId));
  }
}