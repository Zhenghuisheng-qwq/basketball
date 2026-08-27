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
              'is-loser': Boolean(winnerEventTeamId) && !isWinner(leftTeam, winnerEventTeamId),
            }"
          >{{ formatExactScore(score.left) }}</span>
          <span class="match-scoreboard__colon">:</span>
          <span
            :class="{
              'is-loser': Boolean(winnerEventTeamId) && !isWinner(rightTeam, winnerEventTeamId),
            }"
          >{{ formatExactScore(score.right) }}</span>
        </template>
        <span v-else class="match-scoreboard__vs">VS</span>
      </div>
      <p v-if="caption" class="match-scoreboard__caption">{{ caption }}</p>
      <p class="match-scoreboard__status">{{ statusText }}</p>
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
  gap: 14px;
  color: inherit;
  text-decoration: none;
  min-width: 0;
}

.match-scoreboard__name {
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 22px;
  font-weight: 800;
  letter-spacing: 0.2px;
}

.match-scoreboard__advance {
  display: inline-flex;
  align-items: center;
  min-height: 20px;
  padding: 0 8px;
  border: 1px solid var(--cnpa-red, #e51422);
  color: var(--cnpa-red, #e51422);
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.08em;
  line-height: 1;
}

.match-scoreboard__name.is-loser,
.match-scoreboard__score .is-loser {
  color: rgba(255, 255, 255, 0.46);
}

.match-scoreboard__center {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  min-width: 220px;
}

.match-scoreboard__score {
  display: flex;
  align-items: baseline;
  justify-content: center;
  gap: 16px;
  font-size: clamp(52px, 6vw, 72px);
  line-height: 1;
  font-weight: 900;
  letter-spacing: 1px;
  white-space: nowrap;
}

.match-scoreboard__colon {
  color: rgba(255, 255, 255, 0.38);
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

.match-scoreboard__caption,
.match-scoreboard__status {
  color: #fff;
  font-size: 15px;
  font-weight: 700;
}

.match-scoreboard__meta {
  color: rgba(255, 255, 255, 0.62);
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
