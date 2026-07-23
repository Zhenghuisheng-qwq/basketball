import type { TransferRecord } from '@/types/eventTeam'

export const mockTransfers: TransferRecord[] = [
  {
    transferId: 'tf-001',
    type: 'IN',
    typeText: '转入申请',
    playerId: 'player-wolves-03',
    playerName: 'WOLVES_03',
    avatarUrl: '',
    position: 'SF',
    platform: 'Xbox',
    gameId: 'WOLVES03_2K',
    fromTeamCode: 'WOLVES',
    toTeamCode: 'RNG',
    targetCaptainName: 'ZLZyoung',
    status: 'WAIT_ORIGINAL_CAPTAIN_CONFIRM',
    statusText: '等待原队长确认',
    applyTime: '2026-08-03 20:15',
    steps: [
      { key: 'target_captain_apply', text: '目标队长已发起', status: 'done', time: '2026-08-03 20:15' },
      { key: 'player_confirm', text: '球员已确认', status: 'done', time: '2026-08-03 21:02' },
      { key: 'original_captain_confirm', text: '原队长确认中', status: 'current', time: '' },
      { key: 'admin_review', text: '管理员审核', status: 'waiting', time: '' },
    ],
  },
  {
    transferId: 'tf-002',
    type: 'OUT',
    typeText: '转出申请',
    playerId: 'player-rng-04',
    playerName: 'RNG_04',
    avatarUrl: '',
    position: 'PF',
    platform: 'Xbox',
    gameId: 'RNG04_2K',
    fromTeamCode: 'RNG',
    toTeamCode: 'TITAN',
    targetCaptainName: 'TITAN_Captain',
    status: 'WAIT_PLAYER_CONFIRM',
    statusText: '等待球员确认',
    applyTime: '2026-08-04 10:20',
    steps: [
      { key: 'target_captain_apply', text: '目标队长已发起', status: 'done', time: '2026-08-04 10:20' },
      { key: 'player_confirm', text: '球员确认中', status: 'current', time: '' },
      { key: 'original_captain_confirm', text: '原队长待确认', status: 'waiting', time: '' },
      { key: 'admin_review', text: '管理员审核', status: 'waiting', time: '' },
    ],
  },
]

export const mockInitiateTransferPlayer = {
  playerId: 'player-wolves-03',
  playerName: 'WOLVES_03',
  avatarUrl: '',
  position: 'SF',
  platform: 'Xbox',
  gameId: 'WOLVES03_2K',
  fromTeamCode: 'WOLVES',
  fromTeamRosterCount: 6,
  fromTeamRosterMax: 8,
  toTeamCode: 'RNG',
}

export function getTransferById(transferId: string): TransferRecord | undefined {
  return mockTransfers.find((item) => item.transferId === transferId)
}
