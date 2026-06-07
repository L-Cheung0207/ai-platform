<script setup>
import { computed, onMounted, reactive, ref, shallowRef } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Edit, Plus, Refresh } from '@element-plus/icons-vue'
import api from '../../services/api'

const users = ref([])
const total = shallowRef(0)
const currentPage = shallowRef(1)
const pageSize = shallowRef(20)
const loading = shallowRef(false)
const submitting = shallowRef(false)
const deletingUserId = shallowRef(null)
const dialogVisible = shallowRef(false)
const dialogMode = shallowRef('create')
const error = shallowRef('')
const formRef = ref(null)

const userForm = reactive({
  id: null,
  username: '',
  password: '',
  role: 'NORMAL',
  skillGovernanceRole: 'CONTRIBUTOR',
})

const systemRoleOptions = [
  { value: 'NORMAL', label: '普通用户', type: 'info' },
  { value: 'ADMIN', label: '管理员', type: 'danger' },
]

const governanceRoleOptions = [
  { value: 'CONTRIBUTOR', label: '贡献者' },
  { value: 'TECH_LEAD', label: 'Tech Lead' },
  { value: 'PLATFORM_ENGINEERING', label: '平台/效能团队' },
  { value: 'TECHNICAL_COMMITTEE', label: '技术委员会' },
  { value: 'SECURITY_QUALITY', label: '安全/质量团队' },
]

const dialogTitle = computed(() => (dialogMode.value === 'create' ? '新增用户' : '编辑用户'))

const formRules = computed(() => ({
  username: [{ required: true, message: '用户名不能为空', trigger: 'blur' }],
  password: dialogMode.value === 'create'
    ? [
        { required: true, message: '密码不能为空', trigger: 'blur' },
        { min: 6, message: '密码至少6位', trigger: 'blur' },
      ]
    : [],
  role: [{ required: true, message: '系统角色不能为空', trigger: 'change' }],
  skillGovernanceRole: [{ required: true, message: '治理角色不能为空', trigger: 'change' }],
}))

onMounted(loadUsers)

async function loadUsers() {
  loading.value = true
  error.value = ''
  try {
    const data = await api.get('/admin/users', {
      params: { page: currentPage.value, size: pageSize.value },
    })
    users.value = data.items || []
    total.value = data.total || 0
  } catch (e) {
    error.value = e.message || '用户列表加载失败'
  } finally {
    loading.value = false
  }
}

function openCreateDialog() {
  resetForm()
  dialogMode.value = 'create'
  dialogVisible.value = true
}

function openEditDialog(row) {
  userForm.id = row.id
  userForm.username = row.username
  userForm.password = ''
  userForm.role = row.role || 'NORMAL'
  userForm.skillGovernanceRole = row.skillGovernanceRole || 'CONTRIBUTOR'
  dialogMode.value = 'edit'
  dialogVisible.value = true
}

async function submitUser() {
  await formRef.value?.validate()
  submitting.value = true
  error.value = ''
  try {
    if (dialogMode.value === 'create') {
      await api.post('/admin/users', {
        username: userForm.username.trim(),
        password: userForm.password,
        role: userForm.role,
        skillGovernanceRole: userForm.skillGovernanceRole,
      })
      ElMessage.success('用户已创建')
    } else {
      await api.put(`/admin/users/${userForm.id}`, {
        role: userForm.role,
        skillGovernanceRole: userForm.skillGovernanceRole,
      })
      ElMessage.success('用户已更新')
    }
    dialogVisible.value = false
    await loadUsers()
  } catch (e) {
    error.value = e.message || '用户保存失败'
  } finally {
    submitting.value = false
  }
}

async function deleteUser(row) {
  try {
    await ElMessageBox.confirm(`确认删除用户「${row.username}」？`, '删除用户', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      confirmButtonClass: 'el-button--danger',
    })
    deletingUserId.value = row.id
    error.value = ''
    await api.delete(`/admin/users/${row.id}`)
    ElMessage.success('用户已删除')
    await loadUsers()
  } catch (e) {
    if (e === 'cancel' || e === 'close') return
    error.value = e.message || '用户删除失败'
  } finally {
    deletingUserId.value = null
  }
}

