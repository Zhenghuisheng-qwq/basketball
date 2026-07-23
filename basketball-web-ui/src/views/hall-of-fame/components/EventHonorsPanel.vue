<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import TeamLogo from '@/components/team/TeamLogo.vue'
import HallPanelHead from './HallPanelHead.vue'
import HallSelect from './HallSelect.vue'
import HonorLineupCard from './HonorLineupCard.vue'
import ChampionRoster from './ChampionRoster.vue'
import HallPlayerSilhouette from './HallPlayerSilhouette.vue'
import { goToPlayerDetail } from '@/utils/playerNavigate'
import { getEventHonor, getEventTournaments, getTournamentById } from '../mock/hallOfFame'
import mrtMidBackground from '@/assets/icon/mrt/mrt_mid.png'

const championCardStyle = {
  backgroundImage: `linear-gradient(90deg, rgba(0,0,0,0.88) 0%, rgba(0,0,0,0.72) 42%, rgba(0,0,0,0.18) 68%, rgba(0,0,0,0.08) 100%), url(${mrtMidBackground})`,
}

const props = defineProps<{
  tournamentId: string
  rosterExpanded: boolean
}>()

const emit = defineEmits<{
  'update:tournamentId': [value: string]
  'update:rosterExpanded': [value: boolean]
}>()

const router = useRouter()

const tournamentOptions = computed(() =>
  getEventTournaments().map((item) => ({ value: item.id, label: item.fullName })),
)
const currentTournament = computed(() => getTournamentById(props.tournamentId))
const honor = computed(() => getEventHonor(props.tournamentId))

const championTeam = computed(() => {
  const record = honor.value
  if (!record) return null
  return {
    id: record.championTeamId,
    name: record.championTeamName,
    shortName: record.championTeamAbbr,
    logoUrl: record.championTeamLogoUrl,
  }
})

function updateTournament(value: string | number) {
  emit('update:tournamentId', String(value))
  emit('update:rosterExpanded', false)
}

function toggleRoster() {
  emit('update:rosterExpanded', !props.rosterExpanded)
}

function openTeam(teamId: string) {
  router.push(`/tournament/team/${teamId}/overview`)
}

function openMvp() {
  if (!honor.value?.mvp) return
  goToPlayerDetail(router, honor.value.mvp.playerId, 'overview')
}
</script>

