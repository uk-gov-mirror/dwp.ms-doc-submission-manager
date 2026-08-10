package uk.gov.dwp.health.pip.document.submission.manager.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClientBuilder;
import uk.gov.dwp.health.pip.document.submission.manager.config.properties.CloudWatchProperties;

import java.net.URI;

@Configuration
public class CloudWatchClientConfiguration {
  final Logger logger = LoggerFactory.getLogger(CloudWatchClientConfiguration.class);

  private final CloudWatchProperties cloudWatchProperties;

  public CloudWatchClientConfiguration(final CloudWatchProperties cloudWatchProperties) {
    this.cloudWatchProperties = cloudWatchProperties;
  }

  @Primary
  @Bean
  public CloudWatchClient cloudWatchClient() {
    final String override = cloudWatchProperties.getEndpointOverride();
    CloudWatchClientBuilder cloudWatchClientBuilder =
        CloudWatchClient.builder().region(Region.of(cloudWatchProperties.getAwsRegion()));

    if (override == null || override.trim().isEmpty()) {
      logger.info("Environment mode");
      return cloudWatchClientBuilder.build();
    } else {
      logger.warn("Localstack mode");
      return cloudWatchClientBuilder
          .endpointOverride(URI.create(override))
          .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
          .build();
    }
  }
}
