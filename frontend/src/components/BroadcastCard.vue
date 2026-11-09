<script setup>
import StatusBadge from './StatusBadge.vue'
import { formatDate, elapsedSince } from '../lib/format'

defineProps({ broadcast: { type: Object, required: true } })
</script>

<template>
  <RouterLink :to="`/broadcasts/${broadcast.id}`" class="bcard">
    <div class="thumb" :class="{ live: broadcast.status === 'LIVE' }">
      <div class="thumb-icon">{{ broadcast.sourceType === 'MOBILE_CAMERA' ? 'CAMERA' : 'SCREEN' }}</div>
      <div class="thumb-badge"><StatusBadge :status="broadcast.status" :online="broadcast.streamOnline" /></div>
      <div v-if="broadcast.status === 'LIVE'" class="thumb-time small">{{ elapsedSince(broadcast.startedAt) }}</div>
    </div>
    <div class="info">
      <div class="title">{{ broadcast.title }}</div>
      <div class="muted small">{{ broadcast.ownerNickname }}</div>
      <div class="muted small">
        {{ broadcast.status === 'LIVE' ? '방송 시작 ' + formatDate(broadcast.startedAt) : '등록 ' + formatDate(broadcast.createdAt) }}
      </div>
    </div>
  </RouterLink>
</template>

<style scoped>
.bcard { display: flex; flex-direction: column; gap: 10px; }
.thumb {
  position: relative; aspect-ratio: 16/9; border-radius: var(--radius);
  background: linear-gradient(135deg, #1d2027, #262a33);
  display: flex; align-items: center; justify-content: center;
  border: 1px solid var(--border); transition: transform .15s;
}
.bcard:hover .thumb { transform: translateY(-2px); }
.thumb.live { border-color: rgba(255, 59, 74, .6); background: linear-gradient(135deg, #2a1418, #1d2027); }
.thumb-icon { font-size: 14px; font-weight: 800; letter-spacing: .2em; color: var(--text-sub); }
.thumb-badge { position: absolute; top: 10px; left: 10px; }
.thumb-time { position: absolute; bottom: 8px; right: 10px; background: rgba(0,0,0,.6); padding: 1px 6px; border-radius: 4px; }
.title { font-weight: 700; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
</style>
