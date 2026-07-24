export interface PrivacySettingDto {
  profileVisible?: number
  allowTeamInvite?: number
  showTeamHistory?: number
  showMatchData?: number
  showOnlineStatus?: number
}

export interface NotificationSettingDto {
  systemNotice?: number
  certificationNotice?: number
  tournamentNotice?: number
  teamNotice?: number
  announcementNotice?: number
  emailNotice?: number
  smsNotice?: number
}
