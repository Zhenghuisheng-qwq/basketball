import type { Standing } from '../types/events'

export const standings: Standing[] = [
  { rank: 1, teamId: 1, wins: 12, losses: 2, winRate: 85.7, pointDiff: 112, recent: ['W', 'W', 'W', 'W', 'L'] },
  { rank: 2, teamId: 2, wins: 10, losses: 4, winRate: 71.4, pointDiff: 68, recent: ['W', 'W', 'L', 'W', 'W'] },
  { rank: 3, teamId: 3, wins: 9, losses: 5, winRate: 64.3, pointDiff: 34, recent: ['W', 'L', 'W', 'W', 'L'] },
  { rank: 4, teamId: 4, wins: 7, losses: 7, winRate: 50.0, pointDiff: 6, recent: ['L', 'W', 'W', 'L', 'W'] },
  { rank: 5, teamId: 5, wins: 6, losses: 8, winRate: 42.9, pointDiff: -18, recent: ['L', 'L', 'W', 'W', 'L'] },
  { rank: 6, teamId: 7, wins: 6, losses: 8, winRate: 42.9, pointDiff: -22, recent: ['W', 'L', 'L', 'W', 'L'] },
  { rank: 7, teamId: 8, wins: 6, losses: 8, winRate: 42.9, pointDiff: -28, recent: ['L', 'W', 'L', 'L', 'W'] },
  { rank: 8, teamId: 6, wins: 5, losses: 9, winRate: 35.7, pointDiff: -14, recent: ['L', 'W', 'L', 'L', 'L'] },
  { rank: 9, teamId: 9, wins: 5, losses: 9, winRate: 35.7, pointDiff: -36, recent: ['L', 'L', 'W', 'L', 'W'] },
  { rank: 10, teamId: 10, wins: 3, losses: 11, winRate: 21.4, pointDiff: -56, recent: ['L', 'L', 'L', 'W', 'L'] },
  { rank: 11, teamId: 11, wins: 3, losses: 11, winRate: 21.4, pointDiff: -64, recent: ['L', 'W', 'L', 'L', 'L'] },
  { rank: 12, teamId: 12, wins: 2, losses: 12, winRate: 14.3, pointDiff: -74, recent: ['L', 'L', 'L', 'L', 'W'] },
  { rank: 13, teamId: 13, wins: 2, losses: 12, winRate: 14.3, pointDiff: -82, recent: ['L', 'L', 'W', 'L', 'L'] },
  { rank: 14, teamId: 14, wins: 1, losses: 13, winRate: 7.1, pointDiff: -96, recent: ['L', 'L', 'L', 'L', 'L'] },
  { rank: 15, teamId: 15, wins: 1, losses: 13, winRate: 7.1, pointDiff: -102, recent: ['L', 'W', 'L', 'L', 'L'] },
  { rank: 16, teamId: 16, wins: 0, losses: 14, winRate: 0.0, pointDiff: -118, recent: ['L', 'L', 'L', 'L', 'L'] },
]

export const playoffCutoffRank = 8
