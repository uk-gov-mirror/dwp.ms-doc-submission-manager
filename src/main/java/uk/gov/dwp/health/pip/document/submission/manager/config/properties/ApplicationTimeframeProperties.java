package uk.gov.dwp.health.pip.document.submission.manager.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Configuration
@ConfigurationProperties(value = "uk.gov.dwp.health.application")
@Getter
@Setter
@Validated
public class ApplicationTimeframeProperties {
  private int activeDuration;
}
