<script setup lang="ts">
import { computed, inject, ref, watch } from 'vue'
import { getV4PublicDivisionSchedule, getV4PublicStageStandings, getV4PublicStanding } from '@/api/v4Public'
import type { V4PublicStageStandingScope } from '@/api/types/v4Public'
import TeamEntryLink from '@/components/team/TeamEntryLink.vue'
import { toNumber } from '@/utils/publicDisplay'
import type { Standing } from '../types/events'
import { buildTeamLookup } from '../utils/teamLookup'
import type { usePublicSeasonContext } from '../composables/usePublicSeasonContext'
import { v4StandingToPublic } from '../utils/v4PublicAdapters'
import { standingToTeam } from '../utils/mapPublic'
import {
  leftZoneCopy,
  resolveQualifyCount,
  rightZoneCopy,
  splitAt,
  splitLeftRows,
  splitRightRows,
  stageProgressLayout,
  type StageProgressLayout,
  type StandingZoneTone,
  type ZoneCopy,
} from '../utils/stageProgressDisplay'
import { buildTeamRecentFormMap, type RecentFormResult } from '../utils/recentFormDisplay'
import TeamLogo from './TeamLogo.vue'

type SeasonContext = ReturnType<typeof usePublicSeasonContext>
const ctx = inject<SeasonContext>('publicSeasonContext')!

const props = defineProps<{
  stageId?: string
  grouped?: boolean
  stageName?: string
  advanceCount?: number | null
  emptyText?: string
  zoneTone?: StandingZoneTone
}>()

type StandingGroup = {
  key: string
  title: string
  qualifyCount?: number | null
  rows: Standing[]
}

const groups = ref<StandingGroup[]>([])
const formMap = ref<Record<string, RecentFormResult[]>>({})
const formReady = ref(false)
const state = ref<'loading' | 'success' | 'empty' | 'error'>('loading')
const errorMessage = ref('')
let reqId = 0

