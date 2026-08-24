<script setup lang="ts">
import { computed, inject, ref, watch } from 'vue'
import { getV4PublicStageStandings, getV4PublicStanding } from '@/api/v4Public'
import type { V4PublicStageStandingScope } from '@/api/types/v4Public'
import TeamEntryLink from '@/components/team/TeamEntryLink.vue'
import { toNumber } from '@/utils/publicDisplay'
import type { Standing } from '../types/events'
import { buildTeamLookup } from '../utils/teamLookup'
import type { usePublicSeasonContext } from '../composables/usePublicSeasonContext'
import { v4StandingToPublic } from '../utils/v4PublicAdapters'
import { standingToTeam } from '../utils/mapPublic'
import TeamLogo from './TeamLogo.vue'

type SeasonContext = ReturnType<typeof usePublicSeasonContext>
const ctx = inject<SeasonContext>('publicSeasonContext')!

const props = defineProps<{
  stageId?: string
  grouped?: boolean
  stageName?: string
}>()

type StandingGroup = {
  key: string
  title: string
  qualifyCount?: number | null
  rows: Standing[]
}

const groups = ref<StandingGroup[]>([])
const state = ref<'loading' | 'success' | 'empty' | 'error'>('loading')
const errorMessage = ref('')
let reqId = 0

function mapRows(rawRows: Parameters<typeof v4StandingToPublic>[0][]): Standing[] {
  return [...rawRows]
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
        standingPoints: toNumber(pub.standingPoints, 0),
        winRate: pub.winRate != null ? toNumber(pub.winRate, 0) : (played > 0 ? (wins / played) * 100 : 0),
        pointDiff: toNumber(pub.pointDiff, 0),
        recent: [] as Array<'W' | 'L'>,
        disqualified: Boolean(row.disqualified || pub.disqualified),
      }
    })
    .sort((a, b) => a.rank - b.rank)
}

function groupTitle(scope: V4PublicStageStandingScope, index: number, total: number): string {
  if (scope.groupName) return scope.groupName
  if (scope.groupCode) return `${scope.groupCode}组`
  const key = String(scope.groupScopeKey || scope.groupId || '')
  if (key && key !== '0') {
    const n = Number(key)
    if (Number.isFinite(n) && n > 0 && n <= 26) {
      return `${String.fromCharCode(64 + n)}组`
    }
    return `小组 ${key}`
  }
  if (total > 1) return `小组 ${index + 1}`
  return props.stageName ? `${props.stageName}积分榜` : '积分榜'
}

const flatStandings = computed(() => groups.value.flatMap((g) => g.rows))

