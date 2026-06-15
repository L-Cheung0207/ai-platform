import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { test } from 'node:test'

const loginDialogSource = readFileSync(
  new URL('./LoginDialog.vue', import.meta.url),
  'utf8',
)
const globalStylesSource = readFileSync(
  new URL('../../assets/styles.css', import.meta.url),
  'utf8',
)

test('login dialog disables Element Plus body scroll width compensation', () => {
  assert.match(loginDialogSource, /:lock-scroll="false"/)
})

test('login dialog does not render a decorative before pseudo-element', () => {
  assert.doesNotMatch(
    globalStylesSource,
    /\.el-overlay\.login-dialog-modal\s+\.el-dialog::before/,
  )
})
