package uk.gov.dwp.health.pip.document.submission.manager.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

@Getter
@Setter
@Validated
@Configuration
@ConfigurationProperties(prefix = "aws.cloud.watch")
public class CloudWatchProperties {

  @NotBlank(message = "AWS region required")
  private String awsRegion;

  private String endpointOverride;

  private String namespace;

  private String metricProduct;

  private String metricEnvironment;

  private String metricEnvId;

  @Value("${submission.failure.metric.name:submission-failed}")
  private String submissionFailureMetricName;
}
