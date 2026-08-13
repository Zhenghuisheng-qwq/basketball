<script setup lang="ts">
import { computed, ref } from 'vue'
import TeamBadge from '@/components/home/TeamBadge.vue'
import type { PlayoffSeries } from '../types/events'
import { toEventTeam } from '../utils/mapPublic'

const props = withDefaults(
  defineProps<{
    series: PlayoffSeries
    variant?: 'default' | 'semifinal' | 'final'
  }>(),
  {
    variant: 'default',
  },
)

const failedLogoIds = ref<Set<string>>(new Set())

const teamRows = computed(() => [
  {
    teamId: props.series.topTeamId,
    name: props.series.topTeamName,
    shortName: (props.series.topTeamName || '待定').slice(0, 3).toUpperCase() || '--',
    logo: props.series.topTeamLogo,
    badgeKey: undefined as string | undefined,
    score: props.series.topScore,
    seed: props.series.topSeed ?? '-',
    hasTeamLink: isPublicTeamId(props.series.topTeamId),
    isWinner: Boolean(props.series.winnerTeamId) && props.series.winnerTeamId === props.series.topTeamId,
    team: isPublicTeamId(props.series.topTeamId)
      ? toEventTeam({
          teamId: props.series.topTeamId,
          teamName: props.series.topTeamName,
          teamLogoUrl: props.series.topTeamLogo,
        })
      : null,
  },
  {
    teamId: props.series.bottomTeamId,
    name: props.series.bottomTeamName,
    shortName: (props.series.bottomTeamName || '待定').slice(0, 3).toUpperCase() || '--',
    logo: props.series.bottomTeamLogo,
    badgeKey: undefined as string | undefined,
    score: props.series.bottomScore,
    seed: props.series.bottomSeed ?? '-',
    hasTeamLink: isPublicTeamId(props.series.bottomTeamId),
    isWinner: Boolean(props.series.winnerTeamId) && props.series.winnerTeamId === props.series.bottomTeamId,
    team: isPublicTeamId(props.series.bottomTeamId)
      ? toEventTeam({
          teamId: props.series.bottomTeamId,
          teamName: props.series.bottomTeamName,
          teamLogoUrl: props.series.bottomTeamLogo,
        })
      : null,
  },
])

function isPublicTeamId(id?: string | number | null) {
  if (id == null || id === '') return false
  const numeric = Number(id)
  return Number.isFinite(numeric) && numeric > 0
}

function handleLogoError(teamId: string) {
  failedLogoIds.value = new Set(failedLogoIds.value).add(teamId)
}

function showLogoImage(teamId: string, logo?: string) {
  return Boolean(logo) && !failedLogoIds.value.has(teamId)
}
</script>
<template>
  <article
    class="playoff-series-card"
    :class="{
      'playoff-series-card--semifinal': variant === 'semifinal',
      'playoff-series-card--final': variant === 'final',
    }"
  >
    <div v-if="variant === 'final'" class="playoff-series-card__final-title">总决赛</div>

    <div
      v-for="(teamRow, index) in teamRows"
      :key="`${index}-${teamRow.teamId || teamRow.name}`"
      class="playoff-series-card__team"
      :class="{
        'playoff-series-card__team--winner': teamRow.isWinner,
        'playoff-series-card__team--loser': !teamRow.isWinner,
        'playoff-series-card__team--bordered': index > 0,
      }"
    >
      <span class="playoff-series-card__seed">{{ teamRow.seed }}</span>

      <router-link
        v-if="teamRow.hasTeamLink && teamRow.team"
        :to="`/tournament/team/${teamRow.team.teamId}/overview`"
        class="playoff-series-card__team-link"
      >
        <div class="playoff-series-card__logo-wrap">
          <img
            v-if="showLogoImage(teamRow.teamId, teamRow.team.logo)"
            class="playoff-series-card__logo"
            :src="teamRow.team.logo"
            :alt="`${teamRow.team.name} Logo`"
            @error="handleLogoError(teamRow.teamId)"
          />
          <div
            v-else-if="teamRow.team.badgeKey"
            class="playoff-series-card__badge-scale"
          >
            <TeamBadge :type="teamRow.team.badgeKey" />
          </div>
          <span v-else class="playoff-series-card__logo-fallback">
            {{ teamRow.team.shortName.slice(0, 2) }}
          </span>
        </div>

        <span class="playoff-series-card__name">
          {{ teamRow.team.shortName }}
        </span>
      </router-link>
      <div v-else class="playoff-series-card__team-link playoff-series-card__team-link--static">
        <div class="playoff-series-card__logo-wrap">
          <span class="playoff-series-card__logo-fallback">TBD</span>
        </div>
        <span class="playoff-series-card__name">{{ teamRow.name || '待定' }}</span>
      </div>

      <strong class="playoff-series-card__score">{{ teamRow.score }}</strong>
    </div>

    <div v-if="variant === 'final' && series.winnerTeamId" class="playoff-series-card__champion">
      <span aria-hidden="true" />
      <strong>
        总冠军
        {{
          series.winnerTeamId === series.topTeamId
            ? series.topTeamName
            : series.winnerTeamId === series.bottomTeamId
              ? series.bottomTeamName
              : ''
        }}
      </strong>
      <span aria-hidden="true" />
    </div>
  </article>
