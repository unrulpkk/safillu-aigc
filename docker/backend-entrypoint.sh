#!/bin/sh
set -eu
export DB_PASSWORD="$(cat /run/secrets/db_app)"
export DATA_ENCRYPTION_KEY="$(cat /run/secrets/data_key)"
export REDIS_PASSWORD="$(cat /run/secrets/redis_password)"
# JAVA_OPTS is an operator-only JVM option list, never credential input.
exec java ${JAVA_OPTS:--Xms128m -Xmx768m} -Duser.timezone=UTC -jar /app/server.jar --spring.profiles.active=prod
