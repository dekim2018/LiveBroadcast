import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from './stores/auth'

const routes = [
  { path: '/', name: 'home', component: () => import('./views/HomeView.vue') },
  { path: '/login', name: 'login', component: () => import('./views/LoginView.vue') },
  { path: '/signup', name: 'signup', component: () => import('./views/SignupView.vue') },
  { path: '/broadcasts/new', name: 'broadcast-new', component: () => import('./views/BroadcastFormView.vue'), meta: { auth: true } },
  { path: '/broadcasts/:id/edit', name: 'broadcast-edit', component: () => import('./views/BroadcastFormView.vue'), meta: { auth: true } },
  { path: '/broadcasts/:id', name: 'watch', component: () => import('./views/WatchView.vue') },
  { path: '/studio/:id', name: 'studio', component: () => import('./views/StudioView.vue'), meta: { auth: true } },
  { path: '/me/broadcasts', name: 'my-broadcasts', component: () => import('./views/MyBroadcastsView.vue'), meta: { auth: true } },
  { path: '/recordings', name: 'recordings', component: () => import('./views/RecordingsView.vue') },
  { path: '/recordings/:id', name: 'recording', component: () => import('./views/RecordingDetailView.vue') },
  { path: '/donations/success', name: 'donation-success', component: () => import('./views/DonationResultView.vue'), meta: { auth: true }, props: { result: 'success' } },
  { path: '/donations/fail', name: 'donation-fail', component: () => import('./views/DonationResultView.vue'), props: { result: 'fail' } },
  { path: '/:pathMatch(.*)*', name: 'not-found', component: () => import('./views/NotFoundView.vue') }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 })
})

// 화면 접근 제어는 UX 용도이며, 실제 권한 검증은 모든 API 에서 서버가 다시 한다.
router.beforeEach((to) => {
  const auth = useAuthStore()
  if (to.meta.auth && !auth.isLoggedIn) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  return true
})

export default router
