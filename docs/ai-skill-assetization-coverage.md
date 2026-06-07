# AI Skill 资产化覆盖审计

更新时间：2026-05-31

来源方案：`AI_Skill资产化建设方案.docx`

当前结论：按最新范围，只继续做“权限管理”和“GitLab 集成”，其余 GitHub/Jenkins/覆盖率专用适配、企业 IAM 同步、前端自动化测试不再纳入本阶段。当前项目已经具备 AI Skill 资产字段、分层目录、创建来源、模板校验、评审门禁、用户治理角色绑定与后台配置、反馈/使用记录、安全化工具链遥测入口、GitLab webhook/CI 接入、月度运营、季度治理、试点看板、包导入和下架候选队列。

## 覆盖矩阵

| 方案要求 | 当前证据 | 状态 | 后续处理 |
| --- | --- | --- | --- |
| Skill 是结构化能力包，包含场景、输入、步骤、参考、输出、验证、维护人、版本 | `Skill` 资产字段、`CreateSkillRequest`、`SkillAssetForm`、`SkillDetail`、V16/V18 迁移 | 已落地 | 继续保持模板校验为入库前置门禁 |
| 个人 / 团队 / 公司三级资产 | `AssetLevel`、管理端和公开列表筛选、运营指标分层统计 | 已落地 | 可增加升级/降级操作记录 |
| 候选、试用、评审、入库、持续运营、归档/下架生命周期 | `LifecycleStatus`、试用起止日期、评审 API、运营提醒、隐藏/归档能力、下架候选队列 | 已落地 | 可继续增加一键发起归档评审和操作审计 |
| 研发全生命周期分类 | `SkillCategory`、前后端筛选、V21 迁移 | 已落地 | 可按优先建设清单继续补默认样板 |
| P0/P1/P2 建设优先级 | `BuildPriority`、管理/公开筛选、指标统计 | 已落地 | 可在运营看板加入按优先级的待办建议 |
| 统一模板与目录规范 | 模板校验要求 SKILL.md frontmatter；团队/公司 Skill 必须有 `skillDirectory` 和 `sourceRepositoryUrl` | 已落地 | 手工录入仍可能伪造来源，后续可增加仓库链接格式/可访问性校验 |
| 所有团队/公司级 Skill 必须由 skill-creator 初始化 | 包导入要求 `SKILL.md`、目录名匹配、`agents/openai.yaml`、仓库来源、`quick_validate.py` 通过证据；`creationSource` 区分手工录入、skill-creator 包、仓库同步和试点样板 | 已落地 | 可继续让团队/公司级手工录入进入更严格的来源复核 |
| quick_validate.py 校验 | `SkillPackageImportService` 要求 quick_validate 通过证据；导入测试覆盖缺失/通过场景 | 已落地 | 当前是证据校验，不在服务端执行 zip 内脚本 |
| 评审标准：真实、准确、可复用、可执行、安全、可验证、可维护 | `SkillReview` 七项布尔评审项、评审对话框、评审列表 | 已落地 | 可增加低分/未勾选项阻断规则，目前主要由评审结果决定 |
| 入库门禁 | 不能手动设置 `APPROVED`；通过评审前必须模板校验通过；公司级和高风险 Skill 要求更高评审角色；评审角色从登录用户 `skillGovernanceRole` 读取，不信任前端自填 | 已落地 | 单测/API 测试已覆盖关键门禁和角色防伪造 |
| 安全红线 | 模板校验检测私钥、云密钥、JWT/token、凭据字段、身份证号 | 已落地 | 可扩展到更多内部敏感数据模式 |
| 使用记录、反馈、失败案例、改进建议 | 使用记录 API、反馈 API、公开详情治理面板、管理端反馈 inbox；`POST /api/skills/{id}/usage` 支持工具链来源；`POST /api/integrations/skill-telemetry` 支持 token、`skillId` 或 `skillDirectory` 定位、外部事件 ID、仓库/分支/提交/CI 状态；`POST /api/integrations/skill-telemetry/gitlab` 支持 GitLab Secret token 和 pipeline/MR payload 映射；`docs/skill-toolchain-telemetry.md` 和 `examples/skill-telemetry/skill-telemetry-client.mjs` 给出 GitLab 接入口径 | 已落地 | 非 GitLab 平台不纳入本阶段 |
| 节省工时、新人上手、Review 问题减少率、测试覆盖提升 | `SkillUsageEvent` 质量字段、工具链遥测字段、安全 token、GitLab token、幂等入库、指标 DTO、运营看板、月报 Markdown；GitLab webhook/CI 字段模板和 Node 18+ 上报示例已提供 | 已落地 | 非 GitLab 平台不纳入本阶段 |
| 月度运营看板与季度治理 | `/api/admin/skill-operations`、`/api/admin/skill-operations/quarterly-report`、`SkillOpsDashboard`、`SkillQuarterlyReportPanel`、月度趋势、Top Skill、复审队列、下架候选队列、季度 Markdown 报告下载 | 已落地 | 可继续增加季度报告归档表 |
| 月度高价值 Skill 激励 | 高价值候选 DTO、看板组件、月报章节 | 已落地 | 贡献计入晋升/年度贡献属于组织流程，系统未建档 |
| 试点 4-6 周里程碑 | 试点里程碑 DTO、运营看板、DataInitializer 第一批 5 个样板 Skill | 已落地 | 里程碑规则仍是固定计算，可后续配置化 |
| 推荐首批试点 Skill | DataInitializer 覆盖代码 Review、单测生成、接口文档、线上排障、技术方案初稿 | 已落地 | 可继续补 Java/Spring Boot 代码规范、SQL 优化、发布检查、新人熟悉代码库 |
| 下架风险：无人使用、结果不稳定、规范过期、高风险 | `SkillArchiveCandidateDto`、`archiveCandidates()`、运营看板“下架候选”面板；覆盖长期无人/无近期使用、待复审、失败案例和高风险待处理反馈 | 已落地 | 当前只给出候选和建议动作，不自动归档 |
| 组织分工：贡献者、技术委员会、平台工程、Tech Lead、安全/质量 | `ReviewerRole` 覆盖 `CONTRIBUTOR` 和评审角色；普通开发人员使用系统角色 `NORMAL` + 治理角色 `CONTRIBUTOR`，可作为 Skill 贡献者但不能直接评审入库；评审 API 使用登录用户姓名和治理角色；管理员初始化/迁移为 `SECURITY_QUALITY`；`/api/admin/users` 与 `/admin/users` 用户管理模块支持用户 CRUD、系统角色、治理角色配置，并保护至少保留一个管理员 | 已落地 | 企业 IAM/组织架构同步不纳入本阶段 |
| 统一发布与检索入口 | 公开 Skill 列表/详情、管理端 Skill 管理、包导入入口 | 已落地 | IDE/AI 工具入口和内部文档推广不在当前系统内 |
| 自动化测试保障 | `SkillGovernanceGateTest`、`SkillPackageImportServiceTest`、`AdminSkillApiIntegrationTest` 覆盖后端 service/DTO/API 关键链路 | 已落地本阶段后端范围 | 前端交互测试不纳入本阶段 |

