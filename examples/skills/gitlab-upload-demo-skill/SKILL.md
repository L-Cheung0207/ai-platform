---
name: gitlab-upload-demo-skill
description: Use when validating Teleone AI Skill package upload and GitLab publication flow in a local environment.
sourceRepositoryUrl: http://127.0.0.1.nip.io:8929/teleone-ai/ai-skills
skillCategory: coding_implementation
buildPriority: P2
version: 1.0.0
tags: [GitLab, Skill 包导入, 本地联调]
quickValidate: PASSED
---

# GitLab Upload Demo Skill

Use this Skill to validate that the Teleone AI Skill asset platform can import a skill-creator zip package and publish it into GitLab.

## 适用场景

本地联调 Skill 包上传、GitLab 自动建分支、创建 Merge Request、平台登记资产的完整链路。

## 不适用场景

不用于生产代码审查、真实业务交付或包含敏感数据的 Skill 资产。

## 输入要求

本地 GitLab 地址、平台上传入口、贡献者或管理员账号，以及这个 demo Skill zip 包。

## 执行步骤

1. 在平台上传这个 Skill zip 包。
2. 检查包结构校验是否通过。
3. 检查 GitLab 发布状态是否显示已发布。
4. 打开返回的 Merge Request 链接，确认文件进入 `skills/gitlab-upload-demo-skill`。
5. 在平台 Skill 列表中确认资产登记为候选状态。

## 输出格式

输出一次链路验收记录：平台导入结果、GitLab MR 链接、分支名、Skill 路径和待处理问题。

## 质量标准

必须能区分平台包校验、GitLab 发布、资产登记三个阶段；任何失败都需要记录错误消息和复现步骤。

## 验证方式

quick_validate.py PASSED。上传后以平台返回的 GitLab 发布结果和 MR 文件列表作为验收证据。

## 参考资料

参考 `references/upload-checklist.md` 中的联调检查项。
