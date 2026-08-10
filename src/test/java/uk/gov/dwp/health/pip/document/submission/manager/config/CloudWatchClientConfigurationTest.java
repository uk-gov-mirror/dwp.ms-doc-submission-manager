package uk.gov.dwp.health.pip.document.submission.manager.config;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.dwp.health.pip.document.submission.manager.config.properties.CloudWatchProperties;

import java.util.stream.Stream;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CloudWatchClientConfigurationTest {

  private static Stream<Arguments> testArguments() {
    return Stream.of(Arguments.arguments(true), Arguments.arguments(false));
  }

  @ParameterizedTest()
  @MethodSource("testArguments")
  void cloudWatchClient(final boolean useEndpointOverride) {
    final CloudWatchProperties cloudWatchProperties = mock(CloudWatchProperties.class);
    when(cloudWatchProperties.getAwsRegion()).thenReturn("eu-west-2");
    if (useEndpointOverride) {
      when(cloudWatchProperties.getEndpointOverride()).thenReturn("http://localhost:4566");
    }
    final CloudWatchClientConfiguration configuration =
        new CloudWatchClientConfiguration(cloudWatchProperties);
    configuration.cloudWatchClient();
  }
}
