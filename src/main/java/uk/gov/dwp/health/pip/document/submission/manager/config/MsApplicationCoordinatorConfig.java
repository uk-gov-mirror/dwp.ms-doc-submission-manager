package uk.gov.dwp.health.pip.document.submission.manager.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.dwp.health.pip.document.submission.manager.config.properties.MsApplicationCoordinatorProperties;

@Slf4j
@Component
@RequiredArgsConstructor
public class MsApplicationCoordinatorConfig {

  private final MsApplicationCoordinatorProperties msApplicationCoordinatorProperties;

  public String getApplicationByApplicationIdUrl() {
    return this.msApplicationCoordinatorProperties.getBaseUri() + "/"
           + this.msApplicationCoordinatorProperties.getApiVersion() + "/"
           + this.msApplicationCoordinatorProperties.getApiEndpoint();
  }
}