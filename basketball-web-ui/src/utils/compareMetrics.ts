export type MetricFormatType = 'percent' | 'signed' | 'decimal'

export type CompareWinnerSide = 'left' | 'right' | 'draw' | null

export function formatMetricValue(
  value: number | null | undefined,
  type: MetricFormatType,
): string {
  const numericValue = Number(value)

  if (!Number.isFinite(numericValue)) {
    return '--'
  }

  if (type === 'percent') {
    return `${numericValue.toFixed(1)}%`
  }

  if (type === 'signed') {
    return `${numericValue > 0 ? '+' : ''}${numericValue.toFixed(1)}`
  }

  return numericValue.toFixed(1)
}

export function getWinnerSide(
  leftValue: number | null | undefined,
  rightValue: number | null | undefined,
  higherIsBetter: boolean,
): CompareWinnerSide {
  const left = Number(leftValue)
  const right = Number(rightValue)

  if (!Number.isFinite(left) || !Number.isFinite(right)) {
    return null
  }

  if (left === right) {
    return 'draw'
  }

  if (higherIsBetter) {
    return left > right ? 'left' : 'right'
  }

  return left < right ? 'left' : 'right'
}

export function getMetricPercent(
  value: number | null | undefined,
  leftValue: number | null | undefined,
  rightValue: number | null | undefined,
): number {
  const val = Number(value)
  const left = Number(leftValue)
  const right = Number(rightValue)

  if (!Number.isFinite(val)) {
    return 0
  }

  const leftValid = Number.isFinite(left)
  const rightValid = Number.isFinite(right)

  if (!leftValid && !rightValid) {
    return 0
  }

  const minVal = Math.min(leftValid ? left : val, rightValid ? right : val)
  const maxVal = Math.max(leftValid ? left : val, rightValid ? right : val, 1)

  if (minVal < 0) {
    const range = maxVal - minVal || 1
    return Math.max(8, Math.min(100, ((val - minVal) / range) * 100))
  }

  const maxValue = Math.max(leftValid ? left : 0, rightValid ? right : 0, 1)

  if (maxValue <= 0) {
    return 0
  }

  return Math.max(8, Math.min(100, (val / maxValue) * 100))
}

export function getMetricFormatType(key: string): MetricFormatType {
  if (key.endsWith('Pct') || key === 'winRate') {
    return 'percent'
  }

  if (key === 'avgPointDiff') {
    return 'signed'
  }

  return 'decimal'
}
