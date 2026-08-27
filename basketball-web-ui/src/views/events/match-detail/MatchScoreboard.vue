<script setup lang="ts">
import type { Team } from '@/views/events/types/events'
import TeamLogo from '@/views/events/components/TeamLogo.vue'
import TeamEntryLink from '@/components/team/TeamEntryLink.vue'
import { formatExactScore, type ScorePair } from './matchDetailDisplay'

defineProps<{
  leftTeam: Team
  rightTeam: Team
  score: ScorePair
  winnerEventTeamId?: string | null
  statusText: string
  metaText?: string
  caption?: string
  advanceLabel?: string
}>()

function isWinner(team: Team, winnerEventTeamId?: string | null): boolean {
  const winner = String(winnerEventTeamId || '').trim()
  const id = String(team.teamId || '').trim()
  return Boolean(winner && id && winner === id)
}

function secondaryLine(team: Team): string {
  const name = String(team.name || '').trim()
  const short = String(team.shortName || team.code || '').trim()
  const parts: string[] = []
  if (short && short.toUpperCase() !== name.toUpperCase() && !name.toUpperCase().includes(short.toUpperCase())) parts.push(short)
  if ((team.wins || 0) + (team.losses || 0) > 0) {
    parts.push(`${team.wins}-${team.losses}`)
  }
  return parts.join('  ')
}
</script>

<template>
  <div class="match-scoreboard">
    <TeamEntryLink :team="leftTeam" class="match-scoreboard__team">
      <TeamLogo :team="leftTeam" size="hero" />
      <strong
        class="match-scoreboard__name"
        :class="{
          'is-winner': isWinner(leftTeam, winnerEventTeamId),
          'is-loser': Boolean(winnerEventTeamId) && !isWinner(leftTeam, winnerEventTeamId),
        }"
      >
        {{ leftTeam.name }}
      </strong>
      <span v-if="secondaryLine(leftTeam)" class="match-scoreboard__sub">{{ secondaryLine(leftTeam) }}</span>
      <span
        v-if="advanceLabel && isWinner(leftTeam, winnerEventTeamId)"
        class="match-scoreboard__advance"
      >{{ advanceLabel }}</span>
    </TeamEntryLink>

    <div class="match-scoreboard__center">
      <div class="match-scoreboard__score" aria-label="比分">
        <template v-if="score.kind === 'score'">
          <span
            :class="{
              'is-winner': isWinner(leftTeam, winnerEventTeamId),
              'is-loser': Boolean(winnerEventTeamId) && !isWinner(leftTeam, winnerEventTeamId),
            }"
          >{{ formatExactScore(score.left) }}</span>
          <span class="match-scoreboard__colon">:</span>
          <span
            :class="{
              'is-winner': isWinner(rightTeam, winnerEventTeamId),
              'is-loser': Boolean(winnerEventTeamId) && !isWinner(rightTeam, winnerEventTeamId),
            }"
          >{{ formatExactScore(score.right) }}</span>
        </template>
        <span v-else class="match-scoreboard__vs">VS</span>
      </div>
      <p v-if="caption" class="match-scoreboard__caption">{{ caption }}</p>
      <p v-if="statusText" class="match-scoreboard__status">{{ statusText }}</p>
      <p v-if="metaText" class="match-scoreboard__meta">{{ metaText }}</p>
    </div>

    <TeamEntryLink :team="rightTeam" class="match-scoreboard__team">
      <TeamLogo :team="rightTeam" size="hero" />
      <strong
        class="match-scoreboard__name"
        :class="{
          'is-winner': isWinner(rightTeam, winnerEventTeamId),
          'is-loser': Boolean(winnerEventTeamId) && !isWinner(rightTeam, winnerEventTeamId),
        }"
      >
        {{ rightTeam.name }}
      </strong>
      <span v-if="secondaryLine(rightTeam)" class="match-scoreboard__sub">{{ secondaryLine(rightTeam) }}</span>
      <span
        v-if="advanceLabel && isWinner(rightTeam, winnerEventTeamId)"
        class="match-scoreboard__advance"
      >{{ advanceLabel }}</span>
    </TeamEntryLink>
  </div>
</template>

<style scoped lang="scss">
.match-scoreboard {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto minmax(0, 1fr);
  align-items: center;
  gap: 28px;
  color: #fff;
}

.match-scoreboard__team {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  color: inherit;
  text-decoration: none;
  min-width: 0;
}

.match-scoreboard__name {
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: clamp(20px, 2.4vw, 34px);
  font-weight: 900;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.match-scoreboard__sub {
  color: rgba(255, 255, 255, 0.52);
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.match-scoreboard__advance {
  display: inline-flex;
  align-items: center;
  min-height: 22px;
  padding: 0 10px;
  border: 1px solid var(--cnpa-red, #e51422);
  border-radius: 999px;
  color: var(--cnpa-red, #e51422);
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.08em;
  line-height: 1;
}

.match-scoreboard__name.is-loser {
  color: rgba(255, 255, 255, 0.72);
}

.match-scoreboard__center {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  min-width: 220px;
}

.match-scoreboard__score {
  display: flex;
  align-items: baseline;
  justify-content: center;
  gap: 18px;
  font-size: clamp(56px, 7vw, 84px);
  line-height: 0.92;
  font-weight: 900;
  letter-spacing: 1px;
  white-space: nowrap;
}

.match-scoreboard__score .is-winner {
  color: var(--cnpa-red, #e51422);
}

.match-scoreboard__score .is-loser {
  color: #fff;
}

.match-scoreboard__colon {
  color: rgba(255, 255, 255, 0.42);
}

.match-scoreboard__vs {
  font-size: clamp(40px, 5vw, 56px);
  letter-spacing: 4px;
  color: rgba(255, 255, 255, 0.72);
}

.match-scoreboard__caption,
.match-scoreboard__status,
.match-scoreboard__meta {
  margin: 0;
  text-align: center;
}

.match-scoreboard__caption {
  color: #fff;
  font-size: 15px;
  font-weight: 700;
}

.match-scoreboard__status {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 28px;
  padding: 0 14px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 999px;
  background: rgba(0, 0, 0, 0.46);
  color: #fff;
  font-size: 13px;
  font-weight: 700;
}

.match-scoreboard__meta {
  color: rgba(255, 255, 255, 0.58);
  font-size: 13px;
  font-weight: 600;
}

@media (max-width: 1024px) {
  .match-scoreboard {
    gap: 16px;
  }

  .match-scoreboard__name {
    font-size: 16px;
  }

  .match-scoreboard__center {
    min-width: 160px;
  }
}

@media (max-width: 640px) {
  .match-scoreboard {
    gap: 10px;
  }

  .match-scoreboard__name {
    font-size: 13px;
  }

  .match-scoreboard__score {
    gap: 8px;
    font-size: 36px;
  }

  .match-scoreboard__center {
    min-width: 0;
  }
}
</style>
