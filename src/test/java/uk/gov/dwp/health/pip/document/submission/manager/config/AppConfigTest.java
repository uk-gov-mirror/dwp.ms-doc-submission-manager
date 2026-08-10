package uk.gov.dwp.health.pip.document.submission.manager.config;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import uk.gov.dwp.health.pip.document.submission.manager.utils.RequestPartition;

import java.text.DateFormat;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class AppConfigTest {

  private static AppConfig cut;

  @BeforeAll
  static void setupSpec() {
    cut = new AppConfig();
  }

  @Test
  void testCreateBase64Encoder() {
    assertThat(cut.encoder()).isNotNull().isOfAnyClassIn(Base64.Encoder.class);
  }

  @Test
  void testCreateBase64Decoder() {
    assertThat(cut.decoder()).isNotNull().isOfAnyClassIn(Base64.Decoder.class);
  }

  @Test
  void testCreateObjectMapper() {
    assertThat(cut.objectMapper()).isNotNull().isOfAnyClassIn(JsonMapper.class);
  }

  @Test
  void testCreateDateFormatter() {
    assertThat(cut.dateFormatter()).isNotNull().isInstanceOf(DateFormat.class);
  }

  @Test
  @DisplayName("test create request partitioner")
  void testCreateRequestPartitioner() {
    Assertions.assertThat(cut.requestPartition(1, 1))
        .isNotNull()
        .isInstanceOf(RequestPartition.class);
  }
}
