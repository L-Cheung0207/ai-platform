import assert from 'node:assert/strict'
import test from 'node:test'

import { createReviewForm, formatReviewHistoryTime, hasSelectedReviewResult } from './reviewForm.js'

test('review form starts without an implicit pass result', () => {
  const form = createReviewForm()

  assert.equal(form.result, '')
  assert.equal(hasSelectedReviewResult(form), false)
})

test('review form treats an explicit review result as selected', () => {
  const form = createReviewForm({ result: 'NEEDS_CHANGES' })

  assert.equal(form.result, 'NEEDS_CHANGES')
  assert.equal(hasSelectedReviewResult(form), true)
})

test('review history time shows seconds from the created timestamp', () => {
  assert.equal(
    formatReviewHistoryTime({ createdAt: '2026-06-10T01:02:03Z', reviewedAt: '2026-06-09' }),
    '2026-06-10 01:02:03',
  )
})

test('review history time falls back to review date when timestamp is absent', () => {
  assert.equal(formatReviewHistoryTime({ reviewedAt: '2026-06-09' }), '2026-06-09')
})
