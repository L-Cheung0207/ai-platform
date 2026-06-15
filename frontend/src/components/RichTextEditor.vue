<template>
  <div class="rich-text-editor">
    <div v-if="contentType !== 'MARKDOWN'" class="editor-mode-tabs">
      <button
        type="button"
        :class="{ active: mode === 'html' }"
        @click="setMode('html')"
      >
        富文本（所见即所得）
      </button>
      <button
        type="button"
        :class="{ active: mode === 'markdown' }"
        @click="setMode('markdown')"
      >
        Markdown（纯文本语法）
      </button>
    </div>
    <div v-show="contentType !== 'MARKDOWN' && mode === 'html'" class="quill-editor-shell">
      <QuillEditor
        v-model:content="content"
        content-type="html"
        :options="editorOptions"
        @ready="onEditorReady"
      />
    </div>
    <textarea
      v-show="contentType === 'MARKDOWN' || mode === 'markdown'"
      v-model="mdContent"
      class="markdown-textarea"
      placeholder="支持 Markdown：**粗体**、*斜体*、[链接](url)、![图片](url)、# 标题、列表等…"
    />
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { QuillEditor } from '@vueup/vue-quill'
import '@vueup/vue-quill/dist/vue-quill.snow.css'
import { marked } from 'marked'
import TurndownService from 'turndown'
import { ElMessage } from 'element-plus'
import api from '../services/api'

const props = defineProps({
  modelValue: { type: String, default: '' },
  /** 用于文章：RICH_TEXT=富文本存 HTML，MARKDOWN=存原始 Markdown */
  contentType: { type: String, default: 'RICH_TEXT' },
})
const emit = defineEmits(['update:modelValue'])

const mode = ref('html')
const content = ref(props.modelValue || '')
const mdContent = ref('')

const turndownService = new TurndownService()

watch(() => [props.modelValue, props.contentType], () => {
  const s = (props.modelValue || '').trim()
  if (props.contentType === 'MARKDOWN') {
    mode.value = 'markdown'
    mdContent.value = s
    return
  }
  if (mode.value === 'html' && content.value !== s) content.value = s
  if (mode.value === 'markdown') {
    try {
      mdContent.value = s ? turndownService.turndown(s) : ''
    } catch {
      mdContent.value = s
    }
  }
}, { immediate: true })

watch(content, (v) => {
  if (mode.value !== 'html') return
  emit('update:modelValue', (v !== undefined && v !== null) ? String(v) : '')
})

watch(mdContent, (v) => {
  if (mode.value !== 'markdown') return
  const str = (v !== undefined && v !== null) ? String(v) : ''
  if (props.contentType === 'MARKDOWN') {
    emit('update:modelValue', str)
    return
  }
  try {
    const html = str.trim() ? marked.parse(str, { async: false }) : ''
    emit('update:modelValue', html || '')
  } catch (e) {
    console.warn('Markdown parse error', e)
  }
})

function setMode(next) {
  if (next === mode.value) return
  if (props.contentType === 'MARKDOWN') return
  if (next === 'markdown') {
    try {
      mdContent.value = (content.value && String(content.value).trim())
        ? turndownService.turndown(content.value)
        : ''
    } catch {
      mdContent.value = content.value || ''
    }
    mode.value = 'markdown'
    const html = (mdContent.value && mdContent.value.trim()) ? marked.parse(mdContent.value, { async: false }) : ''
    if (html) emit('update:modelValue', html)
  } else {
    const html = (mdContent.value && mdContent.value.trim()) ? marked.parse(mdContent.value, { async: false }) : ''
    content.value = html || ''
    emit('update:modelValue', content.value)
    mode.value = 'html'
  }
}

const editorOptions = {
  placeholder: '输入正文，支持图片、加粗、列表等格式…',
  modules: {
    toolbar: [
      ['bold', 'italic', 'underline', 'strike'],
      [{ header: [1, 2, 3, false] }],
      [{ list: 'ordered' }, { list: 'bullet' }],
      ['link', 'image'],
      [{ color: [] }, { background: [] }],
      ['blockquote', 'code-block'],
      ['clean'],
    ],
  },
}

let quillInstance = null
function onEditorReady(quill) {
  quillInstance = quill
  const toolbar = quill.getModule('toolbar')
  toolbar.addHandler('image', imageHandler)
}

async function imageHandler() {
  const input = document.createElement('input')
  input.setAttribute('type', 'file')
  input.setAttribute('accept', 'image/jpeg,image/png,image/gif,image/webp')
  input.onchange = async () => {
    const file = input.files?.[0]
    if (!file) return
    try {
      const form = new FormData()
      form.append('file', file)
      const data = await api.post('/admin/upload', form)
      const url = data?.url
      if (url) {
        const range = quillInstance.getSelection(true)
        quillInstance.insertEmbed(range.index, 'image', url)
        quillInstance.setSelection(range.index + 1)
      }
    } catch (e) {
      console.error('Image upload failed', e)
      ElMessage.error(e.message || '图片上传失败')
    }
  }
  input.click()
}
</script>

<style scoped>
.rich-text-editor {
  width: 100%;
}
.rich-text-editor :deep(.ql-toolbar),
.rich-text-editor :deep(.ql-container) {
  width: 100%;
}
.rich-text-editor :deep(.ql-container) {
  min-height: 280px;
  font-size: 14px;
}
.rich-text-editor :deep(.ql-editor) {
  min-height: 280px;
}

.editor-mode-tabs {
  display: flex;
  gap: 0;
  margin-bottom: 8px;
}
.editor-mode-tabs button {
  padding: 6px 14px;
  font-size: 13px;
  color: #606266;
  background: #f5f7fa;
  border: 1px solid #e4e7ed;
  cursor: pointer;
}
.editor-mode-tabs button:first-child {
  border-radius: 4px 0 0 4px;
}
.editor-mode-tabs button:last-child {
  border-radius: 0 4px 4px 0;
}
.editor-mode-tabs button.active {
  color: #409eff;
  background: #ecf5ff;
  border-color: #409eff;
}
.editor-mode-tabs button:hover:not(.active) {
  color: #409eff;
}

.markdown-textarea {
  width: 100%;
  min-height: 280px;
  padding: 12px;
  font-size: 14px;
  line-height: 1.6;
  font-family: ui-monospace, monospace;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  resize: vertical;
  box-sizing: border-box;
}
.markdown-textarea:focus {
  outline: none;
  border-color: #409eff;
}
</style>
