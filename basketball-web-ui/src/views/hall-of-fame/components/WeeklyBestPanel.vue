<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import TeamLogo from '@/components/team/TeamLogo.vue'
import HallPanelHead from './HallPanelHead.vue'
import HallSelect from './HallSelect.vue'
import HallPlayerSilhouette from './HallPlayerSilhouette.vue'
import { goToPlayerDetail } from '@/utils/playerNavigate'
import {
  getLatestPublishedWeekNo,
  getPublishedWeeks,
  getTournamentById,
  getWeekByNo,
  getWeeklyBestRecord,
  getWeeklyTournaments,
} from '../mock/hallOfFame'

const props = defineProps<{
  tournamentId: string
  weekNo: number
}>()

const emit = defineEmits<{
  'update:tournamentId': [value: string]
  'update:weekNo': [value: number]
}>()

const router = useRouter()

const tournaments = computed(() => getWeeklyTournaments())
const tournamentOptions = computed(() =>
  tournaments.value.map((item) => ({ value: item.id, label: item.fullName })),
)
const currentTournament = computed(() => getTournamentById(props.tournamentId))
const weeks = computed(() => getPublishedWeeks(props.tournamentId))
const weekOptions = computed(() =>
  weeks.value.map((item) => ({
    value: item.weekNo,
    label: item.weekName,
    subLabel: `${item.startDate} - ${item.endDate}`,
  })),
)
const currentWeek = computed(() => getWeekByNo(props.tournamentId, props.weekNo))
const weeklyBest = computed(() => getWeeklyBestRecord(props.tournamentId, props.weekNo))

const canPrev = computed(() => props.weekNo > (weeks.value[0]?.weekNo ?? 1))
const canNext = computed(() => props.weekNo < (weeks.value[weeks.value.length - 1]?.weekNo ?? props.weekNo))

const teamModel = computed(() => {
  const record = weeklyBest.value
  if (!record) return null
  return {
    id: record.teamId,
    name: record.teamName,
    shortName: record.teamAbbr,
    logoUrl: record.teamLogoUrl,
  }
})

function updateTournament(value: string | number) {
  const tournamentId = String(value)
  emit('update:tournamentId', tournamentId)
  emit('update:weekNo', getLatestPublishedWeekNo(tournamentId))
}

function updateWeek(value: string | number) {
  emit('update:weekNo', Number(value))
}

function shiftWeek(delta: number) {
  const next = props.weekNo + delta
  if (weeks.value.some((item) => item.weekNo === next)) {
    emit('update:weekNo', next)
  }
}

function openPlayer() {
  if (!weeklyBest.value) return
  goToPlayerDetail(router, weeklyBest.value.playerId, 'overview')
}

function openTeam() {
  if (!weeklyBest.value) return
  router.push(`/tournament/team/${weeklyBest.value.teamId}/overview`)
}
</script>

<template>
  <section class="weekly-best-panel">
    <HallPanelHead title="周最佳" subtitle="Weekly Best">
      <template #filters>
        <HallSelect
          :model-value="tournamentId"
          :options="tournamentOptions"
          @update:model-value="updateTournament"
        />
        <HallSelect
          :model-value="weekNo"
          :options="weekOptions"
          trigger-variant="week"
          :disabled="!weekOptions.length"
          @update:model-value="updateWeek"
        />
      </template>
    </HallPanelHead>

    <div v-if="!weeklyBest" class="hall-empty">
      该比赛周的周最佳尚未发布
    </div>

    <article v-else class="weekly-best-card hall-card-surface">
      <div class="weekly-best-card__left">
        <div class="weekly-best-card__visual">
          <HallPlayerSilhouette
            :name="weeklyBest.playerName"
            :avatar-url="weeklyBest.playerAvatarUrl"
            size="feature"
          />
        </div>

        <div class="weekly-best-card__info">
          <p class="weekly-best-card__tag">PLAYER OF THE WEEK</p>
          <button type="button" class="weekly-best-card__name" @click="openPlayer">
            {{ weeklyBest.playerName }}
          </button>
          <button type="button" class="weekly-best-card__team" @click="openTeam">
            <span class="weekly-best-card__logo">
              <TeamLogo v-if="teamModel" :team="teamModel" size="sm" />
            </span>
            <span>{{ weeklyBest.teamAbbr || weeklyBest.teamName }}</span>
          </button>
          <p class="weekly-best-card__event">{{ currentTournament?.fullName }}</p>
          <p class="weekly-best-card__range">
            {{ currentWeek?.startDate }} - {{ currentWeek?.endDate }}
          </p>
        </div>
      </div>

      <div class="weekly-best-card__stats">
        <div><span>出场</span><strong>{{ weeklyBest.games }}</strong></div>
        <div><span>场均得分</span><strong>{{ weeklyBest.averagePoints.toFixed(1) }}</strong></div>
        <div><span>场均篮板</span><strong>{{ weeklyBest.averageRebounds.toFixed(1) }}</strong></div>
        <div><span>场均助攻</span><strong>{{ weeklyBest.averageAssists.toFixed(1) }}</strong></div>
        <div><span>场均抢断</span><strong>{{ weeklyBest.averageSteals.toFixed(1) }}</strong></div>
        <div><span>投篮命中率</span><strong class="is-pct">{{ weeklyBest.fieldGoalPercentage.toFixed(1) }}%</strong></div>
      </div>
    </article>

    <div v-if="weeks.length" class="weekly-best-nav">
      <button type="button" class="weekly-best-nav__btn" :disabled="!canPrev" @click="shiftWeek(-1)">
        上一周
      </button>
      <span class="weekly-best-nav__current">第 {{ weekNo }} 周</span>
      <button type="button" class="weekly-best-nav__btn" :disabled="!canNext" @click="shiftWeek(1)">
        下一周
      </button>
    </div>
  </section>
