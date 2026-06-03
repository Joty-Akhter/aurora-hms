#!/bin/bash
# Wait for PostgreSQL to be ready
set -e

host="$1"
shift
cmd="$@"

until PGPASSWORD=$LIQUIBASE_COMMAND_PASSWORD psql -h "$host" -U "$LIQUIBASE_COMMAND_USERNAME" -d "$LIQUIBASE_COMMAND_DATABASE" -c '\q'; do
  >&2 echo "Postgres is unavailable - sleeping"
  sleep 2
done

>&2 echo "Postgres is up - executing command"
exec $cmd
