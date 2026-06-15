<template>
  <div class="forum-editor-page">
    <PageHero
      variant="teal"
      :title="isEdit ? '编辑帖子' : '发布技术交流帖'"
      subtitle="用 Markdown 记录问题、方案、经验和可复用的上下文"
    />

    <main class="forum-editor-shell max-w-[1100px] mx-auto px-6 py-8">
      <el-card class="forum-editor-card" shadow="never">
        <el-form label-position="top" @submit.prevent="save">
          <div class="forum-editor-grid">
            <el-form-item label="标题" required>
              <el-input v-model="form.title" maxlength="300" show-word-limit placeholder="一句话说明问题或经验" />
            </el-form-item>

            <el-form-item label="类型" required>
              <el-select v-model="form.postType" class="w-full">
                <el-option label="提问：需要一个答案" value="QUESTION" />
                <el-option label="讨论：开放式方案交流" value="DISCUSSION" />
                <el-option label="分享：经验、踩坑、实践沉淀" value="SHARE" />
              </el-select>
            </el-form-item>

            <el-form-item label="分类" required>
              <el-select v-model="form.categoryId" class="w-full" placeholder="选择分类">
                <el-option v-for="cat in categories" :key="cat.id" :label="cat.name" :value="cat.id" />
              </el-select>
            </el-form-item>

            <el-form-item label="标签">
              <el-select v-model="form.tagIds" class="w-full" multiple collapse-tags collapse-tags-tooltip placeholder="最多选择 5 个">
                <el-option v-for="tag in tags" :key="tag.id" :label="tag.name" :value="tag.id" />
              </el-select>
            </el-form-item>
          </div>

          <div class="forum-editor-related">
            <el-form-item label="关联内容类型">
              <el-select v-model="form.relatedType" class="w-full" clearable placeholder="可选">
                <el-option label="Skill" value="SKILL" />
                <el-option label="Rule" value="RULE" />
                <el-option label="AI知识库" value="ARTICLE" />
                <el-option label="AI 工具" value="AI_TOOL" />
              </el-select>
            </el-form-item>
            <el-form-item label="关联 ID">
              <el-input-number v-model="form.relatedId" class="w-full" :min="1" controls-position="right" />
            </el-form-item>
            <el-form-item label="关联标题">
              <el-input v-model="form.relatedTitle" placeholder="例如：java-code-review / Rule 名称 / 工具名" />
            </el-form-item>
          </div>

          <el-form-item label="正文" required>
            <RichTextEditor v-model="form.content" content-type="MARKDOWN" />
          </el-form-item>

          <div class="forum-editor-actions">
            <el-button @click="$router.push('/forum')">取消</el-button>
            <el-button type="primary" :loading="saving" @click="save">
              {{ isEdit ? '保存修改' : '发布帖子' }}
            </el-button>
          </div>
        </el-form>
      </el-card>

      <aside class="forum-editor-tips">
        <h3>发帖小提示</h3>
        <ul>
          <li>提问帖尽量写清上下文、复现步骤和你已经尝试过的方案。</li>
          <li>分享帖建议把结论、适用场景和风险边界写在前面。</li>
          <li>如果这个讨论能沉淀成 Skill / Rule，记得填写关联内容。</li>
        </ul>
      </aside>
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import PageHero from '../components/PageHero.vue'
import RichTextEditor from '../components/RichTextEditor.vue'
import api from '../services/api'

const route = useRoute()
const router = useRouter()

const isEdit = computed(() => route.path.endsWith('/edit'))
const categories = ref([])
const tags = ref([])
const saving = ref(false)
const form = ref({
  title: '',
  content: '',
  postType: 'DISCUSSION',
  categoryId: null,
  tagIds: [],
  relatedType: '',
  relatedId: null,
  relatedTitle: '',
})

onMounted(async () => {
  await Promise.all([loadCategories(), loadTags()])
  if (isEdit.value) await loadPost()
})

async function loadCategories() {
  categories.value = await api.get('/forum/categories')
  if (!form.value.categoryId && categories.value.length) {
    form.value.categoryId = categories.value[0].id
  }
}

async function loadTags() {
  try {
    tags.value = await api.get('/forum/tags')
  } catch {
    tags.value = []
  }
}

async function loadPost() {
  const post = await api.get('/forum/posts/' + route.params.id)
  form.value = {
    title: post.title || '',
    content: post.content || '',
    postType: post.postType || 'DISCUSSION',
    categoryId: post.categoryId,
    tagIds: (post.tags || []).map((tag) => tag.id),
    relatedType: post.relatedType || '',
    relatedId: post.relatedId || null,
    relatedTitle: post.relatedTitle || '',
  }
}

async function save() {
  const title = (form.value.title || '').trim()
  const content = (form.value.content || '').trim()
  if (!title) {
    ElMessage.warning('请填写标题')
    return
  }
  if (!form.value.categoryId) {
    ElMessage.warning('请选择分类')
    return
  }
  if (!content) {
    ElMessage.warning('请填写正文')
    return
  }
  if (form.value.tagIds.length > 5) {
    ElMessage.warning('标签最多选择 5 个')
    return
  }
  saving.value = true
  try {
    const payload = {
      ...form.value,
      title,
      content,
      relatedType: form.value.relatedType || null,
      relatedId: form.value.relatedId || null,
      relatedTitle: (form.value.relatedTitle || '').trim() || null,
    }
    const data = isEdit.value
      ? await api.put('/forum/posts/' + route.params.id, payload)
      : await api.post('/forum/posts', payload)
    ElMessage.success(isEdit.value ? '已保存' : '已发布')
    router.push('/forum/' + data.id)
  } catch (e) {
    ElMessage.error(e.message || '保存失败')
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
@reference "../assets/styles.css";

.forum-editor-page {
  min-height: 100vh;
  background:
    radial-gradient(circle at 10% 8%, rgba(20, 184, 166, 0.08), transparent 26%),
    linear-gradient(180deg, #f8fbfb 0%, #eef5f4 100%);
}

.forum-editor-shell {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 280px;
  gap: 1rem;
  align-items: start;
}

@media (max-width: 960px) {
  .forum-editor-shell {
    grid-template-columns: 1fr;
  }
}

.forum-editor-card {
  border-radius: 20px;
  border: 1px solid rgba(15, 23, 42, 0.06);
  background: rgba(255, 255, 255, 0.88);
  box-shadow: 0 12px 30px rgba(15, 23, 42, 0.06);
}

.forum-editor-grid,
.forum-editor-related {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1rem;
}

.forum-editor-related {
  grid-template-columns: 1fr 180px 1fr;
}

@media (max-width: 768px) {
  .forum-editor-grid,
  .forum-editor-related {
    grid-template-columns: 1fr;
  }
}

.forum-editor-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
  margin-top: 1rem;
}

.forum-editor-tips {
  border: 1px solid rgba(15, 23, 42, 0.06);
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.82);
  padding: 1rem;
  color: #475569;
  position: sticky;
  top: 1rem;
}

.forum-editor-tips h3 {
  margin: 0 0 0.75rem;
  color: #0f172a;
  font-size: 1rem;
  font-weight: 800;
}

.forum-editor-tips ul {
  margin: 0;
  padding-left: 1.2rem;
  display: grid;
  gap: 0.55rem;
  line-height: 1.65;
  font-size: 0.9rem;
}
</style>
