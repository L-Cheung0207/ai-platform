# 项目上下文

最后同步：2026-06-14

## 这是什么

这是一个内部 AI 资源与学习平台。
当前代码库更像 AI 资产运营中心，不只是一个简单内容站。

## 产品面

公开页面：
- `/`
- `/skills`
- `/rules`
- `/articles`
- `/news`
- `/llm-leaderboard`
- `/ai-tools`
- `/mcp`
- `/forum`
- `/forum/new`
- `/forum/:id`
- `/forum/:id/edit`
- `/forum/categories/:id`
- `/forum/tags/:tag`
- `/forum/mine`
- `/external-skills/:id`
- 首页右侧展示 GitHub Trending 周榜/月榜模块，数据来自后端定时同步。

管理后台：
- `/admin/skills`
- `/admin/external-skills`
- `/admin/rules`
- `/admin/articles`
- `/admin/news`
- `/admin/users`
- `/admin/skill-operations`
- `/admin/llm-leaderboard`
- `/admin/github-trending`
- `/admin/forum`
- `/admin/forum/categories`

## 核心领域

- `Skill`：内部受治理资产。包含 clone 命令、正文、仓库地址、资产层级、生命周期、Skill 分类、建设优先级、风险级别、模板校验、评审数据、反馈数据、使用遥测。
- `Rule`：内部文本资产。包含正文、分类、标签、可见性。
- `ExternalSkill`：只存元数据的外部技能。包含安装命令、来源 URL、标签、可见性。
- `LearningArticle`：管理员发布的知识内容。
- `News`：管理员发布的资讯内容。
- `LlmLeaderboardEntry`：公开排行榜数据。
- `GitHubTrendingEntry`：GitHub Trending 周榜/月榜记录，包含排名、仓库、作用、场景、摘要状态和抓取批次。
- `GitHubTrendingConfig`：GitHub Trending 同步配置与状态。首版刷新时间固定为每天 08:00，后台配置不动态修改 cron。
- `AiTool` 和 `McpServer`：应用里已经存在的额外目录面。
- `Forum`：内部技术交流模块。包含分类、帖子、回复、点赞、收藏、采纳答案、置顶、锁定、隐藏和删除。帖子正文按 Markdown 存储，标签复用全局 `Tag`。

## 运行环境

- 前端：Vue 3 + Vite + Pinia + Vue Router + Element Plus。
- 前端开发端口：`8888`。
- 后端：Spring Boot 3 + JPA + Security + Flyway。
- 后端开发端口：`8081`。
- 开发数据库：`./data/ai_platform` 的 H2 文件数据库。
- `backend/src/main/resources/application.yml` 里仍保留生产风格默认数据库配置。
- API 前缀：`/api`。
- Vite 会把 `/api` 和 `/uploads` 代理到后端。

## 认证

- JWT 认证。
- 公共浏览匿名可用。
- 登录/注册走 `/api/auth`。
- 角色：`NORMAL`、`ADMIN`。
- Skill 治理角色用于评审和运营流程。
- 前端只在接口返回 `401` 时清空登录态并跳回登录页；`403` 视为权限失败，不应主动登出，否则会出现“点后台菜单被踢出，刷新后又恢复”的假性掉线。

## 开发事实

- 默认开发管理员：`admin / admin123`。
- `GET /api/home` 负责首页聚合，包含 Skill、外部 Skill、知识、资讯、LLM 排行榜和 GitHub Trending 周榜/月榜数据。
- GitHub Trending 由后端每天早上自动同步，也可在后台手动刷新；手动刷新异步启动并有单实例运行保护；首页读取 `/api/home` 聚合数据，不单独请求公开 Trending 接口。
- GitHub Trending 后台 API 位于 `/api/admin/github-trending`，仅 `ADMIN` 可用，支持状态、配置、列表、手动同步、摘要编辑和单条重生成。
- 论坛默认分类由启动初始化补齐：`提问求助`、`经验分享`、`方案讨论`、`最佳实践`。
- 论坛公开 API：`GET /api/forum/posts`、`GET /api/forum/posts/{id}`、`GET /api/forum/categories`、`GET /api/forum/tags`。
- 论坛登录 API：发帖、编辑、删除、回复、点赞、收藏、采纳、`/api/forum/mine/*`。
- 论坛管理 API：`/api/admin/forum/posts` 和 `/api/admin/forum/categories`，仅 `ADMIN` 可用。
- 当前应用状态就是事实来源。
- 旧的 `specs/` 和 `SPEC_KIT_FEED.md` 已经故意删除。
- 论坛前端已处理两类 Vue/Element Plus 日志坑：不要把 `v-show` 直接挂在 `QuillEditor` 上；后台论坛筛选里的“全部”选项用空字符串，不用 `null`。
- 论坛后端测试重点在 `ForumApiIntegrationTest` 和 `ForumCategoryServiceTest`：覆盖公共读、登录写、权限边界、列表筛选/热度排序、我的内容、管理员审核动作、分类 slug/禁用/重复校验。

## 启动

- 后端：在 `backend/` 下运行 `SPRING_PROFILES_ACTIVE=dev rtk mvn spring-boot:run`
- 前端：在 `frontend/` 下运行 `rtk npm run dev`
- 浏览器检查地址：`http://127.0.0.1:8888`

## 维护规则

- 把这个文件当作本仓库唯一的活上下文。
- 只要路由、端口、认证、实体、种子数据、启动流程或主要 UI 行为变化，就要在同一轮里更新这里。
- 如果代码和文档冲突，以代码为准，直到这里被更新。
- 历史 spec 草稿不要再放进主路径。

## Agent 备注

- 未来在这个仓库里工作时，先读这个文件。
- 如果你让我改代码，而改动会影响仓库事实、启动方式、架构或用户可见行为，我应该同步刷新这个文件。
- 运行 shell 命令前先加 `rtk`。
- 搜索优先用 `rg` / `rg --files`。
- 手工编辑优先用 `apply_patch`。
- 前端主要在 `frontend/src/views`、`frontend/src/components`、`frontend/src/router`、`frontend/src/services/api.js`、`frontend/src/stores/auth.js`。
- 后端主要在 `backend/src/main/java/com/example/platform/{controller,service,entity,config,security}`。
- 配置在 `backend/src/main/resources/application*.yml`。
- 迁移在 `backend/src/main/resources/db/migration`。
- 补充文档在 `docs/ai-skill-assetization-coverage.md`、`docs/skill-toolchain-telemetry.md` 和 `docs/forum-design.md`。