</template>

<style scoped lang="scss">
@use '../styles/hall-shared.scss';

.weekly-best-panel {
  width: 100%;
}

.weekly-best-card {
  display: grid;
  grid-template-columns: 36% 64%;
  min-height: 245px;
}

.weekly-best-card__left {
  display: grid;
  grid-template-columns: 42% 58%;
  gap: 16px;
  padding: 20px;
  border-right: 1px solid rgba(255, 255, 255, 0.08);
}

.weekly-best-card__visual {
  min-height: 205px;
}

.weekly-best-card__info {
  display: flex;
  flex-direction: column;
  justify-content: center;
  min-width: 0;
}

.weekly-best-card__tag {
  margin: 0 0 12px;
  color: var(--hall-red, #e71922);
  font-size: 12px;
  font-weight: 900;
  letter-spacing: 0.14em;
}

.weekly-best-card__name,
.weekly-best-card__team {
  margin: 0;
  padding: 0;
  border: 0;
  background: transparent;
  cursor: pointer;
  text-align: left;
  font: inherit;
}

.weekly-best-card__name {
  display: block;
  color: #fff;
  font-size: clamp(32px, 2.8vw, 38px);
  font-weight: 900;
  letter-spacing: 1px;
  line-height: 1.1;
}

.weekly-best-card__team {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  margin-top: 12px;
  color: var(--hall-gold, #d2a648);
  font-size: 15px;
  font-weight: 700;
}

.weekly-best-card__logo {
  width: 30px;
  height: 30px;
  display: grid;
  place-items: center;
  flex-shrink: 0;

  :deep(.team-logo) {
    width: 30px;
    height: 30px;
  }
}

.weekly-best-card__event,
.weekly-best-card__range {
  margin: 8px 0 0;
  color: rgba(255, 255, 255, 0.62);
  font-size: 13px;
}

.weekly-best-card__stats {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  align-items: stretch;

  div {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 16px 10px;
    border-left: 1px solid rgba(255, 255, 255, 0.1);
    text-align: center;
  }

  span {
    display: block;
    color: #b5b5b5;
    font-size: 13px;
    white-space: nowrap;
  }

  strong {
    display: block;
    margin-top: 10px;
    color: var(--hall-red, #e71922);
    font-size: clamp(28px, 2.2vw, 34px);
    font-weight: 900;
    line-height: 1;
    white-space: nowrap;
  }
}

.weekly-best-nav {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  height: 84px;
  margin-top: 4px;
}

.weekly-best-nav__btn,
.weekly-best-nav__current {
  width: 92px;
  height: 38px;
  display: grid;
  place-items: center;
  font-size: 14px;
  font-weight: 700;
}

.weekly-best-nav__btn {
  border: 1px solid rgba(0, 0, 0, 0.18);
  background: var(--hall-paper, #e9e7e7);
  color: #171717;
  cursor: pointer;

  &:disabled {
    opacity: 0.42;
    cursor: not-allowed;
  }
}

.weekly-best-nav__current {
  background: #111;
  color: #fff;
}

@media (max-width: 1199px) {
  .weekly-best-card {
    grid-template-columns: 1fr;
  }

  .weekly-best-card__left {
    border-right: 0;
    border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  }

  .weekly-best-card__stats {
    grid-template-columns: repeat(3, minmax(0, 1fr));

    div:nth-child(n + 4) {
      border-top: 1px solid rgba(255, 255, 255, 0.1);
    }

    div:nth-child(4) {
      border-left: 0;
    }
  }
}

@media (max-width: 767px) {
  .weekly-best-card__left {
    grid-template-columns: 1fr;
  }

  .weekly-best-card__stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));

    div:nth-child(n + 3) {
      border-top: 1px solid rgba(255, 255, 255, 0.1);
    }

    div:nth-child(3),
    div:nth-child(5) {
      border-left: 0;
    }
  }
}
</style>
