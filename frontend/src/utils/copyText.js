function copyWithExecCommand(text) {
  const textarea = document.createElement('textarea')
  textarea.value = text
  textarea.setAttribute('readonly', '')
  textarea.style.position = 'fixed'
  textarea.style.top = '0'
  textarea.style.left = '0'
  textarea.style.opacity = '0'
  textarea.style.pointerEvents = 'none'
  document.body.appendChild(textarea)
  textarea.focus()
  textarea.select()
  textarea.setSelectionRange(0, text.length)
  let ok = false
  try {
    ok = document.execCommand('copy')
  } catch {
    ok = false
  }
  document.body.removeChild(textarea)
  return ok
}

export function selectElementText(element) {
  if (!element) return false
  const range = document.createRange()
  range.selectNodeContents(element)
  const selection = window.getSelection()
  if (!selection) return false
  selection.removeAllRanges()
  selection.addRange(range)
  return true
}

export async function copyText(text) {
  const value = String(text ?? '')
  if (!value) return false

  if (navigator.clipboard?.writeText) {
    try {
      await navigator.clipboard.writeText(value)
      return true
    } catch {
      // Fall through to legacy copy.
    }
  }

  return copyWithExecCommand(value)
}
