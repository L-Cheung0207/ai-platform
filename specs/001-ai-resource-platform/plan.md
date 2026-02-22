# 实现计划：内部 AI 资源与学习平台

**分支**：`001-ai-resource-platform` | **日期**：2025-02-13 | **规格**：[spec.md](./spec.md)  
**输入**：来自 `specs/001-ai-resource-platform/spec.md` 的功能规格

## 概要

面向公司内部的平台，用于分享 **Skill**（登记 clone 命令，内容在外部 GitLab 维护）、**Rule**（平台内纯文本上传）与**外部 Skill**（元数据 + 安装命令），以及学习内容（文章、资讯）。浏览与阅读匿名；上传/登记、「我的上传」及管理功能需账号与密码；**v1 仅自注册**，无审批。一个 Vue 3 前端（用户端与管理端同应用）与一个 Spring Boot 3 后端；REST API；基于数据库的列表/详情，支持关键词（最大 200 字符、空视为未筛选）与分类/标签筛选及分页。**v1 无首页推荐位**，仅展示最新 N 条。设计遵循现有 README 与 styles.css。

## 技术背景

**前端**
- **语言/版本**：JavaScript/TypeScript，Vue 3.x
- **主要依赖**：Vite、Vue Router、Pinia；复用现有 styles.css（设计变量、布局）
- **目标平台**：现代浏览器；以静态构建部署于 Nginx 后

**后端**
- **语言/版本**：Java 17+，Spring Boot 3.x
- **主要依赖**：Spring Web、Spring Data JPA、Spring Security（账号+密码，仅 JWT；无 session）
- **存储**：MySQL；实体按 data-model.md 定义（Skill、Rule、ExternalSkill、User、Category、Tag、LearningArticle、News；v1 不建 Recommendation 表）
- **测试**：JUnit 5、Spring Boot Test；前端可选 Vitest
- **目标平台**：Linux 服务器（或容器）；单进程，v1 不接入全文检索引擎

**项目类型**：Web 应用（前端 + 后端，仓库根目录下两个项目）  
**性能目标**：列表/详情响应 p95 &lt;500ms；内部规模（数百用户）  
**约束**：v1 不使用 Elasticsearch；筛选/搜索通过数据库（关键词最大 200 字符、空关键词视为未筛选）；列表/详情/首页 API 无需认证；clone_command 仅非空+长度上限（如 2000 字符），不校验格式  
**规模/范围**：内部使用；约 10–50 并发用户；数百条 Skill/Rule 及文章/资讯

## 章程检查

*关卡：阶段 0 调研前必须通过。阶段 1 设计后需再次检查。*

| 原则 | 关卡 | 状态 |
|-----------|------|--------|
| I. 代码质量 | 实现前在 spec/plan 中定义 API 与数据模型 | 通过（spec + plan + data-model + contracts） |
| II. 技术范围 | 一个前端项目（用户+管理）、一个后端项目 | 通过（frontend/ + backend/） |
| III. 安全与访问 | 浏览/阅读匿名；仅上传、我的上传、管理需登录；角色：普通 vs 管理员 | 通过（spec FR-001、FR-002、FR-010） |
| IV. 用户体验 | 遵循 README/styles.css；列表/详情支持搜索、筛选、分页 | 通过（plan 复用 styles；FR-005 分页+筛选） |
| V. 实现顺序 | 核心流程优先（列表/详情/复制，再登录/注册/上传/我的上传），再文章/资讯；v1 无全文搜索 | 通过（阶段 2 任务将按此顺序） |
| 附加约束 | Vue 3 + Spring Boot；v1 无 Elasticsearch；v1 无推荐位 | 通过 |

无违规。复杂度跟踪表留空。

## 项目结构

### 文档（本需求）

```text
specs/001-ai-resource-platform/
├── plan.md              # 本文件
├── research.md          # 阶段 0
├── data-model.md        # 阶段 1
├── quickstart.md        # 阶段 1
├── contracts/           # 阶段 1（API 规格）
│   └── api-spec.md
└── tasks.md             # 阶段 2（/speckit.tasks）
```

### 源代码（仓库根目录）

```text
frontend/
├── src/
│   ├── assets/          # styles.css、图片
│   ├── components/
│   ├── views/           # 用户：首页、Skill/Rule 列表与详情、登记/上传、我的上传；管理：/admin/*
│   ├── router/
│   ├── stores/          # Pinia（auth 等）
│   ├── services/        # API 客户端
│   └── App.vue
├── index.html
├── vite.config.*
└── package.json

backend/
├── src/main/
│   ├── java/.../        # 包：config、controller、service、repository、entity、dto、security
│   └── resources/
│       ├── application.yml
│       └── schema（可选）
└── pom.xml 或 build.gradle
```

**结构决策**：一个前端（Vue 3 + Vite）与一个后端（Spring Boot 3）的 Web 应用。用户端与管理端同属一个前端仓库，通过路由（如 `/admin/*`）区分。符合章程 II。

## 复杂度跟踪

*无章程违规。表留空。*
