import request from '@/utils/request'
import type { AjaxResult } from '@/types'

export interface TeamRosterItemVo {
  registrationId?: number
  teamId?: number
  teamName?: string
  submitTime?: string
  auditStatus?: string
  participationRole?: string
  candidateOrder?: number
}

export interface TeamRosterWorkspaceVo {
  divisionId?: number
  divisionName?: string
  registrationMode?: string
  ruleVersion?: {
    ruleVersionId?: number
    ruleVersionNo?: number
    ruleVersionStatus?: string
    locked?: boolean
  }
  rosterStatus?: string
  currentRosterVersionId?: number
  currentRosterVersionNo?: number
  formalCap?: number
  approvedCount?: number
  formalCount?: number
  candidateCount?: number
  excludedCount?: number
  unallocatedCount?: number
  canEdit?: boolean
  canLock?: boolean
  approvedList?: TeamRosterItemVo[]
  formalList?: TeamRosterItemVo[]
  candidateList?: TeamRosterItemVo[]
  excludedList?: TeamRosterItemVo[]
  unallocatedList?: TeamRosterItemVo[]
  rejectedList?: TeamRosterItemVo[]
}

export interface TeamRosterLockResultVo {
  rosterVersionId?: number
  versionNo?: number
  status?: string
  formalCount?: number
  candidateCount?: number
  excludedCount?: number
  idempotentReplay?: boolean
}

export function getV4TeamRosterWorkspace(divisionId: number): Promise<AjaxResult<TeamRosterWorkspaceVo>> {
  return request({ url: `/basket/v4/divisions/${divisionId}/roster/team`, method: 'get' })
}

export function assignV4TeamRoster(divisionId: number, data: { registrationId: number; participationRole: string }): Promise<AjaxResult> {
  return request({ url: `/basket/v4/divisions/${divisionId}/roster/team/allocation`, method: 'post', data })
}

export function autoAllocateV4TeamRoster(divisionId: number): Promise<AjaxResult> {
  return request({ url: `/basket/v4/divisions/${divisionId}/roster/team/auto-allocate`, method: 'post' })
}

export function moveV4TeamRosterCandidate(divisionId: number, data: { registrationId: number; direction: string }): Promise<AjaxResult> {
  return request({ url: `/basket/v4/divisions/${divisionId}/roster/team/candidate/order`, method: 'post', data })
}

export function lockV4TeamRoster(divisionId: number): Promise<AjaxResult<TeamRosterLockResultVo>> {
  return request({ url: `/basket/v4/divisions/${divisionId}/roster/team/lock`, method: 'post' })
}

export function promoteV4TeamRosterCandidate(divisionId: number, data: { formalRegistrationId: number; candidateRegistrationId: number; reason: string }): Promise<AjaxResult<TeamRosterLockResultVo>> {
  return request({ url: `/basket/v4/divisions/${divisionId}/roster/team/promote-candidate`, method: 'post', data })
}
