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
@ConfigurationProperties(prefix = "uk.gov.dwp.health.hcm")
@Validated
public class HealthCaptureManagerProperties {

  @NotBlank(message = "Base uri should not be blank")
  private String baseUri;

  @NotBlank(message = "Get application endpoint should not be blank")
  private String getApplicationByIdEndpoint;

  @NotBlank(message = "Get form specification endpoint should not be blank")
  private String getFormSpecificationByIdEndpoint;
}