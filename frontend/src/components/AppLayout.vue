<template>
  <div class="min-h-screen flex flex-col bg-[#f7f8fa]">
    <AppNavbar />
    <main class="flex-1">
      <slot />
    </main>
    <AppFooter v-if="!noFooter" />
  </div>
</template>

<script setup>
import AppNavbar from './AppNavbar.vue'
import AppFooter from './AppFooter.vue'

defineProps({
  noFooter: { type: Boolean, default: false },
})
</script>
