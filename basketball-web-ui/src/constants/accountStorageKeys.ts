export function teamRelationsStorageKey(userId: number): string {
  return `cnpa_account_team_relations_${userId}`
}

export function securityStorageKey(userId: number): string {
  return `cnpa_account_security_${userId}`
}

export function myTournamentsStorageKey(userId: number): string {
  return `cnpa_account_my_tournaments_${userId}`
}

export function openApplicationStorageKey(userId: number): string {
  return `cnpa_account_open_application_${userId}`
}
