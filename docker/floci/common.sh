# Shared settings for the Floci bootstrap scripts (DEV ONLY, every value is a throwaway default).
# These values are mirrored in docker-compose.dev.yml; keep both in sync.
export AWS_ACCESS_KEY_ID=test
export AWS_SECRET_ACCESS_KEY=test
export AWS_DEFAULT_REGION=us-east-1
export AWS_PAGER=""

POOL_ID=us-east-1_UniflowDev
CLIENT_NAME=uniflow-spa
DB_IDENTIFIER=uniflow-db
DB_PORT=7001
ACCOUNT_ID=000000000000

log() { echo "[floci-init] $*"; }
