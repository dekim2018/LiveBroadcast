/**
 * 토스페이먼츠 결제위젯 SDK v2 (결제창 연동).
 * 문서: https://docs.tosspayments.com/sdk/v2/js
 */
const SDK_URL = 'https://js.tosspayments.com/v2/standard'
let loading = null

function loadSdk() {
  if (window.TossPayments) return Promise.resolve(window.TossPayments)
  if (loading) return loading
  loading = new Promise((resolve, reject) => {
    const script = document.createElement('script')
    script.src = SDK_URL
    script.onload = () => resolve(window.TossPayments)
    script.onerror = () => {
      loading = null
      reject(new Error('결제 모듈을 불러오지 못했습니다.'))
    }
    document.head.appendChild(script)
  })
  return loading
}

export async function requestCardPayment({ clientKey, customerKey, order, broadcastId }) {
  const TossPayments = await loadSdk()
  const tossPayments = TossPayments(clientKey)
  const payment = tossPayments.payment({ customerKey })
  const origin = window.location.origin
  await payment.requestPayment({
    method: 'CARD',
    amount: { currency: 'KRW', value: order.amount },
    orderId: order.orderId,
    orderName: order.orderName,
    customerName: order.donorNickname,
    successUrl: `${origin}/donations/success?broadcastId=${broadcastId}`,
    failUrl: `${origin}/donations/fail?broadcastId=${broadcastId}`
  })
}
