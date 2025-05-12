package uk.gov.dwp.health.pip.document.submission.manager.model.application;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public abstract class ResultFailure {

  private final String failureReason;
}
