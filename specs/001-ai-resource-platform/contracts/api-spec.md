# API 契约：内部 AI 资源与学习平台

**基础路径**：`/api`（如 `https://host/api`）  
**响应封装**：`{ "code": number, "message": string, "data": T }`  
**分页**：`page`（从 1 开始）与 `size`；响应包含 `data.items` 与 `data.total`。

**认证**：标记为 *Auth* 的接口需要 `Authorization: Bearer <JWT>`。列表/详情/首页无需认证。

---

## 认证

| 方法 | 路径 | 认证 | 说明 |
|--------|------|------|-------------|
| POST   | /api/auth/login    | 否  | Body: `{ "username", "password" }` → `data: { "token", "user": { id, username, role } }` |
| POST   | /api/auth/logout   | 是 | 使 token 失效（JWT 无状态时可选） |
| GET    | /api/auth/me       | 是 | 当前用户 `{ id, username, role }` |
| POST   | /api/auth/register | 否  | **v1 自注册**；body: `{ "username", "password" }` → 201 + 用户信息或 400（用户名已存在等）；注册即可登录，无审批 |

---

## Skill（公开：无需认证）

| 方法 | 路径 | 认证 | 说明 |
|--------|------|------|-------------|
| GET    | /api/skills              | 否 | 列表；查询：`keyword`、`categoryId`、`tags[]`、`page`、`size` → `{ items[], total }`；仅 visibility=VISIBLE |
| GET    | /api/skills/:id          | 否 | 详情；隐藏/删除则 404。响应含 **cloneCommand**（如 `git clone <GitLab URL>`），供前端展示「复制 clone」；无附件存储。 |

---

## Rule（公开：无需认证）

| 方法 | 路径 | 认证 | 说明 |
|--------|------|------|-------------|
| GET    | /api/rules               | 否 | 列表；查询：`keyword`、`categoryId`、`tags[]`、`page`、`size` → `{ items[], total }`；仅 visibility=VISIBLE |
| GET    | /api/rules/:id           | 否 | 详情；隐藏/删除则 404 |

---

## 外部 Skill（公开：无需认证）

| 方法 | 路径 | 认证 | 说明 |
|--------|------|------|-------------|
| GET    | /api/external-skills      | 否 | 列表；查询：`keyword`、`categoryId`、`tags[]`、`page`、`size` → `{ items[], total }`；仅 visibility=VISIBLE |
| GET    | /api/external-skills/:id | 否 | 详情；响应含 `installCommand`、`sourceUrl` 等，供前端展示「安装」 |

---

## Skill（登记、我的登记：需认证）

| 方法 | 路径 | 认证 | 说明 |
|--------|------|------|-------------|
| POST   | /api/skills              | 是 | 登记；body JSON `{ name, description, categoryId, tags[], cloneCommand }`（无文件上传）→ 201 + 实体 |
| PUT    | /api/skills/:id          | 是 | 仅更新自己登记的；非所有者 403 |
| DELETE | /api/skills/:id          | 是 | 仅删除自己登记的；非所有者 403 |
| GET    | /api/me/skills           | 是 | 我登记的 Skill；查询：`page`、`size` → `{ items[], total }` |

---

## Rule（上传、我的上传：需认证）

| 方法 | 路径 | 认证 | 说明 |
|--------|------|------|-------------|
| POST   | /api/rules               | 是 | 创建；body JSON `{ name, description, categoryId, tags[], content }` → 201 + 实体 |
| PUT    | /api/rules/:id           | 是 | 仅更新自己的；非所有者 403 |
| DELETE | /api/rules/:id           | 是 | 仅删除自己的；非所有者 403 |
| GET    | /api/me/rules            | 是 | 我的 Rule 上传；查询：`page`、`size` → `{ items[], total }` |

---

## 分类（公开读；管理员写）

| 方法 | 路径 | 认证 | 说明 |
|--------|------|------|-------------|
| GET    | /api/categories          | 否 | 分类列表（供 Skill/Rule）；查询：`type` 可选 |
| POST   | /api/categories          | Admin | 创建分类 |
| PUT    | /api/categories/:id      | Admin | 更新分类 |
| DELETE | /api/categories/:id      | Admin | 删除（约束：无 Skill/Rule 使用该分类，或定义级联规则） |

