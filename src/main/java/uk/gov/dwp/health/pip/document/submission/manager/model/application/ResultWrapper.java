package uk.gov.dwp.health.pip.document.submission.manager.model.application;

import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

import java.util.List;
import java.util.Map;

@Getter
@Builder
public class ResultWrapper<T> {

  @Singular
  private List<ResultFailure> failures;

  private T value;

  @Singular("meta")
  private Map<String, Object> metaData;

  public boolean isSuccess() {
    return this.getFailures().isEmpty();
  }
}