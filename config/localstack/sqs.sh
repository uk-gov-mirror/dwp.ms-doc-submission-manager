#!/bin/bash

awslocal sqs create-queue --queue-name docbatch-batch-upload
awslocal sqs create-queue --queue-name docbatch-batch-upload-dlq

awslocal sqs create-queue --queue-name docbatch-batch-response
awslocal sqs create-queue --queue-name docbatch-batch-response-dlq

awslocal sqs create-queue --queue-name truemockdrs-test-batch-response
awslocal sqs create-queue --queue-name truemockdrs-test-batch-response-dlq

awslocal sqs create-queue --queue-name workflow-request-queue
SUBSCRIPTION_ARN=$(awslocal sns subscribe --protocol sqs --topic-arn arn:aws:sns:eu-west-2:000000000000:workflow-topic --notification-endpoint arn:aws:sqs:eu-west-2:000000000000:workflow-request-queue --query 'SubscriptionArn' --output text)
awslocal sns set-subscription-attributes --subscription-arn "$SUBSCRIPTION_ARN" --attribute-name FilterPolicy --attribute-value "{\"x-dwp-routing-key\": [ \"workflow\" ] }"
awslocal sns get-subscription-attributes --subscription-arn "$SUBSCRIPTION_ARN"

awslocal sqs create-queue --queue-name environment-config-complete

awslocal sqs create-queue --queue-name pip-docsub-analytics
SUBSCRIPTION_ARN=$(awslocal sns subscribe --protocol sqs --topic-arn arn:aws:sns:eu-west-2:000000000000:pip-docsub-analytics --notification-endpoint arn:aws:sqs:eu-west-2:000000000000:pip-docsub-analytics --query 'SubscriptionArn' --output text)
awslocal sns set-subscription-attributes --subscription-arn "$SUBSCRIPTION_ARN" --attribute-name FilterPolicy --attribute-value "{\"x-dwp-routing-key\": [ \"pip.docsub.mgr.stream\" ] }"

awslocal sqs create-queue --queue-name ms-doc-sub-additional-support-submission-queue
awslocal sqs create-queue --queue-name ms-doc-sub-additional-support-submission-queue-dlq
awslocal sqs set-queue-attributes --queue-url http://localstack:4566/000000000000/ms-doc-sub-additional-support-submission-queue --attributes '{"RedrivePolicy": "{\"deadLetterTargetArn\":\"arn:aws:sqs:eu-west-2:000000000000:ms-doc-sub-additional-support-submission-queue-dlq\",\"maxReceiveCount\":1}"}'
awslocal sqs get-queue-attributes --queue-url http://localstack:4566/000000000000/ms-doc-sub-additional-support-submission-queue --attribute-names All

awslocal sqs create-queue --queue-name ms-app-co-additional-support-submission-queue
