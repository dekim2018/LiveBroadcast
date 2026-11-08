#!/bin/sh
# .env 파일을 무작위 비밀 값으로 생성한다.
#   사용법: ./scripts/generate-env.sh [도메인] [공인IP]
#   예시  : ./scripts/generate-env.sh mylive.duckdns.org 3.35.10.20
set -e
cd "$(dirname "$0")/.."

if [ -f .env ]; then
  echo ".env 가 이미 있습니다. 덮어쓰려면 먼저 삭제하세요." >&2
  exit 1
fi

DOMAIN="${1:-}"
PUBLIC_IP="${2:-127.0.0.1}"
rand() { openssl rand -hex "$1"; }

if [ -n "$DOMAIN" ]; then
  SITE="$DOMAIN"
  SECURE=true
else
  SITE="http://localhost"
  SECURE=false
fi

cp .env.example .env
sed -i.bak \
  -e "s|^SITE_ADDRESS=.*|SITE_ADDRESS=${SITE}|" \
  -e "s|^PUBLIC_HOST=.*|PUBLIC_HOST=${PUBLIC_IP}|" \
  -e "s|^COOKIE_SECURE=.*|COOKIE_SECURE=${SECURE}|" \
  -e "s|^POSTGRES_PASSWORD=.*|POSTGRES_PASSWORD=$(rand 16)|" \
  -e "s|^JWT_SECRET=.*|JWT_SECRET=$(rand 32)|" \
  -e "s|^INTERNAL_API_KEY=.*|INTERNAL_API_KEY=$(rand 24)|" \
  .env
rm -f .env.bak
echo ".env 생성 완료. TOSS_CLIENT_KEY, TOSS_SECRET_KEY 는 직접 입력하세요."
