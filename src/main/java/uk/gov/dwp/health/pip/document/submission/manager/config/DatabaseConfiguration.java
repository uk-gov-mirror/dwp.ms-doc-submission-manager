package uk.gov.dwp.health.pip.document.submission.manager.config;

import com.mongodb.ServerApi;
import com.mongodb.ServerApiVersion;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.mongodb.autoconfigure.MongoClientSettingsBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;

import static org.springframework.data.mongodb.core.convert.MongoCustomConversions.MongoConverterConfigurationAdapter;

@Configuration
public class DatabaseConfiguration {

  private static final ServerApiVersion API_VERSION = ServerApiVersion.V1;

  @Value("${feature.mongo.stable.api.enabled:true}")
  private boolean isMongoStableApiEnabled;

  @Bean
  public MongoClientSettingsBuilderCustomizer mongoDBDefaultSettings() {
    if (isMongoStableApiEnabled) {
      return builder -> builder.serverApi(buildServerApi());
    }
    return clientSettingsBuilder -> {};
  }

  @Bean
  public MongoCustomConversions mongoJsrConversions() {
    return MongoCustomConversions.create(
            MongoConverterConfigurationAdapter::useNativeDriverJavaTimeCodecs);
  }

  private ServerApi buildServerApi() {
    return ServerApi.builder().strict(true).version(API_VERSION).build();
  }
}
