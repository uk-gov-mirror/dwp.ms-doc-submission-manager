package uk.gov.dwp.health.pip.document.submission.manager.config.properties;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Setter
@Getter
@Validated
@Configuration
@ConfigurationProperties("uk.gov.dwp.health.application")
public class ApplicationSubmissionProperties {
  @NotBlank private String submissionQueueName;
  @NotBlank private String submissionRoutingKey;
}
