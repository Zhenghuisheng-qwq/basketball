/** 真实球队不再依赖 mock badgeKey，统一走 logo / 文字占位 */
export function getTeamBadgeKey(_teamId: string): string | undefined {
  return undefined
}
