# GitHub Trending 模块设计

日期：2026-06-14

## 目标

在平台首页增加 GitHub Trending 模块。模块展示 GitHub Trending 周榜和月榜，并把内容整理成中文的“排名 / 仓库 / 作用 / 场景”。

系统每天早上自动刷新数据，同时允许管理员手动刷新，并支持管理员修订自动生成的中文摘要。

## 范围

本期包含：

- 抓取 GitHub Trending 周榜和月榜仓库列表。
- 将 Trending 记录存入后端数据库。
- 为每个仓库生成初始中文“作用”和“场景”。
- 后续刷新时保留管理员人工编辑过的中文摘要。
- 在首页展示一个紧凑的周榜/月榜 tab 模块。
- 在后台增加同步状态、刷新、配置和摘要编辑能力。

本期不包含：

- 独立的公开 GitHub Trending 详情页。
- 用户订阅或通知。
- 日榜展示。
- 在应用运行时直接调用本地 Codex Skill 文件。

## Skill 复用决策

本地 `github-trending-rankings` Skill 可以作为内容规范参考，但不作为应用运行时依赖。它是给 Agent 使用的说明文档，不是可执行代码，也不是稳定 API。

应用应把该 Skill 的输出规则沉淀为后端 prompt 和格式约束：

- 仓库名保留为 `owner/repo`。
- 使用简短、直白的中文描述。
- 面向用户生成“作用”和“场景”两个概念。
- 如果无法可靠判断仓库用途，摘要状态标记为需要复核，不强行猜测。

## 用户体验

### 首页

在现有首页右侧栏新增 `GitHub Trending` 模块，位置靠近当前的排行榜和资讯模块。

模块展示：

- 标题：`GitHub Trending`
- 最近一次成功更新时间。
- tab：`周榜` 和 `月榜`。
- 紧凑排名列表，默认展示 10 条。

每条记录展示：

- 排名。
- 仓库全名，点击跳转 GitHub 原仓库。
- 中文“作用”。
- 中文“场景”，可作为次级文本、悬浮提示或展开内容，具体由可用空间决定。

窄屏下可以减少展示条数，以保持首页可读性。

首页继续使用现有 `/api/home` 聚合接口加载数据。`HomeDto` 增加 `githubTrendingWeekly`、`githubTrendingMonthly` 和 `githubTrendingUpdatedAt` 字段。

### 后台页面

新增后台路由：`/admin/github-trending`。

页面风格参考现有 LLM 排行榜后台页，包含：

- 同步状态卡片：上次同步时间、周榜数量、月榜数量、摘要状态、最近错误。
- 同步配置：语言过滤、关键词过滤、首页展示数量、每日刷新时间。
- 手动操作：刷新全部 Trending 数据、单条重新生成摘要。
- 可编辑表格：用 tab 切换周榜/月榜，列包含排名、仓库、作用、场景、状态和操作。

后台 UI 和后台 API 仅允许 `ADMIN` 用户访问。

## 后端架构

### 实体

新增 `GitHubTrendingRepository` 实体，核心字段：

- `id`
- `period`：`WEEKLY` 或 `MONTHLY`
- `rank`
- `repoFullName`
- `repoUrl`
- `description`
- `language`
- `stars`
- `forks`
- `starsGained`
- `effectCn`
- `scenarioCn`
- `summaryStatus`：`GENERATED`、`MANUAL`、`NEEDS_REVIEW` 或 `FAILED`
- `sourceFetchedAt`
- `createdAt`
- `updatedAt`

增加一个小型配置/状态持久化模型。实现时如果项目已有通用设置表，可复用；如果没有，则新增独立的 `GitHubTrendingConfig` 实体。需要保存的配置：

- 可选语言过滤。
- 可选关键词过滤列表。
- 首页展示数量，默认 10。
- 每日刷新时间，默认早上。
- 最近同步状态和最近错误。

