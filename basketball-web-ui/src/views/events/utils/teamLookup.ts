import type { Team } from '../types/events'

/** 将 V2 eventTeamId 映射为 hist 模板使用的数字 id */
export function teamIdToNumeric(teamId: string): number {
  const parsed = Number(teamId)
  if (teamId && !Number.isNaN(parsed) && parsed > 0) return parsed
  let hash = 0
  for (let i = 0; i < teamId.length; i += 1) {
    hash = (hash * 31 + teamId.charCodeAt(i)) | 0
  }
  return Math.abs(hash) || 1
}

export function buildTeamLookup(teams: Team[]): (id: number | string) => Team | undefined {
  const map = new Map<number | string, Team>()
  for (const team of teams) {
    map.set(team.id, team)
    map.set(team.teamId, team)
    map.set(String(team.id), team)
    map.set(String(team.teamId), team)
    const numeric = Number(team.teamId)
    if (!Number.isNaN(numeric)) map.set(numeric, team)
  }
  return (id: number | string) =>
    map.get(id) ?? map.get(String(id)) ?? map.get(Number(id))
}
