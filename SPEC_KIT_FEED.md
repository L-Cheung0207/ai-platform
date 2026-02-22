# 如何把本项目内容喂给 Spec-Kit

[Spec-Kit](https://github.com/github/spec-kit) 是 GitHub 的 **Spec-Driven Development** 工具：先写规范（what/why），再写技术方案（how），最后拆任务并让 AI 按任务实现。  
下面按 Spec-Kit 的 **6 步流程** 分类，并给出每步「喂什么内容」的总结，方便你直接复制到对应 slash 命令里或作为上下文给 AI。

---

## Spec-Kit 流程速览

| 步骤 | 命令 | 产出 | 注意 |
|------|------|------|------|
| 1 | `/speckit.constitution` | 项目原则与约束（不涉及具体功能） | 只讲「怎么开发」，不讲「做什么」 |
| 2 | `/speckit.specify` | 功能规格：做什么、为什么 | **不讲技术栈**，只讲业务与用户故事 |
| 3 | `/speckit.clarify` | 澄清歧义、补全边界 | 建议在 plan 之前做 |
| 4 | `/speckit.plan` | 技术方案：技术栈、架构、接口 | 这里才定 Vue、Java、DB、API |
| 5 | `/speckit.tasks` | 可执行任务列表 | 由 plan 自动生成 |
| 6 | `/speckit.implement` | 按任务实现代码 | AI 按 tasks 执行 |

可选：`/speckit.analyze` 在 tasks 之后、implement 之前做一次一致性检查。

---

## 使用前准备

1. 在项目目录执行：`specify init . --ai cursor-agent`（或你用的 AI）。
2. 用 **Git 分支** 区分功能（如 `001-skill-rules-share`），Spec-Kit 会按当前分支识别当前在做哪条 spec。

---

## Step 1：Constitution（项目原则）

**用途**：约定「开发时怎么决策」，不写具体功能。  
**喂给的内容**：下面整段可作为 `/speckit.constitution` 的输入（或稍作删改）。

```
本项目是公司内部 AI 资源与学习平台，采用 Spec-Driven 开发。原则如下：
- 代码质量：保持可读、可维护；接口与数据模型优先在 spec/plan 里定义清楚再实现。
- 技术选型：一个前端项目（用户端 + 管理端同仓）、一个后端项目；不过度拆分服务。
- 安全与权限：仅内网使用；登录采用账号密码。**用户端浏览与阅读无需登录**，所有 Skill/Rule、学习文章、资讯均可匿名访问；仅上传、「我的上传」、管理后台需要登录；管理员与普通用户通过角色区分。
- 用户体验：沿用现有设计规范（见 README 与 styles.css 的配色、字体、布局）；列表与详情要支持搜索、筛选与分页。
- 实现顺序：先跑通「登录 → 上传 → 列表 → 详情 → 复制」主链路，再叠加学习文章与资讯；首版不做复杂全文搜索，用数据库过滤即可。
```

**来源**：来自 `PRODUCT_RECOMMENDATION.md` 的「小结」与「技术实现建议」的抽象原则。

---

## Step 2：Specify（功能规格：做什么、为什么）

**用途**：描述要做的**产品与功能**，不写技术栈。  
**喂给的内容**：下面整段适合作为 `/speckit.specify` 的输入（可再按需补充用户故事细节）。

```
建设公司内部「AI 资源与学习平台」，目标有二：
1）让员工上传并共享自己觉得好的 Skill 或 Rule，上传即发布，所有人可搜索、查看、一键复制使用；
2）在同一平台提供 AI 概念与用法文章、以及 AI 相关资讯，方便大家学习。

访问与角色：
- **无需登录**：所有人（含未登录访客）均可浏览、搜索、复制所有 Skill/Rule，阅读学习文章与资讯，查看首页。用户端不要求登录。
- **已登录普通用户**：在上传 Skill/Rule、进入「我的上传」查看/编辑/删除自己上传的内容时需要登录。
- **管理员**：在普通用户能力基础上，可管理分类与标签、发布与编辑学习文章和资讯、维护首页推荐位（可选）；管理后台需登录。

核心功能（按优先级）：

第一优先级 — Skill/Rules 共享：
- 上传：提交名称、简介、分类、标签、正文（粘贴文本或上传 .md）；上传后立即出现在列表中，无需审核。
- 展示：列表与详情**无需登录**即可访问；支持按分类、标签、关键词搜索；卡片展示名称、描述、分类、上传者、时间；详情页支持一键复制内容或下载。
- 我的上传：**需登录**；仅登录用户可见「我提交的」列表，且仅能编辑或删除自己上传的内容。

第二优先级 — 学习与资讯：
- 学习文章：标题、摘要、富文本正文、分类；由管理员发布与维护；列表与详情**无需登录**即可阅读。
- 资讯：标题、摘要、来源链接、日期；由管理员录入；按日期分组展示；**无需登录**即可阅读。
- 首页：聚合最新/精选 Skill 与 Rule、最新学习文章、最新资讯；侧栏可有「热门」或「推荐」。

约束与范围：
- 登录方式仅账号 + 密码；账号由管理员创建或开放注册（可选审批）。
- Skill 与 Rule 用同一套数据模型，用类型字段区分，前端用 Tab 或两个入口展示。
- 仅公司内部使用；首版不对外、不做 SEO。
```

**来源**：`PRODUCT_RECOMMENDATION.md` 的「一～三、五」及「七」的功能与角色描述；与现有静态页的对应关系见该文档「一」。

---

## Step 3：Clarify（澄清）

**用途**：在写 plan 之前，把规格里的模糊点、边界情况说清楚。  
**建议**：在 AI 里执行 `/speckit.clarify`，并可根据需要附加一句焦点，例如：

```
/speckit.clarify 请重点澄清：用户能否看到或搜索到「谁」上传的；编辑/删除是否仅限本人；管理员能否删除或隐藏他人上传的 Skill/Rule；分类与标签是预置还是可由用户自建。
```

也可以让 AI 自己从 spec 里找歧义，不附加参数直接 `/speckit.clarify`。  
**来源**：从 `PRODUCT_RECOMMENDATION.md` 与 `VUE_JAVA_IMPLEMENTATION_NEEDS.md` 里抽出的、容易产生歧义的点。

---

## Step 4：Plan（技术方案）

**用途**：定技术栈、架构、数据与接口。  
**喂给的内容**：下面整段适合作为 `/speckit.plan` 的输入。

```
技术栈与架构：
- 前端：一个 Vue 3 项目，包含用户端与管理端（/admin）；Vue Router、Pinia；沿用现有 styles.css 的设计变量与布局；构建用 Vite。
- 后端：一个 Java 项目，Spring Boot 3；持久层用 JPA 或 MyBatis-Plus；**数据库仅 MySQL**；认证采用账号密码，**仅 JWT**（不用 Session）。
- 部署：前后端分离；前端打包到 Nginx，后端独立服务；Nginx 将 /api 反向代理到后端。

数据与接口要点：
- 共享内容：一张表存 Skill/Rule，用 type 区分；字段含 name, description, content, category_id, tags(JSON), uploader_id, created_at, updated_at；无审核状态。分类单独表。列表/详情接口支持 keyword、category、分页。
- 用户：用户表；角色区分普通用户与管理员。
- 学习文章、资讯：按 PRODUCT_RECOMMENDATION 第四节的数据模型；文章与资讯的 CRUD 仅管理员可用。
- 首页：聚合接口返回最新/精选 Skill/Rule、最新文章、最新资讯；或分别调用多个列表接口由前端拼装；**列表/详情/首页等浏览类接口均无需鉴权**。
- API 风格：REST；统一返回格式如 { code, message, data }；分页用 page/size 或 offset/limit。仅上传、我的上传、管理后台相关接口需要登录鉴权。
```

**来源**：`PRODUCT_RECOMMENDATION.md` 的「四、六」及「七」的 Phase 1；项目结构见此前讨论的「1 个前端 + 1 个后端」。

---

## Step 5 & 6：Tasks 与 Implement

- **Tasks**：在 plan 和（可选）clarify 都满意后，在 AI 里执行 `/speckit.tasks`，由 Spec-Kit 根据当前 spec 与 plan 生成 `tasks.md`，无需你再写大段「喂给」内容。
- **Implement**：任务列表生成后，执行 `/speckit.implement`，AI 会按 `tasks.md` 顺序实现。若需要，可在 implement 前执行 `/speckit.analyze` 做一次一致性检查。

---

## 文档与步骤对应表

| 本仓库文档 | 主要喂给哪一步 | 说明 |
|------------|----------------|------|
| `README.md` | Plan（前端样式时参考） | 设计规范、配色、字体、布局，实现时复用 |
| `styles.css` | Plan / Implement | 现有 CSS 变量与类名，Vue 组件可直接沿用 |
| `PRODUCT_RECOMMENDATION.md` | Constitution + Specify + Plan | 原则→功能→数据/技术，对应 1、2、4 步 |
| `VUE_JAVA_IMPLEMENTATION_NEEDS.md` | Specify + Clarify | 业务与边界问题，可补到 specify/clarify |
| `SPEC_KIT_FEED.md`（本文） | — | 按 6 步整理好的「喂给清单」 |

---

## 建议使用顺序（一次完整流程）

1. `specify init . --ai cursor-agent`（若尚未初始化）。
2. 新建分支，例如：`001-skill-rules-share`。
3. 在 AI 中依次：
   - 粘贴 **Step 1** 内容 → `/speckit.constitution`
   - 粘贴 **Step 2** 内容 → `/speckit.specify`
   - `/speckit.clarify`（可选，可加焦点说明）
   - 粘贴 **Step 4** 内容 → `/speckit.plan`
   - `/speckit.tasks`
   - （可选）`/speckit.analyze`
   - `/speckit.implement`

这样就把「上面的内容」按 Spec-Kit 的步骤分类喂给了 spec-kit，便于它按规范生成并实现任务。
