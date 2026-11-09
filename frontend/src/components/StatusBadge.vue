<script setup>
defineProps({ status: { type: String, required: true }, online: { type: Boolean, default: true } })
const LABELS = { LIVE: 'LIVE', READY: '대기', ENDED: '종료', RECORDING: '녹화 중', VOD: '다시보기', FAILED: '실패' }
</script>

<template>
  <span class="badge" :class="status.toLowerCase()">
    <template v-if="status === 'LIVE'">{{ online ? 'LIVE' : 'LIVE (연결 대기)' }}</template>
    <template v-else>{{ LABELS[status] || status }}</template>
  </span>
</template>
