import type { TeamInvitation, TeamRelationsData } from '@/types/account'

export function getDefaultTeamRelations(userId: number): TeamRelationsData {
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
