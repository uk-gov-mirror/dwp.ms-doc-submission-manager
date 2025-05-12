#!/bin/bash

awslocal sqs create-queue --queue-name docbatch-batch-upload
awslocal sqs create-queue --queue-name docbatch-batch-upload-dlq

awslocal sqs create-queue --queue-name docbatch-batch-response
awslocal sqs create-queue --queue-name docbatch-batch-response-dlq

awslocal sqs create-queue --queue-name truemockdrs-test-batch-response
awslocal sqs create-queue --queue-name truemockdrs-test-batch-response-dlq

awslocal sqs create-queue --queue-name state-change-in
awslocal sqs create-queue --queue-name state-change-in-dlq

awslocal sqs create-queue --queue-name application-submission
awslocal sqs create-queue --queue-name application-submission-dlq

awslocal sns subscribe \
  --return-subscription-arn \
  --topic-arn "arn:aws:sns:eu-west-2:000000000000:pip-application-coordinator-state-change" \
  --protocol sqs \
  --attributes '{"FilterPolicy": "{\"x-dwp-routing-key\": [\"state.change\"]}"}' \
  --notification-endpoint "arn:aws:sqs:eu-west-2:000000000000:state-change-in"

awslocal sqs create-queue --queue-name submitted-application-queue
SUBSCRIPTION_ARN=$(awslocal sns subscribe --protocol sqs --topic-arn arn:aws:sns:eu-west-2:000000000000:submitted-application-topic --notification-endpoint arn:aws:sqs:eu-west-2:000000000000:submitted-application-queue --query 'SubscriptionArn' --output text)
awslocal sns set-subscription-attributes --subscription-arn "$SUBSCRIPTION_ARN" --attribute-name FilterPolicy --attribute-value "{\"x-dwp-routing-key\": [ \"submitted.application\" ] }"
awslocal sns get-subscription-attributes --subscription-arn "$SUBSCRIPTION_ARN"

awslocal sqs create-queue --queue-name workflow-request-queue
SUBSCRIPTION_ARN=$(awslocal sns subscribe --protocol sqs --topic-arn arn:aws:sns:eu-west-2:000000000000:workflow-topic --notification-endpoint arn:aws:sqs:eu-west-2:000000000000:workflow-request-queue --query 'SubscriptionArn' --output text)
awslocal sns set-subscription-attributes --subscription-arn "$SUBSCRIPTION_ARN" --attribute-name FilterPolicy --attribute-value "{\"x-dwp-routing-key\": [ \"workflow\" ] }"
awslocal sns get-subscription-attributes --subscription-arn "$SUBSCRIPTION_ARN"

awslocal sqs create-queue --queue-name environment-config-complete
