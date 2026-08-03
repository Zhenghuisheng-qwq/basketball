<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import PaperTear from '@/components/common/PaperTear.vue'
import {
  getV4PublicPlayerStat,
  getV4PublicRank,
  getV4PublicSeasonList,
  getV4PublicStanding,
  getV4PublicTeamStat,
} from '@/api/v4Public'
import type {
  V4PublicDivisionBrief,
  V4PublicPlayerStatItem,
  V4PublicRankItem,
  V4PublicSeasonItem,
  V4PublicStandingItem,
  V4PublicTeamStatItem,
} from '@/api/types/v4Public'
import { displayText, formatDecimal } from '@/utils/publicDisplay'
import { v4PlatformLabel, v4RankTypeLabel, v4SeriesLabel } from '@/utils/v4Display'
import '@/views/events/styles/v4-events.scss'

type DataPanel = 'standing' | 'team' | 'player' | 'rank'

const seasons = ref<V4PublicSeasonItem[]>([])
const selectedSeasonId = ref('')
const selectedDivisionId = ref('')
const activePanel = ref<DataPanel>('standing')
const rankType = ref('PLAYER_SCORE')
const loading = ref(true)
const panelLoading = ref(false)
const error = ref('')

const standings = ref<V4PublicStandingItem[]>([])
const teamStats = ref<V4PublicTeamStatItem[]>([])
const playerStats = ref<V4PublicPlayerStatItem[]>([])
const ranks = ref<V4PublicRankItem[]>([])

const currentSeason = computed(() =>
  seasons.value.find((item) => item.seasonId === selectedSeasonId.value) ?? null,
)

const divisions = computed<V4PublicDivisionBrief[]>(() => currentSeason.value?.divisions ?? [])

async function loadSeasons() {
  loading.value = true
  error.value = ''
  try {
    const res = await getV4PublicSeasonList()
    seasons.value = res.data ?? []
    if (!selectedSeasonId.value && seasons.value[0]) {
      selectedSeasonId.value = seasons.value[0].seasonId
      selectedDivisionId.value = seasons.value[0].divisions?.[0]?.divisionId ?? ''
    }
  } catch (err) {
    seasons.value = []
    error.value = err instanceof Error ? err.message : '加载赛季失败'
  } finally {
    loading.value = false
  }
}

async function loadPanel() {
  if (!selectedDivisionId.value) return
  panelLoading.value = true
  try {
    if (activePanel.value === 'standing') {
      standings.value = (await getV4PublicStanding(selectedDivisionId.value)).data ?? []
    } else if (activePanel.value === 'team') {
      teamStats.value = (await getV4PublicTeamStat(selectedDivisionId.value)).data ?? []
    } else if (activePanel.value === 'player') {
      playerStats.value = (await getV4PublicPlayerStat(selectedDivisionId.value)).data ?? []
    } else {
      ranks.value = (await getV4PublicRank(selectedDivisionId.value, rankType.value)).data ?? []
    }
  } catch {
    standings.value = []
    teamStats.value = []
    playerStats.value = []
    ranks.value = []
  } finally {
    panelLoading.value = false
  }
}

watch(selectedSeasonId, (value) => {
  const season = seasons.value.find((item) => item.seasonId === value)
  selectedDivisionId.value = season?.divisions?.[0]?.divisionId ?? ''
})

watch([selectedDivisionId, activePanel, rankType], () => {
  void loadPanel()
})

onMounted(async () => {
  await loadSeasons()
  await loadPanel()
})
</script>

