import type { BatchAuditOperationReq, BatchAuditResult } from '@/types'

export interface BatchAuditSelectionOptions<T> {
  getRowId: (row: T) => number | undefined
  canAudit: (row: T) => boolean
}

export function useBatchAuditSelection<T>(options: BatchAuditSelectionOptions<T>) {
  const selectedRows = ref<T[]>([])
  const multiple = computed(() => !selectedRows.value.length)
  const auditableSelected = computed(() => selectedRows.value.filter(options.canAudit))

  function handleSelectionChange(selection: T[]) {
    selectedRows.value = selection
  }

  function rowSelectable(row: T) {
    return options.canAudit(row)
  }

  function clearSelection() {
    selectedRows.value = []
  }

  function getAuditableIds(): number[] {
    return auditableSelected.value
      .map(row => options.getRowId(row))
      .filter((id): id is number => id != null)
  }

  return {
    selectedRows,
    multiple,
    auditableSelected,
    handleSelectionChange,
    rowSelectable,
    clearSelection,
    getAuditableIds
  }
}

export function formatBatchAuditMessage(result?: BatchAuditResult) {
  if (!result) {
    return '操作完成'
  }
  const { successCount, failCount } = result
  if (failCount > 0) {
    const firstFailure = result.failures?.[0]
    const detail = firstFailure?.message ? `，例如：${firstFailure.message}` : ''
    return `成功 ${successCount} 条，失败 ${failCount} 条${detail}`
  }
  return `已成功处理 ${successCount} 条`
}

export function buildBatchAuditPayload(ids: number[], extra: Partial<BatchAuditOperationReq> = {}): BatchAuditOperationReq {
  return {
    ids,
    ...extra
  }
}
