#!/bin/sh
# Reminder pipeline of the `reminders` compose profile, mirroring terraform/messaging.tf:
#   EventBridge Scheduler group + role -> SQS "expirations" -> Lambda process_expiration -> SNS topic
#   -> SQS "in-app-notifications" / "email-notifications".
# DEV ONLY. Idempotent: every resource is looked up before it is created.
set -eu
. /floci/common.sh

PREFIX=uniflow
LAMBDA_NAME=$PREFIX-process-expiration
SCHEDULER_ROLE=$PREFIX-scheduler
LAMBDA_ROLE=$PREFIX-lambda
GROUP_NAME=$PREFIX

trust_policy() {
  printf '{"Version":"2012-10-17","Statement":[{"Effect":"Allow","Principal":{"Service":"%s"},"Action":"sts:AssumeRole"}]}' "$1"
}

ensure_role() {
  name=$1
  service=$2
  if aws iam get-role --role-name "$name" >/dev/null 2>&1; then
    log "role $name already exists"
  else
    log "creating role $name"
    aws iam create-role --role-name "$name" --assume-role-policy-document "$(trust_policy "$service")" >/dev/null
  fi
}

queue_url() {
  aws sqs create-queue --queue-name "$1" --query QueueUrl --output text
}

queue_arn() {
  aws sqs get-queue-attributes --queue-url "$1" --attribute-names QueueArn --query Attributes.QueueArn --output text
}

# --- Queues (names follow terraform: <project>-<name> and <project>-<name>-dlq) --------------------
# create-queue is idempotent when the attributes do not change, and set-queue-attributes keeps them aligned.
for q in expirations in-app-notifications email-notifications; do
  dlq_arn=$(queue_arn "$(queue_url "$PREFIX-$q-dlq")")
  url=$(queue_url "$PREFIX-$q")
  attributes=$(printf '{"VisibilityTimeout":"60","RedrivePolicy":"{\\"deadLetterTargetArn\\":\\"%s\\",\\"maxReceiveCount\\":\\"3\\"}"}' "$dlq_arn")
  aws sqs set-queue-attributes --queue-url "$url" --attributes "$attributes"
  log "queue $PREFIX-$q ready"
done
EXPIRATIONS_URL=$(queue_url "$PREFIX-expirations")
EXPIRATIONS_ARN=$(queue_arn "$EXPIRATIONS_URL")

# --- SNS topic fanned out to the notification queues ---------------------------------------------
TOPIC_ARN=$(aws sns create-topic --name "$PREFIX-notifications" --query TopicArn --output text)
for q in in-app-notifications email-notifications; do
  arn=$(queue_arn "$(queue_url "$PREFIX-$q")")
  already=$(aws sns list-subscriptions-by-topic --topic-arn "$TOPIC_ARN" \
    --query "Subscriptions[?Endpoint=='$arn'] | length(@)" --output text)
  if [ "$already" = "0" ]; then
    aws sns subscribe --topic-arn "$TOPIC_ARN" --protocol sqs --notification-endpoint "$arn" >/dev/null
    log "subscribed $PREFIX-$q to $TOPIC_ARN"
  fi
done

# --- EventBridge Scheduler group + role ------------------------------------------------------------
ensure_role "$SCHEDULER_ROLE" scheduler.amazonaws.com
aws iam put-role-policy --role-name "$SCHEDULER_ROLE" --policy-name send-to-expirations \
  --policy-document "$(printf '{"Version":"2012-10-17","Statement":[{"Effect":"Allow","Action":"sqs:SendMessage","Resource":"%s"}]}' "$EXPIRATIONS_ARN")"
if aws scheduler get-schedule-group --name "$GROUP_NAME" >/dev/null 2>&1; then
  log "schedule group $GROUP_NAME already exists"
else
  aws scheduler create-schedule-group --name "$GROUP_NAME" >/dev/null
  log "created schedule group $GROUP_NAME"
fi

# --- Lambda: the real process_expiration handler (copied from lambdas/process_expiration) -----------
ensure_role "$LAMBDA_ROLE" lambda.amazonaws.com
# index.mjs must sit at the archive root, as Lambda expects.
(cd /floci/lambdas/process_expiration && python3 -m zipfile -c /tmp/process_expiration.zip index.mjs)

if aws lambda get-function --function-name "$LAMBDA_NAME" >/dev/null 2>&1; then
  log "updating function $LAMBDA_NAME"
  aws lambda update-function-code --function-name "$LAMBDA_NAME" --zip-file fileb:///tmp/process_expiration.zip >/dev/null
else
  log "creating function $LAMBDA_NAME"
  aws lambda create-function --function-name "$LAMBDA_NAME" \
    --runtime nodejs22.x --handler index.handler --timeout 30 \
    --role "arn:aws:iam::$ACCOUNT_ID:role/$LAMBDA_ROLE" \
    --environment "Variables={NOTIFICATIONS_TOPIC_ARN=$TOPIC_ARN}" \
    --zip-file fileb:///tmp/process_expiration.zip >/dev/null
fi

mappings=$(aws lambda list-event-source-mappings --function-name "$LAMBDA_NAME" \
  --query "EventSourceMappings[?EventSourceArn=='$EXPIRATIONS_ARN'] | length(@)" --output text)
if [ "$mappings" = "0" ]; then
  aws lambda create-event-source-mapping --function-name "$LAMBDA_NAME" \
    --event-source-arn "$EXPIRATIONS_ARN" --batch-size 10 \
    --function-response-types ReportBatchItemFailures >/dev/null
  log "wired $PREFIX-expirations -> $LAMBDA_NAME"
fi

# --- Settings consumed by the app container (sourced by its dev entrypoint) -------------------------
cat > /out/reminders.env <<ENV
export REMINDER_SCHEDULER=eventbridge
export REMINDER_GROUP_NAME=$GROUP_NAME
export REMINDER_QUEUE_ARN=$EXPIRATIONS_ARN
export REMINDER_ROLE_ARN=arn:aws:iam::$ACCOUNT_ID:role/$SCHEDULER_ROLE
export REMINDER_LEAD_TIME_HOURS=\${REMINDER_LEAD_TIME_HOURS:-0}
ENV
log "done: reminders enabled (queue $EXPIRATIONS_ARN)"
