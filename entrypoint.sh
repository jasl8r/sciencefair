#!/bin/bash
set -e

[[ $DEBUG == true ]] && set -x

check_database_connection() {
  echo -n "Configuring database..."

  export DB_PROTOCOL=mysql
  export DB_HOST=${DB_HOST:-${MYSQL_PORT_3306_TCP_ADDR}}
  export DB_PORT=${DB_PORT:-${MYSQL_PORT_3306_TCP_PORT}}
  export DB_USER=${DB_USER:-${MYSQL_ENV_MYSQL_USER}}
  export DB_PASS=${DB_PASS:-${MYSQL_ENV_MYSQL_PASSWORD}}
  export DB_NAME=${DB_NAME:-${MYSQL_ENV_MYSQL_DATABASE}}
  export DB_PATH="//${DB_HOST}/${DB_NAME}"

  prog=(mysqladmin -h ${DB_HOST} -P ${DB_PORT} -u ${DB_USER} ${DB_PASS:+-p$DB_PASS} status)
  timeout=60
  while ! "${prog[@]}" >/dev/null 2>&1
  do
    timeout=$(expr $timeout - 1)
    if [[ $timeout -eq 0 ]]; then
      echo
      echo "Could not connect to database server. Aborting..."
      return 1
    fi
    echo -n "."
    sleep 1
  done
  echo
}

migrate() {
  # check if this is a new installation
  QUERY="SELECT count(*) FROM information_schema.tables WHERE table_schema = '${DB_NAME}';"
  COUNT=$(mysql -h ${DB_HOST} -P ${DB_PORT} -u ${DB_USER} ${DB_PASS:+-p$DB_PASS} -ss -e "${QUERY}")

  if [[ -z ${COUNT} || ${COUNT} -eq 0 ]]; then
    echo "Configuring new installation..."
    mysql -h ${DB_HOST} -P ${DB_PORT} -u ${DB_USER} ${DB_PASS:+-p$DB_PASS} -ss ${DB_NAME} < init.sql
  fi
}

check_database_connection
migrate

exec "$@"
