package uk.gov.dwp.health.pip.document.submission.manager.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import uk.gov.dwp.health.pip.application.coordinator.openapi.v1.ApiClient;
import uk.gov.dwp.health.pip.application.coordinator.openapi.v1.api.HealthAndDisabilityApiClientV1;
import uk.gov.dwp.health.pip.document.submission.manager.config.properties.MsApplicationCoordinatorProperties;

@Slf4j
@Component
@RequiredArgsConstructor
public class MsApplicationCoordinatorConfig {

  private final MsApplicationCoordinatorProperties msApplicationCoordinatorProperties;
  private final RestTemplateBuilder restTemplateBuilder;

  public String getApplicationByApplicationIdUrl() {
    return this.msApplicationCoordinatorProperties.getBaseUri()
        + "/"
        + this.msApplicationCoordinatorProperties.getApiVersion()
        + "/"
        + this.msApplicationCoordinatorProperties.getApiEndpoint();
  }

  @Bean
  public HealthAndDisabilityApiClientV1 coordinatorApiClientV1() {
    log.info(
        "Configuring the coordinator api v1 client for {}",
        msApplicationCoordinatorProperties.getBaseUri());
    ApiClient apiClient = new ApiClient(restTemplateBuilder.build());
    apiClient.setBasePath(msApplicationCoordinatorProperties.getBaseUri());
    return new HealthAndDisabilityApiClientV1(apiClient);
  }
}
