# 快速开始：001-ai-resource-platform

**分支**：`001-ai-resource-platform`  
**规格**：[spec.md](./spec.md) | **计划**：[plan.md](./plan.md) | **数据模型**：[data-model.md](./data-model.md) | **API**：[contracts/api-spec.md](./contracts/api-spec.md)

## 前置条件

- Node 18+（前端：Vite、Vue 3）
- Java 17+（后端：Spring Boot 3）
- MySQL（后端存储）
- Git（功能分支 `001-ai-resource-platform`）

## 仓库布局

- **frontend/** – Vue 3 + Vite + Pinia + Vue Router；复用现有 `styles.css`（从项目根目录复制或链接）。
- **backend/** – Spring Boot 3 + Spring Data JPA + Spring Security（JWT）；REST 前缀 `/api`。

## 开发环境搭建（概要）

1. **后端**
   - 用 Spring Initializr 或现有 Boot 3 应用创建 `backend/`：Web、Data JPA、Security、MySQL 驱动。
   - 配置 `application.yml`：数据源 URL、JPA（ddl-auto 或 Flyway）、JWT 密钥与过期时间。
   - 按 data-model.md 实现实体（User、Category、Tag、**Skill**、**Rule**、ExternalSkill、LearningArticle、News）；v1 不建 Recommendation 表。
   - 按 contracts/api-spec.md 实现 REST controller 与 service；用 JWT 保护端点；列表/详情/首页无需认证；提供 POST /api/auth/register（v1 自注册）。
   - 运行：`./mvnw spring-boot:run`（或 Gradle 等价命令）；API 地址 `http://localhost:8080/api`。

2. **前端**
   - 用 `npm create vite@latest . -- --template vue`（或 vue-ts）创建 `frontend/`；添加 Vue Router、Pinia。
   - 将项目根目录的 `styles.css` 复制或软链到 `frontend/src/assets/` 并全局引入。
   - 配置 Vite 开发代理：`/api` → `http://localhost:8080/api`。
   - 实现视图：首页（最新 N 条，v1 无推荐位）、Skill/Rule 列表（筛选、搜索、分页；关键词最大 200 字符）、详情（Rule 复制/下载，Skill 展示 clone 命令）、登记 Skill/上传 Rule、我的上传；管理端路由在 `/admin` 下（分类、文章、资讯、外部 Skill、隐藏/删除 Skill/Rule）。
   - 运行：`npm run dev`；应用地址 `http://localhost:5173`（或配置端口）。

3. **首次运行**
   - 启动数据库；运行后端；通过 **注册接口** 自注册首个账号（若需管理员，可在数据库中将该用户 role 设为 ADMIN 或通过种子脚本）。
   - 启动前端；注册/登录；创建分类（管理员）；登记一条 Skill（clone 命令）、上传一条 Rule；确认未登录可访问列表/详情并复制/clone。

## 部署（概要）

- 前端：`npm run build` → 用 Nginx（或类似）托管 `dist/`。
- 后端：以单进程运行（如 `java -jar backend.jar`）；Nginx 将 `/api` 反向代理到后端。
- 环境：按环境配置 DB URL、JWT 密钥及前端 API 基础地址。

## 实现顺序（章程 V）

1. 核心流程：Skill/Rule 列表（无需认证）、详情、复制/下载；关键词 + 分类/标签筛选、分页。
2. 认证：登录、JWT；保护上传与我的上传。
3. 上传：创建 Skill/Rule；我的上传列表、编辑、删除自己的。
4. 分类与标签：管理员 CRUD 分类；标签来自管理员或用户上传时；按二者筛选。
5. 管理端：隐藏/删除任意 Skill/Rule；学习文章与资讯 CRUD；外部 Skill 管理；首页聚合**最新 N 条**（v1 无推荐位）。