function mapRows(rawRows: Parameters<typeof v4StandingToPublic>[0][]): Standing[] {
  return [...rawRows].map((row) => {
    const pub = v4StandingToPublic(row, ctx.seasonId.value)
    const wins = toNumber(pub.wins, 0)
    const losses = toNumber(pub.losses, 0)
    return {
      rank: toNumber(pub.rankNo, 999),
      teamId: String(pub.teamId),
      teamName: pub.teamName || '',
      teamLogo: pub.teamLogoUrl || '',
      wins,
      losses,
      standingPoints: toNumber(pub.standingPoints, 0),
      winRate: 0,
      pointDiff: toNumber(pub.pointDiff, 0),
      recent: [] as Array<'W' | 'L'>,
      disqualified: Boolean(row.disqualified || pub.disqualified),
    }
  })
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

function formatPointDiff(value: number) {
  return value > 0 ? `+${value}` : `${value}`
}

function rowForm(row: Standing): RecentFormResult[] {
  return formMap.value[String(row.teamId)] || []
}

function groupQualifyCount(group: StandingGroup): number {
  return resolveQualifyCount(group.qualifyCount, props.advanceCount)
}

function groupLayout(group: StandingGroup): StageProgressLayout {
  return stageProgressLayout(group.rows.length, groupQualifyCount(group))
}

function groupSplitAt(group: StandingGroup): number {
  return splitAt(group.rows.length, groupQualifyCount(group), groupLayout(group))
}

function groupLeftCopy(group: StandingGroup): ZoneCopy {
  return leftZoneCopy(
    groupLayout(group),
    groupQualifyCount(group),
    group.rows.length,
    groupSplitAt(group),
    props.zoneTone || 'playoff',
  )
}

function groupRightCopy(group: StandingGroup): ZoneCopy {
  return rightZoneCopy(
    groupLayout(group),
    groupQualifyCount(group),
    group.rows.length,
    groupSplitAt(group),
    props.zoneTone || 'playoff',
  )
}

function groupLeftRows(group: StandingGroup): Standing[] {
  return splitLeftRows(group.rows, groupSplitAt(group))
}

function groupRightRows(group: StandingGroup): Standing[] {
  return splitRightRows(group.rows, groupSplitAt(group))
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
  formMap.value = {}
  formReady.value = false
  try {
    const schedulePromise = divisionId
      ? getV4PublicDivisionSchedule(divisionId).catch(() => ({ data: [] }))
      : Promise.resolve({ data: [] })
    if (stageId) {
      const [res, scheduleRes] = await Promise.all([
        getV4PublicStageStandings(stageId),
        schedulePromise,
      ])
      if (current !== reqId) return
      formMap.value = buildTeamRecentFormMap(scheduleRes.data ?? [], { stageId, limit: 5 })
      formReady.value = true
      const scopes = res.data ?? []
      const mapped = scopes
        .map((scope, index) => ({
          key: String(scope.groupScopeKey ?? scope.groupId ?? index),
          title: groupTitle(scope, index, scopes.length),
          qualifyCount: resolveQualifyCount(scope.qualifyCount, props.advanceCount) || null,
          rows: mapRows(scope.rows ?? []),
        }))
        .filter((g) => g.rows.length)
      if (mapped.length) {
        groups.value = mapped
        state.value = 'success'
        return
      }
    }
    const [res, scheduleRes] = await Promise.all([
      getV4PublicStanding(divisionId),
      schedulePromise,
    ])
    if (current !== reqId) return
    formMap.value = buildTeamRecentFormMap(scheduleRes.data ?? [], { stageId, limit: 5 })
    formReady.value = true
    const rows = mapRows(res.data ?? [])
    groups.value = rows.length
      ? [{
          key: 'all',
          title: props.stageName ? `${props.stageName}积分榜` : '积分榜',
          qualifyCount: resolveQualifyCount(null, props.advanceCount) || null,
          rows,
        }]
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
  () => [ctx.divisionId.value, props.stageId, props.grouped, props.advanceCount] as const,
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
    <p v-else-if="state === 'empty'" class="standings-note">{{ emptyText || '暂无积分榜数据' }}</p>

    <div v-else class="standings-groups">
      <div v-for="group in groups" :key="group.key" class="standings-group">
        <h3 v-if="groups.length > 1 || grouped" class="standings-group__title">{{ group.title }}</h3>
        <div
          class="standings-grid"
          :class="groupRightRows(group).length ? 'standings-grid--dual' : 'standings-grid--single'"
        >
          <div
            class="standings-panel standings-panel--lead"
            :class="groupLayout(group) === 'cutoff' ? 'standings-panel--qualify' : 'standings-panel--neutral'"
          >
            <div class="standings-panel__head">
              <h3>{{ groupLeftCopy(group).title }}</h3>
              <em v-if="groupLeftCopy(group).en">{{ groupLeftCopy(group).en }}</em>
              <span v-if="groupLeftCopy(group).hint">{{ groupLeftCopy(group).hint }}</span>
            </div>
            <div class="table-wrap" :class="{ 'table-wrap--form': formReady }">
              <div class="table-head">
                <span>排名</span>
                <span>战队</span>
                <div class="stats">
                  <span>胜</span><span>负</span><span>积分</span><span>净胜分</span>
                </div>
                <span v-if="formReady" class="form-head">近况</span>
              </div>
              <div v-for="row in groupLeftRows(group)" :key="`${group.key}-${row.teamId}`" class="table-row">
                <span class="rank rank-playoff">{{ row.rank }}</span>
                <TeamEntryLink
                  v-if="getTeamById(row.teamId)"
                  :team="getTeamById(row.teamId)!"
                  class="team"
                >
                  <TeamLogo :team="getTeamById(row.teamId)!" size="sm" />
                  <span class="team-name">{{ getTeamById(row.teamId)!.name }}</span>
                  <em v-if="row.disqualified" class="dq-tag">取消资格</em>
                </TeamEntryLink>
                <span v-else class="team"><span class="team-name">{{ row.teamName }}</span></span>
                <div class="stats">
                  <span class="num">{{ row.wins }}</span>
                  <span class="num">{{ row.losses }}</span>
                  <span class="num points">{{ row.standingPoints }}</span>
                  <span class="num diff">{{ formatPointDiff(row.pointDiff) }}</span>
                </div>
                <div v-if="formReady" class="form">
                  <span v-if="!rowForm(row).length" class="form-empty">—</span>
                  <i
                    v-for="(mark, index) in rowForm(row)"
                    :key="`${row.teamId}-l-${index}`"
                    class="form-dot"
                    :class="mark === 'W' ? 'form-dot--w' : 'form-dot--l'"
                  >{{ mark }}</i>
                </div>
              </div>
            </div>
          </div>

          <div
            v-if="groupRightRows(group).length"
            class="standings-panel standings-panel--rest"
          >
            <div class="standings-panel__head">
              <h3>{{ groupRightCopy(group).title }}</h3>
              <em v-if="groupRightCopy(group).en">{{ groupRightCopy(group).en }}</em>
              <span aria-hidden="true">&nbsp;</span>
            </div>
            <div class="table-wrap" :class="{ 'table-wrap--form': formReady }">
              <div class="table-head">
                <span>排名</span>
                <span>战队</span>
                <div class="stats">
                  <span>胜</span><span>负</span><span>积分</span><span>净胜分</span>
                </div>
                <span v-if="formReady" class="form-head">近况</span>
              </div>
              <div v-for="row in groupRightRows(group)" :key="`${group.key}-r-${row.teamId}`" class="table-row">
                <span class="rank rank-regular">{{ row.rank }}</span>
                <TeamEntryLink
                  v-if="getTeamById(row.teamId)"
                  :team="getTeamById(row.teamId)!"
                  class="team"
                >
                  <TeamLogo :team="getTeamById(row.teamId)!" size="sm" />
                  <span class="team-name">{{ getTeamById(row.teamId)!.name }}</span>
                  <em v-if="row.disqualified" class="dq-tag">取消资格</em>
                </TeamEntryLink>
                <span v-else class="team"><span class="team-name">{{ row.teamName }}</span></span>
                <div class="stats">
                  <span class="num">{{ row.wins }}</span>
                  <span class="num">{{ row.losses }}</span>
                  <span class="num points">{{ row.standingPoints }}</span>
                  <span class="num diff">{{ formatPointDiff(row.pointDiff) }}</span>
                </div>
                <div v-if="formReady" class="form">
                  <span v-if="!rowForm(row).length" class="form-empty">—</span>
                  <i
                    v-for="(mark, index) in rowForm(row)"
                    :key="`${row.teamId}-r-${index}`"
                    class="form-dot"
                    :class="mark === 'W' ? 'form-dot--w' : 'form-dot--l'"
                  >{{ mark }}</i>
                </div>
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
  padding-top: 2px;
}

.standings-groups {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.standings-group__title {
  margin: 0 0 10px;
  color: #111;
  font-size: 18px;
  font-weight: 900;
}

.standings-grid {
  display: grid;
  gap: 16px;
  align-items: stretch;

  &--dual {
    grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  }

  &--single {
    grid-template-columns: minmax(0, 1fr);
    max-width: 720px;
  }
}

.standings-panel {
  display: flex;
  flex-direction: column;
  min-width: 0;
  overflow: hidden;
  border: 1px solid rgba(17, 17, 17, 0.1);
  border-radius: 2px;
  background: #faf8f6;
  box-shadow: 0 2px 10px rgba(17, 17, 17, 0.05);
}

.standings-panel--lead .standings-panel__head h3 {
  color: var(--cnpa-red, #e10613);
}

.standings-panel--qualify {
  box-shadow:
    inset 0 3px 0 0 var(--cnpa-red, #e10613),
    inset 2px 0 0 0 var(--cnpa-red, #e10613),
    0 2px 10px rgba(17, 17, 17, 0.05);

  .standings-panel__head em {
    color: rgba(225, 6, 19, 0.55);
  }

  .standings-panel__head span {
    color: rgba(225, 6, 19, 0.72);
  }
}

.standings-panel__head {
  display: flex;
  flex-direction: column;
  justify-content: flex-start;
  min-height: 72px;
  padding: 12px 14px 10px;
  border-bottom: 1px solid rgba(17, 17, 17, 0.08);

  h3 {
    margin: 0;
    font-size: 14px;
    font-weight: 800;
    line-height: 1.3;
    color: #111;
  }

  em {
    display: block;
    margin-top: 2px;
    color: rgba(17, 17, 17, 0.38);
    font-size: 10px;
    font-weight: 700;
    font-style: italic;
    letter-spacing: 0.08em;
  }

  span {
    display: block;
    margin-top: 4px;
    color: rgba(17, 17, 17, 0.45);
    font-size: 11px;
    font-weight: 600;
    line-height: 1.3;
  }
}

.standings-panel--rest .standings-panel__head h3 {
  color: rgba(17, 17, 17, 0.78);
}

.table-wrap {
  width: 100%;
  flex: 1;
  overflow-x: auto;
}

.table-head,
.table-row {
  display: grid;
  grid-template-columns: 36px minmax(108px, 1fr) auto;
  column-gap: 8px;
  align-items: center;
  justify-content: start;
  padding: 0 12px 0 10px;
  min-width: 420px;
}

.table-wrap--form .table-head,
.table-wrap--form .table-row {
  grid-template-columns: 36px minmax(108px, 1fr) auto 128px;
  min-width: 560px;
}

.stats {
  display: grid;
  grid-template-columns: 22px 22px 30px 42px;
  column-gap: 6px;
  align-items: center;
  justify-items: center;
}

.table-head {
  min-height: 38px;
  background: #161616;
  color: #f4f1ec;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.04em;
}

.table-row {
  min-height: 52px;
  border-top: 1px solid rgba(17, 17, 17, 0.08);
  font-size: 13px;
  color: #111;

  &:first-of-type {
    border-top: 0;
  }
}

.rank,
.num {
  text-align: center;
  font-variant-numeric: tabular-nums;
}

.rank {
  display: grid;
  place-items: center;
  width: 24px;
  height: 24px;
  border-radius: 2px;
  font-size: 12px;
  font-weight: 800;
  line-height: 1;
}

.rank-playoff {
  background: var(--cnpa-red, #e10613);
  color: #fff;
}

.rank-regular {
  background: #2b2b2b;
  color: #f4f1ec;
}

.points {
  color: var(--cnpa-red, #e10613);
  font-weight: 700;
}

.diff {
  letter-spacing: 0;
}

.team {
  display: flex;
  align-items: center;
  min-width: 0;
  overflow: hidden;
}

.team-name {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 14px;
  font-weight: 800;
}

:deep(.team-entry-link) {
  display: flex;
  align-items: center;
  width: 100%;
  min-width: 0;
  gap: 10px;
}

:deep(.team-logo) {
  flex: none;
}

:deep(.team-logo-sm) {
  width: 32px;
  height: 32px;
}

.form {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 4px;
  min-width: 0;
}

.form-head {
  text-align: right;
}

.form-empty {
  color: rgba(17, 17, 17, 0.35);
  font-size: 13px;
  font-weight: 700;
}

.form-dot {
  display: grid;
  place-items: center;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  font-size: 9px;
  font-weight: 800;
  font-style: normal;
  line-height: 1;
  color: #fff;
}

.form-dot--w {
  background: var(--cnpa-red, #e10613);
}

.form-dot--l {
  background: #2b2b2b;
}

.dq-tag {
  flex: none;
  margin-left: 2px;
  font-style: normal;
  font-size: 11px;
  color: #f87171;
  border: 1px solid rgba(248, 113, 113, 0.45);
  border-radius: 2px;
  padding: 0 4px;
}

.standings-note {
  margin: 16px 0 0;
  color: rgba(17, 17, 17, 0.5);
  font-size: 13px;
}

@media (max-width: 1100px) {
  .standings-grid--dual {
    grid-template-columns: minmax(0, 1fr);
  }
}

@media (max-width: 640px) {
  .table-head,
  .table-row {
    min-width: 420px;
    column-gap: 6px;
    padding: 0 10px 0 8px;
  }

  .table-wrap--form .table-head,
  .table-wrap--form .table-row {
    min-width: 540px;
  }

  .stats {
    column-gap: 4px;
  }
}
</style>
