<script setup>
import AppHeader from './components/AppHeader.vue'
import ToastHost from './components/ToastHost.vue'
</script>

<template>
  <AppHeader />
  <main class="container">
    <RouterView :key="$route.fullPath" />
  </main>
  <ToastHost />
</template>
