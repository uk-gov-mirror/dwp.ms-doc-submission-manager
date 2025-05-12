package uk.gov.dwp.health.pip.document.submission.manager.config.properties;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "uk.gov.dwp.health.identity")
@Validated
public class MsIdentityStatusProperties {

  @NotBlank(message = "Base uri should not be blank")
  private String baseUri;

  @NotBlank(message = "Version should not be blank")
  private String apiVersion;

  @NotBlank(message = "Endpoint should not be blank")
  private String apiEndpoint;
}