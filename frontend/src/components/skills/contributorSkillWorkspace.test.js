import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { test } from 'node:test'
import { fileURLToPath } from 'node:url'
import { dirname, join } from 'node:path'

const __dirname = dirname(fileURLToPath(import.meta.url))
const source = readFileSync(join(__dirname, 'ContributorSkillWorkspace.vue'), 'utf8')

test('front-stage skill edit dialog hides governance intake fields', () => {
  assert.match(source, /<SkillAssetForm[\s\S]*hide-review-fields[\s\S]*@submit="saveSkill"/)
})
