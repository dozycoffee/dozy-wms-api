#!/usr/bin/env bash
# 컨테이너 이미지를 실제로 띄워 기동되고 비루트로 실행되며 종료 신호에 정상 종료(SIGTERM, 종료 코드 143)하는지 확인한다.
# CI와 로컬이 공통으로 쓴다.
# 사용: scripts/image-smoke-test.sh [이미지 이름]   (기본 dozy-wms-api:local)
# 임시 MySQL 컨테이너와 네트워크를 만들고 끝나면 모두 지운다. DB 비밀번호는 실행마다 새로 만든다.
set -euo pipefail

IMAGE="${1:-dozy-wms-api:local}"
SUFFIX="$(openssl rand -hex 4)"
NETWORK="wms-smoke-${SUFFIX}"
MYSQL="wms-smoke-mysql-${SUFFIX}"
APP="wms-smoke-app-${SUFFIX}"
DB_PASSWORD="$(openssl rand -hex 16)"

cleanup() {
  status=$?
  if [ "$status" -ne 0 ]; then
    echo "--- 앱 로그 ---" >&2
    docker logs "$APP" 2>&1 | tail -n 80 >&2 || true
  fi
  docker rm -f "$APP" "$MYSQL" > /dev/null 2>&1 || true
  docker network rm "$NETWORK" > /dev/null 2>&1 || true
  exit "$status"
}
trap cleanup EXIT

docker network create "$NETWORK" > /dev/null
docker run -d --name "$MYSQL" --network "$NETWORK" \
  -e MYSQL_ROOT_PASSWORD="$DB_PASSWORD" -e MYSQL_DATABASE=dozy_wms -e TZ=Asia/Seoul \
  mysql:8.0 --character-set-server=utf8mb4 --collation-server=utf8mb4_unicode_ci > /dev/null

echo "MySQL 준비를 기다린다"
for _ in $(seq 1 60); do
  if docker exec "$MYSQL" mysqladmin ping -h 127.0.0.1 -uroot -p"$DB_PASSWORD" --silent > /dev/null 2>&1; then
    # 초기화 중 임시 서버가 먼저 응답하므로 실제 스키마 접속까지 확인한다
    if docker exec "$MYSQL" mysql -h 127.0.0.1 -uroot -p"$DB_PASSWORD" -e "USE dozy_wms" > /dev/null 2>&1; then
      break
    fi
  fi
  sleep 2
done

# Kafka와 인증 서버는 띄우지 않는다. 브로커에 닿지 못해도 클라이언트가 재시도하므로 기동에는 필요하지 않다.
# 다만 Kafka 주소는 이름이 풀리는 형식이어야 한다(풀리지 않는 호스트는 컨슈머가 기동 단계에서 실패한다)
docker run -d --name "$APP" --network "$NETWORK" \
  -e DB_HOST="$MYSQL" -e DB_PORT=3306 -e DB_NAME=dozy_wms -e DB_USERNAME=root -e DB_PASSWORD="$DB_PASSWORD" \
  -e KAFKA_BOOTSTRAP_SERVERS=localhost:9092 -e AUTH_ISSUER_BASE_URI=http://auth.invalid \
  "$IMAGE" > /dev/null

echo "앱 기동을 기다린다"
started=false
# 브로커에 닿지 못하면 KafkaAdmin이 토픽 생성을 시도하다 포기할 때까지(약 30초) 기동이 늦어지므로 넉넉히 기다린다.
# 로그를 변수에 받아 검사한다(pipefail에서는 grep -q가 먼저 끝나 docker logs가 SIGPIPE로 실패한 것으로 처리된다)
for _ in $(seq 1 90); do
  logs="$(docker logs "$APP" 2>&1)"
  if grep -q "Started DozyWmsApiApplicationKt" <<< "$logs"; then
    started=true
    break
  fi
  if [ "$(docker inspect -f '{{.State.Running}}' "$APP")" != "true" ]; then
    echo "앱 컨테이너가 종료되었다" >&2
    exit 1
  fi
  sleep 2
done
[ "$started" = true ] || { echo "앱이 기동되지 않았다" >&2; exit 1; }
echo "앱 기동 확인"

# 비루트로 실행된다
uid="$(docker exec "$APP" id -u)"
[ "$uid" != "0" ] || { echo "루트로 실행되고 있다" >&2; exit 1; }
echo "실행 사용자 uid=${uid}"

# 마이그레이션이 적용되었다
tables="$(docker exec "$MYSQL" mysql -h 127.0.0.1 -uroot -p"$DB_PASSWORD" -N -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='dozy_wms' AND table_name='flyway_schema_history'")"
[ "$tables" = "1" ] || { echo "마이그레이션이 적용되지 않았다" >&2; exit 1; }

# 종료 신호를 받으면 정상 종료한다(SIGTERM이면 종료 코드 143)
docker stop -t 40 "$APP" > /dev/null
exit_code="$(docker inspect -f '{{.State.ExitCode}}' "$APP")"
[ "$exit_code" = "143" ] || [ "$exit_code" = "0" ] || { echo "종료 코드가 ${exit_code}이다(143 또는 0이어야 한다)" >&2; exit 1; }
echo "정상 종료 확인(종료 코드 ${exit_code})"
echo "이미지 스모크 테스트 통과"
