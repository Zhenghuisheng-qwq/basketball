export interface RegistrationAuditLoads {
  loadTeamRegistration: boolean
  loadTeamRoster: boolean
  loadPersonalRegistration: boolean
  loadFormation: boolean
}

export function resolveRegistrationAuditLoads(registrationMode?: string | null): RegistrationAuditLoads {
  const mode = String(registrationMode || '').toUpperCase()
  const isTeam = mode === 'TEAM'
  const isPersonal = mode === 'PERSONAL'
  return {
    loadTeamRegistration: isTeam,
    loadTeamRoster: isTeam,
    loadPersonalRegistration: isPersonal,
    loadFormation: isPersonal
  }
}