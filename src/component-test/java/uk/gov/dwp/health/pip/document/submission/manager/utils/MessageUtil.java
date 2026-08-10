package uk.gov.dwp.health.pip.document.submission.manager.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.PurgeQueueRequest;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.net.URI;
import java.time.Duration;
import java.util.Map;

import static org.awaitility.Awaitility.await;

public class MessageUtil {
  private static final Logger logger = LoggerFactory.getLogger(MessageUtil.class);
  private static final Duration QUEUE_CLEAR_TIMEOUT = Duration.ofMinutes(1);
  private static final int MAX_MESSAGES_TO_RECEIVE = 1;
  private static final String ZERO_MESSAGES = "0";

  private final SqsClient sqsClient;

  public MessageUtil(String serviceEndpoint, String awsRegion) {
    this.sqsClient =
        SqsClient.builder()
            .endpointOverride(URI.create(serviceEndpoint))
            .region(Region.of(awsRegion))
            .build();
  }

  public String getMessageCount(String queueUrl) {
    Map<QueueAttributeName, String> queueAttributes = getQueueAttributes(queueUrl);
    return queueAttributes.get(QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES);
  }

  public void clearQueue(String queueUrl) {
    sqsClient.purgeQueue(PurgeQueueRequest.builder().queueUrl(queueUrl).build());

    await()
        .atMost(QUEUE_CLEAR_TIMEOUT)
        .until(() -> ZERO_MESSAGES.equals(getMessageCount(queueUrl)));
  }

  public void sendMessageToQueue(String queueUrl, String messageBody) {
    SendMessageRequest sendMessageRequest =
        SendMessageRequest.builder().queueUrl(queueUrl).messageBody(messageBody).build();

    logger.info("Sending message to queue: queueUrl=[{}], messageBody=[{}]", queueUrl, messageBody);
    sqsClient.sendMessage(sendMessageRequest);
  }

  public Message getMessage(String queueUrl) {
    ReceiveMessageRequest receiveMessageRequest =
        ReceiveMessageRequest.builder()
            .queueUrl(queueUrl)
            .messageAttributeNames("All")
            .maxNumberOfMessages(MAX_MESSAGES_TO_RECEIVE)
            .build();

    ReceiveMessageResponse receiveMessageResponse = sqsClient.receiveMessage(receiveMessageRequest);
    return receiveMessageResponse.messages().get(0);
  }

  private Map<QueueAttributeName, String> getQueueAttributes(String queueUrl) {
    GetQueueAttributesRequest getQueueAttributesRequest =
        GetQueueAttributesRequest.builder()
            .queueUrl(queueUrl)
            .attributeNames(QueueAttributeName.ALL)
            .build();

    return sqsClient.getQueueAttributes(getQueueAttributesRequest).attributes();
  }
}
