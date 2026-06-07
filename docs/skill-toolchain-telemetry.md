# Skill GitLab 遥测接入规范

更新时间：2026-05-31

## 接入范围

当前只保留 GitLab 集成。GitHub、Jenkins、覆盖率平台专用适配器不再纳入本阶段范围。

通用入口仍可用于脚本上报：

`POST /api/integrations/skill-telemetry`

GitLab webhook 入口用于 GitLab 原生事件映射：

`POST /api/integrations/skill-telemetry/gitlab`

## 鉴权

通用入口使用请求头 `X-Skill-Telemetry-Token`，值来自 `app.skill.telemetry.token` / `SKILL_TELEMETRY_TOKEN`。

GitLab 入口使用请求头 `X-Gitlab-Token`，值来自 `app.skill.telemetry.gitlab.token` / `SKILL_TELEMETRY_GITLAB_TOKEN`。该值应配置为 GitLab webhook 的 Secret token，不要写进仓库、日志或构建产物。

## 字段口径

| 字段 | 必填 | 说明 |
| --- | --- | --- |
| `skillId` | 与 `skillDirectory` 二选一 | Skill 主键，适合平台已经保存资产 ID 的场景 |
| `skillDirectory` | 与 `skillId` 二选一 | Skill 目录；GitLab webhook 也可用请求头 `X-Skill-Directory` 传入 |
| `toolchainSource` | 通用入口必填 | 来源枚举；GitLab `pipeline` 默认映射为 `CI`，`merge_request` 默认映射为 `CODE_REVIEW` |
| `externalEventId` | 强烈建议 | 外部事件幂等键；GitLab webhook 未显式传入时会生成 `gitlab:<repo>:<event-kind>:<event-id>` |
| `repository` | 否 | GitLab 项目路径，如 `teleone/platform` |
| `branchName` | 否 | 分支名 |
| `commitSha` | 否 | 提交 SHA |
| `ciStatus` | 否 | GitLab pipeline / MR 状态，如 `success`、`failed`、`opened`、`merged` |
| `userName` | 否 | GitLab 触发用户 |
| `scenario` | 否 | 使用场景说明；未传时由 GitLab 事件生成 |
| `savedMinutes` | 否 | 预估节省工时，单位分钟 |
| `newcomerOnboardingSavedMinutes` | 否 | 新人上手节省时间，单位分钟 |
| `reviewIssuesBefore` | 否 | 使用前 Review 问题数 |
| `reviewIssuesAfter` | 否 | 使用后 Review 问题数 |
| `testCoverageBefore` | 否 | 使用前测试覆盖率，0-100 |
| `testCoverageAfter` | 否 | 使用后测试覆盖率，0-100 |

## GitLab Webhook 示例

GitLab 项目中配置 webhook：

- URL：`https://ai-skill.example.com/api/integrations/skill-telemetry/gitlab`
- Secret token：使用平台配置的 `SKILL_TELEMETRY_GITLAB_TOKEN`
- Trigger：建议先启用 Pipeline events；如要统计评审信号，再启用 Merge request events
- Custom header：`X-Skill-Directory: unit-test-generator`

GitLab pipeline event 的关键字段会被映射为：

```json
{
  "object_kind": "pipeline",
  "user": {
    "username": "gitlab-ci"
  },
  "project": {
    "path_with_namespace": "teleone/platform",
    "web_url": "https://gitlab.example.com/teleone/platform"
  },
  "object_attributes": {
    "id": 2201,
    "status": "success",
    "ref": "main",
    "sha": "abc2201"
  },
  "savedMinutes": 25
}
```

映射后的遥测事件：

- `toolchainSource`: `CI`
- `externalEventId`: `gitlab:teleone/platform:pipeline:2201`
- `repository`: `teleone/platform`
- `branchName`: `main`
- `commitSha`: `abc2201`
- `ciStatus`: `success`

## GitLab CI 脚本上报

如果暂时不启用 webhook，也可以在 GitLab CI 里用通用脚本上报：

```yaml
skill_telemetry:
  image: node:20-alpine
  stage: report
  rules:
    - if: $CI_PIPELINE_SOURCE == "push"
  variables:
    SKILL_TELEMETRY_URL: https://ai-skill.example.com/api/integrations/skill-telemetry
    SKILL_DIRECTORY: unit-test-generator
    TOOLCHAIN_SOURCE: CI
    EXTERNAL_EVENT_ID: gitlab:$CI_PROJECT_PATH:$CI_PIPELINE_ID:$CI_COMMIT_SHA
    REPOSITORY: $CI_PROJECT_PATH
    BRANCH_NAME: $CI_COMMIT_REF_NAME
    COMMIT_SHA: $CI_COMMIT_SHA
    CI_STATUS: $CI_JOB_STATUS
    USER_NAME: $GITLAB_USER_LOGIN
    SCENARIO: GitLab pipeline reported Skill usage telemetry
    SAVED_MINUTES: "30"
  script:
    - node examples/skill-telemetry/skill-telemetry-client.mjs
```
