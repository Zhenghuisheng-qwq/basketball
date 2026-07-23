import type { MyTournament } from '@/types/account'
import { MOCK_TOURNAMENT_ID } from './eventTeamMock'

export const MY_TOURNAMENTS_STORAGE_PREFIX = 'cnpa_mock_my_tournaments_'

export const mockMyTournaments: MyTournament[] = [
  {
    id: MOCK_TOURNAMENT_ID,
    name: '2026 CNPA League S1',
    type: '联赛',
    period: '2026.03 - 2026.08',
    status: 'NOT_JOINED',
    statusText: '未参加',
    teamName: '',
    role: '',
    eligibility: 'NEED_PLAYER_ID',
  },
  {
    id: '2026-arena-s1',
    name: '2026 CNPA Arena S1',
    type: '杯赛',
    period: '2026.04 - 2026.06',
    status: 'INVITED',
    statusText: '收到邀请',
    teamName: '示例球队 D',
    role: '普通球员',
    eligibility: 'OK',
    inviteTime: '2026-07-18 14:20',
    inviteExpires: '2026-07-25 23:59',
    invitationId: 'inv-arena-d',
  },
  {
    id: '2026-league',
    name: '2026 CNPA League',
    type: '联赛',
    period: '2026.01 - 2026.12',
    status: 'LIMITED',
    statusText: '资格受限',
    teamName: '',
    role: '',
    eligibility: 'LIMITED',
  },
]

function applyVerifiedCaptainDefaults(items: MyTournament[]): MyTournament[] {
  return items.map((item) => {
    if (item.id !== MOCK_TOURNAMENT_ID) return item
    return {
      ...item,
      eligibility: 'OK',
      status: 'JOINED',
      statusText: '已加入阵容',
      teamName: 'RNG',
      role: '赛事队长',
    }
  })
}

export function getDefaultMyTournaments(userId: number): MyTournament[] {
  const cloned = JSON.parse(JSON.stringify(mockMyTournaments)) as MyTournament[]
  if (userId === 10001 || userId === 10002 || userId === 10005) {
    return applyVerifiedCaptainDefaults(cloned)
  }
  return cloned
}

export function myTournamentsStorageKey(userId: number): string {
  return `${MY_TOURNAMENTS_STORAGE_PREFIX}${userId}`
}
