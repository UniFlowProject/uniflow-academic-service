#!/bin/sh
# Idempotent bootstrap of the local AWS resources the academic service needs, run against Floci:
# Cognito user pool + app client + two dev users, and an RDS PostgreSQL instance.
# DEV ONLY. Safe to re-run: every resource is looked up before it is created.
set -eu
. /floci/common.sh

: "${DB_NAME:?}" "${DB_USER:?}" "${DB_PASSWORD:?}" "${USER_A_PASSWORD:?}" "${USER_B_PASSWORD:?}"

# The reminders profile writes /out/reminders.env; drop a stale one so a run without the profile starts clean.
rm -f /out/reminders.env

# --- Cognito -----------------------------------------------------------------------------------
# The pool id and the client id are pinned so the issuer URI and COGNITO_ALLOWED_CLIENT_IDS are known
# upfront: Floci honours the floci:override-id tag and, with use-name, makes the client id equal to its name.
if aws cognito-idp describe-user-pool --user-pool-id "$POOL_ID" >/dev/null 2>&1; then
  log "user pool $POOL_ID already exists"
else
  log "creating user pool $POOL_ID"
  # Mirrors terraform/auth.tf: email as username, auto-verified email, same password policy.
  aws cognito-idp create-user-pool \
    --pool-name uniflow-users \
    --username-attributes email \
    --auto-verified-attributes email \
    --policies 'PasswordPolicy={MinimumLength=10,RequireLowercase=true,RequireUppercase=true,RequireNumbers=true,RequireSymbols=false}' \
    --user-pool-tags "floci:override-id=$POOL_ID,floci:override-cognito-client-id=use-name" \
    --query UserPool.Id --output text >/dev/null
fi

CLIENT_ID=$(aws cognito-idp list-user-pool-clients --user-pool-id "$POOL_ID" \
  --query "UserPoolClients[?ClientName=='$CLIENT_NAME'].ClientId | [0]" --output text)
if [ "$CLIENT_ID" = "None" ] || [ -z "$CLIENT_ID" ]; then
  log "creating app client $CLIENT_NAME"
  # Public client without secret like the SPA client in terraform/auth.tf. USER_PASSWORD_AUTH is added
  # (dev only) so the token helper can sign in with the AWS CLI; the real client uses SRP.
  CLIENT_ID=$(aws cognito-idp create-user-pool-client \
    --user-pool-id "$POOL_ID" \
    --client-name "$CLIENT_NAME" \
    --no-generate-secret \
    --explicit-auth-flows ALLOW_USER_PASSWORD_AUTH ALLOW_USER_SRP_AUTH ALLOW_REFRESH_TOKEN_AUTH \
    --prevent-user-existence-errors ENABLED \
    --enable-token-revocation \
    --query UserPoolClient.ClientId --output text)
fi
log "app client id: $CLIENT_ID"

ensure_user() {
  email=$1
  password=$2
  if aws cognito-idp admin-get-user --user-pool-id "$POOL_ID" --username "$email" >/dev/null 2>&1; then
    log "user $email already exists"
  else
    log "creating user $email"
    aws cognito-idp admin-create-user --user-pool-id "$POOL_ID" --username "$email" \
      --message-action SUPPRESS \
      --user-attributes "Name=email,Value=$email" "Name=email_verified,Value=true" >/dev/null
  fi
  aws cognito-idp admin-set-user-password --user-pool-id "$POOL_ID" --username "$email" \
    --password "$password" --permanent
}
ensure_user user-a@uniflow.local "$USER_A_PASSWORD"
ensure_user user-b@uniflow.local "$USER_B_PASSWORD"

# --- RDS ---------------------------------------------------------------------------------------
if aws rds describe-db-instances --db-instance-identifier "$DB_IDENTIFIER" >/dev/null 2>&1; then
  log "RDS instance $DB_IDENTIFIER already exists"
else
  log "creating RDS instance $DB_IDENTIFIER (a real PostgreSQL container started by Floci)"
  aws rds create-db-instance \
    --db-instance-identifier "$DB_IDENTIFIER" \
    --engine postgres --engine-version 16 \
    --db-instance-class db.t3.micro --allocated-storage 20 \
    --master-username "$DB_USER" --master-user-password "$DB_PASSWORD" \
    --db-name "$DB_NAME" >/dev/null
fi

log "waiting for $DB_IDENTIFIER to become available"
aws rds wait db-instance-available --db-instance-identifier "$DB_IDENTIFIER"

port=$(aws rds describe-db-instances --db-instance-identifier "$DB_IDENTIFIER" \
  --query 'DBInstances[0].Endpoint.Port' --output text)
if [ "$port" != "$DB_PORT" ]; then
  log "ERROR: the instance listens on port $port but docker-compose.dev.yml expects $DB_PORT"
  exit 1
fi

log "done: issuer http://localhost.floci.io:4566/$POOL_ID, client $CLIENT_ID, database floci:$DB_PORT"
