import type { FreeAgent, FreeAgentInvite } from '@/types/eventTeam'

export const mockFreeAgents: FreeAgent[] = [
  {
    playerId: 'free-001',
    playerNo: 'CNPA-P-000301',
    nickname: 'FREE_King',
    avatarUrl: '',
    position: 'SF',
    platform: 'Xbox',
    gameId: 'FREEKing2K',
    status: 'AVAILABLE',
    statusText: '本届自由球员',
  },
]

export const mockFreeAgentInvite: FreeAgentInvite = {
  inviteId: 'fa-invite-001',
  playerId: 'free-phx-fire',
  playerName: 'PHX_Fire',
  position: 'SG',
  status: 'WAIT_PLAYER_CONFIRM',
  statusText: '等待球员确认',
  steps: [
    { key: 'player_confirm', text: '球员确认', status: 'current' },
    { key: 'admin_review', text: '管理员审核', status: 'waiting' },
    { key: 'joined', text: '正式加入', status: 'waiting' },
  ],
}

export function searchFreeAgents(query: string): FreeAgent[] {
  const normalized = query.trim().toLowerCase()
  if (!normalized) return []
  return mockFreeAgents.filter(
    (item) =>
      item.playerNo.toLowerCase().includes(normalized)
      || item.gameId.toLowerCase().includes(normalized)
      || item.nickname.toLowerCase().includes(normalized),
  )
}
