#!/usr/bin/env node

const endpoint = env('SKILL_TELEMETRY_URL', 'http://localhost:8080/api/integrations/skill-telemetry')
const token = requiredEnv('SKILL_TELEMETRY_TOKEN')

const payload = compact({
  skillId: numberEnv('SKILL_ID'),
  skillDirectory: env('SKILL_DIRECTORY'),
  userName: env('USER_NAME'),
  scenario: env('SCENARIO'),
  savedMinutes: numberEnv('SAVED_MINUTES'),
  newcomerOnboardingSavedMinutes: numberEnv('NEWCOMER_ONBOARDING_SAVED_MINUTES'),
  reviewIssuesBefore: numberEnv('REVIEW_ISSUES_BEFORE'),
  reviewIssuesAfter: numberEnv('REVIEW_ISSUES_AFTER'),
  testCoverageBefore: numberEnv('TEST_COVERAGE_BEFORE'),
  testCoverageAfter: numberEnv('TEST_COVERAGE_AFTER'),
  toolchainSource: requiredEnv('TOOLCHAIN_SOURCE'),
  externalEventId: env('EXTERNAL_EVENT_ID'),
  repository: env('REPOSITORY'),
  branchName: env('BRANCH_NAME'),
  commitSha: env('COMMIT_SHA'),
  ciStatus: env('CI_STATUS'),
})

if (!payload.skillId && !payload.skillDirectory) {
  fail('Set SKILL_ID or SKILL_DIRECTORY so the telemetry endpoint can resolve the Skill.')
}

const response = await fetch(endpoint, {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'X-Skill-Telemetry-Token': token,
  },
  body: JSON.stringify(payload),
})

const body = await response.text()
if (!response.ok) {
  fail(`Skill telemetry failed with HTTP ${response.status}: ${body}`)
}

console.log(body)

function env(name, fallback = undefined) {
  const value = process.env[name]
  return value === undefined || value === '' ? fallback : value
}

function requiredEnv(name) {
  const value = env(name)
  if (!value) fail(`Missing required environment variable: ${name}`)
  return value
}

function numberEnv(name) {
  const value = env(name)
  if (value === undefined) return undefined
  const number = Number(value)
  if (!Number.isFinite(number)) fail(`${name} must be a finite number.`)
  return number
}

function compact(value) {
  return Object.fromEntries(
    Object.entries(value).filter(([, entry]) => entry !== undefined && entry !== null && entry !== '')
  )
}

function fail(message) {
  console.error(message)
  process.exit(1)
}
