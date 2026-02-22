# 任务：内部 AI 资源与学习平台

**输入**：来自 `specs/001-ai-resource-platform/` 的设计文档  
**前置条件**：plan.md、spec.md、research.md、data-model.md、contracts/

**组织方式**：任务按用户故事分组，便于独立实现与测试。  
**规格变更说明**：Skill 与 Rule 的创建、编辑、删除均在管理后台完成；前台无「上传」「我的上传」。

## 格式：`[ID] [P?] [Story] 描述`

- **[P]**：可并行执行（不同文件、无依赖）
- **[Story]**：用户故事（US1、US2、US3）
- 描述中需包含精确文件路径

## 路径约定

- **Web 应用**：仓库根目录下的 `frontend/src/`、`backend/src/main/`

---

## 阶段 1：搭建（共享基础设施）

**目的**：项目初始化与基础结构

- [X] T001 按 plan 创建前后端目录结构（frontend/、backend/，布局见 plan.md）
- [X] T002 使用 Vite + Vue 3 初始化前端：`frontend/package.json` 含 vue、vue-router、pinia、vite
- [X] T003 使用 Spring Boot 3 初始化后端：`backend/pom.xml` 或 `backend/build.gradle` 含 spring-boot-starter-web、spring-boot-starter-data-jpa、spring-boot-starter-security、mysql-connector-j
- [X] T004 [P] 将项目根目录的 styles.css 复制或链接到 frontend/src/assets/，并在 frontend/src/main.js（或 main.ts）中配置全局引入
- [X] T005 [P] 在 frontend/vite.config.js 中配置 Vite 开发代理：将 /api 代理到 http://localhost:8080

---

## 阶段 2：基础（阻塞性前置）

**目的**：在任一用户故事实现前必须完成的核心基础设施

**检查点**：基础就绪后，方可开始用户故事实现

