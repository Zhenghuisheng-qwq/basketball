<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import PlayerAvatar from '@/components/player/PlayerAvatar.vue'
import PlayerMatchTable from './PlayerMatchTable.vue'
import DisciplineStatusPanel from '@/components/discipline/DisciplineStatusPanel.vue'
import type { PlayerCareerStatistics, PlayerDetailData } from '../types'

const props = defineProps<{
  detail: PlayerDetailData
}>()

const router = useRouter()

const profile = computed(() => props.detail.profile)

const seasonStat = computed<PlayerCareerStatistics | null>(() => {
  const divisionId = String(props.detail.profile.divisionId || '')
  return props.detail.careerStats.find((row) => String(row.divisionId) === divisionId)
    || props.detail.careerStats[0]
    || null
})

const seasonBoxes = computed(() => {
  const row = seasonStat.value
  if (!row || !row.games) return []
  return [
    { key: 'games', label: '出场次数', unit: '场', value: formatStat(row.games, false), accent: false },
    { key: 'pts', label: '场均得分', unit: '分', value: formatStat(row.points, true), accent: true },
    { key: 'reb', label: '场均篮板', unit: '板', value: formatStat(row.rebounds, true), accent: false },
    { key: 'ast', label: '场均助攻', unit: '助', value: formatStat(row.assists, true), accent: false },
  ]
})

const honorItems = computed(() => props.detail.honors.slice(0, 4))

function formatStat(value: number | null | undefined, average: boolean): string {
  if (value == null || Number.isNaN(Number(value))) return '--'
  const num = Number(value)
  if (!average || Number.isInteger(num)) return String(num)
  return num.toFixed(1)
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

    <section class="player-detail-overview__section">
      <div class="player-detail-section-head">
        <div>
          <div class="player-detail-section-head__group">
            <h2 class="player-detail-section-head__title">球员荣誉</h2>
            <span class="player-detail-section-head__sub">Player Honors</span>
          </div>
          <p class="player-detail-section-head__desc">
            当前赛事身份与真实荣誉记录，不跨赛季拼接
          </p>
        </div>
      </div>

      <div class="player-honor-board">
        <article class="player-honor-card">
          <span class="player-honor-card__brand">CNPA</span>
          <PlayerAvatar
            :name="profile.displayName"
            :avatar-url="profile.avatarUrl"
            :player-id="profile.id"
            size="card"
            tone="gold"
          />
          <strong>{{ profile.displayName }}</strong>
          <em>{{ profile.position }}</em>
          <span>{{ profile.currentTeamAbbr }}</span>
          <small>{{ profile.registrationText }}</small>
        </article>

        <div class="player-honor-board__main">
          <div v-if="seasonBoxes.length" class="player-honor-stats">
            <div
              v-for="item in seasonBoxes"
              :key="item.key"
              class="player-honor-stats__item"
              :class="{ 'is-accent': item.accent }"
            >
              <span>{{ item.label }}</span>
              <strong>{{ item.value }}</strong>
              <em>{{ item.unit }}</em>
            </div>
          </div>

          <div class="player-honor-board__footer">
            <div class="player-honor-board__footer-head">赛事荣誉</div>
            <div v-if="honorItems.length === 0" class="player-honor-board__empty">
              暂无荣誉记录
            </div>
            <div v-else class="player-honor-board__list">
              <article v-for="honor in honorItems" :key="honor.id">
                <img v-if="honor.imageUrl" :src="honor.imageUrl" alt="" />
                <div>
                  <strong>{{ honor.honorName }}</strong>
                  <span>{{ [honor.seasonName, honor.honorDate].filter(Boolean).join(' · ') }}</span>
                </div>
              </article>
            </div>
          </div>
        </div>
      </div>
    </section>

    <section class="player-detail-overview__section">
      <div class="player-detail-section-head">
        <div class="player-detail-section-head__group">
          <h2 class="player-detail-section-head__title">最近比赛</h2>
          <span class="player-detail-section-head__sub">Recent Matches</span>
        </div>
        <button type="button" class="player-detail-section-head__action" @click="goAllMatches">
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
  margin-top: 36px;
}

