package uk.gov.dwp.health.pip.document.submission.manager.service.impl;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.env.Environment;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.MetricDatum;
import software.amazon.awssdk.services.cloudwatch.model.PutMetricDataRequest;
import software.amazon.awssdk.services.cloudwatch.model.StandardUnit;
import uk.gov.dwp.health.pip.document.submission.manager.config.properties.CloudWatchProperties;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class CloudWatchMetricsServiceTest {

  @Test
  void incrementMetric() {
    final String metricName = "fred";
    final String namespace = "namespace";

    final CloudWatchProperties cloudWatchProperties = new CloudWatchProperties();
    cloudWatchProperties.setNamespace(namespace);

    final CloudWatchClient cloudWatchClient = mock(CloudWatchClient.class);
    final Environment environment = mock(Environment.class);

    final CloudWatchMetricsServiceImpl cloudWatchMetricsService =
        new CloudWatchMetricsServiceImpl(cloudWatchClient, cloudWatchProperties, environment);
    cloudWatchMetricsService.incrementMetric(metricName);

    final ArgumentCaptor<PutMetricDataRequest> putMetricDataRequestArgumentCaptor =
        ArgumentCaptor.forClass(PutMetricDataRequest.class);
    verify(cloudWatchClient, times(1)).putMetricData(putMetricDataRequestArgumentCaptor.capture());

    final PutMetricDataRequest putMetricDataRequest = putMetricDataRequestArgumentCaptor.getValue();
    final List<MetricDatum> metricData = putMetricDataRequest.metricData();
    assertThat(metricData).hasSize(1);
    assertThat(metricData.get(0).metricName()).isEqualTo(metricName);
    assertThat(metricData.get(0).unit()).isEqualTo(StandardUnit.NONE);
    assertThat(metricData.get(0).value()).isEqualTo(1d);
    assertThat(metricData.get(0).dimensions()).hasSize(5);
    assertThat(putMetricDataRequest.namespace()).isEqualTo(namespace);
  }
}
