<script setup lang="ts">
import { computed, inject, ref, watch } from 'vue'
import { getV4PublicStanding } from '@/api/v4Public'
import { toNumber } from '@/utils/publicDisplay'
import type { Standing } from '../types/events'
import { buildTeamLookup } from '../utils/teamLookup'
import type { usePublicSeasonContext } from '../composables/usePublicSeasonContext'
import { v4StandingToPublic } from '../utils/v4PublicAdapters'
import { standingToTeam } from '../utils/mapPublic'
import TeamLogo from './TeamLogo.vue'

type SeasonContext = ReturnType<typeof usePublicSeasonContext>
const ctx = inject<SeasonContext>('publicSeasonContext')!

const playoffCutoffRank = 8
const standings = ref<Standing[]>([])
const state = ref<'loading' | 'success' | 'empty' | 'error'>('loading')
const errorMessage = ref('')
let reqId = 0

const playoffZone = computed(() => standings.value.filter((item) => item.rank <= playoffCutoffRank))
const regularZone = computed(() => standings.value.filter((item) => item.rank > playoffCutoffRank))

const teamLookup = computed(() => {
  const teams = standings.value.map((row) =>
    standingToTeam({
      teamId: row.teamId,
      teamName: row.teamName,
      teamLogoUrl: row.teamLogo,
      wins: row.wins,
      losses: row.losses,
    }),
  )
  return buildTeamLookup(teams)
})

function getTeamById(id: number | string) {
  return teamLookup.value(id)
}

function formatWinRate(value: number) {
  return `${value.toFixed(1)}%`
}

function formatPointDiff(value: number) {
  return value > 0 ? `+${value}` : `${value}`
}

async function load(divisionId: string) {
  const current = ++reqId
  if (!divisionId) {
    standings.value = []
    state.value = 'empty'
    return
  }
  state.value = 'loading'
  errorMessage.value = ''
  try {
    const res = await getV4PublicStanding(divisionId)
    if (current !== reqId) return
    standings.value = [...(res.data ?? [])]
      .map((row) => {
        const pub = v4StandingToPublic(row, ctx.seasonId.value)
        const wins = toNumber(pub.wins, 0)
        const losses = toNumber(pub.losses, 0)
        const played = wins + losses
        return {
          rank: toNumber(pub.rankNo, 999),
          teamId: String(pub.teamId),
          teamName: pub.teamName || '',
          teamLogo: pub.teamLogoUrl || '',
          wins,
          losses,
          winRate: played > 0 ? (wins / played) * 100 : 0,
          pointDiff: toNumber(pub.pointDiff, 0),
          recent: [] as Array<'W' | 'L'>,
        }
      })
      .sort((a, b) => a.rank - b.rank)
    state.value = standings.value.length ? 'success' : 'empty'
  } catch (err) {
    if (current !== reqId) return
    standings.value = []
    state.value = 'error'
    errorMessage.value = err instanceof Error ? err.message : '加载积分榜失败'
  }
}

watch(
  () => ctx.divisionId.value,
  (id) => {
    void load(id)
  },
  { immediate: true },
)
</script>
<template>
  <section class="regular-standings">
    <div class="standings-grid">
      <div class="standings-panel">
        <h3>季后赛区（前8名晋级季后赛）</h3>
        <div class="table-wrap">
          <div class="table-head">
            <span>排名</span><span>战队</span><span>胜</span><span>负</span><span>胜率</span><span>净胜分</span><span>近况</span>
          </div>
          <div v-for="row in playoffZone" :key="row.rank" class="table-row">
            <span class="rank rank-playoff">{{ row.rank }}</span>
            <TeamEntryLink
              v-if="getTeamById(row.teamId)"
              :team="getTeamById(row.teamId)!"
              class="team"
            >
              <TeamLogo :team="getTeamById(row.teamId)!" size="sm" />
              {{ getTeamById(row.teamId)!.name }}
            </TeamEntryLink>
            <span>{{ row.wins }}</span>
            <span>{{ row.losses }}</span>
            <span>{{ formatWinRate(row.winRate) }}</span>
            <span>{{ formatPointDiff(row.pointDiff) }}</span>
            <span class="recent">
              <i v-for="(item, index) in row.recent" :key="index" :class="item === 'W' ? 'win' : 'loss'" />
            </span>
          </div>
        </div>
      </div>

      <div class="standings-panel">
        <h3>非季后赛区（9名及以后）</h3>
        <div class="table-wrap">
          <div class="table-head">
            <span>排名</span><span>战队</span><span>胜</span><span>负</span><span>胜率</span><span>净胜分</span><span>近况</span>
          </div>
          <div v-for="row in regularZone" :key="row.rank" class="table-row">
            <span class="rank rank-regular">{{ row.rank }}</span>
            <TeamEntryLink
              v-if="getTeamById(row.teamId)"
              :team="getTeamById(row.teamId)!"
              class="team"
            >
              <TeamLogo :team="getTeamById(row.teamId)!" size="sm" />
              {{ getTeamById(row.teamId)!.name }}
            </TeamEntryLink>
            <span>{{ row.wins }}</span>
            <span>{{ row.losses }}</span>
            <span>{{ formatWinRate(row.winRate) }}</span>
            <span>{{ formatPointDiff(row.pointDiff) }}</span>
            <span class="recent">
              <i v-for="(item, index) in row.recent" :key="index" :class="item === 'W' ? 'win' : 'loss'" />
            </span>
          </div>
        </div>
      </div>
    </div>
    <p class="standings-note">常规赛前8名战队晋级季后赛。</p>
  </section>
</template>
<style scoped lang="scss">
.regular-standings {
  padding-top: 8px;
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
  min-height: 52px;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
  font-size: 13px;
}

.rank {
  display: grid;
  place-items: center;
  width: 32px;
  height: 28px;
  font-weight: 900;
}

.rank-playoff {
  background: var(--cnpa-red);
}

.rank-regular {
  background: #333;
}

.team {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 800;
}

.recent {
  display: flex;
  gap: 4px;
}

.recent i {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}

.recent .win {
  background: var(--cnpa-red);
}

.recent .loss {
  background: #444;
}

.standings-note {
  margin: 18px 0 0;
  font-size: 13px;
  color: var(--cnpa-text-secondary);
}

@media (max-width: 992px) {
  .standings-grid {
    grid-template-columns: 1fr;
  }

  .table-head,
  .table-row {
    grid-template-columns: 44px 1.2fr repeat(4, 48px) 72px;
    font-size: 11px;
  }
}
</style>