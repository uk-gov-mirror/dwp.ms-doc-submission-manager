package uk.gov.dwp.health.pip.document.submission.manager.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;
import uk.gov.dwp.health.monitoring.interceptor.OutgoingInterceptor;

import java.util.List;

@Configuration
public class HttpConfig {

  private final Logger log = LoggerFactory.getLogger(HttpConfig.class);

  @Primary
  @Bean("restTemplate")
  public RestTemplate restTemplate(final OutgoingInterceptor outgoingInterceptor) {
    log.info("Create entity rest template bean");
    return new RestTemplateBuilder()
        .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
        .interceptors(List.of(outgoingInterceptor))
        .build();
  }
}
