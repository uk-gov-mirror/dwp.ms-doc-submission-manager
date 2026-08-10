package uk.gov.dwp.health.pip.document.submission.manager.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.MetricDatum;
import software.amazon.awssdk.services.cloudwatch.model.PutMetricDataRequest;
import software.amazon.awssdk.services.cloudwatch.model.StandardUnit;
import software.amazon.awssdk.services.cloudwatch.model.Dimension;
import uk.gov.dwp.health.pip.document.submission.manager.config.properties.CloudWatchProperties;
import uk.gov.dwp.health.pip.document.submission.manager.service.CloudWatchMetricsService;

@Service
@RequiredArgsConstructor
public class CloudWatchMetricsServiceImpl implements CloudWatchMetricsService {

  private final CloudWatchClient cloudWatchClient;
  private final CloudWatchProperties cloudWatchProperties;
  private final Environment environment;

  private String buildVersion = null;

  @Override
  public void incrementSubmissionFailureMetric() {
    incrementMetric(cloudWatchProperties.getSubmissionFailureMetricName());
  }

  @Override
  public void incrementMetric(final String metricName) {
    final PutMetricDataRequest request =
        PutMetricDataRequest.builder()
            .namespace(cloudWatchProperties.getNamespace())
            .metricData(
                MetricDatum.builder()
                    .metricName(metricName)
                    .unit(StandardUnit.NONE)
                    .value(1d)
                    .dimensions(
                        Dimension.builder().name("AppVersion").value(getBuildVersion()).build(),
                        Dimension.builder()
                            .name("Product")
                            .value(cloudWatchProperties.getMetricProduct())
                            .build(),
                        Dimension.builder()
                            .name("Environment")
                            .value(cloudWatchProperties.getMetricEnvironment())
                            .build(),
                        Dimension.builder()
                            .name("Env_id")
                            .value(cloudWatchProperties.getMetricEnvId())
                            .build(),
                        Dimension.builder().name("channel").value("strategic").build())
                    .build())
            .build();

    cloudWatchClient.putMetricData(request);
  }

  private String getBuildVersion() {
    if (buildVersion == null) {
      buildVersion = environment.getProperty("app_version");
    }
    return buildVersion;
  }
}
