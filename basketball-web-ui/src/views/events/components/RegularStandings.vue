<script setup lang="ts">
import { computed, inject, ref, watch } from 'vue'
import { getPublicSeasonStanding } from '@/api/publicStanding'
import type { PublicStandingItemVo } from '@/api/types/public'
import {
  displayNumber,
  displayText,
  formatPercent,
  formatStreak,
  toNumber,
} from '@/utils/publicDisplay'
import type { usePublicSeasonContext } from '../composables/usePublicSeasonContext'
import { standingToTeam } from '../utils/mapPublic'
import TeamEntryLink from '@/components/team/TeamEntryLink.vue'
import TeamLogo from './TeamLogo.vue'

type SeasonContext = ReturnType<typeof usePublicSeasonContext>

const ctx = inject<SeasonContext>('publicSeasonContext')!

const standings = ref<PublicStandingItemVo[]>([])
const state = ref<'loading' | 'success' | 'empty' | 'error'>('loading')
const errorMessage = ref('')
let reqId = 0

const playoffCutoffRank = 8

const playoffZone = computed(() =>
  standings.value.filter((item) => toNumber(item.rankNo, 999) <= playoffCutoffRank),
)
const regularZone = computed(() =>
  standings.value.filter((item) => toNumber(item.rankNo, 0) > playoffCutoffRank),
)

function formatPointDiff(value: number | null | undefined) {
  const num = toNumber(value, 0)
  return num > 0 ? `+${num}` : `${num}`
}

async function loadStandings(seasonId: string) {
  const current = ++reqId
  if (!seasonId) {
    standings.value = []
    state.value = 'empty'
    return
  }
  state.value = 'loading'
  errorMessage.value = ''
  try {
    const res = await getPublicSeasonStanding(seasonId)
    if (current !== reqId) return
    standings.value = [...(res.data ?? [])].sort(
      (a, b) => toNumber(a.rankNo, 999) - toNumber(b.rankNo, 999),
    )
    state.value = standings.value.length ? 'success' : 'empty'
  } catch (err) {
    if (current !== reqId) return
    standings.value = []
    state.value = 'error'
    errorMessage.value = err instanceof Error ? err.message : '加载积分榜失败'
  }
}

watch(
  () => ctx.seasonId.value,
  (sid) => {
    void loadStandings(sid)
  },
  { immediate: true },
)
</script>

<template>
  <section class="regular-standings">
    <div v-if="state === 'loading'" class="standings-state">加载中...</div>
    <div v-else-if="state === 'error'" class="standings-state is-error">{{ errorMessage }}</div>
    <div v-else-if="state === 'empty'" class="standings-state">暂无积分榜数据</div>
    <template v-else>
      <div class="standings-grid">
        <div class="standings-panel">
          <h3>季后赛区（前8名晋级季后赛）</h3>
          <div class="table-wrap">
            <div class="table-head">
              <span>排名</span><span>战队</span><span>胜</span><span>负</span><span>胜率</span><span>净胜分</span><span>近况</span>
            </div>
            <div v-if="playoffZone.length === 0" class="table-empty">暂无数据</div>
            <div v-for="row in playoffZone" :key="row.teamId" class="table-row">
              <span class="rank rank-playoff">{{ displayNumber(row.rankNo) }}</span>
              <TeamEntryLink :team="standingToTeam(row)" class="team">
                <TeamLogo :team="standingToTeam(row)" size="sm" />
                {{ displayText(row.teamName) }}
              </TeamEntryLink>
              <span>{{ displayNumber(row.wins) }}</span>
              <span>{{ displayNumber(row.losses) }}</span>
              <span>{{ formatPercent(row.winRate) }}</span>
              <span>{{ formatPointDiff(row.pointDiff) }}</span>
              <span class="streak">{{ formatStreak(row.streakType, row.streakCount) }}</span>
            </div>
          </div>
        </div>

        <div class="standings-panel">
          <h3>非季后赛区（9名及以后）</h3>
          <div class="table-wrap">
            <div class="table-head">
              <span>排名</span><span>战队</span><span>胜</span><span>负</span><span>胜率</span><span>净胜分</span><span>近况</span>
            </div>
            <div v-if="regularZone.length === 0" class="table-empty">暂无数据</div>
            <div v-for="row in regularZone" :key="row.teamId" class="table-row">
              <span class="rank rank-regular">{{ displayNumber(row.rankNo) }}</span>
              <TeamEntryLink :team="standingToTeam(row)" class="team">
                <TeamLogo :team="standingToTeam(row)" size="sm" />
                {{ displayText(row.teamName) }}
              </TeamEntryLink>
              <span>{{ displayNumber(row.wins) }}</span>
              <span>{{ displayNumber(row.losses) }}</span>
              <span>{{ formatPercent(row.winRate) }}</span>
              <span>{{ formatPointDiff(row.pointDiff) }}</span>
              <span class="streak">{{ formatStreak(row.streakType, row.streakCount) }}</span>
            </div>
          </div>
        </div>
      </div>
      <p class="standings-note">常规赛前8名战队晋级季后赛。近况展示连胜/连败，无假排名箭头。</p>
    </template>
  </section>
</template>

<style scoped lang="scss">
.regular-standings {
  padding-top: 8px;
}

.standings-state {
  padding: 40px 0;
  text-align: center;
  color: rgba(255, 255, 255, 0.55);
  font-weight: 600;

  &.is-error {
    color: #e10613;
  }
}

.standings-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.standings-panel {
  border: 1px solid rgba(255, 255, 255, 0.1);
  background: #111;

  h3 {
    margin: 0;
    padding: 16px;
    font-size: 14px;
    border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  }
}

.table-wrap {
  width: 100%;
}

.table-empty {
  padding: 24px 12px;
  color: rgba(255, 255, 255, 0.45);
  font-size: 13px;
}

.table-head,
.table-row {
  display: grid;
  grid-template-columns: 52px 1.4fr repeat(4, 64px) 88px;
  gap: 8px;
  align-items: center;
  padding: 0 12px;
}

.table-head {
  min-height: 42px;
  font-size: 12px;
  color: var(--cnpa-text-secondary);
}

.table-row {
  min-height: 48px;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
  font-size: 13px;
}

.rank {
  font-weight: 900;
}

.rank-playoff {
  color: #e10613;
}

.team {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  color: inherit;
  text-decoration: none;
}

.streak {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.72);
}

.standings-note {
  margin: 14px 0 0;
  color: rgba(255, 255, 255, 0.45);
  font-size: 12px;
}

@media (max-width: 992px) {
  .standings-grid {
    grid-template-columns: 1fr;
  }

  .table-head,
  .table-row {
    grid-template-columns: 44px 1.2fr repeat(3, 48px) 64px 72px;
    font-size: 12px;
  }
}
</style>
