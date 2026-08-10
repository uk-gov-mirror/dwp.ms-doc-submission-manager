package uk.gov.dwp.health.pip.document.submission.manager.repository.events;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.BeforeConvertEvent;
import org.springframework.stereotype.Component;
import uk.gov.dwp.health.mongo.changestream.config.properties.WatcherConfigProperties;
import uk.gov.dwp.health.pip.document.submission.manager.entity.DrsUpload;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnExpression("${feature.data.changestream.enabled:false}")
public class MongoEventListenerDrsUpload extends AbstractMongoEventListener<DrsUpload> {

  private final WatcherConfigProperties watcherConfigProperties;

  @Override
  public void onBeforeConvert(BeforeConvertEvent<DrsUpload> event) {
    log.info(
            "Set change stream channel.  changeStreamClass is {}, collectionName is drs_upload",
            event.getSource());
    watcherConfigProperties.setChangeStreamChannel(event.getSource(), "drs_upload");
  }
}
