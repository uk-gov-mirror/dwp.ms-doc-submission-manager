package uk.gov.dwp.health.pip.document.submission.manager.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.dwp.health.pip.document.submission.manager.config.properties.MsRegistrationCaptureManagerProperties;
import uk.gov.dwp.health.pip.registration.capture.openapi.v4.ApiClient;
import uk.gov.dwp.health.pip.registration.capture.openapi.v4.api.RegistrationApiClientV4;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class MsRegistrationCaptureManagerConfig {

  private final MsRegistrationCaptureManagerProperties msRegistrationCaptureManagerProperties;
  private final RestTemplateBuilder restTemplateBuilder;

  public String getRegistrationDataUri() {
    return this.msRegistrationCaptureManagerProperties.getBaseUri()
        + "/"
        + this.msRegistrationCaptureManagerProperties.getApiVersion()
        + "/"
        + this.msRegistrationCaptureManagerProperties.getApiEndpoint();
  }

  @Bean
  public RegistrationApiClientV4 registrationApiClientV4() {
    log.info(
        "Configuring the registration api v4 client for {}",
        msRegistrationCaptureManagerProperties.getBaseUri());
    ApiClient apiClient = new ApiClient(restTemplateBuilder.build());
    apiClient.setBasePath(msRegistrationCaptureManagerProperties.getBaseUri());
    return new RegistrationApiClientV4(apiClient);
  }
}
