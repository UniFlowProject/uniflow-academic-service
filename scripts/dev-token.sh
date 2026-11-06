#!/bin/sh
# Prints a Cognito access token (issued by Floci) for a dev user: user-a or user-b.
# Requires the stack from docker-compose.dev.yml to be running (DEV ONLY).
set -eu
cd "$(dirname "$0")/.."
exec docker compose -f docker-compose.dev.yml run --rm -q -e TOKEN_USER="${1:-user-a}" token
