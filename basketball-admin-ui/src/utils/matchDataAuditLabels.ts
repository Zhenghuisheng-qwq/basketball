export function formatVersion(versionNo?: number | null): string {
  if (versionNo == null) return '-'
  return `V${versionNo}`
}

export function submissionStatusTagType(status?: number | null, auditStatus?: string | null): '' | 'success' | 'warning' | 'danger' | 'info' {
  if (status === 2 || auditStatus === 'APPROVED') return 'success'
  if (status === 3 || auditStatus === 'REJECTED') return 'danger'
  if (status === 1 || auditStatus === 'PENDING') return 'warning'
  return 'info'
}

export function startedFlagText(flag?: number | null): string {
  if (flag === 1) return '首发'
  if (flag === 0) return '替补'
  return '-'
}

export function formatFileSize(size?: number | null): string {
  if (size == null || size <= 0) return '-'
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`
  return `${(size / 1024 / 1024).toFixed(1)} MB`
}

export const REJECT_REASON_PRESETS = [
  '比分与分节合计不一致',
  '球员得分合计不一致',
  '证明材料不清晰',
  '球员不在赛事阵容中',
  '数据与对方提交不一致',
  '其他',
]