</template>
<style scoped lang="scss">
.playoff-series-card {
  width: 100%;
  min-height: 76px;
  padding: 0;
  color: #111111;
  background: linear-gradient(145deg, rgba(246, 245, 242, 0.98), rgba(232, 231, 227, 0.98));
  border: 1px solid rgba(10, 10, 10, 0.15);
  border-radius: 5px;
  box-sizing: border-box;
  transition: transform 0.2s ease, border-color 0.2s ease;

  &:hover {
    transform: translateY(-1px);
    border-color: rgba(225, 6, 19, 0.4);
  }
}

.playoff-series-card--semifinal {
  min-height: 100px;
}

.playoff-series-card--final {
  min-height: 146px;
  padding-bottom: 8px;
  border-color: rgba(10, 10, 10, 0.2);
  background: linear-gradient(145deg, #f4f3ef, #e6e5e1);
}

.playoff-series-card__final-title {
  padding: 10px 12px 0;
  color: #111111;
  font-size: 14px;
  font-weight: 800;
  text-align: center;
}

.playoff-series-card__team {
  display: grid;
  grid-template-columns: 24px 36px minmax(0, 1fr) 28px;
  align-items: center;
  column-gap: 9px;
  min-height: 56px;
  padding: 8px 14px;
  box-sizing: border-box;
}

.playoff-series-card__team--bordered {
  border-top: 1px solid rgba(10, 10, 10, 0.1);
}

.playoff-series-card__team-link {
  grid-column: 2 / 4;
  display: grid;
  grid-template-columns: 36px minmax(0, 1fr);
  align-items: center;
  column-gap: 9px;
  min-width: 0;
  color: inherit;
  text-decoration: none;
  cursor: pointer;
  transition: opacity 0.2s ease;

  &:hover {
    opacity: 0.82;
  }
}

.playoff-series-card__team-link--static {
  cursor: default;

  &:hover {
    opacity: 1;
  }
}

.playoff-series-card__seed {
  color: #202020;
  font-size: 13px;
  font-weight: 700;
  text-align: center;
  line-height: 1.2;
}

.playoff-series-card__logo-wrap {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  min-width: 34px;
  overflow: visible;
}

.playoff-series-card__logo {
  display: block;
  width: 30px;
  height: 30px;
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
  object-position: center;
}

.playoff-series-card__badge-scale {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  overflow: visible;

  :deep(.badge) {
    transform: scale(0.46875);
    transform-origin: center;
  }
}

.playoff-series-card--final .playoff-series-card__logo-wrap {
  width: 38px;
  height: 38px;
  min-width: 38px;
}

.playoff-series-card--final .playoff-series-card__logo {
  width: 34px;
  height: 34px;
}

.playoff-series-card--final .playoff-series-card__badge-scale {
  width: 34px;
  height: 34px;

  :deep(.badge) {
    transform: scale(0.53125);
  }
}

.playoff-series-card__logo-fallback {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  color: rgba(17, 17, 17, 0.7);
  font-size: 10px;
  font-weight: 800;
  border: 1px solid rgba(17, 17, 17, 0.14);
  box-sizing: border-box;
}

.playoff-series-card__name {
  overflow: hidden;
  min-width: 0;
  color: #444444;
  font-size: clamp(13px, 0.9vw, 16px);
  font-weight: 700;
  line-height: 1.2;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.playoff-series-card__score {
  text-align: right;
  font-size: 14px;
  font-weight: 800;
  line-height: 1.2;
}

.playoff-series-card__team--winner {
  .playoff-series-card__name,
  .playoff-series-card__score {
    color: #e10613;
  }

  .playoff-series-card__logo,
  :deep(.badge) {
    opacity: 1;
  }
}

.playoff-series-card__team--loser {
  .playoff-series-card__name {
    color: rgba(17, 17, 17, 0.72);
  }

  .playoff-series-card__logo,
  :deep(.badge) {
    opacity: 0.72;
  }
}

.playoff-series-card__champion {
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  align-items: center;
  gap: 10px;
  margin-top: 10px;
  padding: 10px 14px 14px;

  strong {
    color: #e10613;
    font-size: 18px;
    font-weight: 900;
    letter-spacing: 0.08em;
  }

  span {
    height: 1px;
    background: rgba(225, 6, 19, 0.55);

    &:first-child {
      margin-left: 8px;
      transform: rotate(-8deg);
      transform-origin: right center;
    }

    &:last-child {
      margin-right: 8px;
      transform: rotate(8deg);
      transform-origin: left center;
    }
  }
}

@media (max-width: 1100px) {
  .playoff-series-card__logo-wrap {
    width: 32px;
    height: 32px;
    min-width: 32px;
  }

  .playoff-series-card__logo {
    width: 27px;
    height: 27px;
  }

  .playoff-series-card__badge-scale {
    width: 27px;
    height: 27px;

    :deep(.badge) {
      transform: scale(0.421875);
    }
  }

  .playoff-series-card__team {
    grid-template-columns: 22px 32px minmax(0, 1fr) 26px;
    padding-inline: 10px;
  }

  .playoff-series-card__team-link {
    grid-column: 2 / 4;
    grid-template-columns: 32px minmax(0, 1fr);
  }
}
</style>