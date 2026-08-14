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

export interface RosterVersionHistoryItemVo {
  rosterVersionId?: number
  versionNo?: number
  status?: string
  statusName?: string
  isCurrent?: number
  formalCount?: number
  candidateCount?: number
  excludedCount?: number
  createTime?: string
  lockedTime?: string
  lockedBy?: number
}

export function listV4TeamRosterVersions(divisionId: number): Promise<AjaxResult<RosterVersionHistoryItemVo[]>> {
  return request({ url: `/basket/v4/divisions/${divisionId}/roster/team/versions`, method: 'get' })
}

export function getV4TeamRosterVersion(divisionId: number, rosterVersionId: number): Promise<AjaxResult<TeamRosterVersionDetailVo>> {
  return request({ url: `/basket/v4/divisions/${divisionId}/roster/team/versions/${rosterVersionId}`, method: 'get' })
}

export interface TeamRosterVersionDetailVo {
  rosterVersionId?: number
  divisionId?: number
  versionNo?: number
  status?: string
  isCurrent?: number
  formalCount?: number
  candidateCount?: number
  excludedCount?: number
  lockedTime?: string
  lockedBy?: number
  formalList?: TeamRosterItemVo[]
  candidateList?: TeamRosterItemVo[]
  excludedList?: TeamRosterItemVo[]
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
