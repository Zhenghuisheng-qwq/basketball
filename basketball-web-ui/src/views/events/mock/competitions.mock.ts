import type { CompetitionOption } from '../types/competition'

export const DEFAULT_COMPETITION_ID = 'cnpa-1v1'

export const competitionOptions: CompetitionOption[] = [
  {
    id: 'cnpa-1v1',
    code: 'CNPA_1V1',
    name: '1v1',
    shortName: '1v1',
    region: 'domestic',
    logo: '/images/competitions/cnpa-1v1.png',
    enabled: true,
    defaultTab: 'overview',
  },
  {
    id: 'cnpa-2v2',
    code: 'CNPA_2V2',
    name: '2v2',
    shortName: '2v2',
    region: 'domestic',
    logo: '/images/competitions/cnpa-2v2.png',
    enabled: true,
    defaultTab: 'overview',
  },
  {
    id: 'cnpa-3v3',
    code: 'CNPA_3V3',
    name: '3v3',
    shortName: '3v3',
    region: 'domestic',
    logo: '/images/competitions/cnpa-3v3.png',
    enabled: true,
    defaultTab: 'overview',
  },
]

export const competitionMap = new Map(
  competitionOptions.map((item) => [item.id, item]),
)

export const defaultCompetition =
  competitionMap.get(DEFAULT_COMPETITION_ID) ?? competitionOptions[0]
