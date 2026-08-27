<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import PlayerMatchTable from './PlayerMatchTable.vue'
import DisciplineStatusPanel from '@/components/discipline/DisciplineStatusPanel.vue'
import type { PlayerCareerStatistics, PlayerDetailData } from '../types'

const props = defineProps<{
  detail: PlayerDetailData
}>()

const router = useRouter()

const seasonStat = computed<PlayerCareerStatistics | null>(() => {
  const divisionId = String(props.detail.profile.divisionId || '')
  return props.detail.careerStats.find((row) => String(row.divisionId) === divisionId)
    || props.detail.careerStats[0]
    || null
})

const seasonBoxes = computed(() => {
  const row = seasonStat.value
  if (!row) return []
  return [
    { key: 'games', label: '出场', value: formatStat(row.games, false), accent: false },
    { key: 'pts', label: '场均得分', value: formatStat(row.points, true), accent: true },
    { key: 'reb', label: '场均篮板', value: formatStat(row.rebounds, true), accent: false },
    { key: 'ast', label: '场均助攻', value: formatStat(row.assists, true), accent: false },
  ]
})

const honorItems = computed(() => props.detail.honors)

function formatStat(value: number | null | undefined, average: boolean): string {
  if (value == null || Number.isNaN(Number(value))) return '--'
  const num = Number(value)
  if (!average || Number.isInteger(num)) return String(num)
  return num.toFixed(1)
}

function honorMeta(honor: { seasonName: string; honorDate: string; honorValue?: string }): string {
  return [honor.seasonName, honor.honorDate || honor.honorValue].filter(Boolean).join(' · ')
}

function goAllMatches() {
  router.replace({ query: { ...router.currentRoute.value.query, tab: 'matches' } })
}
</script>

<template>
  <div class="player-detail-overview">
    <DisciplineStatusPanel
      :items="detail.profile.activeDisciplineSummary || []"
      title="处罚状态"
    />

    <section class="player-detail-overview__section" aria-labelledby="player-event-stats">
      <div class="player-detail-section-head">
        <div class="player-detail-section-head__group">
          <h2 id="player-event-stats" class="player-detail-section-head__title">本赛事数据</h2>
          <span class="player-detail-section-head__sub">Event Stats</span>
        </div>
      </div>

      <div v-if="seasonBoxes.length" class="player-event-stats">
        <div
          v-for="item in seasonBoxes"
          :key="item.key"
          class="player-event-stats__item"
          :class="{ 'is-accent': item.accent }"
        >
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
        </div>
      </div>
      <div v-else class="player-overview-empty">
        <strong>暂无本赛事数据</strong>
        <span>正式比赛审核通过后将在此展示</span>
      </div>
    </section>

    <section class="player-detail-overview__section" aria-labelledby="player-honors">
      <div class="player-detail-section-head player-detail-overview__honor-head">
        <div class="player-detail-section-head__group">
          <h2 id="player-honors" class="player-detail-section-head__title">球员荣誉</h2>
          <span class="player-detail-section-head__sub">Player Honors</span>
        </div>
      </div>

      <div v-if="honorItems.length === 0" class="player-overview-empty">
        <strong>暂无荣誉记录</strong>
        <span>球员获得赛事荣誉后将在此展示</span>
      </div>
      <ul v-else class="player-honor-list">
        <li v-for="honor in honorItems" :key="honor.id" class="player-honor-list__item">
          <img v-if="honor.imageUrl" :src="honor.imageUrl" alt="" />
          <div>
            <strong>{{ honor.honorName }}</strong>
            <span>{{ honorMeta(honor) }}</span>
          </div>
        </li>
      </ul>
    </section>

    <section class="player-detail-overview__section" aria-labelledby="player-recent-matches">
      <div class="player-detail-section-head">
        <div class="player-detail-section-head__group">
          <h2 id="player-recent-matches" class="player-detail-section-head__title">最近比赛</h2>
          <span class="player-detail-section-head__sub">Recent Matches</span>
        </div>
        <button
          type="button"
          class="player-detail-section-head__action player-detail-overview__all-matches"
          @click="goAllMatches"
        >
          查看全部比赛 →
        </button>
      </div>

      <div class="player-recent-panel">
        <PlayerMatchTable :rows="detail.recentMatches" compact />
      </div>
    </section>
  </div>
</template>

<style scoped lang="scss">
@use '../styles/player-detail.scss';

.player-detail-overview__section + .player-detail-overview__section {
  margin-top: 40px;
}

.player-detail-overview__section {
  padding: 0;
  box-sizing: border-box;
}

.player-detail-overview__honor-head {
  margin-bottom: 16px;
}

.player-event-stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  border: 1px solid rgba(17, 17, 17, 0.08);
  background: #fff;
}

.player-event-stats__item {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  min-height: 64px;
  padding: 16px 20px;
  border-right: 1px solid rgba(17, 17, 17, 0.06);

  &:last-child {
    border-right: 0;
  }

  span {
    color: #777;
    font-size: 13px;
    font-weight: 700;
  }

  strong {
    color: #171717;
    font-size: 24px;
    font-weight: 900;
    line-height: 1;
  }

  &.is-accent strong {
    color: var(--cnpa-red, #e10613);
  }
}

.player-overview-empty {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 6px;
  min-height: 80px;
  padding: 16px 18px;
  border: 1px solid rgba(17, 17, 17, 0.08);
  background: #f7f5f2;
  box-sizing: border-box;

  strong {
    color: #333;
    font-size: 14px;
    font-weight: 800;
  }

  span {
    color: #888;
    font-size: 12px;
    font-weight: 600;
  }
}

.player-honor-list {
  display: flex;
  flex-direction: column;
  margin: 0;
  padding: 0;
  list-style: none;
  border: 1px solid rgba(17, 17, 17, 0.08);
  background: #fff;
}

.player-honor-list__item {
  display: flex;
  align-items: center;
  gap: 12px;
  min-height: 64px;
  padding: 12px 16px;
  border-bottom: 1px solid rgba(17, 17, 17, 0.06);

  &:last-child {
    border-bottom: 0;
  }

  img {
    width: 36px;
    height: 36px;
    object-fit: cover;
    flex-shrink: 0;
  }

  strong,
  span {
    display: block;
  }

  strong {
    color: #111;
    font-size: 15px;
    font-weight: 800;
  }

  span {
    margin-top: 4px;
    color: #777;
    font-size: 12px;
    font-weight: 600;
  }
}

.player-recent-panel {
  overflow: hidden;
  border: 1px solid rgba(17, 17, 17, 0.08);
  background: rgba(255, 255, 255, 0.55);
}

.player-detail-overview__all-matches {
  min-height: 40px;
  padding: 10px 4px 10px 16px;
}

@media (max-width: 1279px) {
  .player-event-stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .player-event-stats__item:nth-child(2n) {
    border-right: 0;
  }

  .player-event-stats__item:nth-child(-n + 2) {
    border-bottom: 1px solid rgba(17, 17, 17, 0.06);
  }
}

@media (max-width: 767px) {
  .player-event-stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