---

## 标签（公开读；管理员可选；用户上传时创建）

| 方法 | 路径 | 认证 | 说明 |
|--------|------|------|-------------|
| GET    | /api/tags                | 否 | 标签列表（筛选/自动补全）；查询：`q` 可选 |
| POST   | /api/tags                | Admin | 创建标签（可选精选集） |
|        | （用户创建的标签）      |      | 上传带新标签名的 Skill 或 Rule 时隐式创建 |

---

## 学习文章（公开读；管理员写）

| 方法 | 路径 | 认证 | 说明 |
|--------|------|------|-------------|
| GET    | /api/articles            | 否 | 列表；查询：`categoryId`、`page`、`size` → `{ items[], total }`；仅已发布 |
| GET    | /api/articles/:id        | 否 | 详情 |
| POST   | /api/articles            | Admin | 创建 |
| PUT    | /api/articles/:id        | Admin | 更新 |
| DELETE | /api/articles/:id        | Admin | 删除 |

---

## 资讯（公开读；管理员写）

| 方法 | 路径 | 认证 | 说明 |
|--------|------|------|-------------|
| GET    | /api/news                | 否 | 列表；查询：`page`、`size`；响应中可选的按日期分组 |
| GET    | /api/news/:id            | 否 | 详情 |
| POST   | /api/news                | Admin | 创建 |
| PUT    | /api/news/:id            | Admin | 更新 |
| DELETE | /api/news/:id            | Admin | 删除 |

---

## 首页（公开：无需认证）

| 方法 | 路径 | 认证 | 说明 |
|--------|------|------|-------------|
| GET    | /api/home                | 否 | 聚合：`{ latestSkills[], latestRules[], latestExternalSkills[], latestArticles[], latestNews[] }` 或分字段；可选来自推荐表的精选 |

---

## 管理端：隐藏/删除 Skill

| 方法 | 路径 | 认证 | 说明 |
|--------|------|------|-------------|
| POST   | /api/admin/skills/:id/hide   | Admin | 将 visibility 设为 HIDDEN（或软删除） |
| POST   | /api/admin/skills/:id/unhide | Admin | 恢复可见（若隐藏为软删实现） |
| DELETE | /api/admin/skills/:id        | Admin | 永久删除（或硬删） |

---

## 管理端：隐藏/删除 Rule

| 方法 | 路径 | 认证 | 说明 |
|--------|------|------|-------------|
| POST   | /api/admin/rules/:id/hide   | Admin | 将 visibility 设为 HIDDEN（或软删除） |
| POST   | /api/admin/rules/:id/unhide | Admin | 恢复可见（若隐藏为软删实现） |
| DELETE | /api/admin/rules/:id        | Admin | 永久删除（或硬删） |

---

## 管理端：外部 Skill（仅管理员）

| 方法 | 路径 | 认证 | 说明 |
|--------|------|------|-------------|
| POST   | /api/admin/external-skills        | Admin | 创建；body: `{ name, description, installCommand, sourceUrl?, categoryId?, tags[] }` → 201 |
| PUT    | /api/admin/external-skills/:id    | Admin | 更新 |
| DELETE | /api/admin/external-skills/:id   | Admin | 删除 |
| POST   | /api/admin/external-skills/crawl  | Admin | 可选；触发从 skills.sh 等爬取并写入/更新外部 Skill（具体爬取范围由实现定义） |

---

## 错误响应

- `401 Unauthorized`：受保护接口缺少或 token 无效。
- `403 Forbidden`：token 有效但非所有者（编辑/删除 Skill 或 Rule）或非管理员（管理端接口）。
- `404 Not Found`：资源不存在或已隐藏/删除。
- `400 Bad Request`：校验错误；`message` 及可选的 `data.errors`（字段级信息）。

所有错误使用封装：`{ "code": 4xx, "message": "...", "data": null }`。
