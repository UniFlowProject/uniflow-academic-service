#!/bin/sh
# Prints a dev access token for the given user (user-a, user-b or dev-user) on stdout.
# Requires the stack from docker-compose.dev.yml to be running (DEV ONLY).
set -eu
cd "$(dirname "$0")/.."
exec docker compose -f docker-compose.dev.yml run --rm -q -e TOKEN_USER="${1:-dev-user}" token
