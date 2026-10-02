#!/usr/bin/env bash
# 개발 DB를 삭제 후 재생성한다. 다음 bootRun에서 Flyway가 스키마를 다시 만들고,
# dev 시드가 켜져 있으면 목 데이터도 다시 적재된다.
set -euo pipefail

cd "$(dirname "$0")/.."

if [[ -f .env ]]; then
  set -a
  # shellcheck disable=SC1091
  source .env
  set +a
fi

: "${DB_PASSWORD:?DB_PASSWORD가 필요합니다 (.env 또는 환경변수)}"
DB_NAME="${DB_NAME:-dozy_wms}"
DB_CONTAINER="${DB_CONTAINER:-dozy-wms-mysql}"

if [[ "$DB_NAME" != dozy_wms* ]]; then
  echo "안전을 위해 dozy_wms로 시작하는 DB만 초기화할 수 있습니다: $DB_NAME" >&2
  exit 1
fi

read -r -p "'$DB_NAME' 데이터베이스를 삭제하고 다시 생성합니다. 계속할까요? (yes/no) " answer
if [[ "$answer" != "yes" ]]; then
  echo "취소했습니다."
  exit 1
fi

docker exec -e MYSQL_PWD="$DB_PASSWORD" "$DB_CONTAINER" mysql -uroot -e \
  "DROP DATABASE IF EXISTS \`$DB_NAME\`; CREATE DATABASE \`$DB_NAME\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

echo "'$DB_NAME' 초기화 완료. bootRun으로 서버를 띄우면 스키마가 다시 생성됩니다."
