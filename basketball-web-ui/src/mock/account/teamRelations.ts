import type { TeamRelationsData, TeamInvitation } from '@/types/account'

export const TEAM_RELATIONS_STORAGE_KEY_PREFIX = 'cnpa_mock_team_relations_'

export const defaultTeamRelationsByUserId: Record<number, TeamRelationsData> = {
  10001: {
    userId: 10001,
    lineupRelations: [],
    invitations: [],
    managedTeamName: '',
    hasCaptainPermission: false,
    createRequestStatus: 'NONE',
    createRequestSubmittedAt: '',
    createFeatureEnabled: true,
  },
  10002: {
    userId: 10002,
    lineupRelations: [
      {
        id: 'lr-10002-1',
        userId: 10002,
        tournamentId: '2026-spring',
        tournamentName: '2026 CNPA 春季赛',
        teamId: 'team-rng',
        teamName: 'Royal Never Give Up',
        roleName: '队长 / SF',
        status: 'ACTIVE',
      },
    ],
    invitations: [],
    managedTeamName: 'Royal Never Give Up',
    hasCaptainPermission: true,
    createRequestStatus: 'APPROVED',
    createRequestSubmittedAt: '2025-08-18T10:00:00+08:00',
    createFeatureEnabled: true,
  },
  10005: {
    userId: 10005,
    lineupRelations: [
      {
        id: 'lr-10005-1',
        userId: 10005,
        tournamentId: '2026-spring',
        tournamentName: '2026 CNPA 春季赛',
        teamId: 'team-edg',
        teamName: 'EDward Gaming',
        roleName: 'SG',
        status: 'ACTIVE',
      },
    ],
    invitations: [
      {
        id: 'inv-10005-1',
        userId: 10005,
        tournamentId: '2026-summer',
        tournamentName: '2026 CNPA 夏季赛',
        teamId: 'team-fpx',
        teamName: 'FunPlus Phoenix',
        inviteRole: 'PF',
        status: 'PENDING',
        createdAt: '2026-01-20T10:00:00+08:00',
        expiresAt: '2026-02-05T23:59:59+08:00',
      },
    ],
    managedTeamName: '',
    hasCaptainPermission: false,
    createRequestStatus: 'NONE',
    createRequestSubmittedAt: '',
    createFeatureEnabled: true,
  },
}

export function teamRelationsStorageKey(userId: number): string {
  return `${TEAM_RELATIONS_STORAGE_KEY_PREFIX}${userId}`
}

export function getDefaultTeamRelations(userId: number): TeamRelationsData {
  if (defaultTeamRelationsByUserId[userId]) {
    return JSON.parse(JSON.stringify(defaultTeamRelationsByUserId[userId]))
  }
  return {
    userId,
    lineupRelations: [],
    invitations: [],
    managedTeamName: '',
    hasCaptainPermission: false,
    createRequestStatus: 'NONE',
    createRequestSubmittedAt: '',
    createFeatureEnabled: true,
  }
}

export function getPendingInvitations(data: TeamRelationsData): TeamInvitation[] {
  return data.invitations.filter((item) => item.status === 'PENDING')
}

export function countPendingInvitations(data: TeamRelationsData): number {
  return getPendingInvitations(data).length
}
