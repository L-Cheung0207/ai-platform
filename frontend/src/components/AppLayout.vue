<template>
  <div class="app-layout flex flex-col bg-[#f7f8fa]" :class="{ 'app-layout--auth': isAuthRoute }">
    <AppNavbar />
    <main class="flex-1 min-h-0">
      <slot />
    </main>
    <AppFooter v-if="!noFooter && !isAuthRoute" />
  </div>
</template>

<script setup>
import { computed, watch, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import AppNavbar from './AppNavbar.vue'
import AppFooter from './AppFooter.vue'

defineProps({
  noFooter: { type: Boolean, default: false },
})

const route = useRoute()
const isAuthRoute = computed(() => route.path === '/login' || route.path === '/register')

const CLASS_SCROLLBAR = 'scrollbar-visible'

function updateScrollbarClass() {
  document.documentElement.classList.toggle(CLASS_SCROLLBAR, !isAuthRoute.value)
}

onMounted(updateScrollbarClass)
watch(isAuthRoute, updateScrollbarClass)
onUnmounted(() => document.documentElement.classList.remove(CLASS_SCROLLBAR))
</script>

<style scoped>
.app-layout {
  min-height: 100vh;
}
.app-layout--auth {
  height: 100vh;
  min-height: 100vh;
  overflow: hidden;
}
</style>