### 服务

服务按职责拆分：

- `GitHubTrendingScraperService`：抓取并解析 GitHub Trending 页面。
- `GitHubTrendingSummaryService`：生成中文“作用 / 场景”摘要。
- `GitHubTrendingService`：编排同步、upsert、后台编辑和首页查询。
- `GitHubTrendingScheduler`：触发每日自动刷新。

抓取默认页面：

- `https://github.com/trending?since=weekly`
- `https://github.com/trending?since=monthly`

如果配置了语言过滤，抓取服务构造对应的 GitHub Trending 语言 URL。如果配置了关键词过滤，服务在解析后对仓库列表做过滤。

### 同步行为

每日同步同时抓取周榜和月榜。

按 `period + repoFullName` 做 upsert：

- 每次同步更新排名和 GitHub 原始元数据。
- 只为新记录或明确要求重新生成的记录生成摘要。
- 当 `summaryStatus=MANUAL` 时，保留 `effectCn` 和 `scenarioCn`。
- 新一轮抓取成功前，不删除旧的成功数据。

从 Trending 消失的仓库可以保留在数据库中，但不应出现在首页结果里，除非它属于该周期最近一次成功抓取的数据。

### 摘要生成

摘要服务输入仓库名、description、语言、可用时的 README 摘要和仓库 URL。

如果配置了模型/API，服务调用模型，并使用本地 Skill 规则作为生成约束。如果没有模型配置，或生成失败，则基于 GitHub description 生成保守文案，并将 `summaryStatus` 设置为 `NEEDS_REVIEW` 或 `FAILED`。

管理员编辑后，将 `summaryStatus` 设置为 `MANUAL`。

## API 设计

公开接口：

- 扩展 `GET /api/home`，返回 GitHub Trending 周榜和月榜列表。

后台接口：

- `GET /api/admin/github-trending/status`
- `GET /api/admin/github-trending?period=WEEKLY|MONTHLY`
- `PUT /api/admin/github-trending/{id}`：编辑 `effectCn` 和 `scenarioCn`
- `POST /api/admin/github-trending/sync`：手动刷新所有配置周期
- `POST /api/admin/github-trending/{id}/regenerate-summary`
- `GET /api/admin/github-trending/config`
- `PUT /api/admin/github-trending/config`

所有后台接口都要求 `ADMIN` 权限。

## 异常处理

如果 GitHub 抓取失败：

- 首页继续展示上一轮成功数据。
- 同步状态标记为 `FAILED`。
- 保存最近错误，供后台查看。

如果单个仓库 README 抓取失败：

- 继续使用 GitHub description 和元数据。
- 如果摘要质量不足，将记录标记为需要复核。

如果摘要生成失败：

- 保留仓库记录。
- 将 `summaryStatus` 设置为 `FAILED` 或 `NEEDS_REVIEW`。
- 允许管理员重试或手动编辑。

如果已有同步任务正在运行，管理员又触发手动同步：

- 拒绝第二个请求并返回清晰提示，或返回当前运行中的同步状态。

## 测试

后端测试：

- 将 GitHub Trending HTML 解析为仓库记录。
- 同步周榜和月榜数据。
- upsert 已有记录时不覆盖人工摘要。
- 缺少模型配置时能降级生成保守摘要。
- 同步失败时保留上一轮首页数据。
- 后台接口强制 `ADMIN` 权限。
- `/api/home` 按配置数量返回展示数据。

前端验证：

- 首页展示周榜/月榜 tab，并使用首页聚合数据。
- 后台页面能加载状态、切换周期 tab、编辑摘要并触发刷新。
- 新增路由和组件后前端构建通过。

## 实现备注

实现前应确认项目是否已有通用设置表。如果没有，则使用独立的 GitHub Trending 配置/状态模型，保持功能边界清晰。

首版不增加独立公开详情页。仓库链接直接跳转到 GitHub 原仓库。
