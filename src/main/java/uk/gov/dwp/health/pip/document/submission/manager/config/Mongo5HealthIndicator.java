package uk.gov.dwp.health.pip.document.submission.manager.config;

import org.bson.Document;
import org.springframework.boot.health.contributor.AbstractHealthIndicator;
import org.springframework.boot.health.contributor.Health;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

@Component
public class Mongo5HealthIndicator extends AbstractHealthIndicator {
  private final MongoTemplate mongoTemplate;

  public Mongo5HealthIndicator(MongoTemplate mongoTemplate) {
    super("MongoDB health check failed");
    Assert.notNull(mongoTemplate, "MongoTemplate must not be null");
    this.mongoTemplate = mongoTemplate;
  }

  protected void doHealthCheck(Health.Builder builder) {
    Document result = this.mongoTemplate.executeCommand("{ ping: 1 }");
    final Double okValue = result.getDouble("ok");
    Assert.notNull(okValue, "Mongo5 health check fails with a null");
    Assert.isTrue(okValue.equals(1.0D),
            "Mongo5 health check fails with a non-OK");
    builder.up();
  }
}

