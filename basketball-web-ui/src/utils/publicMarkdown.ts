function escapeHtml(value: string): string {
  return value
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
}

function renderInline(value: string): string {
  let text = escapeHtml(value.trim())
  text = text.replace(/\[([^\]]+)\]\(([^)\s]+)\)/g, (_match, label: string, href: string) => {
    if (!/^(https?:\/\/|\/|#)/i.test(href)) return label
    return `<a href="${href}" target="_blank" rel="noopener noreferrer">${label}</a>`
  })
  text = text.replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>')
  text = text.replace(/__([^_]+)__/g, '<strong>$1</strong>')
  text = text.replace(/(^|[^\w*])\*([^*\n]+)\*/g, '$1<em>$2</em>')
  return text
}

function isHtmlContent(value: string): boolean {
  return /<\/?(p|br|div|span|strong|em|b|i|u|h[1-6]|ul|ol|li|img|a|blockquote|pre|code)\b/i.test(value)
}

export function renderPublicMarkdown(source?: string | null): string {
  const raw = String(source || '').replace(/\r\n/g, '\n').trim()
  if (!raw) return ''
  if (isHtmlContent(raw)) return raw

  const lines = raw.split('\n')
  const blocks: string[] = []
  let paragraph: string[] = []
  let list: { type: 'ul' | 'ol'; items: Array<{ value?: number; html: string }> } | null = null

  function flushParagraph() {
    if (!paragraph.length) return
    blocks.push(`<p>${paragraph.join('<br>')}</p>`)
    paragraph = []
  }

  function flushList() {
    if (!list) return
    const items = list.items.map((item) => {
      const value = item.value ? ` value="${item.value}"` : ''
      return `<li${value}>${item.html}</li>`
    }).join('')
    blocks.push(`<${list.type}>${items}</${list.type}>`)
    list = null
  }

  lines.forEach((line) => {
    const trimmed = line.trim()
    if (!trimmed) {
      flushParagraph()
      flushList()
      return
    }

    const heading = /^(#{1,6})\s+(.+)$/.exec(trimmed)
    if (heading) {
      flushParagraph()
      flushList()
      const level = heading[1].length
      blocks.push(`<h${level}>${renderInline(heading[2])}</h${level}>`)
      return
    }

    const unordered = /^[-*]\s+(.+)$/.exec(trimmed)
    if (unordered) {
      flushParagraph()
      if (!list || list.type !== 'ul') {
        flushList()
        list = { type: 'ul', items: [] }
      }
      list.items.push({ html: renderInline(unordered[1]) })
      return
    }

    const ordered = /^(\d+)\.\s+(.+)$/.exec(trimmed)
    if (ordered) {
      flushParagraph()
      if (!list || list.type !== 'ol') {
        flushList()
        list = { type: 'ol', items: [] }
      }
      list.items.push({ value: Number(ordered[1]), html: renderInline(ordered[2]) })
      return
    }

    if (list) flushList()
    paragraph.push(renderInline(trimmed))
  })

  flushParagraph()
  flushList()
  return blocks.join('')
}
