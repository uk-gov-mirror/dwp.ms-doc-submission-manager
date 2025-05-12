package uk.gov.dwp.health.pip.document.submission.manager.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import uk.gov.dwp.health.pip.document.submission.manager.config.properties.MsAccountProperties;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class MsAccountConfig {

  private final MsAccountProperties msAccountProperties;

  public String getAccountMgrDataUri() {
    return this.msAccountProperties.getBaseUri() + "/"
        + this.msAccountProperties.getApiVersion() + "/"
        + this.msAccountProperties.getApiEndpoint();
  }
}