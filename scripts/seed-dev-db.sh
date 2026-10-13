#!/usr/bin/env bash
# 개발 DB에 창고 구조(창고, WorkArea, Zone, Location)를 적재한다. 창고가 이미 있으면 건너뛴다.
# 스키마가 먼저 있어야 하므로 앱을 한 번 기동(Flyway)한 뒤에 실행한다. 초기화는 scripts/reset-dev-db.sh를 사용한다.
set -euo pipefail

cd "$(dirname "$0")/.."

# 명령줄에서 준 DB_NAME이 .env보다 우선한다
DB_NAME_OVERRIDE="${DB_NAME:-}"

if [[ -f .env ]]; then
  set -a
  # shellcheck disable=SC1091
  source .env
  set +a
fi

: "${DB_PASSWORD:?DB_PASSWORD가 필요합니다 (.env 또는 환경변수)}"
DB_NAME="${DB_NAME_OVERRIDE:-${DB_NAME:-dozy_wms}}"
DB_CONTAINER="${DB_CONTAINER:-dozy-wms-mysql}"

if [[ "$DB_NAME" != dozy_wms* ]]; then
  echo "안전을 위해 dozy_wms로 시작하는 DB에만 적재할 수 있습니다: $DB_NAME" >&2
  exit 1
fi

docker exec -i -e MYSQL_PWD="$DB_PASSWORD" "$DB_CONTAINER" \
  mysql -uroot --default-character-set=utf8mb4 "$DB_NAME" < scripts/seed/dev-master-data.sql

echo "'$DB_NAME'에 창고 구조를 적재했습니다(이미 있으면 변경 없음)."
