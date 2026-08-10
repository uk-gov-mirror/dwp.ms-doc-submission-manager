package uk.gov.dwp.health.pip.document.submission.manager.event;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.MessageHeaders;
import uk.gov.dwp.health.pip.application.coordinator.generated.event.AdditionalSupportApplicationAutoSubmittedEventV1Payload;
import uk.gov.dwp.health.pip.document.submission.manager.service.AdditionalSupportSubmissionService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdditionalSupportSubmissionConsumerTest {

  @Mock private AdditionalSupportSubmissionService additionalSupportSubmissionService;
  @Mock private EventInboundConfigurationProperties properties;
  @InjectMocks private AdditionalSupportSubmissionConsumer additionalSupportSubmissionConsumer;

  @Test
  void getQueueName() {
    when(properties.getAdditionalSupportSubmissionQueueName()).thenReturn("queue-name");

    String queueName = additionalSupportSubmissionConsumer.getQueueName();

    assertThat(queueName).isEqualTo("queue-name");
  }

  @Test
  void handleMessage() {
    MessageHeaders messageHeaders = mock(MessageHeaders.class);

    AdditionalSupportApplicationAutoSubmittedEventV1Payload payload =
        new AdditionalSupportApplicationAutoSubmittedEventV1Payload()
            .withApplicationId("application-id-1")
            .withClaimantId("claimant-id-1");

    additionalSupportSubmissionConsumer.handleMessage(messageHeaders, payload);

    verify(additionalSupportSubmissionService, times(1))
        .submitAdditionalSupportApplication("application-id-1", "claimant-id-1");
  }
}
