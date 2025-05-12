package uk.gov.dwp.health.pip.document.submission.manager.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
@Setter
@ConfigurationProperties(prefix = "uk.gov.dwp.submitted.health.application")
public class SubmittedApplicationEventProperties {
  private String topicName;
  private String routingKey;
}