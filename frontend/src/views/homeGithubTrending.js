export function githubTrendingSummary(repo) {
  const effect = repo?.effectCn?.trim()
  if (effect) return effect

  return '暂无描述'
}