const teamLookup = computed(() => {
  const teams = flatStandings.value.map((row) =>
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

function zoneTitle(group: StandingGroup, kind: 'qualify' | 'rest'): string {
  const n = group.qualifyCount
  if (kind === 'qualify') {
    if (props.grouped) {
      return n && n > 0 ? `晋级区（前${n}名）` : '积分榜'
    }
    return n && n > 0 ? `晋级区（前${n}名）` : '积分榜'
  }
  return n && n > 0 ? `其余排名（${n + 1}名及以后）` : '其余排名'
}

function qualifyRows(group: StandingGroup): Standing[] {
  const n = group.qualifyCount
  if (!n || n <= 0 || n >= group.rows.length) return group.rows
  return group.rows.filter((row) => row.rank <= n)
}

function restRows(group: StandingGroup): Standing[] {
  const n = group.qualifyCount
  if (!n || n <= 0 || n >= group.rows.length) return []
  return group.rows.filter((row) => row.rank > n)
}

async function load() {
  const current = ++reqId
  const divisionId = ctx.divisionId.value
  const stageId = props.stageId
  if (!divisionId && !stageId) {
    groups.value = []
    state.value = 'empty'
    return
  }
  state.value = 'loading'
  errorMessage.value = ''
  try {
    if (stageId) {
      const res = await getV4PublicStageStandings(stageId)
      if (current !== reqId) return
      const scopes = res.data ?? []
      const mapped = scopes
        .map((scope, index) => ({
          key: String(scope.groupScopeKey ?? scope.groupId ?? index),
          title: groupTitle(scope, index, scopes.length),
          qualifyCount: scope.qualifyCount,
          rows: mapRows(scope.rows ?? []),
        }))
        .filter((g) => g.rows.length)
      if (mapped.length) {
        groups.value = mapped
        state.value = 'success'
        return
      }
    }
    // fallback：Division 级 standing（无 stage 或 stage 空）
    const res = await getV4PublicStanding(divisionId)
    if (current !== reqId) return
    const rows = mapRows(res.data ?? [])
    groups.value = rows.length
      ? [{ key: 'all', title: props.stageName ? `${props.stageName}积分榜` : '积分榜', rows }]
      : []
    state.value = groups.value.length ? 'success' : 'empty'
  } catch (err) {
    if (current !== reqId) return
    groups.value = []
    state.value = 'error'
    errorMessage.value = err instanceof Error ? err.message : '加载积分榜失败'
  }
}

watch(
  () => [ctx.divisionId.value, props.stageId, props.grouped] as const,
  () => {
    void load()
  },
  { immediate: true },
)
</script>
<template>
  <section class="regular-standings">
    <p v-if="state === 'loading'" class="standings-note">加载积分榜...</p>
    <p v-else-if="state === 'error'" class="standings-note">{{ errorMessage }}</p>
    <p v-else-if="state === 'empty'" class="standings-note">暂无积分榜数据</p>

    <div v-else class="standings-groups">
      <div v-for="group in groups" :key="group.key" class="standings-group">
        <h3 v-if="groups.length > 1 || grouped" class="standings-group__title">{{ group.title }}</h3>
        <div class="standings-grid" :class="{ 'standings-grid--single': !restRows(group).length }">
          <div class="standings-panel">
            <h3>{{ zoneTitle(group, 'qualify') }}</h3>
            <div class="table-wrap">
              <div class="table-head">
                <span>排名</span><span>战队</span><span>胜</span><span>负</span><span>积分</span><span>胜率</span><span>净胜分</span><span>近况</span>
              </div>
              <div v-for="row in qualifyRows(group)" :key="`${group.key}-${row.teamId}`" class="table-row">
                <span class="rank rank-playoff">{{ row.rank }}</span>
                <TeamEntryLink
                  v-if="getTeamById(row.teamId)"
                  :team="getTeamById(row.teamId)!"
                  class="team"
                >
                  <TeamLogo :team="getTeamById(row.teamId)!" size="sm" />
                  {{ getTeamById(row.teamId)!.name }}
                  <em v-if="row.disqualified" class="dq-tag">取消资格</em>
                </TeamEntryLink>
                <span>{{ row.wins }}</span>
                <span>{{ row.losses }}</span>
                <span class="points">{{ row.standingPoints }}</span>
                <span>{{ formatWinRate(row.winRate) }}</span>
                <span>{{ formatPointDiff(row.pointDiff) }}</span>
                <span class="recent">
                  <i v-for="(item, index) in row.recent" :key="index" :class="item === 'W' ? 'win' : 'loss'" />
                </span>
              </div>
            </div>
          </div>

          <div v-if="restRows(group).length" class="standings-panel">
            <h3>{{ zoneTitle(group, 'rest') }}</h3>
            <div class="table-wrap">
              <div class="table-head">
                <span>排名</span><span>战队</span><span>胜</span><span>负</span><span>积分</span><span>胜率</span><span>净胜分</span><span>近况</span>
              </div>
              <div v-for="row in restRows(group)" :key="`${group.key}-r-${row.teamId}`" class="table-row">
                <span class="rank rank-regular">{{ row.rank }}</span>
                <TeamEntryLink
                  v-if="getTeamById(row.teamId)"
                  :team="getTeamById(row.teamId)!"
                  class="team"
                >
                  <TeamLogo :team="getTeamById(row.teamId)!" size="sm" />
                  {{ getTeamById(row.teamId)!.name }}
                  <em v-if="row.disqualified" class="dq-tag">取消资格</em>
                </TeamEntryLink>
                <span>{{ row.wins }}</span>
                <span>{{ row.losses }}</span>
                <span class="points">{{ row.standingPoints }}</span>
                <span>{{ formatWinRate(row.winRate) }}</span>
                <span>{{ formatPointDiff(row.pointDiff) }}</span>
                <span class="recent">
                  <i v-for="(item, index) in row.recent" :key="index" :class="item === 'W' ? 'win' : 'loss'" />
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
    <p v-if="state === 'success' && grouped" class="standings-note">小组积分按 groupScope 隔离展示；晋级规则以赛段配置为准。</p>
  </section>
</template>
<style scoped lang="scss">
.regular-standings {
  padding-top: 8px;
}

.standings-groups {
  display: flex;
  flex-direction: column;
  gap: 28px;
}

.standings-group__title {
  margin: 0 0 12px;
  color: #f5f5f5;
  font-size: 18px;
  font-weight: 900;
}

.standings-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;

  &--single {
    grid-template-columns: minmax(0, 1fr);
  }
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
  overflow-x: auto;
}

.table-head,
.table-row {
  display: grid;
  grid-template-columns: 52px 1.4fr repeat(5, 64px) 88px;
  gap: 8px;
  align-items: center;
  padding: 0 12px;
  min-width: 720px;
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

.rank-playoff {
  color: var(--cnpa-red);
  font-weight: 800;
}

.rank-regular {
  color: rgba(255, 255, 255, 0.72);
  font-weight: 700;
}

.points {
  font-weight: 800;
}

.team {
  min-width: 0;
}

.recent {
  display: inline-flex;
  gap: 4px;

  i {
    width: 8px;
    height: 8px;
    border-radius: 50%;
  }

  .win {
    background: #22c55e;
  }

  .loss {
    background: #ef4444;
  }
}

.dq-tag {
  margin-left: 6px;
  font-style: normal;
  font-size: 11px;
  color: #f87171;
  border: 1px solid rgba(248, 113, 113, 0.45);
  border-radius: 4px;
  padding: 0 4px;
}

.standings-note {
  margin: 16px 0 0;
  color: rgba(255, 255, 255, 0.55);
  font-size: 13px;
}

@media (max-width: 900px) {
  .standings-grid {
    grid-template-columns: 1fr;
  }
}
</style>
