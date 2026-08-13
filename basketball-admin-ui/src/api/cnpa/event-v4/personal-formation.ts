import request from '@/utils/request'
import type { AjaxResult } from '@/types'
import type { TeamRosterLockResultVo } from '@/api/cnpa/event-v4/team-roster'

export interface PersonalFormationPersonVo {
  personalRegistrationId?: number
  playerId?: number
  userId?: number
  playerName?: string
  preferredPosition?: string
  userCreateTime?: string
  drawTeamId?: number
  assignmentRole?: string
}

export interface PersonalFormationTeamCardVo {
  drawTeamId?: number
  temporaryTeamCode?: string
  temporaryTeamName?: string
  memberCount?: number
  captainPersonalRegistrationId?: number
  captainPlayerId?: number
  captainUserId?: number
  captainPlayerName?: string
  members?: PersonalFormationPersonVo[]
}

export interface PersonalFormationWorkspaceVo {
  divisionId?: number
  divisionName?: string
  seasonId?: number
  seasonTitle?: string
  seriesCode?: string
  seriesName?: string
  gameMode?: string
  registrationMode?: string
  ruleStatus?: string
  rosterStatus?: string
  formationStatus?: string
  currentBatchId?: number
  currentRosterVersionId?: number
  currentRosterVersionNo?: number
  minTeamSize?: number
  maxTeamSize?: number
  minimumValidPersonCount?: number
  validPersonCount?: number
  temporaryTeamCount?: number
  unassignedCount?: number
  canGenerate?: boolean
  canAdjust?: boolean
  canLock?: boolean
  unassignedList?: PersonalFormationPersonVo[]
  teams?: PersonalFormationTeamCardVo[]
}

export function getV4PersonalFormationWorkspace(divisionId: number): Promise<AjaxResult<PersonalFormationWorkspaceVo>> {
  return request({ url: `/basket/v4/admin/personal-formation/${divisionId}/workspace`, method: 'get' })
}

export function generateV4PersonalFormation(divisionId: number): Promise<AjaxResult> {
  return request({ url: `/basket/v4/admin/personal-formation/${divisionId}/generate`, method: 'post' })
}

export function regenerateV4PersonalFormation(divisionId: number): Promise<AjaxResult> {
  return request({ url: `/basket/v4/admin/personal-formation/${divisionId}/regenerate`, method: 'post' })
}

export function renameV4PersonalFormationTeam(divisionId: number, drawTeamId: number, temporaryTeamName: string): Promise<AjaxResult> {
  return request({ url: `/basket/v4/admin/personal-formation/${divisionId}/teams/${drawTeamId}/name`, method: 'put', data: { temporaryTeamName } })
}

export function moveV4PersonalFormationMember(divisionId: number, data: { personalRegistrationId: number; fromDrawTeamId?: number | null; toDrawTeamId?: number | null }): Promise<AjaxResult> {
  return request({ url: `/basket/v4/admin/personal-formation/${divisionId}/move`, method: 'post', data })
}

export function swapV4PersonalFormationMembers(divisionId: number, data: { leftPersonalRegistrationId: number; rightPersonalRegistrationId: number }): Promise<AjaxResult> {
  return request({ url: `/basket/v4/admin/personal-formation/${divisionId}/swap`, method: 'post', data })
}

export function lockV4PersonalFormation(divisionId: number): Promise<AjaxResult<TeamRosterLockResultVo>> {
  return request({ url: `/basket/v4/admin/personal-formation/${divisionId}/lock`, method: 'post' })
}
