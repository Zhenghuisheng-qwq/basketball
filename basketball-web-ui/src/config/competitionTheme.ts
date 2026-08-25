import tournamentTop from '@/assets/icon/tournament_top.png'

export type CompetitionSeriesCode = 'LEAGUE' | 'ARENA' | 'CUP' | 'OPEN'

export interface CompetitionTheme {
  code: CompetitionSeriesCode
  fallbackImage: string
  overlay: string
  accent: string
  backgroundPosition: string
}

export const competitionThemes: Record<CompetitionSeriesCode, CompetitionTheme> = {
  LEAGUE: {
    code: 'LEAGUE',
    fallbackImage: tournamentTop,
    overlay: 'linear-gradient(90deg, rgba(4, 8, 14, 0.72), rgba(18, 21, 26, 0.18) 58%, rgba(138, 18, 18, 0.34))',
    accent: '#ff4b35',
    backgroundPosition: 'center',
  },
  ARENA: {
    code: 'ARENA',
    fallbackImage: tournamentTop,
    overlay: 'linear-gradient(90deg, rgba(7, 13, 20, 0.74), rgba(11, 49, 70, 0.18) 58%, rgba(20, 116, 154, 0.4))',
    accent: '#58c7ee',
    backgroundPosition: 'center 42%',
  },
  CUP: {
    code: 'CUP',
    fallbackImage: tournamentTop,
    overlay: 'linear-gradient(90deg, rgba(15, 10, 4, 0.76), rgba(57, 36, 13, 0.18) 58%, rgba(191, 132, 38, 0.42))',
    accent: '#e0ae4e',
    backgroundPosition: 'center 56%',
  },
  OPEN: {
    code: 'OPEN',
    fallbackImage: tournamentTop,
    overlay: 'linear-gradient(90deg, rgba(10, 7, 18, 0.74), rgba(47, 22, 67, 0.18) 58%, rgba(117, 57, 161, 0.38))',
    accent: '#c58cf0',
    backgroundPosition: 'center 48%',
  },
}

export function resolveCompetitionTheme(seriesCode?: string | null): CompetitionTheme {
  const normalized = String(seriesCode || '').trim().toUpperCase() as CompetitionSeriesCode
  return competitionThemes[normalized] || competitionThemes.LEAGUE
}
