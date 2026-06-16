import assert from 'node:assert/strict'
import test from 'node:test'

import { githubTrendingSummary } from './homeGithubTrending.js'

test('github trending description uses the admin effect field', () => {
  assert.equal(
    githubTrendingSummary({
      description: 'An open-source framework for building AI agents.',
      descriptionCn: '一个用于构建 AI Agent 的开源框架。',
      effectCn: '用于构建 AI Agent 的开源框架。',
    }),
    '用于构建 AI Agent 的开源框架。',
  )
})

test('github trending description does not fall back to other summary fields', () => {
  assert.equal(
    githubTrendingSummary({
      description: 'An open-source framework for building AI agents.',
      descriptionCn: '一个用于构建 AI Agent 的开源框架。',
      effectCn: '  ',
      scenarioCn: '适合企业智能助手场景。',
    }),
    '暂无描述',
  )
})

test('github trending description shows a placeholder when effect is absent', () => {
  assert.equal(
    githubTrendingSummary({
      description: 'An open-source framework for building AI agents.',
      effectCn: '',
      scenarioCn: '',
    }),
    '暂无描述',
  )
})
