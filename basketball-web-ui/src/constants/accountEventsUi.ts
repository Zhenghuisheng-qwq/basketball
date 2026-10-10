import type { AccountEventTab, MyEventsTabKey } from '@/types/accountEvents'
import type { MyEventsRoleMode } from '@/api/types/myEvents'
import { translate } from '@/i18n'

function buildAccountEventTabs(): AccountEventTab[] {
  return [
    { key: 'available', label: translate('accountEvents.tabs.available'), title: translate('accountEvents.tabs.available'), subtitle: 'AVAILABLE EVENTS' },
    { key: 'applications', label: translate('accountEvents.tabs.applications'), title: translate('accountEvents.tabs.applications'), subtitle: 'MY APPLICATIONS' },
    { key: 'joined', label: translate('accountEvents.tabs.joined'), title: translate('accountEvents.tabs.joined'), subtitle: 'PARTICIPATING EVENTS' },
    { key: 'schedule', label: translate('accountEvents.tabs.schedule'), title: translate('accountEvents.tabs.scheduleTitle'), subtitle: 'MY SCHEDULE' },
  ]
}

export function accountEventTabsAll(): AccountEventTab[] {
  return buildAccountEventTabs()
}

export function accountEventTabsCaptain(): AccountEventTab[] {
  return buildAccountEventTabs()
}

export function getTabsForRole(roleMode?: MyEventsRoleMode): AccountEventTab[] {
  const tabs = buildAccountEventTabs()
  if (roleMode === 'TEAM_CAPTAIN') {
    return tabs
  }
  if (roleMode === 'NO_TEAM') {
    return tabs.filter((tab) => tab.key === 'available')
  }
  return tabs
}

export function getTabMeta(tab: string, roleMode?: MyEventsRoleMode) {
  const tabs = getTabsForRole(roleMode)
  return tabs.find((item) => item.key === tab) || tabs[0]
}

export function getBannerCopy(tab: string, roleMode?: MyEventsRoleMode): { title: string; description: string } {
  if (roleMode === 'NO_TEAM') {
    return {
      title: translate('accountEvents.banner.openTitle'),
      description: translate('accountEvents.banner.noTeamDesc'),
    }
  }
  if (roleMode === 'TEAM_MEMBER') {
    if (tab === 'applications') {
      return {
        title: translate('accountEvents.banner.memberApplicationsTitle'),
        description: translate('accountEvents.banner.memberApplicationsDesc'),
      }
    }
    if (tab === 'joined') {
      return {
        title: translate('accountEvents.banner.memberJoinedTitle'),
        description: translate('accountEvents.banner.memberJoinedDesc'),
      }
    }
    if (tab === 'schedule') {
      return {
        title: translate('accountEvents.banner.memberScheduleTitle'),
        description: translate('accountEvents.banner.memberScheduleDesc'),
      }
    }
    return {
      title: translate('accountEvents.banner.openTitle'),
      description: translate('accountEvents.banner.memberDefaultDesc'),
    }
  }
  if (roleMode === 'TEAM_CAPTAIN') {
    if (tab === 'applications') {
      return {
        title: translate('accountEvents.banner.captainApplicationsTitle'),
        description: translate('accountEvents.banner.captainApplicationsDesc'),
      }
    }
    if (tab === 'joined') {
      return {
        title: translate('accountEvents.banner.captainJoinedTitle'),
        description: translate('accountEvents.banner.captainJoinedDesc'),
      }
    }
    if (tab === 'schedule') {
      return {
        title: translate('accountEvents.banner.captainScheduleTitle'),
        description: translate('accountEvents.banner.captainScheduleDesc'),
      }
    }
    return {
      title: translate('accountEvents.banner.openTitle'),
      description: translate('accountEvents.banner.captainDefaultDesc'),
    }
  }
  return getBannerCopyLegacy(tab)
}

function getBannerCopyLegacy(tab: string): { title: string; description: string } {
  if (tab === 'applications') {
    return {
      title: translate('accountEvents.banner.legacyApplicationsTitle'),
      description: translate('accountEvents.banner.legacyApplicationsDesc'),
    }
  }
  if (tab === 'joined') {
    return {
      title: translate('accountEvents.banner.legacyJoinedTitle'),
      description: translate('accountEvents.banner.legacyJoinedDesc'),
    }
  }
  return {
    title: translate('accountEvents.banner.legacyDefaultTitle'),
    description: translate('accountEvents.banner.legacyDefaultDesc'),
  }
}

export function getFooterNote(tab: string, roleMode?: MyEventsRoleMode): string {
  if (roleMode === 'NO_TEAM') {
    return translate('accountEvents.footer.noTeam')
  }
  if (roleMode === 'TEAM_MEMBER') {
    if (tab === 'applications') {
      return translate('accountEvents.footer.memberApplications')
    }
    return translate('accountEvents.footer.memberDefault')
  }
  if (roleMode === 'TEAM_CAPTAIN') {
    if (tab === 'applications') {
      return translate('accountEvents.footer.captainApplications')
    }
    if (tab === 'schedule') {
      return translate('accountEvents.footer.schedule')
    }
    return translate('accountEvents.footer.captainDefault')
  }
  if (tab === 'schedule') {
    return translate('accountEvents.footer.schedule')
  }
  return getFooterNoteLegacy(tab)
}

function getFooterNoteLegacy(tab: string): string {
  if (tab === 'applications') {
    return translate('accountEvents.footer.legacyApplications')
  }
  if (tab === 'joined') {
    return translate('accountEvents.footer.legacyJoined')
  }
  return translate('accountEvents.footer.legacyDefault')
}

export function getEmptyMessage(tab: MyEventsTabKey, roleMode?: MyEventsRoleMode): string {
  if (tab === 'available') {
    if (roleMode === 'NO_TEAM') {
      return translate('accountEvents.empty.noAvailable')
    }
    if (roleMode === 'TEAM_CAPTAIN') {
      return translate('accountEvents.empty.noRegisterable')
    }
    return translate('accountEvents.empty.noAvailable')
  }
  if (tab === 'applications') {
    if (roleMode === 'TEAM_MEMBER') {
      return translate('accountEvents.empty.memberNoApplications')
    }
    return translate('accountEvents.empty.noApplications')
  }
  if (tab === 'schedule') {
    return translate('accountEvents.empty.noSchedule')
  }
  return translate('accountEvents.empty.noJoined')
}

/** @deprecated 使用 getTabsForRole */
export function accountEventTabs(): AccountEventTab[] {
  return buildAccountEventTabs()
}
