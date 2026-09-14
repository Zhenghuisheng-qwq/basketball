import tournamentTop from '@/assets/icon/tournament_top.png'
import tournamentFoot from '@/assets/icon/tournament_foot.png'
import tournamentArenaTop from '@/assets/icon/tournament_arena_top.png'
import tournamentArenaFoot from '@/assets/icon/tournament_arena_foot.png'
import tournamentCupTop from '@/assets/icon/tournament_cup_top.png'
import tournamentCupFoot from '@/assets/icon/tournament_cup_foot.png'
import tournamentOpenTop from '@/assets/icon/tournament_open_top.png'
import tournamentOpenFoot from '@/assets/icon/tournament_open_foot.png'

export type CompetitionSeriesCode = 'LEAGUE' | 'ARENA' | 'CUP' | 'OPEN'

export interface CompetitionTheme {
  code: CompetitionSeriesCode
  fallbackImage: string
  topImage: string
  footImage: string
  overlay: string
  accent: string
  backgroundPosition: string
}

const leagueOverlay =
  'linear-gradient(90deg, rgba(4, 8, 14, 0.42), rgba(18, 21, 26, 0.12) 58%, rgba(138, 18, 18, 0.22))'

export const competitionThemes: Record<CompetitionSeriesCode, CompetitionTheme> = {
  LEAGUE: {
    code: 'LEAGUE',
    fallbackImage: tournamentTop,
    topImage: tournamentTop,
    footImage: tournamentFoot,
    overlay: leagueOverlay,
    accent: '#ff4b35',
    backgroundPosition: 'left center',
  },
  ARENA: {
    code: 'ARENA',
    fallbackImage: tournamentArenaTop,
    topImage: tournamentArenaTop,
    footImage: tournamentArenaFoot,
    overlay: leagueOverlay,
    accent: '#ff4b35',
    backgroundPosition: 'center',
  },
  CUP: {
    code: 'CUP',
    fallbackImage: tournamentCupTop,
    topImage: tournamentCupTop,
    footImage: tournamentCupFoot,
    overlay: leagueOverlay,
    accent: '#ff4b35',
    backgroundPosition: 'center',
  },
  OPEN: {
    code: 'OPEN',
    fallbackImage: tournamentOpenTop,
    topImage: tournamentOpenTop,
    footImage: tournamentOpenFoot,
    overlay: leagueOverlay,
    accent: '#ff4b35',
    backgroundPosition: 'center',
  },
}

export function resolveCompetitionTheme(seriesCode?: string | null): CompetitionTheme {
  const normalized = String(seriesCode || '').trim().toUpperCase() as CompetitionSeriesCode
  return competitionThemes[normalized] || competitionThemes.LEAGUE
}

export function pickCompetitionSeriesCode(
  ...candidates: Array<string | null | undefined>
): CompetitionSeriesCode {
  for (const candidate of candidates) {
    const normalized = String(candidate || '').trim().toUpperCase()
    if (normalized === 'LEAGUE' || normalized === 'ARENA' || normalized === 'CUP' || normalized === 'OPEN') {
      return normalized
    }
  }
  return 'LEAGUE'
}
