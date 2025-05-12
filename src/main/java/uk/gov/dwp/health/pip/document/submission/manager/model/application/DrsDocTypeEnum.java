package uk.gov.dwp.health.pip.document.submission.manager.model.application;

import java.util.HashMap;
import java.util.Map;

public enum DrsDocTypeEnum {
  PIP2_FORM("1274"),
  PIP2_EVIDENCE("1241");

  private static final Map<String, DrsDocTypeEnum> map = new HashMap<>();

  static {
    for (DrsDocTypeEnum type : DrsDocTypeEnum.values()) {
      map.put(type.identifier(), type);
    }
  }

  private final String identifier;

  DrsDocTypeEnum(final String identifier) {
    this.identifier = identifier;
  }

  public static DrsDocTypeEnum get(final String stage) {
    return map.get(stage);
  }

  public String identifier() {
    return this.identifier;
  }
}
