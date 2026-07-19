import { teams } from '@/views/events/mock/teams.mock'

const badgeKeyMap = new Map<string, string>()

for (const team of teams) {
  if (team.badgeKey) {
    badgeKeyMap.set(team.teamId, team.badgeKey)
  }
}

export function getTeamBadgeKey(teamId: string): string | undefined {
  const normalized = teamId.toLowerCase().replace(/_/g, '-')
  return badgeKeyMap.get(normalized)
}
