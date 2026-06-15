import { marked } from 'marked'
import DOMPurify from 'dompurify'

const allowedTags = ['p', 'br', 'strong', 'em', 'a', 'ul', 'ol', 'li', 'h1', 'h2', 'h3', 'h4', 'h5', 'h6', 'pre', 'code', 'blockquote', 'hr', 'img', 'table', 'thead', 'tbody', 'tr', 'th', 'td', 'span']

export const forumPostTypeLabels = {
  QUESTION: '提问',
  DISCUSSION: '讨论',
  SHARE: '分享',
}

export const forumStatusLabels = {
  NORMAL: '公开',
  HIDDEN: '隐藏',
  LOCKED: '锁定',
  DELETED: '删除',
}

export const forumRelatedTypeLabels = {
  SKILL: 'Skill',
  RULE: 'Rule',
  ARTICLE: 'AI知识库',
  AI_TOOL: 'AI 工具',
}

export function renderForumMarkdown(raw) {
  const value = raw || ''
  if (!value.trim()) return ''
  try {
    const html = marked.parse(value, { async: false, gfm: true })
    return DOMPurify.sanitize(html, { ALLOWED_TAGS: allowedTags })
  } catch (error) {
    return DOMPurify.sanitize(value.replace(/</g, '&lt;').replace(/>/g, '&gt;'))
  }
}

export function formatForumTime(value) {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return ''
  return new Intl.DateTimeFormat('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' }).format(date)
}

export function formatForumRelativeTime(value) {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return ''
  const diff = Date.now() - date.getTime()
  const minute = 60 * 1000
  const hour = 60 * minute
  const day = 24 * hour
  if (diff < minute) return '刚刚'
  if (diff < hour) return `${Math.max(1, Math.floor(diff / minute))} 分钟前`
  if (diff < day) return `${Math.max(1, Math.floor(diff / hour))} 小时前`
  if (diff < 7 * day) return `${Math.max(1, Math.floor(diff / day))} 天前`
  return formatForumTime(value)
}

export function forumExcerpt(text, limit = 120) {
  const raw = (text || '').replace(/\s+/g, ' ').trim()
  if (!raw) return ''
  return raw.length <= limit ? raw : `${raw.slice(0, limit)}…`
}