<template>
  <div class="data-center-page v4-events-page">
    <section class="v4-events-hero">
      <div class="site-shell">
        <p class="v4-events-hero__eyebrow">数据中心</p>
        <h1 class="v4-events-hero__title">V4 赛区数据</h1>
        <p class="v4-events-hero__desc">按 Season + Division 浏览积分榜、球队统计、球员统计与排行榜。</p>
        <div v-if="loading" class="v4-state">加载中...</div>
        <div v-else-if="error" class="v4-state v4-state--error">{{ error }}</div>
        <template v-else>
          <div class="v4-data-filters">
            <label>
              赛季
              <select v-model="selectedSeasonId" class="v4-select">
                <option v-for="season in seasons" :key="season.seasonId" :value="season.seasonId">
                  {{ v4SeriesLabel(season.seriesCode) }} · {{ displayText(season.displayTitle) }}
                </option>
              </select>
            </label>
            <label>
              赛区
              <select v-model="selectedDivisionId" class="v4-select">
                <option v-for="division in divisions" :key="division.divisionId" :value="division.divisionId">
                  {{ v4PlatformLabel(division.platformCode) }} · {{ displayText(division.divisionName) }}
                </option>
              </select>
            </label>
          </div>
        </template>
      </div>
    </section>

    <section class="v4-events-list-section">
      <div class="site-shell">
        <div class="v4-stats-subtabs">
          <button
            v-for="item in ([['standing', '积分榜'], ['team', '球队统计'], ['player', '球员统计'], ['rank', '排行榜']] as const)"
            :key="item[0]"
            type="button"
            class="v4-detail-tabs__btn"
            :class="{ 'v4-detail-tabs__btn--active': activePanel === item[0] }"
            @click="activePanel = item[0]"
          >
            {{ item[1] }}
          </button>
        </div>
        <div v-if="activePanel === 'rank'" class="v4-stats-subtabs">
          <button
            v-for="type in ['PLAYER_SCORE', 'PLAYER_ASSIST', 'PLAYER_REBOUND']"
            :key="type"
            type="button"
            class="v4-detail-tabs__btn"
            :class="{ 'v4-detail-tabs__btn--active': rankType === type }"
            @click="rankType = type"
          >
            {{ v4RankTypeLabel(type) }}
          </button>
        </div>

        <div class="data-center-panel" style="margin-top: 20px">
          <div class="data-center-panel__tear-top" aria-hidden="true">
            <PaperTear position="top" tone="light" />
          </div>
          <div class="data-center-panel__shell">
            <div class="data-center-panel__body">
              <div v-if="panelLoading" class="v4-state">加载中...</div>
              <template v-else>
                <div v-if="activePanel === 'standing'">
                  <div v-if="!standings.length" class="v4-state">暂无积分榜</div>
                  <div v-else class="v4-table-wrap">
                    <table class="v4-table">
                      <thead>
                        <tr>
                          <th>排名</th><th>队伍</th><th>胜</th><th>负</th><th>净胜分</th><th>积分</th>
                        </tr>
                      </thead>
                      <tbody>
                        <tr v-for="row in standings" :key="row.eventTeamId">
                          <td>{{ row.rankNo ?? '--' }}</td>
                          <td>{{ displayText(row.teamName) }}</td>
                          <td>{{ row.winCount ?? 0 }}</td>
                          <td>{{ row.loseCount ?? 0 }}</td>
                          <td>{{ row.pointDiff ?? 0 }}</td>
                          <td>{{ row.standingPoints ?? 0 }}</td>
                        </tr>
                      </tbody>
                    </table>
                  </div>
                </div>
                <div v-else-if="activePanel === 'team'">
                  <div v-if="!teamStats.length" class="v4-state">暂无球队统计</div>
                  <div v-else class="v4-table-wrap">
                    <table class="v4-table">
                      <thead>
                        <tr>
                          <th>队伍</th><th>场次</th><th>场均得分</th><th>篮板</th><th>助攻</th>
                        </tr>
                      </thead>
                      <tbody>
                        <tr v-for="row in teamStats" :key="row.eventTeamId">
                          <td>{{ displayText(row.teamName) }}</td>
                          <td>{{ row.matchCount ?? 0 }}</td>
                          <td>{{ formatDecimal(row.avgPointsFor) }}</td>
                          <td>{{ row.rebound ?? 0 }}</td>
                          <td>{{ row.assist ?? 0 }}</td>
                        </tr>
                      </tbody>
                    </table>
                  </div>
                </div>
                <div v-else-if="activePanel === 'player'">
                  <div v-if="!playerStats.length" class="v4-state">暂无球员统计</div>
                  <div v-else class="v4-table-wrap">
                    <table class="v4-table">
                      <thead>
                        <tr>
                          <th>球员</th><th>球队</th><th>场均得分</th><th>场均篮板</th><th>场均助攻</th>
                        </tr>
                      </thead>
                      <tbody>
                        <tr v-for="row in playerStats" :key="row.eventPlayerId">
                          <td>{{ displayText(row.playerName) }}</td>
                          <td>{{ displayText(row.teamName) }}</td>
                          <td>{{ formatDecimal(row.avgPoints) }}</td>
                          <td>{{ formatDecimal(row.avgRebound) }}</td>
                          <td>{{ formatDecimal(row.avgAssist) }}</td>
                        </tr>
                      </tbody>
                    </table>
                  </div>
                </div>
                <div v-else>
                  <div v-if="!ranks.length" class="v4-state">暂无排行榜</div>
                  <div v-else class="v4-table-wrap">
                    <table class="v4-table">
                      <thead>
                        <tr><th>排名</th><th>名称</th><th>数值</th></tr>
                      </thead>
                      <tbody>
                        <tr v-for="row in ranks" :key="`${row.rankNo}-${row.eventPlayerId || row.eventTeamId}`">
                          <td>{{ row.rankNo ?? '--' }}</td>
                          <td>{{ displayText(row.playerName || row.teamName) }}</td>
                          <td>{{ formatDecimal(row.rankValue) }}</td>
                        </tr>
                      </tbody>
                    </table>
                  </div>
                </div>
              </template>
            </div>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped lang="scss">
.v4-data-filters {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  margin-top: 16px;

  label {
    display: flex;
    flex-direction: column;
    gap: 6px;
    color: rgba(255, 255, 255, 0.72);
    font-size: 13px;
    font-weight: 700;
  }
}

.v4-select {
  min-width: 220px;
  min-height: 40px;
  padding: 0 12px;
  border: 1px solid rgba(255, 255, 255, 0.24);
  border-radius: 2px;
  background: rgba(0, 0, 0, 0.24);
  color: #fff;
  font-weight: 700;
}

.data-center-panel__shell {
  background: #e9e7e7;
  padding: 24px 16px 32px;
}

.data-center-panel__body .v4-state {
  color: #333;
}

.data-center-panel__body .v4-table th,
.data-center-panel__body .v4-table td {
  color: #171717;
}

.data-center-panel__body .v4-table th {
  color: rgba(0, 0, 0, 0.56);
}
</style>
