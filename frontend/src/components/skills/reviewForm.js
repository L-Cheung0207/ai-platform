export function createReviewForm(overrides = {}) {
  return {
    reviewerName: '',
    reviewerRole: 'CONTRIBUTOR',
    reviewStage: 'TEAM_REVIEW',
    result: '',
    truthful: true,
    accurate: true,
    reusable: true,
    executable: true,
    secure: true,
    verifiable: true,
    maintainable: true,
    reviewedAt: new Date().toISOString().slice(0, 10),
    nextReviewAt: '',
    notes: '',
    ...overrides,
  }
}

export function hasSelectedReviewResult(form) {
  return Boolean(form?.result)
}

export function formatReviewHistoryTime(row) {
  const value = row?.createdAt || row?.reviewedAt
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 19)
}
