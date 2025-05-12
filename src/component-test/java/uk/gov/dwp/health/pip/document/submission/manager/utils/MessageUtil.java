package uk.gov.dwp.health.pip.document.submission.manager.utils;

import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.GetQueueAttributesRequest;
import com.amazonaws.services.sqs.model.PurgeQueueRequest;
import com.amazonaws.services.sqs.model.QueueAttributeName;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import com.amazonaws.services.sqs.model.SendMessageRequest;

import java.time.Duration;
import java.util.Map;

import static org.awaitility.Awaitility.await;

public class MessageUtil {
  private final AmazonSQS amazonSQS;
  private final String queueUrl;

  public MessageUtil(
      String serviceEndpoint,
      String awsRegion,
      String queueUrl) {
    var endpointConfiguration =
        new AwsClientBuilder.EndpointConfiguration(serviceEndpoint, awsRegion);
    amazonSQS =
        AmazonSQSClientBuilder.standard().withEndpointConfiguration(endpointConfiguration).build();
    this.queueUrl = queueUrl;
  }

  public void sendMessageToQueue(final String payload) {
    SendMessageRequest sendMessageRequest = new SendMessageRequest(queueUrl, payload);
    amazonSQS.sendMessage(sendMessageRequest);
  }


  private Map<String, String> getQueueAttributes(String queueUrl) {
    var getQueueAttributesRequest =
        new GetQueueAttributesRequest(queueUrl).withAttributeNames(QueueAttributeName.All);
    return amazonSQS.getQueueAttributes(getQueueAttributesRequest).getAttributes();
  }

  public String getMessageCount() {
    var queueAttributes = getQueueAttributes(queueUrl);
    return queueAttributes.get(QueueAttributeName.ApproximateNumberOfMessages.toString());
  }

  public void clearQueue() {
    amazonSQS.purgeQueue(new PurgeQueueRequest(queueUrl));

    await()
        .atMost(Duration.ofMinutes(1))
        .until(() -> getMessageCount().equals("0"));
  }

  public ReceiveMessageResult receiveMessage() {
    return amazonSQS.receiveMessage(queueUrl);
  }

  public static String createSubmissionTestMessage(String applicationId) {
    return """
        {
          "application_id": "%s"
        }
        """
        .formatted(applicationId);
  }

}
