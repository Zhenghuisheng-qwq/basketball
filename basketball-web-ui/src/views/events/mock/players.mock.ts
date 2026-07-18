import type { PlayerLeader } from '../types/events'

/** 球员半身像占位：后续替换为 @/assets/images/players/*.png */
const playerPortrait = (name: string) => `/images/players/${name.toLowerCase()}.png`

export const seasonLeaders: PlayerLeader[] = [
  {
    id: 1,
    title: '得分王',
    value: 28.6,
    unit: 'PPG',
    playerName: 'Knight',
    teamId: 1,
    portrait: playerPortrait('knight'),
  },
  {
    id: 2,
    title: '助攻王',
    value: 10.4,
    unit: 'APG',
    playerName: 'Elk',
    teamId: 2,
    portrait: playerPortrait('elk'),
  },
  {
    id: 3,
    title: '篮板王',
    value: 13.2,
    unit: 'RPG',
    playerName: 'PHX_Fire',
    teamId: 17,
    portrait: playerPortrait('phx_fire'),
  },
]
