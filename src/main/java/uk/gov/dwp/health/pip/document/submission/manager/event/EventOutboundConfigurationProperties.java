package uk.gov.dwp.health.pip.document.submission.manager.event;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "event.outbound")
@Getter
@Setter
public class EventOutboundConfigurationProperties {

  @NotNull(message = "Outbound queue name must not be null")
  @NotBlank(message = "Outbound queue name must not be blank")
  private String applicationCoordinatorAdditionalSupportSubmissionQueueName;
}
