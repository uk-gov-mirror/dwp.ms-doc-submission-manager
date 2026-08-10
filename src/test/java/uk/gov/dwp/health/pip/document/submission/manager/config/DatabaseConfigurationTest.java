package uk.gov.dwp.health.pip.document.submission.manager.config;

import com.mongodb.MongoClientSettings;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.mongodb.autoconfigure.MongoClientSettingsBuilderCustomizer;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class DatabaseConfigurationTest {

  private DatabaseConfiguration databaseConfiguration;

  @BeforeEach
  public void beforeEach() {
    this.databaseConfiguration = new DatabaseConfiguration();
  }

  @Test
  @DisplayName("Ensure that the mongo client builds correctly")
  void buildMongoClientTest() {
        databaseConfiguration.mongoDBDefaultSettings();
    }

  @Test
  @DisplayName("Ensure that the mongo client does not return server api for non stable")
  void buildMongoClientVersionApiFalse(){
    ReflectionTestUtils.setField(this.databaseConfiguration, "isMongoStableApiEnabled", false);
    Assertions.assertNull(build().getServerApi());
  }

  @Test
  @DisplayName("Ensure that the mongo client returns strict server api for stable")
  void buildMongoClientVersionApiTest(){
    ReflectionTestUtils.setField(this.databaseConfiguration, "isMongoStableApiEnabled", true);
    Assertions.assertTrue(build().getServerApi().getStrict().get());
  }

  private MongoClientSettings build() {
    final MongoClientSettingsBuilderCustomizer clientSettingsBuilderCustomizer = databaseConfiguration.mongoDBDefaultSettings();
    final MongoClientSettings.Builder builder = MongoClientSettings.builder();
    clientSettingsBuilderCustomizer.customize(builder);
    return builder.build();
  }
}

