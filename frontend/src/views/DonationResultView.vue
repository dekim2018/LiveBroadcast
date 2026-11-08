<script setup>
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { donationApi } from '../api/interactions'
import { errorMessage } from '../api/http'
import { formatWon } from '../lib/format'
import { useAuthStore } from '../stores/auth'

/**
 * 토스 결제창에서 돌아오는 페이지.
 * success: paymentKey, orderId, amount 를 서버로 보내 승인(서버가 토스에 직접 승인 요청 후 검증)
 * fail   : 주문을 실패 처리
 */
const props = defineProps({ result: { type: String, required: true } })
const route = useRoute()
const auth = useAuthStore()
const state = ref('processing') // processing | done | failed
const message = ref('')
const donation = ref(null)
const broadcastId = route.query.broadcastId

onMounted(async () => {
  const { paymentKey, orderId, amount, code } = route.query
  if (props.result === 'success') {
    try {
      donation.value = await donationApi.confirm({ paymentKey, orderId, amount: Number(amount) })
      state.value = 'done'
    } catch (e) {
      state.value = 'failed'
      message.value = errorMessage(e, '후원을 확정하지 못했습니다.')
    }
  } else {
    state.value = 'failed'
    message.value = route.query.message || '결제가 취소되었거나 실패했습니다.'
    if (orderId && auth.isLoggedIn) {
      donationApi.cancel(orderId, code || 'PAYMENT_FAILED').catch(() => {})
    }
  }
})
</script>

<template>
  <div class="result card stack">
    <template v-if="state === 'processing'">
      <h1>후원을 확정하는 중입니다...</h1>
      <p class="muted">서버에서 결제 승인 결과와 금액을 확인하고 있습니다.</p>
    </template>
    <template v-else-if="state === 'done'">
      <h1>후원이 완료되었습니다</h1>
      <p><strong>{{ donation.streamerNickname }}</strong>님에게 <strong class="accent">{{ formatWon(donation.amount) }}</strong>을 후원했습니다.</p>
      <p v-if="donation.message" class="muted">"{{ donation.message }}"</p>
    </template>
    <template v-else>
      <h1>후원에 실패했습니다</h1>
      <p class="muted">{{ message }}</p>
    </template>
    <div class="row">
      <RouterLink v-if="broadcastId" :to="`/broadcasts/${broadcastId}`" class="btn primary">방송으로 돌아가기</RouterLink>
      <RouterLink to="/" class="btn">홈으로</RouterLink>
    </div>
  </div>
</template>

<style scoped>
.result { max-width: 520px; margin: 40px auto; }
.accent { color: var(--accent); }
</style>
