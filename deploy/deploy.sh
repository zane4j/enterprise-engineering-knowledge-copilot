#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="/opt/enterprise-engineering-knowledge-copilot"
ENV_FILE="$ROOT_DIR/config/production.env"
COMPOSE_FILE="$ROOT_DIR/deploy/compose.production.yml"

cd "$ROOT_DIR"
test -f "$ENV_FILE" || { echo "Missing $ENV_FILE" >&2; exit 1; }

git fetch --prune origin main
git checkout --detach origin/main

set -a
# shellcheck disable=SC1090
source "$ENV_FILE"
set +a

: "${APP_IMAGE_TAG:?APP_IMAGE_TAG must be set}"

docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" pull
docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" up -d --wait --wait-timeout 180 --remove-orphans
docker image prune -f
