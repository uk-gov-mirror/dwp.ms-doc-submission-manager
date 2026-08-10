package uk.gov.dwp.health.pip.document.submission.manager.repository.events;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.BeforeConvertEvent;
import org.springframework.stereotype.Component;
import uk.gov.dwp.health.mongo.changestream.config.properties.WatcherConfigProperties;
import uk.gov.dwp.health.pip.document.submission.manager.entity.Documentation;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnExpression("${feature.data.changestream.enabled:false}")
public class MongoEventListenerDocument extends AbstractMongoEventListener<Documentation> {

  private final WatcherConfigProperties watcherConfigProperties;

  @Override
  public void onBeforeConvert(BeforeConvertEvent<Documentation> event) {
    log.info(
            "Set change stream channel.  changeStreamClass is {}, collectionName is document",
            event.getSource());
    watcherConfigProperties.setChangeStreamChannel(event.getSource(), "document");
  }
}
