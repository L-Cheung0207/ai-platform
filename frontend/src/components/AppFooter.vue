<template>
  <footer class="mt-auto py-8 px-6 bg-white border-t border-[#e5e7eb]">
    <div class="max-w-[1280px] mx-auto flex flex-col items-center gap-4 text-center">
      <p class="text-sm text-[#6b7280]">© {{ year }} AI原生实践中心（AI Native Hub）</p>
    </div>
  </footer>
</template>

<script setup>
import { computed } from 'vue'
const year = computed(() => new Date().getFullYear())
</script>
