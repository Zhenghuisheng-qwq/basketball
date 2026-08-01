import type { TableDataInfo } from '@/types'

/** 将接口 total（可能是字符串）规范为分页组件可用的数字 */
export function resolvePageTotal(total: unknown, rowsLength = 0): number {
  const n = Number(total)
  if (Number.isFinite(n) && n >= 0) {
    return n
  }
  return rowsLength
}

/** 统一解析 TableDataInfo 列表响应 */
export function applyTablePage<T>(response: TableDataInfo<T[]>, rows: { value: T[] }, total: { value: number }) {
  rows.value = response.rows ?? []
  total.value = resolvePageTotal(response.total, rows.value.length)
}