.player-detail-overview__section {
  padding: 8px 0 4px;
  box-sizing: border-box;
}

.player-honor-board {
  display: grid;
  grid-template-columns: 168px minmax(0, 1fr);
  gap: 16px;
  padding: 16px;
  border: 1px solid rgba(17, 17, 17, 0.08);
  background: rgba(255, 255, 255, 0.42);
}

.player-honor-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  min-height: 268px;
  padding: 18px 14px 16px;
  background:
    linear-gradient(180deg, rgba(18, 16, 12, 0.16), rgba(8, 8, 8, 0.08)),
    #14110d;
  color: #f3e3c2;
  text-align: center;
  box-sizing: border-box;
}

.player-honor-card__brand {
  margin-bottom: 14px;
  color: rgba(243, 227, 194, 0.72);
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.22em;
}

.player-honor-card strong {
  margin-top: 12px;
  color: #fff;
  font-size: 16px;
  font-weight: 900;
  line-height: 1.2;
}

.player-honor-card em {
  margin-top: 6px;
  color: #e10613;
  font-size: 13px;
  font-style: normal;
  font-weight: 800;
}

.player-honor-card span {
  margin-top: 4px;
  color: rgba(255, 255, 255, 0.62);
  font-size: 12px;
  font-weight: 700;
}

.player-honor-card small {
  margin-top: auto;
  padding-top: 14px;
  color: rgba(243, 227, 194, 0.46);
  font-size: 9px;
  font-weight: 800;
  letter-spacing: 0.14em;
}

.player-honor-board__main {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 12px;
}

.player-honor-stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  min-height: 132px;
  border: 1px solid rgba(17, 17, 17, 0.08);
  background: rgba(255, 255, 255, 0.72);
}

.player-honor-stats__item {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 8px;
  padding: 16px 12px;
  border-right: 1px solid rgba(17, 17, 17, 0.06);

  &:last-child {
    border-right: 0;
  }

  span {
    color: #777;
    font-size: 12px;
    font-weight: 700;
  }

  strong {
    color: #171717;
    font-size: 28px;
    font-weight: 900;
    line-height: 1;
  }

  em {
    color: #999;
    font-size: 12px;
    font-style: normal;
    font-weight: 700;
  }

  &.is-accent strong {
    color: var(--cnpa-red, #e10613);
  }
}

.player-honor-board__footer {
  min-height: 92px;
  overflow: hidden;
  border: 1px solid rgba(17, 17, 17, 0.08);
  background: #fff;
}

.player-honor-board__footer-head {
  padding: 8px 14px;
  background: #171717;
  color: #fff;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.08em;
}

.player-honor-board__empty {
  display: flex;
  align-items: center;
  min-height: 60px;
  padding: 0 14px;
  color: #888;
  font-size: 13px;
  font-weight: 700;
}

.player-honor-board__list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px 16px;
  padding: 12px 14px;
}

.player-honor-board__list article {
  display: flex;
  align-items: center;
  gap: 10px;
}

.player-honor-board__list img {
  width: 36px;
  height: 36px;
  object-fit: cover;
}

.player-honor-board__list strong,
.player-honor-board__list span {
  display: block;
}

.player-honor-board__list strong {
  color: #111;
  font-size: 14px;
  font-weight: 800;
}

.player-honor-board__list span {
  color: #777;
  font-size: 12px;
}

.player-recent-panel {
  overflow: hidden;
  border: 1px solid rgba(17, 17, 17, 0.08);
  background: rgba(255, 255, 255, 0.55);
}

@media (max-width: 991px) {
  .player-honor-board {
    grid-template-columns: 1fr;
  }

  .player-honor-card {
    min-height: 0;
    flex-direction: row;
    flex-wrap: wrap;
    justify-content: center;
    gap: 8px 12px;
    padding: 16px;
  }

  .player-honor-card small {
    width: 100%;
    padding-top: 4px;
  }

  .player-honor-stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    min-height: 0;
  }

  .player-honor-board__list {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 767px) {
  .player-honor-stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
