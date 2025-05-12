#!/bin/sh

QUEUE_URL=$(aws --endpoint-url=http://localhost:4566 sqs get-queue-url --queue-name application-submission --output text)

echo "$QUEUE_URL"

MESSAGE_BODY=$(cat <<EOF
{
  "application_id": "123456789012345678901234"
}
EOF
)

echo "$MESSAGE_BODY"

aws --endpoint-url=http://localhost:4566 sqs send-message --queue-url "$QUEUE_URL" --message-body "$MESSAGE_BODY"

echo "Message sent"
