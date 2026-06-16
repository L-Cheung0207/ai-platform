import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)
const routerSource = fs.readFileSync(path.join(__dirname, 'index.js'), 'utf8')

test('requiresAuth routes refresh axios auth token before navigation continues', () => {
  const normalized = routerSource.replace(/\s+/g, ' ')
  assert.match(
    normalized,
    /const requiresAuth = to\.matched\.some\(\(record\) => record\.meta\.requiresAuth\).*?if \(requiresAuth && !auth\.isAuthenticated\).*?return.*?if \(requiresAuth\).*?setAuthToken\(auth\.token\).*?next\(\)/,
  )
})

test('admin child routes inherit requiresAdmin from matched parent records', () => {
  const normalized = routerSource.replace(/\s+/g, ' ')
  assert.match(
    normalized,
    /const requiresAdmin = to\.matched\.some\(\(record\) => record\.meta\.requiresAdmin\).*?if \(requiresAdmin\).*?setAuthToken\(auth\.token\)/,
  )
})
