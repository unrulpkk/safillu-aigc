#!/bin/sh
set -eu
cd "$(dirname "$0")"
env_file="${1:-.env.local}"
docker info --format '{{.ServerVersion}}' >/dev/null
if [ ! -f "$env_file" ]; then
  case "$env_file" in *prod*) cp .env.prod.example "$env_file";; *) cp .env.example "$env_file";; esac
fi
for service in secrets-init backend web admin; do
  docker compose --env-file "$env_file" build "$service"
done
docker compose --env-file "$env_file" up -d --no-build --wait --wait-timeout 300
docker compose --env-file "$env_file" ps
printf '%s\n' 'Safillu is ready. Default creator: http://localhost:8088 ; admin: http://localhost:8089 .' 'Initial passwords: <SAFILLU_RUNTIME_DIR>/secrets/initial-accounts.json (default .runtime).'
