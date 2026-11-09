#!/bin/sh
# MediaMTX 이벤트 훅 -> broadcast-service 알림
#   ready            : 송출 영상이 들어오기 시작함(방송 송출 상태 ON, 녹화 시작)
#   not-ready        : 송출이 끊김(송출 상태 OFF, 녹화 마무리)
#   segment-complete : 녹화 파일 하나가 저장 완료됨
EVENT="$1"
BASE_URL="${BROADCAST_HOOK_URL:-http://broadcast-service:8082}"
DATA="path=${MTX_PATH}"
if [ -n "${MTX_SEGMENT_PATH}" ]; then
  DATA="${DATA}&segmentPath=${MTX_SEGMENT_PATH}"
fi

wget -q -O /dev/null -T 5 \
  --header="X-Internal-Key: ${INTERNAL_API_KEY}" \
  --post-data="${DATA}" \
  "${BASE_URL}/internal/media/hooks/${EVENT}" \
  || echo "[hook] ${EVENT} 전달 실패 path=${MTX_PATH}"
