#!/bin/sh
# Idempotent bootstrap of the local AWS resources the academic service needs, run against Floci:
# Cognito user pool + app client + two dev users, and an RDS PostgreSQL instance.
# DEV ONLY. Safe to re-run: every resource is looked up before it is created.
set -eu
. /floci/common.sh

: "${FRONTEND_ORIGIN:?}" "${DB_NAME:?}" "${DB_USER:?}" "${DB_PASSWORD:?}" "${USER_A_PASSWORD:?}" "${USER_B_PASSWORD:?}"

# The reminders profile writes /out/reminders.env; drop a stale one so a run without the profile starts clean.
rm -f /out/reminders.env /out/db.env

# --- Cognito -----------------------------------------------------------------------------------
# The pool id and the client id are pinned so the issuer URI and COGNITO_ALLOWED_CLIENT_IDS are known
# upfront: Floci honours the floci:override-id tag and, with use-name, makes the client id equal to its name.
if aws cognito-idp describe-user-pool --user-pool-id "$POOL_ID" >/dev/null 2>&1; then
  log "user pool $POOL_ID already exists"
else
  log "creating user pool $POOL_ID"
  # Mirrors infra/auth.tf (central repo): email as username, auto-verified email, same password policy.
  aws cognito-idp create-user-pool \
    --pool-name uniflow-users \
    --username-attributes email \
    --auto-verified-attributes email \
    --policies 'PasswordPolicy={MinimumLength=10,RequireLowercase=true,RequireUppercase=true,RequireNumbers=true,RequireSymbols=false}' \
    --user-pool-tags "floci:override-id=$POOL_ID,floci:override-cognito-client-id=use-name" \
    --query UserPool.Id --output text >/dev/null
fi

# Hosted UI domain. Floci serves the managed login under <FLOCI_BASE_URL>/cognito-idp whatever the prefix is,
# but the pool needs a domain for the Hosted UI to exist.
domain=$(aws cognito-idp describe-user-pool --user-pool-id "$POOL_ID" --query UserPool.Domain --output text)
if [ "$domain" = "None" ] || [ -z "$domain" ]; then
  log "creating Hosted UI domain $POOL_DOMAIN"
  aws cognito-idp create-user-pool-domain --user-pool-id "$POOL_ID" --domain "$POOL_DOMAIN"
fi

# Public client without secret, like the SPA client in infra/auth.tf (central repo): Authorization Code (+ PKCE) through
# the Hosted UI, scopes openid email profile, Cognito as the only identity provider. USER_PASSWORD_AUTH is added
# (dev only) so the token helper can sign in with the AWS CLI; the real client uses SRP.
# The SPA sends redirect_uri=<origin>/auth/callback (UniFlow-Frontend/src/lib/cognito.ts) and logout_uri=<origin>.
CALLBACK_URL="$FRONTEND_ORIGIN/auth/callback"
CLIENT_ARGS="--user-pool-id $POOL_ID \
  --explicit-auth-flows ALLOW_USER_PASSWORD_AUTH ALLOW_USER_SRP_AUTH ALLOW_REFRESH_TOKEN_AUTH \
  --prevent-user-existence-errors ENABLED --enable-token-revocation \
  --allowed-o-auth-flows code --allowed-o-auth-scopes openid email profile --allowed-o-auth-flows-user-pool-client \
  --supported-identity-providers COGNITO --callback-urls $CALLBACK_URL --logout-urls $FRONTEND_ORIGIN"
CLIENT_ID=$(aws cognito-idp list-user-pool-clients --user-pool-id "$POOL_ID" \
  --query "UserPoolClients[?ClientName=='$CLIENT_NAME'].ClientId | [0]" --output text)
if [ "$CLIENT_ID" = "None" ] || [ -z "$CLIENT_ID" ]; then
  log "creating app client $CLIENT_NAME"
  # shellcheck disable=SC2086
  CLIENT_ID=$(aws cognito-idp create-user-pool-client --client-name "$CLIENT_NAME" --no-generate-secret $CLIENT_ARGS \
    --query UserPoolClient.ClientId --output text)
else
  # The pool persists in Floci's state, so a client created by an older version of this script is updated in place.
  log "app client $CLIENT_NAME already exists, refreshing its settings"
  # shellcheck disable=SC2086
  aws cognito-idp update-user-pool-client --client-id "$CLIENT_ID" --client-name "$CLIENT_NAME" $CLIENT_ARGS >/dev/null
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

# Floci hands out RDS proxy ports from a counter starting at 7001 and does not give back the ones used by failed
# CreateDBInstance attempts, so the instance may listen on 7002, 7003... Publish the actual port to the app
# (its entrypoint sources /out/db.env) instead of assuming 7001.
port=$(aws rds describe-db-instances --db-instance-identifier "$DB_IDENTIFIER" \
  --query 'DBInstances[0].Endpoint.Port' --output text)
if [ "$port" != "$DB_PORT" ]; then
  log "WARNING: the instance listens on port $port instead of the default $DB_PORT (host access: localhost:$port)"
fi
printf 'export DB_PORT=%s\n' "$port" > /out/db.env

log "done: issuer http://localhost.floci.io:4566/$POOL_ID, client $CLIENT_ID, database floci:$port"
