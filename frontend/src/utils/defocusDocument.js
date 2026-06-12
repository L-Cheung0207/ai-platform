/** 将焦点从 body 移到隐藏节点，避免 Chrome 刷新后画出整页黑框 */
export function defocusDocument() {
  const guard = document.getElementById('focus-guard')
  if (!guard) return
  if (
    document.activeElement === document.body
    || document.activeElement === document.documentElement
    || document.activeElement == null
  ) {
    guard.focus({ preventScroll: true })
  }
}