function resetForm() {
  userForm.id = null
  userForm.username = ''
  userForm.password = ''
  userForm.role = 'NORMAL'
  userForm.skillGovernanceRole = 'CONTRIBUTOR'
  formRef.value?.clearValidate()
}

function systemRoleMeta(value) {
  return systemRoleOptions.find((item) => item.value === value) || systemRoleOptions[0]
}

function governanceRoleLabel(value) {
  return governanceRoleOptions.find((item) => item.value === value)?.label || value || '-'
}

function formatDate(value) {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 19)
}

function handlePageChange(page) {
  currentPage.value = page
  loadUsers()
}
</script>

<template>
  <div class="user-manage">
    <div class="user-manage__header">
      <div>
        <h1>用户管理</h1>
        <p>创建用户、维护系统权限和 Skill 治理评审权限</p>
      </div>
      <div class="user-manage__actions">
        <el-button :icon="Refresh" :loading="loading" @click="loadUsers">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="openCreateDialog">新增用户</el-button>
      </div>
    </div>

    <section class="user-panel">
      <el-table v-loading="loading" :data="users" empty-text="暂无用户">
        <el-table-column prop="id" label="ID" width="64" />
        <el-table-column prop="username" label="用户" min-width="150" show-overflow-tooltip />
        <el-table-column label="系统角色" width="110">
          <template #default="{ row }">
            <el-tag :type="systemRoleMeta(row.role).type" effect="plain">
              {{ systemRoleMeta(row.role).label }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="治理角色" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">{{ governanceRoleLabel(row.skillGovernanceRole) }}</template>
        </el-table-column>
        <el-table-column label="创建时间" width="160">
          <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <div class="user-table-actions">
              <el-button size="small" :icon="Edit" @click="openEditDialog(row)">编辑</el-button>
              <el-button
                size="small"
                type="danger"
                :icon="Delete"
                :loading="deletingUserId === row.id"
                @click="deleteUser(row)"
              >
                删除
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="user-panel__footer">
        <el-pagination
          background
          layout="prev, pager, next"
          :current-page="currentPage"
          :page-size="pageSize"
          :total="total"
          @current-change="handlePageChange"
        />
      </div>
    </section>

    <el-alert v-if="error" type="error" :title="error" show-icon />

    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="460px"
      destroy-on-close
      @closed="resetForm"
    >
      <el-form ref="formRef" :model="userForm" :rules="formRules" label-width="96px">
        <el-form-item label="用户名" prop="username">
          <el-input
            v-model="userForm.username"
            :disabled="dialogMode === 'edit'"
            maxlength="100"
            show-word-limit
          />
        </el-form-item>
        <el-form-item v-if="dialogMode === 'create'" label="密码" prop="password">
          <el-input v-model="userForm.password" type="password" show-password maxlength="100" />
        </el-form-item>
        <el-form-item label="系统角色" prop="role">
          <el-select v-model="userForm.role" class="user-dialog-select">
            <el-option
              v-for="item in systemRoleOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="治理角色" prop="skillGovernanceRole">
          <el-select v-model="userForm.skillGovernanceRole" class="user-dialog-select">
            <el-option
              v-for="item in governanceRoleOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitUser">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.user-manage {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.user-manage__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.user-manage__header h1 {
  color: #1f2937;
  font-size: 20px;
  font-weight: 700;
  margin: 0 0 6px;
}

.user-manage__header p {
  color: #6b7280;
  font-size: 14px;
  margin: 0;
}

.user-manage__actions {
  display: flex;
  gap: 10px;
}

.user-panel {
  min-width: 0;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
  padding: 16px;
}

.user-table-actions {
  display: flex;
  gap: 8px;
}

.user-dialog-select {
  width: 100%;
}

.user-panel__footer {
  display: flex;
  justify-content: flex-end;
  padding-top: 16px;
}

@media (max-width: 680px) {
  .user-manage__header {
    flex-direction: column;
  }

  .user-manage__actions {
    width: 100%;
    flex-wrap: wrap;
  }
}
</style>
