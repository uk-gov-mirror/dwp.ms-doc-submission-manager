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
@ConfigurationProperties(prefix = "uk.gov.dwp.health.application.coordinator")
@Validated
public class MsApplicationCoordinatorProperties {

  @NotBlank(message = "Application coordinator base uri should not be blank")
  private String baseUri;

  @NotBlank(message = "Application coordinator version should not be blank")
  private String apiVersion;

  @NotBlank(message = "Application coordinator endpoint should not be blank")
  private String apiEndpoint;
}