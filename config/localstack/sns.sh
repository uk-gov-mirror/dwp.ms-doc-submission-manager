#!/bin/bash

awslocal sns create-topic --name pip-apply-local-dev
awslocal sns create-topic --name pip-docsub-analytics
awslocal sns create-topic --name pip-application-coordinator-state-change
awslocal sns create-topic --name workflow-topic
awslocal sns create-topic --name submitted-application-topic

subHandlerSubArn=$(awslocal sns subscribe --topic-arn "arn:aws:sns:eu-west-2:000000000000:pip-apply-local-dev" --protocol sqs --notification-endpoint "arn:aws:sqs:elasticmq:000000000000:docbatch-batch-upload" --output text --query 'SubscriptionArn')
awslocal sns set-subscription-attributes --subscription-arn $subHandlerSubArn --attribute-name FilterPolicy --attribute-value "{\"x-dwp-routing-key\":[{\"prefix\": \"batch.upload\"}]}"

subHandlerSubArn=$(awslocal sns subscribe --topic-arn "arn:aws:sns:eu-west-2:000000000000:pip-apply-local-dev" --protocol sqs --notification-endpoint "arn:aws:sqs:elasticmq:000000000000:docbatch-batch-response" --output text --query 'SubscriptionArn')
awslocal sns set-subscription-attributes --subscription-arn $subHandlerSubArn --attribute-name FilterPolicy --attribute-value "{\"x-dwp-routing-key\":[{\"prefix\": \"pip.batch.response\"}]}"

subHandlerSubArn=$(awslocal sns subscribe --topic-arn "arn:aws:sns:eu-west-2:000000000000:pip-apply-local-dev" --protocol sqs --notification-endpoint "arn:aws:sqs:elasticmq:000000000000:truemockdrs-test-batch-response" --output text --query 'SubscriptionArn')
awslocal sns set-subscription-attributes --subscription-arn $subHandlerSubArn --attribute-name FilterPolicy --attribute-value "{\"x-dwp-routing-key\":[{\"prefix\": \"test.batch.response\"}]}"
