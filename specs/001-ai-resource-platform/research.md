# 调研：001-ai-resource-platform

**分支**：`001-ai-resource-platform` | **日期**：2025-02-13

## 1. 后端持久化：JPA vs MyBatis-Plus

**决策**：Spring Data JPA。

**理由**：模型以 CRUD 为主（Skill/Rule、Category、Tag、User、Article、News），实体与关系清晰。JPA 减少样板代码并与 Spring Boot 默认一致；可按需用 Flyway/Liquibase 生成或管理 schema。

**备选**：MyBatis-Plus 便于精细控制 SQL；暂不采用，除非查询复杂度或性能需要。

---

## 2. 数据库

**决策**：仅 MySQL。

**理由**：项目单一数据库；MySQL 使用广泛，足以支撑 JPA 实体、关键词/分类/标签筛选，以及按需使用 JSON 列（MySQL 5.7+ JSON 类型）。

**备选**：PostgreSQL（按项目约束未选）；SQLite 仅用于单节点开发（不用于生产）。

---

## 3. 认证

**决策**：仅 JWT（无服务端 session）。

**理由**：无状态认证；前端发送 `Authorization: Bearer <token>`；Nginx 可将 `/api` 反向代理到后端，无需 session 亲和。项目约束：使用 JWT，不用 session。

**备选**：基于 cookie 的 session（按项目选择未采用）。

---

## 4. 前端状态与路由

**决策**：Pinia 管理全局状态（用户、token）；Vue Router 配置用户端与 `/admin` 路由；复用现有 `styles.css` 作为全局资源。

**理由**：规格与章程要求一个前端应用同时包含用户端与管理端；对 `/admin` 及「上传」「我的上传」使用路由守卫（未认证时重定向到登录）。Pinia 保存认证状态及可选的列表缓存。

**备选**：Vuex 4；Pinia 为 Vue 3 推荐 store，且已列入计划。

---

## 5. API 响应格式与分页

**决策**：REST；响应封装 `{ code, message, data }`；分页使用 `page` 与 `size`；列表响应包含 `data.items`（或 `data.list`）与 `data.total`。

**理由**：与 SPEC_KIT_FEED 及 spec 一致；前端易于处理。列表/详情/首页无需认证；仅写操作与「我的上传」/管理端需要 `Authorization`。

**备选**：无封装（裸数组/对象）；采用封装便于统一错误处理与后续扩展。

---

## 6. v1 范围与澄清（来自 /speckit.clarify）

**决策**：v1 账号仅自注册（提供注册接口，无需审批）；搜索关键词最大 200 字符、超长拒绝 400；空关键词视为未筛选；首页仅展示最新 N 条，不实现推荐位与 Recommendation 表；clone_command 仅校验非空与长度上限（如 2000 字符），不校验格式。

**理由**：与规格澄清一致，减少首版实现范围并明确接口与校验行为。