## 本阶段收口项

| 项目 | 状态 | 说明 |
| --- | --- | --- |
| 权限管理 | 已落地 | 后台 `/admin/users` 用户管理模块和 `POST/GET/PUT/DELETE /api/admin/users`、`PUT /api/admin/users/{id}/role`、`PUT /api/admin/users/{id}/skill-governance-role` 已支持用户 CRUD、系统角色与治理角色配置；普通开发默认 `NORMAL` + `CONTRIBUTOR` |
| GitLab 集成 | 已落地 | `/api/integrations/skill-telemetry/gitlab` 支持 `X-Gitlab-Token`、`X-Skill-Directory`、pipeline/MR payload 映射和幂等入库 |
| 其他平台/IAM/前端自动化测试 | 不纳入本阶段 | 按最新范围不继续执行 |

## 最近验证

- `rtk mvn test`：通过，29 个后端 service/DTO/API 测试全部成功，包含安全化工具链遥测 token、目录解析、幂等上报、公开 usage API、评审角色防伪造、登录用户治理角色绑定和管理员用户角色管理测试。
- `rtk pnpm build`：通过，保留既有 Vite 动态导入和大 chunk 警告。
- API 验证：`POST /api/skills/{id}/usage` 支持 `toolchainSource`、`externalEventId`、仓库/分支/提交/CI 状态；`POST /api/integrations/skill-telemetry` 支持 `X-Skill-Telemetry-Token`、`skillDirectory` 定位和幂等上报，缺 token 返回 401；`/api/admin/skill-metrics` 返回 `toolchainUsageCount`、来源分布和 CI/代码评审/覆盖率计数；用户管理支持 `POST/GET/PUT/DELETE /api/admin/users`，可创建用户、编辑系统角色/治理角色、删除用户，且阻止降级最后一个管理员和删除当前登录用户；贡献者 `CONTRIBUTOR` 不能直接评审入库。
- GitLab 验证：`POST /api/integrations/skill-telemetry/gitlab` 使用 `X-Gitlab-Token` 校验；pipeline payload 可映射为 `CI` 使用事件，重复 webhook 通过 `gitlab:<repo>:pipeline:<id>` 幂等。
- SDK 示例验证：`rtk node --check examples/skill-telemetry/skill-telemetry-client.mjs` 通过；使用开发 token、`skillDirectory=unit-test-generator` 和 `toolchainSource=CI` 调用示例脚本可成功写入遥测。
- 季报验证：`/api/admin/skill-operations/quarterly-report` 返回季度治理指标、工具链治理发现、风险、建议、下架候选和 Markdown。
- 角色验证：登录响应返回 `skillGovernanceRole`；`POST /api/admin/skills/{id}/reviews` 会忽略 payload 里的伪造评审人/评审角色，使用登录用户姓名和治理角色。
- 创建来源：V25 增加 `creation_source`；手工 API 创建返回 `MANUAL`，skill-creator zip 导入写入 `SKILL_CREATOR_PACKAGE`，试点样板回填 `SEED`。
- 浏览器验证：`/admin/skill-operations` 正常渲染“工具链信号”KPI、季度治理和下架候选面板；`/admin/skills` 评审弹窗正常展示登录用户和治理角色；`/admin/users` 正常展示用户角色列表和治理角色下拉。
- `rtk git diff --check`：通过。
- 后端服务：`8080` 已启动。
- 前端服务：`5173` 已启动。
