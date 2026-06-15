# 论坛详细设计

更新时间：2026-06-12

## 目标

给同事一个内部技术交流场，重点不是泛社交，而是：
- 问题提问
- 经验分享
- 方案讨论
- 经验沉淀回流到 `Skill` / `Rule` / 知识库

## 设计原则

- 先轻后重，先做能用的 MVP。
- 复用现有登录、标签、上传、富文本/Markdown、管理后台风格。
- 公开浏览匿名可读，发帖和回复需要登录。
- 帖子要能和现有 `Skill`、`Rule`、`Article`、`AiTool` 关联。
- 所有内容都要可搜索、可筛选、可管理。

## 信息架构

建议路由：
- `/forum`
- `/forum/new`
- `/forum/:id`
- `/forum/:id/edit`
- `/forum/categories/:id`
- `/forum/tags/:tag`
- `/forum/mine`
- `/forum/notifications`
- `/admin/forum`
- `/admin/forum/categories`

## 内容类型

建议先做 3 种帖子类型：
- `QUESTION`：提问，支持最佳答案
- `DISCUSSION`：讨论，适合方案对比和观点交流
- `SHARE`：经验分享，适合沉淀踩坑和实践记录

## 数据模型

### ForumCategory

- `id`
- `name`
- `slug`
- `description`
- `sortOrder`
- `isEnabled`
- `createdAt`
- `updatedAt`

### ForumPost

- `id`
- `title`
- `content`
- `postType`
- `categoryId`
- `authorId`
- `status`：`NORMAL` / `HIDDEN` / `LOCKED` / `DELETED`
- `isPinned`
- `isFeatured`
- `acceptedReplyId`
- `viewCount`
- `likeCount`
- `replyCount`
- `favoriteCount`
- `lastReplyAt`
- `lastActivityAt`
- `createdAt`
- `updatedAt`
- `deletedAt`

### ForumReply

- `id`
- `postId`
- `parentReplyId`
- `content`
- `authorId`
- `status`
- `likeCount`
- `createdAt`
- `updatedAt`
- `deletedAt`

### ForumReaction

- `id`
- `targetType`：`POST` / `REPLY`
- `targetId`
- `userId`
- `reactionType`：`LIKE`
- `createdAt`

### ForumFavorite

- `id`
- `postId`
- `userId`
- `createdAt`

### ForumNotification

- `id`
- `userId`
- `type`
- `targetType`
- `targetId`
- `actorId`
- `title`
- `content`
- `isRead`
- `createdAt`

### ForumEditHistory

- `id`
- `targetType`
- `targetId`
- `editorId`
- `beforeSnapshot`
- `afterSnapshot`
- `createdAt`

## 标签策略

- 复用现有 `Tag` 体系。
- 论坛帖子和回复不单独建一套标签表。
- 需要一个论坛专用关联表，例如 `forum_post_tags`。
- 列表页和详情页继续支持按标签筛选。

## 权限设计

- 匿名访客：可看列表、详情、搜索、分类、标签、热帖。
- 已登录用户：可发帖、回复、点赞、收藏、编辑自己的内容、查看自己的通知。
- 作者：可编辑自己的帖子和回复，建议保留编辑历史。
- 管理员：可置顶、加精、锁帖、隐藏、删除、恢复、改分类、合并重复帖。

## 发帖规则

- 标题必填。
- 正文必填。
- 分类必选。
- 标签可选，但建议最多 5 个。
- 提问帖支持“采纳最佳答案”。
- 帖子默认公开，不需要审核。

## 回复规则

- 回复支持楼中楼，最多一层嵌套即可，别一开始做太深。
- 回复可引用原文，也可纯文本。
- 被锁帖后不能继续回复。
- 回复可被作者删除，建议软删。

## 页面设计

### 论坛列表页

左侧：
- 分类
- 标签
- 热门话题

主区：
- 发帖按钮
- 搜索框
- 排序切换：最新 / 最热 / 未解决 / 我参与的
- 帖子卡片：标题、摘要、标签、分类、作者、回复数、点赞数、最后活跃时间、是否已解决

### 详情页

主区：
- 帖子正文
- 作者信息
- 标签
- 关联对象
- 回复列表
- 最佳答案

侧栏：
- 同类帖子
- 相关 `Skill` / `Rule`
- 热门标签

### 发帖页

- 标题
- 类型
- 分类
- 标签
- 正文编辑器
- 关联 `Skill` / `Rule` / `Article`

### 我的页面

- 我的帖子
- 我的回复
- 我的收藏
- 我的通知

### 管理页

- 置顶 / 加精 / 锁帖 / 隐藏 / 删除
- 分类管理
- 举报处理
- 重复帖合并

## API 草案

公开：
- `GET /api/forum/posts`
- `GET /api/forum/posts/:id`
- `GET /api/forum/categories`
- `GET /api/forum/tags`

登录后：
- `POST /api/forum/posts`
- `PUT /api/forum/posts/:id`
- `DELETE /api/forum/posts/:id`
- `POST /api/forum/posts/:id/replies`
- `PUT /api/forum/replies/:id`
- `DELETE /api/forum/replies/:id`
- `POST /api/forum/posts/:id/like`
- `DELETE /api/forum/posts/:id/like`
- `POST /api/forum/posts/:id/favorite`
- `DELETE /api/forum/posts/:id/favorite`
- `GET /api/forum/mine/posts`
- `GET /api/forum/mine/replies`
- `GET /api/forum/notifications`

管理员：
- `POST /api/admin/forum/posts/:id/pin`
- `POST /api/admin/forum/posts/:id/unpin`
- `POST /api/admin/forum/posts/:id/lock`
- `POST /api/admin/forum/posts/:id/unlock`
- `POST /api/admin/forum/posts/:id/hide`
- `POST /api/admin/forum/posts/:id/unhide`
- `DELETE /api/admin/forum/posts/:id`
- `GET /POST /PUT /DELETE /api/admin/forum/categories`

## 排序与搜索

建议支持：
- 最新
- 最热
- 未解决
- 已解决
- 我参与的

热度可以先用简单分数：
- 回复数
- 点赞数
- 收藏数
- 最后活跃时间
- 发布时间衰减

## 和现有模块的关系

- `Skill` 详情页展示“相关讨论”。
- `Rule` 详情页展示“相关讨论”。
- 帖子可手动关联 `Skill`、`Rule`、`Article`。
- 论坛高质量帖子可以后续一键转成 `Rule` 或知识库文章。
- 论坛里重复出现的问题可以反向补充成新的 `Skill`。

## 编辑器建议

- 正文优先用 Markdown。
- 代码块、引用、列表、表格要可用。
- 前端可复用现有 `marked` + `DOMPurify` 的渲染方案。
- 图片上传先别作为 MVP 强依赖；需要时再开放用户上传能力。

## MVP 范围

第一阶段只做：
- 列表
- 详情
- 发帖
- 回复
- 分类/标签筛选
- 搜索
- 点赞
- 收藏
- 采纳最佳答案
- 管理员置顶/锁帖/隐藏/删除

## V2 再做

- @ 用户
- 消息通知
- 编辑历史展示
- 举报与审核流
- 草稿箱
- 附件与图片上传
- 话题关注
- 周榜 / 月榜
- 精华帖
- 帖子转知识库

## 实现建议

- 优先独立一套论坛实体，不要硬塞进现有 `Skill` / `Rule` 表。
- 标签复用现有 `Tag`，分类独立。
- 先做匿名可读、登录可发、管理员可管。
- 先把“提问 + 回复 + 采纳”跑通，再扩展社区玩法。
