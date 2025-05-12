package uk.gov.dwp.health.pip.document.submission.manager.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import uk.gov.dwp.health.pip.document.submission.manager.config.properties.HealthCaptureManagerProperties;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class HealthCaptureManagerConfig {

  private final HealthCaptureManagerProperties healthCaptureManagerProperties;

  public String getHcmApplicationByIdUri() {
    return this.healthCaptureManagerProperties.getBaseUri() + "/"
        + this.healthCaptureManagerProperties.getGetApplicationByIdEndpoint();
  }

  public String getHcmFormSpecificationByIdUri() {
    return this.healthCaptureManagerProperties.getBaseUri() + "/"
        + this.healthCaptureManagerProperties.getGetFormSpecificationByIdEndpoint();
  }
}