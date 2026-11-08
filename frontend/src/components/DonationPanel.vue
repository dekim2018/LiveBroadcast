<script setup>
import { computed, onMounted, ref } from 'vue'
import { donationApi } from '../api/interactions'
import { errorMessage } from '../api/http'
import { requestCardPayment } from '../lib/toss'
import { formatDate, formatWon } from '../lib/format'
import { useAuthStore } from '../stores/auth'
import { useToastStore } from '../stores/toast'

/**
 * 후원: 금액 선택 -> 서버에 주문 생성(후원 대상은 서버가 방송 소유자로 결정) -> 토스 테스트 결제창
 * -> 성공 페이지에서 서버 승인. 확정된 후원은 SSE 로 모든 시청자에게 실시간 표시된다.
 */
const props = defineProps({
  broadcast: { type: Object, required: true },
  events: { type: Object, required: true }
})

const PRESETS = [1000, 5000, 10000, 50000]
const auth = useAuthStore()
const toast = useToastStore()
const config = ref(null)
const amount = ref(5000)
const message = ref('')
const paying = ref(false)
const summary = ref({ totalAmount: 0, count: 0 })
const donations = ref([])
const highlight = ref(null)

const isOwner = computed(() => auth.user?.id === props.broadcast.ownerId)
const canDonate = computed(() => auth.isLoggedIn && !isOwner.value && !!config.value?.clientKey)

async function load() {
  try {
    const [cfg, sum, list] = await Promise.all([
      donationApi.config(),
      donationApi.summary(props.broadcast.id),
      donationApi.list(props.broadcast.id, { size: 10 })
    ])
    config.value = cfg
    summary.value = sum
    donations.value = list.content
  } catch (e) {
    toast.error(errorMessage(e, '후원 정보를 불러오지 못했습니다.'))
  }
}

async function donate() {
  if (paying.value) return
  const value = Number(amount.value)
  if (!Number.isInteger(value) || value < config.value.minAmount || value > config.value.maxAmount) {
    toast.error(`후원 금액은 ${formatWon(config.value.minAmount)} ~ ${formatWon(config.value.maxAmount)} 사이로 입력해 주세요.`)
    return
  }
  paying.value = true
  try {
    const order = await donationApi.createOrder({ broadcastId: props.broadcast.id, amount: value, message: message.value })
    await requestCardPayment({
      clientKey: config.value.clientKey,
      customerKey: order.customerKey,
      order,
      broadcastId: props.broadcast.id
    })
  } catch (e) {
    // 사용자가 결제창을 닫은 경우 등
    if (e?.code !== 'USER_CANCEL') toast.error(errorMessage(e, '결제를 시작하지 못했습니다.'))
  } finally {
    paying.value = false
  }
}

props.events.on('donation.completed', (d) => {
  donations.value = [d, ...donations.value].slice(0, 10)
  highlight.value = d
  setTimeout(() => { if (highlight.value === d) highlight.value = null }, 6000)
})
props.events.on('donation.summary', (s) => { summary.value = s })

onMounted(load)
</script>

<template>
  <section class="card stack">
    <div class="row">
      <h3 style="margin:0">후원</h3>
      <span class="spacer"></span>
      <span class="small muted">총 {{ summary.count }}건</span>
      <strong class="total">{{ formatWon(summary.totalAmount) }}</strong>
    </div>

    <transition name="pop">
      <div v-if="highlight" class="alert">
        <strong>{{ highlight.donorNickname }}</strong>님이 {{ formatWon(highlight.amount) }} 후원!
        <div v-if="highlight.message" class="small">"{{ highlight.message }}"</div>
      </div>
    </transition>

    <div v-if="canDonate" class="stack">
      <div class="small muted">후원 대상: <strong>{{ broadcast.ownerNickname }}</strong></div>
      <div class="row">
        <button v-for="p in PRESETS" :key="p" class="btn sm" :class="{ primary: amount === p }" @click="amount = p">
          {{ formatWon(p) }}
        </button>
      </div>
      <input v-model.number="amount" class="input" type="number" :min="config.minAmount" :max="config.maxAmount" step="1000" />
      <input v-model="message" class="input" maxlength="200" placeholder="후원 메시지 (선택)" />
      <button class="btn primary block" :disabled="paying" @click="donate">
        {{ paying ? '결제창 여는 중...' : `${formatWon(amount)} 후원하기 (테스트 결제)` }}
      </button>
      <div class="small muted">토스페이먼츠 테스트 모드라 실제로 결제되지 않습니다.</div>
    </div>
    <div v-else-if="!auth.isLoggedIn" class="small muted">로그인하면 후원할 수 있습니다.</div>
    <div v-else-if="isOwner" class="small muted">내 방송에는 후원할 수 없습니다.</div>
    <div v-else-if="config && !config.clientKey" class="small muted">결제 키가 설정되지 않아 후원을 사용할 수 없습니다.</div>

    <div v-if="donations.length" class="stack list">
      <div v-for="d in donations" :key="d.id" class="item small">
        <div class="row">
          <strong>{{ d.donorNickname }}</strong>
          <span class="accent">{{ formatWon(d.amount) }}</span>
          <span class="spacer"></span>
          <span class="muted">{{ formatDate(d.approvedAt) }}</span>
        </div>
        <div v-if="d.message" class="muted">{{ d.message }}</div>
      </div>
    </div>
  </section>
</template>

<style scoped>
.total { color: var(--accent); }
.accent { color: var(--accent); font-weight: 700; }
.alert { background: linear-gradient(90deg, rgba(0,229,160,.18), rgba(0,229,160,.05)); border: 1px solid var(--accent); padding: 10px 12px; border-radius: 10px; }
.list { gap: 8px; max-height: 240px; overflow-y: auto; }
.item { padding: 8px 0; border-top: 1px solid var(--border); }
.pop-enter-active, .pop-leave-active { transition: all .25s; }
.pop-enter-from, .pop-leave-to { opacity: 0; transform: translateY(-6px); }
</style>
