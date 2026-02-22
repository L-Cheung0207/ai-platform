# 数据模型：001-ai-resource-platform

**分支**：`001-ai-resource-platform` | **日期**：2025-02-13  
**规格**：[spec.md](./spec.md) | **计划**：[plan.md](./plan.md)

## 实体关系概览

- **User**：账号与角色（普通、管理员）。一个用户可对应多条 Skill、多条 Rule（作为上传者）。
- **Category**：由管理员定义；被 Skill、Rule 使用，可选被 LearningArticle 使用。一个分类可对应多条 Skill、多条 Rule。
- **Tag**：由管理员精选或用户上传时创建；按 Skill、Rule 分别存储（如 JSON 数组或与 Skill–Tag、Rule–Tag 多对多）。
- **Skill**：**内部 Skill** 独立表；仅元数据 + **clone_command**（内容在外部 GitLab 维护，平台不存正文与附件）；category_id、uploader_id（登记人）；visibility。
- **Rule**：**内部 Rule** 独立表；仅纯文本正文，无附件；category_id、uploader_id；visibility。
- **ExternalSkill**：**外部 Skill**（如来自 [skills.sh](https://skills.sh/)）；仅元数据：名称、描述、安装命令、来源 URL、可选 category_id、标签；无上传者、无正文/附件存储。
- **LearningArticle**：仅管理员；分类可选。
- **News**：仅管理员；规格中无分类。
- **Recommendation**（**v1 不实现**）：首页仅展示最新 N 条，不建推荐位表；后续版本可增加。

---

## 实体

### User

| 字段         | 类型        | 约束 |
|-------------|-------------|------|
| id          | PK（UUID 或 bigint） | |
| username    | string      | 唯一、非空 |
| password_hash | string    | 非空 |
| role        | enum        | NORMAL, ADMIN |
| created_at  | timestamp   | |
| updated_at  | timestamp   | |

- 一个 User 对应多条 Skill、多条 Rule（作为 uploader）。
- **v1 账号仅通过自注册创建**，无需审批（见 spec）。

---

### Category

| 字段        | 类型        | 约束 |
|------------|-------------|------|
| id         | PK          | |
| name       | string      | 非空 |
| type       | enum?       | 如 SKILL_RULE, ARTICLE；或全表单一类型 |
| sort_order | int         | 默认 0 |
| created_at | timestamp   | |

- 仅由管理员定义。被 Skill、Rule 使用；可选被 LearningArticle 使用。
- 一个 Category 可对应多条 Skill、多条 Rule。

---

### Tag

| 字段        | 类型        | 约束 |
|------------|-------------|------|
| id         | PK          | |
| name       | string      | 非空、唯一（规范化后） |
| created_at | timestamp   | |

- 标签可由管理员创建或用户在上传时创建（自由文本）；持久化并复用于筛选/展示。
- 与 Skill、Rule 分别多对多（如 skill_tag、rule_tag 关联表）。

---

### Skill（内部 Skill，独立表）

| 字段           | 类型        | 约束 |
|----------------|-------------|------|
| id             | PK          | |
| name           | string      | 非空 |
| description    | string      | |
| clone_command  | string      | 非空；如 `git clone <GitLab 仓库 URL>`，供用户复制以获取完整内容 |
| category_id    | FK → Category | 非空 |
| uploader_id    | FK → User   | 非空（登记人） |
| tags           | 标签名 JSON 数组，或与 Tag M:N | |
| visibility     | enum        | VISIBLE, HIDDEN（管理员隐藏）；或软删除 deleted_at |
| created_at     | timestamp   | |
| updated_at     | timestamp   | |

- **不存 content、附件**；内容与多文件在外部 GitLab 仓库维护，平台仅存储并展示 clone_command。列表/详情仅返回 visibility = VISIBLE。无审批状态；发布即生效。

---

### Rule（内部 Rule，独立表）

| 字段         | 类型        | 约束 |
|-------------|-------------|------|
| id          | PK          | |
| name        | string      | 非空 |
| description | string      | |
| content     | text        | 非空（markdown 或纯文本） |
| category_id | FK → Category | 非空 |
| uploader_id | FK → User   | 非空 |
| tags        | 标签名 JSON 数组，或与 Tag M:N | |
| visibility  | enum        | VISIBLE, HIDDEN（管理员隐藏）；或软删除 deleted_at |
| created_at  | timestamp   | |
| updated_at  | timestamp   | |

- 仅正文（纯文本），无附件。列表/详情仅返回 visibility = VISIBLE。无审批状态；发布即生效。

---

### ExternalSkill（外部 Skill）

| 字段           | 类型        | 约束 |
|----------------|-------------|------|
| id             | PK          | |
| name           | string      | 非空 |
| description    | string      | |
| install_command| string      | 非空；如 `npx skills add owner/repo` |
| source_url     | string      | 可选；来源页链接（如 skills.sh 或 GitHub） |
| category_id    | FK → Category（可选） | |
| tags           | 标签名 JSON 数组，或与 Tag M:N | |
| visibility     | enum        | VISIBLE, HIDDEN（管理员可隐藏） |
| created_at     | timestamp   | |
| updated_at     | timestamp   | |

- 仅存元数据，不存储完整技能内容。由管理员手工添加或通过爬取 [skills.sh](https://skills.sh/) 等录入。无 uploader_id。
- 列表/详情公开（无需认证）；详情页提供「安装」引导（展示安装命令、可复制或跳转 source_url）。
- 可选：`external_id`（如 skills.sh 的 id）用于爬取去重。

---

### LearningArticle

| 字段         | 类型        | 约束 |
|-------------|-------------|------|
| id          | PK          | |
| title       | string      | 非空 |
| summary     | string      | |
| content     | text        | 富文本（如 HTML） |
| category_id | FK → Category（可选） | |
| author_id   | FK → User   | 管理员 |
| status      | enum        | 如 DRAFT, PUBLISHED |
| created_at  | timestamp   | |
| updated_at  | timestamp   | |

- 仅管理员创建/更新。列表/详情公开（无需认证）。

---

### News

| 字段          | 类型        | 约束 |
|--------------|-------------|------|
| id           | PK          | |
| title        | string      | 非空 |
| summary      | string      | |
| source_url   | string      | |
| source_name  | string      | |
| publish_date | date        | |
| created_at   | timestamp   | |

- 仅管理员创建/更新。列表/详情公开；可在 API 或前端按日期分组。

---

### Recommendation（可选）

| 字段          | 类型        | 约束 |
|--------------|-------------|------|
| id           | PK          | |
| position_key | string      | 如 home_skill, home_article |
| target_type  | enum        | SKILL, RULE, ARTICLE, NEWS（或 EXTERNAL_SKILL） |
| target_id    | FK          | 目标实体 id |
| sort_order   | int         | |
| valid_until  | timestamp   | 可选 |

- 用于首页「精选」位。若首页仅用「最新 N」可不在 v1 实现。

---

## 校验规则（来自 spec）

- Skill：创建时必填 name、description、category、tags、**clone_command**；不存 content/附件。
- Rule：创建时必填 name、description、category、tags、content。
- Category：name 必填；仅管理员创建/更新。
- User：username 唯一；角色 NORMAL 或 ADMIN。
- LearningArticle / News：title 必填；仅管理员变更。

## 状态流转

- **Skill / Rule**：创建 → VISIBLE。管理员可设为 HIDDEN 或删除（软删或硬删）。
- **LearningArticle**：DRAFT → PUBLISHED（管理员）。公开列表仅展示 PUBLISHED。
- **User**：无状态；角色按 spec 在未扩展前不可变。