<template>
  <section class="event-honors-panel">
    <HallPanelHead title="赛事荣誉" subtitle="EVENT HONORS" layout="inline">
      <template #filters>
        <HallSelect
          :model-value="tournamentId"
          :options="tournamentOptions"
          @update:model-value="updateTournament"
        />
      </template>
    </HallPanelHead>

    <div v-if="!honor || !honor.published" class="hall-empty">
      该赛事荣誉尚未发布
    </div>

    <template v-else>
      <article class="event-champion-card" :style="championCardStyle">
        <button
          type="button"
          class="event-champion-card__team"
          @click="openTeam(honor.championTeamId)"
        >
          <p class="event-champion-card__label">赛事冠军</p>
          <span class="event-champion-card__label-en">CHAMPION</span>
          <span class="event-champion-card__logo">
            <TeamLogo v-if="championTeam" :team="championTeam" size="xl" />
          </span>
          <strong>{{ honor.championTeamName }}</strong>
        </button>

        <div class="event-champion-card__info">
          <p class="event-champion-card__tournament">{{ currentTournament?.fullName }}</p>
          <p class="event-champion-card__desc">{{ honor.description }}</p>
          <div class="event-champion-card__date-block">
            <span>夺冠日期</span>
            <strong>{{ honor.championDate }}</strong>
          </div>
          <button type="button" class="event-champion-card__btn" @click="toggleRoster">
            {{ rosterExpanded ? '收起冠军阵容' : '查看冠军阵容' }}
            <span aria-hidden="true">›</span>
          </button>
        </div>

        <div class="event-champion-card__visual" aria-hidden="true" />
      </article>

      <ChampionRoster
        v-if="rosterExpanded && honor.championRoster.length"
        :members="honor.championRoster"
      />

      <article v-if="honor.mvp?.published" class="event-mvp-card">
        <div class="event-mvp-card__visual">
          <p class="event-mvp-card__label">
            <span class="event-mvp-card__label-cn">赛事</span>
            <span class="event-mvp-card__label-en">MVP</span>
          </p>
          <HallPlayerSilhouette
            :name="honor.mvp.playerName"
            :avatar-url="honor.mvp.playerAvatarUrl"
            size="mvp"
          />
        </div>

        <div class="event-mvp-card__identity">
          <button type="button" class="event-mvp-card__name" @click="openMvp">
            {{ honor.mvp.playerName }}
          </button>
          <button type="button" class="event-mvp-card__team" @click="openTeam(honor.mvp.teamId)">
            <span class="event-mvp-card__logo">
              <TeamLogo
                :team="{
                  id: honor.mvp.teamId,
                  name: honor.mvp.teamName,
                  shortName: honor.mvp.teamAbbr,
                  logoUrl: honor.mvp.teamLogoUrl,
                }"
                size="sm"
              />
            </span>
            <span>{{ honor.mvp.teamAbbr || honor.mvp.teamName }}</span>
          </button>
          <p class="event-mvp-card__event">{{ currentTournament?.fullName }}</p>
          <p class="event-mvp-card__note">赛事全程数据</p>
        </div>

        <div class="event-mvp-card__stats">
          <div class="event-mvp-stats__row event-mvp-stats__row--three">
            <div class="event-mvp-stat">
              <span class="event-mvp-stat__label">场均得分</span>
              <strong class="event-mvp-stat__value">{{ honor.mvp.averagePoints.toFixed(1) }}</strong>
            </div>
            <div class="event-mvp-stat">
              <span class="event-mvp-stat__label">场均篮板</span>
              <strong class="event-mvp-stat__value">{{ honor.mvp.averageRebounds.toFixed(1) }}</strong>
            </div>
            <div class="event-mvp-stat">
              <span class="event-mvp-stat__label">场均助攻</span>
              <strong class="event-mvp-stat__value">{{ honor.mvp.averageAssists.toFixed(1) }}</strong>
            </div>
          </div>
          <div class="event-mvp-stats__row event-mvp-stats__row--two">
            <div class="event-mvp-stat">
              <span class="event-mvp-stat__label">场均抢断</span>
              <strong class="event-mvp-stat__value">{{ honor.mvp.averageSteals.toFixed(1) }}</strong>
            </div>
            <div class="event-mvp-stat">
              <span class="event-mvp-stat__label">投篮命中率</span>
              <strong class="event-mvp-stat__value">{{ honor.mvp.fieldGoalPercentage.toFixed(1) }}%</strong>
            </div>
          </div>
        </div>
      </article>
      <div v-else class="hall-empty event-honors-panel__pending">赛事 MVP 尚未公布</div>

      <section class="hall-lineup-section">
        <div class="hall-section-head hall-lineup-section__header">
          <h3 class="hall-section-head__title">赛事最佳阵容</h3>
          <span class="hall-section-head__sub">ALL-TOURNAMENT TEAM</span>
        </div>
        <div v-if="honor.allTournamentPublished && honor.allTournamentTeam.length" class="hall-lineup-grid">
          <HonorLineupCard
            v-for="player in honor.allTournamentTeam"
            :key="`${player.playerId}-${player.position}`"
            :player="player"
          />
        </div>
        <div v-else class="hall-empty event-honors-panel__pending">赛事最佳阵容尚未公布</div>
      </section>

      <section class="hall-lineup-section">
        <div class="hall-section-head hall-lineup-section__header">
          <h3 class="hall-section-head__title">最佳防守阵容</h3>
          <span class="hall-section-head__sub">ALL-DEFENSIVE TEAM</span>
        </div>
        <div v-if="honor.allDefensivePublished && honor.allDefensiveTeam.length" class="hall-lineup-grid">
          <HonorLineupCard
            v-for="player in honor.allDefensiveTeam"
            :key="`${player.playerId}-${player.position}`"
            :player="player"
          />
        </div>
        <div v-else class="hall-empty event-honors-panel__pending">最佳防守阵容尚未公布</div>
      </section>
    </template>
  </section>
</template>

<style scoped lang="scss">
@use '../styles/hall-shared.scss';

.event-honors-panel {
  width: 100%;
  padding-bottom: 42px;
}

.event-honors-panel__pending {
  padding: 24px;
  margin-top: 20px;
}

.event-champion-card {
  position: relative;
  display: grid;
  grid-template-columns: 28% 30% 42%;
  min-height: 320px;
  overflow: hidden;
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  background-color: #050505;
  background-size: cover;
  background-position: center right;
  background-repeat: no-repeat;
}