- [X] T006 在 backend/src/main/resources/application.yml 中配置后端 MySQL 与 JWT（datasource url、jpa、自定义 JWT secret/expiry 等）
- [X] T007 在后端创建全局 API 响应封装与异常处理：DTO `{ code, message, data }` 及针对 4xx/5xx 的 @ControllerAdvice，位于 backend/src/main/java/.../config/ 或 .../dto/
- [X] T008 [P] 创建 User 实体（id, username, passwordHash, role enum, createdAt, updatedAt），位于 backend/src/main/java/.../entity/User.java
- [X] T009 [P] 创建 Category 实体（id, name, type, sortOrder, createdAt），位于 backend/src/main/java/.../entity/Category.java
- [X] T010 [P] 创建 Tag 实体（id, name, createdAt），位于 backend/src/main/java/.../entity/Tag.java
- [X] T011 [P] 创建 **Skill** 实体（id, name, description, **cloneCommand**（必填）, categoryId, uploaderId, tags, visibility, createdAt, updatedAt）；**不存 content/附件**，位于 backend/src/main/java/.../entity/Skill.java
- [X] T011a [P] 创建 **Rule** 实体（id, name, description, content, categoryId, uploaderId, tags, visibility, createdAt, updatedAt），位于 backend/src/main/java/.../entity/Rule.java
- [X] T012 [P] 创建 LearningArticle 实体（id, title, summary, content, categoryId, authorId, status, createdAt, updatedAt），位于 backend/src/main/java/.../entity/LearningArticle.java
- [X] T013 [P] 创建 News 实体（id, title, summary, sourceUrl, sourceName, publishDate, createdAt），位于 backend/src/main/java/.../entity/News.java
- [X] T013a [P] 创建 ExternalSkill 实体（id, name, description, installCommand, sourceUrl, categoryId, tags, visibility, createdAt, updatedAt），位于 backend/src/main/java/.../entity/ExternalSkill.java
- [X] T014 为 User、Category、Tag、**Skill**、**Rule**、ExternalSkill、LearningArticle、News 创建 Spring Data JPA 仓库，位于 backend/src/main/java/.../repository/
- [X] T015 实现 JWT 生成与校验（如 JwtService 或 filter），位于 backend/src/main/java/.../security/ 或 .../config/
- [X] T016 配置 Spring Security：放行 GET /api/categories、/api/tags、/api/skills、/api/skills/*、/api/rules、/api/rules/*、/api/external-skills、/api/external-skills/*、/api/articles、/api/articles/*、/api/news、/api/news/*、/api/home；放行 POST /api/auth/login、POST /api/auth/register（v1 自注册）；**仅管理端接口与 Skill/Rule 写操作需 JWT + ADMIN**；前台浏览无需认证，位于 backend/src/main/java/.../config/SecurityConfig.java

---

## 阶段 3：用户故事 1 - 未登录浏览与使用全部内容（优先级：P1）— MVP

**目标**：未认证用户可浏览首页，列表/筛选/搜索 Skill/Rule，查看详情、复制/下载；浏览并查看文章与资讯。前台无「上传」「我的上传」入口。

**独立测试**：无痕模式打开应用；打开首页、Skill/Rule 列表（按分类/标签筛选、关键词搜索）、打开详情、复制内容；打开文章与资讯列表/详情。不出现登录提示；导航栏无「上传」「我的上传」。

- [X] T017 [P] [US1] 实现 GET /api/categories（列表；可选 type 查询）于后端 controller 与 service（如 backend/src/main/java/.../controller/CategoryController.java、.../service/CategoryService.java）
- [X] T018 [P] [US1] 实现 GET /api/tags（列表；可选 q 用于自动补全）于后端 controller 与 service（如 backend/src/main/java/.../controller/TagController.java、.../service/TagService.java）
- [X] T019 [US1] 实现 GET /api/skills（查询：keyword、categoryId、tags、page、size；**keyword 最大 200 字符、空则未筛选**；仅 visibility=VISIBLE；返回 items + total）与 GET /api/skills/:id（详情；响应含 **cloneCommand**）于后端（如 backend/src/main/java/.../controller/SkillController.java、.../service/SkillService.java）
- [X] T019a [US1] 实现 GET /api/rules（查询：keyword、categoryId、tags、page、size；**keyword 最大 200 字符、空则未筛选**；仅 visibility=VISIBLE）与 GET /api/rules/:id（详情）于后端（如 backend/src/main/java/.../controller/RuleController.java、.../service/RuleService.java）
- [X] T021 [US1] 实现 GET /api/articles（列表、分页）与 GET /api/articles/:id（详情；仅已发布）于后端（如 backend/src/main/java/.../controller/ArticleController.java、.../service/ArticleService.java）
- [X] T022 [US1] 实现 GET /api/news（列表、分页）与 GET /api/news/:id（详情）于后端（如 backend/src/main/java/.../controller/NewsController.java、.../service/NewsService.java）
- [X] T022a [US1] 实现 GET /api/external-skills（列表、分页、keyword/categoryId/tags；**keyword 最大 200 字符、空则未筛选**）与 GET /api/external-skills/:id（详情；仅 visibility=VISIBLE）于后端（如 backend/src/main/java/.../controller/ExternalSkillController.java、.../service/ExternalSkillService.java）
- [X] T023 [US1] 实现 GET /api/home（聚合 latestSkills、latestRules、latestExternalSkills、articles、news）于后端（如 backend/src/main/java/.../controller/HomeController.java、.../service/HomeService.java）
- [X] T024 [US1] 在前端创建 API 客户端（axios 或 fetch、baseURL、响应封装解析），位于 frontend/src/services/api.js（或 api.ts）
- [X] T025 [US1] 创建 Vue Router，路由：/、/skills、/skills/:id、/rules、/rules/:id、/external-skills、/external-skills/:id、/articles、/articles/:id、/news、/news/:id；**不包含** /upload、/my-uploads，位于 frontend/src/router/index.js
- [X] T026 [US1] 创建首页视图，调用 GET /api/home 并展示最新条目（含 latestSkills、latestRules、latestExternalSkills），位于 frontend/src/views/Home.vue
- [X] T027 [US1] 创建 **Skill** 列表视图（GET /api/skills）与 **Rule** 列表视图（GET /api/rules）：分类/标签筛选、关键词搜索、分页、卡片；可合并为同一页用 Tab 切换或分路由；**可合并展示外部 Skill**（GET /api/external-skills）并标识「来源：内部/外部」，位于 frontend/src/views/SkillList.vue、frontend/src/views/RuleList.vue（或合并视图）
- [X] T028 [US1] 创建 **Skill 详情视图**（GET /api/skills/:id；展示 **clone 命令**与「复制」按钮，无附件下载）、**Rule 详情视图**（GET /api/rules/:id，复制/下载正文）、**外部 Skill 详情视图**（GET /api/external-skills/:id，展示安装命令与「复制/跳转来源」），位于 frontend/src/views/SkillDetail.vue、frontend/src/views/RuleDetail.vue、frontend/src/views/ExternalSkillDetail.vue
- [X] T029 [US1] 创建文章列表与详情视图（公开），位于 frontend/src/views/ArticleList.vue 与 frontend/src/views/ArticleDetail.vue
- [X] T030 [US1] 创建资讯列表与详情视图（公开），位于 frontend/src/views/NewsList.vue 与 frontend/src/views/NewsDetail.vue
- [X] T031 [US1] 在 frontend/src/main.js 中引入全局样式（assets 中的 styles.css），并确保布局符合 README/styles.css 设计；在 frontend/src/components/AppNavbar.vue 中**不显示**「上传」「我的上传」入口

**检查点**：用户故事 1 完成；访客可未登录浏览全部内容；前台无上传相关入口。

---

## 阶段 4：用户故事 2 - 管理员管理 Skill、Rule、标签、文章与资讯（优先级：P2）

**目标**：管理员在管理后台创建、编辑、删除 Skill 与 Rule；管理标签、学习文章与资讯。**分类由种子数据预置**，不在管理后台维护。前台不提供「上传」「我的上传」。

**独立测试**：以管理员身份登录管理后台；创建 Skill（clone 命令）与 Rule；确认二者出现在前台公开列表。未认证访客打开前台，确认无「上传」「我的上传」入口。

- [X] T032 [US2] 实现 POST /api/auth/login（body：username、password；返回 JWT + user { id, username, role }）与 POST /api/auth/register（v1 自注册；body：username、password；返回 201 或 400）于后端；实现 GET /api/auth/me（从 JWT 返回当前用户），位于 backend/src/main/java/.../controller/AuthController.java、.../service/AuthService.java
- [X] T033 [US2] 实现 Skill 与 Rule 的**仅管理员**写操作：POST /api/admin/skills（body JSON `{ name, description, categoryId, tags[], cloneCommand }`）、PUT /api/admin/skills/:id、DELETE /api/admin/skills/:id；POST /api/admin/rules（body JSON `{ name, description, categoryId, tags[], content }`）、PUT /api/admin/rules/:id、DELETE /api/admin/rules/:id；**要求 ADMIN 角色**；创建者设为当前管理员，位于 backend/src/main/java/.../controller/AdminController.java 或 SkillController/RuleController（admin 路径）
- [X] T034 [US2] ~~分类管理~~ **已移除**：分类由种子数据预置（DataInitializer），仅保留 GET /api/categories 供筛选与选择
- [X] T035 [US2] 实现 POST /api/tags（仅管理员；标签仅由管理员维护）于后端
- [X] T036 [US2] 实现文章管理端 CRUD：POST /api/articles、PUT /api/articles/:id、DELETE /api/articles/:id（仅管理员）于后端
- [X] T037 [US2] 实现资讯管理端 CRUD：POST /api/news、PUT /api/news/:id、DELETE /api/news/:id（仅管理员）于后端
- [X] T038 [US2] 创建 Pinia 认证 store（token、user、login、logout、isAuthenticated），位于 frontend/src/stores/auth.js（或 auth.ts）
- [X] T039 [US2] 在 frontend/src/router/index.js 中为 /admin 下路径添加管理端路由守卫（要求 role 为 ADMIN）；**不包含** /upload、/my-uploads 路由
- [X] T040 [US2] 创建登录视图与注册视图（供管理员登录；登录：用户名、密码 → auth store；注册：调用 POST /api/auth/register），位于 frontend/src/views/Login.vue、frontend/src/views/Register.vue
- [X] T041 [US2] 为 /admin/* 创建管理端布局，位于 frontend/src/views/admin/AdminLayout.vue
- [X] T042 [US2] 创建管理端 Skill 管理视图（列表、创建、编辑、删除）；Skill 创建表单含 name、description、categoryId、tags[]、cloneCommand，位于 frontend/src/views/admin/SkillManage.vue
- [X] T043 [US2] 创建管理端 Rule 管理视图（列表、创建、编辑、删除）；Rule 创建表单含 name、description、categoryId、tags[]、content，位于 frontend/src/views/admin/RuleManage.vue
- [X] T044 [US2] ~~分类管理视图~~ **已移除**：分类由种子数据预置，管理后台不提供分类管理入口
- [X] T045 [US2] 创建管理端文章管理视图（列表、创建、编辑、删除），位于 frontend/src/views/admin/ArticleManage.vue
- [X] T046 [US2] 创建管理端资讯管理视图（列表、创建、编辑、删除），位于 frontend/src/views/admin/NewsManage.vue
- [X] T047 [US2] 在前端布局（如 frontend/src/components/AppNavbar.vue）中：**不显示**「上传」「我的上传」；仅当 user.role === ADMIN 时显示「管理后台」入口；未登录时可显示「登录」以进入管理后台

**检查点**：用户故事 2 完成；管理员可在管理后台完整管理 Skill、Rule、标签、文章、资讯；前台无上传入口。分类由种子数据预置。

---

## 阶段 5：用户故事 3 - 管理员隐藏或删除不当内容（优先级：P3）

**目标**：管理员可隐藏或删除任意 Skill 或 Rule；被隐藏/删除的条目不再在前台展示。

**独立测试**：以管理员身份登录；隐藏或删除某条 Skill/Rule；确认该条目在前台不可见。

- [X] T048 [US3] 实现 POST /api/admin/skills/:id/hide、DELETE /api/admin/skills/:id 与 POST /api/admin/rules/:id/hide、DELETE /api/admin/rules/:id（仅管理员）于后端（如 backend/src/main/java/.../controller/AdminController.java）
- [X] T049 [US3] 实现外部 Skill 管理端 CRUD：POST /api/admin/external-skills、PUT /api/admin/external-skills/:id、DELETE /api/admin/external-skills/:id（仅管理员）；可选 POST /api/admin/external-skills/crawl（触发从 skills.sh 爬取）于后端
- [X] T050 [US3] 在管理端 Skill/Rule 管理视图中添加隐藏或删除操作（如 SkillManage.vue、RuleManage.vue）；可选创建 frontend/src/views/admin/ExternalSkillManage.vue 管理外部 Skill

**检查点**：用户故事 3 完成；管理员可管理内容并隐藏/删除 Skill/Rule。

---

## 阶段 6：收尾与横切关注点

**目的**：安全、校验与文档

- [X] T056 确保用户创建与登录时使用密码哈希（如 BCrypt）于后端（如 backend/src/main/java/.../service/AuthService.java）
- [X] T057 确保 JWT 密钥与数据库 URL 来自环境（如 application.yml 占位符或环境变量）于后端
- [X] T058 为请求体（登录、注册、创建 Skill、Rule、分类、文章、资讯）添加校验（如 @Valid、DTO 约束）于后端；**关键词 keyword 最大 200 字符**（超长 400）；**clone_command 非空且长度上限（如 2000 字符）**
- [X] T059 执行 quickstart.md 步骤：启动 MySQL、后端、前端；**通过种子脚本或自注册后手动提升创建首个管理员**；验证未登录下的列表/详情、管理员登录后创建 Skill/Rule、前台无「上传」「我的上传」入口、管理端分类/文章/资讯/外部 Skill 管理

---

## 依赖与执行顺序

### 阶段依赖

- **阶段 1（搭建）**：无依赖。
- **阶段 2（基础）**：依赖阶段 1。阻塞所有用户故事。
- **阶段 3（US1）**：依赖阶段 2。与 US2/US3 无依赖。
- **阶段 4（US2）**：依赖阶段 2；复用 US1 的前端结构（router、API 客户端）。
- **阶段 5（US3）**：依赖阶段 2；依赖 US2 的认证（管理员角色）。
- **阶段 6（收尾）**：视需要依赖阶段 3–5 的完成。

### 用户故事依赖

- **US1（P1）**：仅依赖基础阶段。可独立测试（未登录浏览）。
- **US2（P2）**：依赖基础阶段；在 US1 的 router 与 API 客户端上扩展；需认证与管理员角色。可独立测试（管理员登录、管理后台 CRUD）。
- **US3（P3）**：依赖 US2 认证以获取管理员角色。可独立测试（管理端隐藏/删除）。

### 可并行项

- 阶段 1：T004、T005 可在 T001–T003 之后并行。
- 阶段 2：T008–T013a（实体）可并行；T014 在实体之后。
- 阶段 3：T017、T018 [P]；T019 与 T019a 可并行。
- 阶段 4：T033–T037（后端管理）可并行；T042–T046（前端管理视图）可并行。

---

## 实现策略

### 先做 MVP（仅用户故事 1）

1. 完成阶段 1（搭建）与阶段 2（基础）。
2. 完成阶段 3（US1）：公开列表/详情/首页及文章/资讯；带样式的前端视图；**无上传/我的上传入口**。
3. 验证：未认证打开应用，浏览首页、列表、详情、复制。无需登录。
4. 部署或演示 MVP。

### 增量交付

1. 搭建 + 基础 → 启动后端与前端。
2. US1 → MVP（未登录浏览，前台纯消费）。
3. US2 → 管理员登录、管理后台 Skill/Rule/分类/文章/资讯 CRUD；**前台仍无上传**。
4. US3 → 管理端隐藏/删除 Skill/Rule、外部 Skill 管理。
5. 收尾 → 安全与 quickstart 验证。

---

## 说明

- [P] = 可并行（不同文件，阶段内无顺序依赖）。
- [USn] = 任务归属该用户故事，便于追溯。
- **规格变更**：前台不提供「上传」「我的上传」；Skill 与 Rule 的创建、编辑、删除仅限管理后台。
- 若现有代码包含用户端上传/我的上传，需移除对应视图、路由与 API（如 GET /api/me/skills、GET /api/me/rules），并将 Skill/Rule 写操作移至仅管理员可用的接口。
