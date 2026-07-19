export type CompetitionRegion = 'domestic' | 'international'

export type CompetitionDefaultTab = 'overview' | 'schedule' | 'stage'

export interface CompetitionOption {
  id: string
  code: string
  name: string
  shortName: string
  region: CompetitionRegion
  logo: string
  enabled: boolean
  defaultTab: CompetitionDefaultTab
}

export interface CompetitionGroup {
  key: CompetitionRegion
  title: string
  items: CompetitionOption[]
}
