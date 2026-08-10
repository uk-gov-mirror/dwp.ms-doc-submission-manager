package uk.gov.dwp.health.pip.document.submission.manager.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import uk.gov.dwp.health.pip.document.submission.manager.config.properties.MsIdentityStatusProperties;

@Slf4j
@Configuration
public class MsIdentityStatusConfig {

  private final MsIdentityStatusProperties msIdentityStatusProperties;

  public MsIdentityStatusConfig(MsIdentityStatusProperties msIdentityStatusProperties) {
    this.msIdentityStatusProperties = msIdentityStatusProperties;
  }

  public String getUri() {
    return this.msIdentityStatusProperties.getBaseUri() + "/"
        + this.msIdentityStatusProperties.getApiVersion() + "/"
        + this.msIdentityStatusProperties.getApiEndpoint();
  }

  public String getIdentityByUserIdUrl() {
    return this.msIdentityStatusProperties.getBaseUri()
        + "/"
        + this.msIdentityStatusProperties.getApiVersionOne()
        + "/"
        + this.msIdentityStatusProperties.getGetByIdApiEndpoint()
        + "/";
  }
}