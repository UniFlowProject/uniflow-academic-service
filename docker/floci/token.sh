#!/bin/sh
# Prints only a Cognito ACCESS token for TOKEN_USER (user-a or user-b) on stdout. DEV ONLY.
set -eu
. /floci/common.sh

case "${TOKEN_USER:-}" in
  user-a) password=$USER_A_PASSWORD ;;
  user-b) password=$USER_B_PASSWORD ;;
  *) echo "usage: TOKEN_USER must be user-a or user-b" >&2; exit 2 ;;
esac

exec aws cognito-idp initiate-auth \
  --client-id "$CLIENT_NAME" \
  --auth-flow USER_PASSWORD_AUTH \
  --auth-parameters "USERNAME=${TOKEN_USER}@uniflow.local,PASSWORD=$password" \
  --query AuthenticationResult.AccessToken --output text