.event-champion-card__team {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  justify-content: center;
  gap: 10px;
  margin: 0;
  padding: 28px 24px 28px 30px;
  border: 0;
  border-right: 1px solid rgba(255, 255, 255, 0.12);
  background: transparent;
  color: #fff;
  cursor: pointer;
  text-align: left;

  strong {
    margin-top: 4px;
    font-size: 36px;
    font-weight: 900;
    letter-spacing: 0.04em;
    line-height: 1.1;
  }
}

.event-champion-card__label {
  margin: 0;
  color: var(--hall-red, #e71922);
  font-size: 22px;
  font-weight: 900;
}

.event-champion-card__label-en {
  display: block;
  margin-top: -2px;
  color: #fff;
  font-size: 18px;
  font-style: italic;
  font-weight: 800;
  letter-spacing: 0.08em;
}

.event-champion-card__logo {
  display: grid;
  place-items: center;
  width: 120px;
  height: 120px;
  margin-top: 8px;

  :deep(.team-logo--xl) {
    width: 120px;
    height: 120px;
  }
}

.event-champion-card__info {
  display: flex;
  flex-direction: column;
  justify-content: center;
  min-width: 0;
  padding: 28px 24px;
}

.event-champion-card__tournament {
  margin: 0;
  color: #fff;
  font-size: clamp(22px, 2vw, 28px);
  font-weight: 800;
  font-style: italic;
  line-height: 1.2;
}

.event-champion-card__desc {
  margin: 10px 0 0;
  color: rgba(255, 255, 255, 0.72);
  font-size: 14px;
  line-height: 1.5;
}

.event-champion-card__date-block {
  margin-top: 22px;

  span {
    display: block;
    color: rgba(255, 255, 255, 0.52);
    font-size: 13px;
  }

  strong {
    display: block;
    margin-top: 6px;
    color: #fff;
    font-size: 20px;
    font-weight: 800;
  }
}

.event-champion-card__btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-top: 22px;
  align-self: flex-start;
  min-width: 148px;
  height: 40px;
  padding: 0 16px;
  border: 1px solid rgba(255, 255, 255, 0.78);
  background: rgba(0, 0, 0, 0.35);
  color: #fff;
  font: inherit;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  transition: background 0.18s ease, border-color 0.18s ease;

  span {
    transition: transform 0.18s ease;
  }

  &:hover {
    background: rgba(255, 255, 255, 0.08);
    border-color: #fff;

    span {
      transform: translateX(3px);
    }
  }
}

.event-champion-card__visual {
  min-height: 100%;
}

