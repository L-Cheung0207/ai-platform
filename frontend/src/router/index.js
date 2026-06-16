import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { setAuthToken } from '../services/api'
import Home from '../views/Home.vue'

const routes = [
  { path: '/', name: 'Home', component: Home, meta: { title: '首页' } },
  { path: '/skills', name: 'SkillList', component: () => import('../views/SkillList.vue'), meta: { title: 'Skill 列表' } },
  { path: '/skills/:id', name: 'SkillDetail', component: () => import('../views/SkillDetail.vue'), meta: { title: 'Skill 详情' } },
  { path: '/rules', name: 'RuleList', component: () => import('../views/RuleList.vue'), meta: { title: 'Rule 列表' } },
  { path: '/rules/:id', name: 'RuleDetail', component: () => import('../views/RuleDetail.vue'), meta: { title: 'Rule 详情' } },
  { path: '/articles', name: 'ArticleList', component: () => import('../views/ArticleList.vue'), meta: { title: 'AI知识库' } },
  { path: '/articles/:id', name: 'ArticleDetail', component: () => import('../views/ArticleDetail.vue'), meta: { title: '知识库详情' } },
  { path: '/news', name: 'NewsList', component: () => import('../views/NewsList.vue'), meta: { title: '资讯' } },
  { path: '/news/:id', name: 'NewsDetail', component: () => import('../views/NewsDetail.vue'), meta: { title: '资讯详情' } },
  { path: '/ai-tools', name: 'AiToolList', component: () => import('../views/AiToolList.vue'), meta: { title: 'AI 工具' } },
  { path: '/ai-tools/:id', name: 'AiToolDetail', component: () => import('../views/AiToolDetail.vue'), meta: { title: 'AI 工具详情' } },
  { path: '/llm-leaderboard', name: 'LlmLeaderboard', component: () => import('../views/LlmLeaderboard.vue'), meta: { title: 'LLM 排行榜' } },
  { path: '/forum', name: 'ForumList', component: () => import('../views/ForumList.vue'), meta: { title: '论坛' } },
  { path: '/forum/new', name: 'ForumNew', component: () => import('../views/ForumEditor.vue'), meta: { title: '发布论坛帖', requiresAuth: true } },
  { path: '/forum/mine', name: 'ForumMine', component: () => import('../views/ForumMine.vue'), meta: { title: '我的论坛', requiresAuth: true } },
  { path: '/forum/categories/:id', name: 'ForumCategory', component: () => import('../views/ForumList.vue'), meta: { title: '论坛分类' } },
  { path: '/forum/tags/:tag', name: 'ForumTag', component: () => import('../views/ForumList.vue'), meta: { title: '论坛标签' } },
  { path: '/forum/:id/edit', name: 'ForumEdit', component: () => import('../views/ForumEditor.vue'), meta: { title: '编辑论坛帖', requiresAuth: true } },
  { path: '/forum/:id', name: 'ForumDetail', component: () => import('../views/ForumDetail.vue'), meta: { title: '论坛详情' } },
  { path: '/login', name: 'Login', component: () => import('../views/Login.vue'), meta: { title: '登录' } },
  { path: '/register', name: 'Register', component: () => import('../views/Register.vue'), meta: { title: '注册' } },
  {
    path: '/admin',
    component: () => import('../views/admin/AdminLayout.vue'),
    meta: { requiresAdmin: true, title: '管理后台' },
    redirect: '/admin/skills',
    children: [
      { path: 'articles', name: 'AdminArticles', component: () => import('../views/admin/ArticleManage.vue'), meta: { title: 'AI知识库管理' } },
      { path: 'news', name: 'AdminNews', component: () => import('../views/admin/NewsManage.vue'), meta: { title: 'AI资讯' } },
      { path: 'github-trending', name: 'AdminGitHubTrending', component: () => import('../views/admin/GitHubTrendingManage.vue'), meta: { title: 'GitHub Trending' } },
      { path: 'skills', name: 'AdminSkills', component: () => import('../views/admin/SkillManage.vue'), meta: { title: 'Skill 管理' } },
      { path: 'skill-operations', name: 'AdminSkillOperations', component: () => import('../views/admin/SkillOperations.vue'), meta: { title: 'Skill 运营' } },
      { path: 'users', name: 'AdminUsers', component: () => import('../views/admin/UserManage.vue'), meta: { title: '用户管理' } },
      { path: 'rules', name: 'AdminRules', component: () => import('../views/admin/RuleManage.vue'), meta: { title: 'Rule 管理' } },
      { path: 'llm-leaderboard', name: 'AdminLlmLeaderboard', component: () => import('../views/admin/LlmLeaderboardManage.vue'), meta: { title: 'LLM 排行榜' } },
      { path: 'forum', name: 'AdminForum', component: () => import('../views/admin/ForumManage.vue'), meta: { title: '论坛管理' } },
      { path: 'forum/categories', name: 'AdminForumCategories', component: () => import('../views/admin/ForumCategoryManage.vue'), meta: { title: '论坛分类管理' } },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior(to, from, savedPosition) {
    // 浏览器前进/后退时恢复之前的滚动位置
    if (savedPosition) return savedPosition
    // 带 hash 的链接滚动到对应锚点
    if (to.hash) return { el: to.hash, behavior: 'smooth' }
    // 进入新页面（含详情页）时滚动到顶部
    return { top: 0 }
  },
})

router.beforeEach((to, from, next) => {
  const auth = useAuthStore()
  if (to.meta.requiresAuth && !auth.isAuthenticated) {
    next({ name: 'Login', query: { redirect: to.fullPath } })
    return
  }
  if (to.meta.requiresAuth) {
    setAuthToken(auth.token)
  }
  if (to.meta.requiresAdmin) {
    if (!auth.isAuthenticated) {
      next({ name: 'Login', query: { redirect: to.fullPath } })
      return
    }
    if (auth.user?.role !== 'ADMIN') {
      next({ name: 'Home' })
      return
    }
    // 进入管理后台前确保请求头带上 token，避免首屏请求 403
    setAuthToken(auth.token)
  }
  next()
})

export default router
