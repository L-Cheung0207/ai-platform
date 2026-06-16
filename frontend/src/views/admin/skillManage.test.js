import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { test } from 'node:test'
import { fileURLToPath } from 'node:url'
import { dirname, join } from 'node:path'

const __dirname = dirname(fileURLToPath(import.meta.url))
const source = readFileSync(join(__dirname, 'SkillManage.vue'), 'utf8')
const formSource = readFileSync(join(__dirname, '../../components/skills/SkillAssetForm.vue'), 'utf8')

function assertHiddenByReviewFields(modelName) {
  const modelIndex = formSource.indexOf(`form.${modelName}`)
  assert.notEqual(modelIndex, -1, `${modelName} should exist in SkillAssetForm`)
  const nearbyTemplate = formSource.slice(Math.max(0, modelIndex - 520), modelIndex + 120)
  assert.match(
    nearbyTemplate,
    /v-if="!props\.hideReviewFields"/,
    `${modelName} should be hidden when /admin/skills uses hide-review-fields`,
  )
}

test('/admin/skills does not expose the skill review action', () => {
  assert.equal(source.includes('<SkillReviewDialog'), false)
  assert.equal(source.includes('openReview(row)'), false)
  assert.equal(source.includes("import SkillReviewDialog"), false)
  assert.equal(source.includes('reviewDialogVisible'), false)
})

test('/admin/skills hides review and re-review fields in the edit form', () => {
  assert.match(source, /<SkillAssetForm[\s\S]*hide-review-fields/)
  assert.match(formSource, /hideReviewFields/)
  assert.match(formSource, /v-if="!props\.hideReviewFields"/)
})

test('/admin/skills hides governance intake fields in the edit form', () => {
  assertHiddenByReviewFields('riskLevel')
  assertHiddenByReviewFields('buildPriority')
  assertHiddenByReviewFields('teamName')
  assertHiddenByReviewFields('trialStartedAt')
  assertHiddenByReviewFields('trialEndsAt')
  assertHiddenByReviewFields('validationMethod')
  assertHiddenByReviewFields('qualityStandard')
})