.event-mvp-card {
  position: relative;
  display: grid;
  grid-template-columns: 38% 24% 38%;
  min-height: 260px;
  max-height: 290px;
  margin-top: 20px;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 4px;
  background:
    linear-gradient(90deg, #0b0b0b 0%, #080808 42%, #0b0b0b 100%);
}

.event-mvp-card__visual {
  position: relative;
  min-width: 0;
  min-height: 260px;
  overflow: hidden;
  background:
    radial-gradient(circle at 58% 36%, rgba(255, 255, 255, 0.14), transparent 28%),
    linear-gradient(90deg, #090909 0%, #101010 70%, #080808 100%);

  :deep(.hall-player-visual) {
    position: absolute;
    inset: 0;
  }
}

.event-mvp-card__visual::after {
  content: '';
  position: absolute;
  top: 0;
  right: -1px;
  bottom: 0;
  width: 34%;
  background: linear-gradient(90deg, transparent, rgba(8, 8, 8, 0.9));
  pointer-events: none;
  z-index: 2;
}

.event-mvp-card__label {
  position: absolute;
  top: 22px;
  left: 24px;
  z-index: 3;
  margin: 0;
  font-size: clamp(24px, 2.2vw, 28px);
  font-weight: 900;
  font-style: italic;
  line-height: 1;
  letter-spacing: 0.04em;
}

.event-mvp-card__label-cn {
  color: var(--hall-red, #e71922);
}

.event-mvp-card__label-en {
  margin-left: 4px;
  color: #fff;
}

.event-mvp-card__identity {
  display: flex;
  flex-direction: column;
  justify-content: center;
  min-width: 0;
  padding: 26px 22px;
  border-left: 1px solid rgba(255, 255, 255, 0.08);
  border-right: 1px solid rgba(255, 255, 255, 0.08);
}

.event-mvp-card__name,
.event-mvp-card__team {
  margin: 0;
  padding: 0;
  border: 0;
  background: transparent;
  cursor: pointer;
  text-align: left;
  font: inherit;
}

.event-mvp-card__name {
  display: block;
  color: #fff;
  font-size: clamp(36px, 3.2vw, 48px);
  font-weight: 900;
  letter-spacing: 1px;
  line-height: 1;
  transition: color 0.18s ease;

  &:hover {
    color: var(--hall-red, #e71922);
  }
}

.event-mvp-card__team {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  margin-top: 16px;
  color: #fff;
  font-size: 18px;
  font-weight: 700;
  transition: color 0.18s ease;

  &:hover {
    color: var(--hall-gold, #d2a648);
  }
}

.event-mvp-card__logo {
  width: 34px;
  height: 34px;
  display: grid;
  place-items: center;
  flex-shrink: 0;

  :deep(.team-logo) {
    width: 34px;
    height: 34px;
  }
}

.event-mvp-card__event {
  margin: 14px 0 0;
  color: #b8b8b8;
  font-size: 14px;
}

.event-mvp-card__note {
  margin: 8px 0 0;
  color: #9d9d9d;
  font-size: 13px;
}

.event-mvp-card__stats {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 18px;
  min-width: 0;
  padding: 28px 20px;
}

.event-mvp-stats__row {
  display: grid;
  align-items: stretch;
  min-width: 0;
}

.event-mvp-stats__row--three {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.event-mvp-stats__row--two {
  grid-template-columns: repeat(2, minmax(0, 1fr));
  width: 72%;
  margin-inline: auto;
}

.event-mvp-stat {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: flex-start;
  min-width: 0;
  padding: 10px 18px;
  border-left: 1px solid rgba(255, 255, 255, 0.16);

  &:first-child {
    border-left: 0;
  }
}

.event-mvp-stat__label {
  margin-bottom: 12px;
  color: #b9b9b9;
  font-size: 13px;
  white-space: nowrap;
}

.event-mvp-stat__value {
  color: var(--hall-red, #e71922);
  font-size: clamp(34px, 2.6vw, 46px);
  font-weight: 900;
  line-height: 1;
  white-space: nowrap;
  font-variant-numeric: tabular-nums;
}

.hall-lineup-section {
  margin-top: 30px;
  width: 100%;
}

.hall-lineup-section__header {
  margin-bottom: 16px;
}

.hall-lineup-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 12px;
  width: 100%;
}

@media (max-width: 1199px) {
  .event-champion-card {
    grid-template-columns: 1fr 1fr;
    background-position: 70% center;
  }

  .event-champion-card__visual {
    display: none;
  }

  .event-champion-card__team {
    border-right: 1px solid rgba(255, 255, 255, 0.12);
  }

  .event-mvp-card {
    grid-template-columns: 42% 58%;
    max-height: none;
  }

  .event-mvp-card__stats {
    grid-column: 1 / -1;
    border-top: 1px solid rgba(255, 255, 255, 0.08);
    padding: 22px 18px;
  }

  .event-mvp-stats__row--two {
    width: 80%;
  }

  .event-mvp-card__identity {
    border-right: 0;
  }

  .hall-lineup-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 767px) {
  .event-champion-card,
  .event-mvp-card {
    grid-template-columns: 1fr;
  }

  .event-champion-card {
    min-height: 0;
    background-position: center top;
  }

  .event-champion-card__team {
    border-right: 0;
    border-bottom: 1px solid rgba(255, 255, 255, 0.12);
    padding: 24px 20px;
  }

  .event-champion-card__info {
    padding: 24px 20px 28px;
  }

  .event-mvp-card {
    max-height: none;
  }

  .event-mvp-card__visual {
    min-height: 260px;
  }

  .event-mvp-card__identity {
    border-left: 0;
    border-right: 0;
    border-top: 1px solid rgba(255, 255, 255, 0.08);
    padding: 22px 18px;
  }

  .event-mvp-card__name {
    font-size: 36px;
  }

  .event-mvp-stats__row--three,
  .event-mvp-stats__row--two {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    width: 100%;
  }

  .event-mvp-stats__row--three .event-mvp-stat:last-child {
    grid-column: 1 / -1;
    border-left: 0;
    border-top: 1px solid rgba(255, 255, 255, 0.12);
    padding-top: 16px;
  }

  .event-mvp-stats__row--two .event-mvp-stat:first-child {
    border-left: 0;
  }

  .event-mvp-stat__value {
    font-size: 34px;
  }

  .hall-lineup-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 10px;
  }
}

@media (max-width: 479px) {
  .hall-lineup-grid {
    grid-template-columns: 1fr;
  }
}
</style>
