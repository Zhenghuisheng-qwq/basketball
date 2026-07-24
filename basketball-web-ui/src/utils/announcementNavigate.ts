export function normalizeAnnouncementCategory(value: unknown, allowed?: string[]): string {
  if (typeof value !== 'string' || !value.trim()) return 'ALL'
  const code = value.trim().toUpperCase()
  if (code === 'ALL') return 'ALL'
  if (allowed && allowed.length > 0 && !allowed.includes(code)) return 'ALL'
  return code
}

export function parseAnnouncementPage(value: unknown, fallback = 1): number {
  const parsed = Number(value)
  if (!Number.isFinite(parsed) || parsed < 1) return fallback
  return Math.floor(parsed)
}

export function parseAnnouncementKeyword(value: unknown): string {
  if (typeof value !== 'string') return ''
  return value.trim()
}

export function buildAnnouncementListQuery(state: {
  category?: string
  keyword?: string
  page?: number
}) {
  const query: Record<string, string> = {
    category: state.category ?? 'ALL',
    page: String(state.page ?? 1),
  }
  const keyword = (state.keyword ?? '').trim()
  if (keyword) {
    query.keyword = keyword
  }
  return query
}

export function buildAnnouncementDetailFromQuery(query: Record<string, unknown>) {
  return {
    fromCategory: normalizeAnnouncementCategory(query.fromCategory),
    fromKeyword: parseAnnouncementKeyword(query.fromKeyword),
    fromPage: parseAnnouncementPage(query.fromPage, 1),
  }
}

export function goToAnnouncementDetail(
  router: { push: (location: { path: string; query?: Record<string, string> }) => void },
  announcementId: string | number,
  listState?: { category: string; keyword: string; page: number },
) {
  const query: Record<string, string> = {}
  if (listState) {
    query.fromCategory = listState.category
    query.fromPage = String(listState.page)
    if (listState.keyword) {
      query.fromKeyword = listState.keyword
    }
  }
  router.push({
    path: `/announcements/${announcementId}`,
    query,
  })
}

export function goToAnnouncementList(
  router: { push: (location: { path: string; query?: Record<string, string> }) => void },
  state?: { category?: string; keyword?: string; page?: number },
) {
  router.push({
    path: '/announcements',
    query: buildAnnouncementListQuery({
      category: state?.category,
      keyword: state?.keyword,
      page: state?.page,
    }),
  })
}

export type PaginationItem = number | 'ellipsis'

export function buildPaginationItems(current: number, totalPages: number): PaginationItem[] {
  if (totalPages <= 7) {
    return Array.from({ length: totalPages }, (_, index) => index + 1)
  }

  const items: PaginationItem[] = [1]

  if (current > 3) {
    items.push('ellipsis')
  }

  const start = Math.max(2, current - 1)
  const end = Math.min(totalPages - 1, current + 1)

  for (let page = start; page <= end; page += 1) {
    items.push(page)
  }

  if (current < totalPages - 2) {
    items.push('ellipsis')
  }

  items.push(totalPages)
  return items
}

export function formatAnnouncementDate(value?: string | null): string {
  if (!value) return '--'
  const normalized = value.replace('T', ' ').trim()
  const datePart = normalized.slice(0, 10)
  return datePart || '--'
}
