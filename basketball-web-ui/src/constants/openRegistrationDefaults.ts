import type { OpenApplication, OpenTournament } from '@/types/account'

export const CLOSED_OPEN_TOURNAMENT: OpenTournament = {
  id: '',
  name: '暂无开放报名',
  shortName: '',
  seasonName: '',
  type: 'OPEN',
  platform: '',
  registrationType: 'INDIVIDUAL',
  registrationTypeText: '个人报名',
  registrationDeadline: '',
  quotaTotal: 0,
  quotaUsed: 0,
  quotaLeft: 0,
  status: 'REGISTRATION_CLOSED',
  statusText: '暂无开放报名',
}

export function createEmptyOpenApplication(userId: number, tournament?: Pick<OpenTournament, 'id' | 'name'>): OpenApplication {
  return {
    applicationNo: '',
    tournamentId: tournament?.id || '',
    tournamentName: tournament?.name || '',
    applicationType: 'INDIVIDUAL',
    applicationTypeText: '个人报名',
    status: 'NOT_SUBMITTED',
    statusText: '未提交',
    playerNo: `CNPA-P-${String(userId).padStart(6, '0')}`,
    nickname: '',
    platform: '',
    gameId: '',
    submitTime: '',
    reviewTime: '',
    reviewRemark: '',
  }
}
