<template>
  <div class="pt-8">
    <div class="max-w-[1400px] mx-auto px-6 py-8">
    <template v-if="article">
      <h1 class="text-2xl font-bold text-gray-800 mb-2">{{ article.title }}</h1>
      <p v-if="article.summary" class="text-gray-600 mb-6">{{ article.summary }}</p>
      <div class="mt-6 text-gray-800 leading-relaxed [&_a]:text-primary [&_h1]:text-xl [&_h2]:text-lg [&_ul]:list-disc [&_ul]:pl-6 [&_ol]:list-decimal [&_ol]:pl-6" v-html="article.content"></div>
    </template>
    <template v-else>
      <p v-if="loading" class="text-gray-500">加载中…</p>
      <el-alert v-else type="error" :title="error || '未找到'" show-icon />
    </template>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import api from '../services/api'

const route = useRoute()
const article = ref(null)
const loading = ref(true)
const error = ref('')

onMounted(async () => {
  try {
    article.value = await api.get('/articles/' + route.params.id)
  } catch (e) {
    error.value = e.message || '加载失败'
  } finally {
    loading.value = false
  }
})
</script>
