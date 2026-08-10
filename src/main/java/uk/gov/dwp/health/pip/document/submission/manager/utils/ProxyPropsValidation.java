package uk.gov.dwp.health.pip.document.submission.manager.utils;

import uk.gov.dwp.health.pip.document.submission.manager.config.properties.NotifyConfigProperties;
import java.util.function.Function;

public interface ProxyPropsValidation extends Function<NotifyConfigProperties, Boolean> {

  static ProxyPropsValidation proxyHostNotBlank() {
    return config -> config.getProxyHost() != null && !config.getProxyHost().isBlank();
  }

  static ProxyPropsValidation proxyPortNotBlank() {
    return config -> config.getProxyPort() != null;
  }

  default ProxyPropsValidation and(ProxyPropsValidation other) {
    return config -> this.apply(config) && other.apply(config);
  }
}
