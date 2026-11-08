<script setup>
import { useToastStore } from '../stores/toast'
const toast = useToastStore()
</script>

<template>
  <div class="toast-host">
    <div v-for="t in toast.items" :key="t.id" class="toast" :class="t.type" @click="toast.dismiss(t.id)">
      {{ t.message }}
    </div>
  </div>
</template>

<style scoped>
.toast-host {
  position: fixed; left: 50%; bottom: 24px; transform: translateX(-50%);
  display: flex; flex-direction: column; gap: 8px; z-index: 100; width: min(92vw, 420px);
}
.toast {
  padding: 12px 16px; border-radius: 10px; background: #2a2e37; color: var(--text);
  box-shadow: 0 6px 24px rgba(0,0,0,.4); font-size: 14px; cursor: pointer;
}
.toast.success { border-left: 4px solid var(--accent); }
.toast.error { border-left: 4px solid var(--danger); }
</style>
