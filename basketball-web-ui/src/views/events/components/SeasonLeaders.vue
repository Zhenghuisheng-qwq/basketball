<script setup lang="ts">
import { computed, inject, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { getV4PublicPlayerStat, getV4PublicTeamList } from '@/api/v4Public'
import type { V4PublicPlayerStatItem } from '@/api/types/v4Public'
import { formatDecimal, initials, toNumber } from '@/utils/publicDisplay'
import { calculateAverage } from '@/utils/statAverage'
import TeamEntryLink from '@/components/team/TeamEntryLink.vue'
import type { PlayerLeader } from '../types/events'
import type { usePublicSeasonContext } from '../composables/usePublicSeasonContext'
import { standingToTeam } from '../utils/mapPublic'
import { buildTeamLookup } from '../utils/teamLookup'
import TeamLogo from './TeamLogo.vue'

type SeasonContext = ReturnType<typeof usePublicSeasonContext>

type LeaderRoleKey = 'points' | 'assists' | 'rebounds'

type LeaderView = PlayerLeader & {
  roleKey: LeaderRoleKey
  roleEn: string
}

const LEADER_CONFIG = [
  {
    key: 'avgPoints' as const,
    roleKey: 'points' as const,
    roleEn: 'POINTS LEADER',
    unit: 'PPG' as const,
    totalKey: 'points' as const,
  },
  {
    key: 'avgAssist' as const,
    roleKey: 'assists' as const,
    roleEn: 'ASSISTS LEADER',
    unit: 'APG' as const,
    totalKey: 'assist' as const,
  },
  {
    key: 'avgRebound' as const,
    roleKey: 'rebounds' as const,
    roleEn: 'REBOUNDS LEADER',
    unit: 'RPG' as const,
    totalKey: 'rebound' as const,
  },
]

function playerAverageValue(
  item: V4PublicPlayerStatItem,
  avgKey: 'avgPoints' | 'avgAssist' | 'avgRebound',
  totalKey: 'points' | 'assist' | 'rebound',
): number {
  if (item[avgKey] != null) {
    return toNumber(item[avgKey], 0)
  }
  return calculateAverage(item[totalKey], item.matchCount)
}

function pickTotalLeader(
  rows: V4PublicPlayerStatItem[],
  avgKey: 'avgPoints' | 'avgAssist' | 'avgRebound',
  totalKey: 'points' | 'assist' | 'rebound',
): V4PublicPlayerStatItem | null {
  let best: V4PublicPlayerStatItem | null = null
  let bestTotal = Number.NEGATIVE_INFINITY
  let bestAverage = Number.NEGATIVE_INFINITY
  for (const row of rows) {
    const games = toNumber(row.matchCount, 0)
    if (games <= 0) continue
    const total = toNumber(row[totalKey], 0)
    const average = playerAverageValue(row, avgKey, totalKey)
    if (total > bestTotal || (total === bestTotal && average > bestAverage)) {
      best = row
      bestTotal = total
      bestAverage = average
    }
  }
  return best
}

function readAvatarUrl(item: V4PublicPlayerStatItem): string {
  const extra = item as V4PublicPlayerStatItem & { avatarUrl?: string | null }
  return extra.avatarUrl?.trim() || ''
}

const ctx = inject<SeasonContext>('publicSeasonContext')!
const { t } = useI18n()

const seasonLeaders = ref<LeaderView[]>([])
const state = ref<'loading' | 'success' | 'empty' | 'error'>('loading')
const errorMessage = ref('')
const imageErrors = ref<Record<number, boolean>>({})
let reqId = 0

function handlePortraitError(id: number) {
  imageErrors.value[id] = true
}

function hasPortrait(leader: LeaderView) {
  return Boolean(leader.portrait?.trim()) && !imageErrors.value[leader.id]
}

async function load(divisionId: string) {
  const current = ++reqId
  if (!divisionId) {
    seasonLeaders.value = []
    state.value = 'empty'
    return
  }
  state.value = 'loading'
  errorMessage.value = ''
  try {
    const [res, teamRes] = await Promise.all([
      getV4PublicPlayerStat(divisionId),
      getV4PublicTeamList(divisionId),
    ])
    const rows = res.data ?? []
    const logoByTeamId = new Map(
      (teamRes.data ?? []).map((team) => [String(team.eventTeamId || ''), team.teamLogoUrl?.trim() || '']),
    )
    const results = LEADER_CONFIG.map((config, index) => {
      const leader = pickTotalLeader(rows, config.key, config.totalKey)
      if (!leader) return null
      const teamId = String(leader.eventTeamId || '')
      return {
        id: index + 1,
        title: config.roleKey,
        value: Number(formatDecimal(playerAverageValue(leader, config.key, config.totalKey), 1)),
        unit: config.unit,
        playerName: leader.playerName || '--',
        teamId,
        teamName: leader.teamName || '',
        teamLogo: logoByTeamId.get(teamId) || '',
        portrait: readAvatarUrl(leader),
        roleKey: config.roleKey,
        roleEn: config.roleEn,
      } satisfies LeaderView
    })
    if (current !== reqId) return
    seasonLeaders.value = results.filter((item) => item !== null) as LeaderView[]
    state.value = seasonLeaders.value.length ? 'success' : 'empty'
  } catch (err) {
    if (current !== reqId) return
    seasonLeaders.value = []
    state.value = 'error'
    errorMessage.value = err instanceof Error ? err.message : ''
  }
}

const teamLookup = computed(() => {
  const teams = seasonLeaders.value.map((leader) =>
    standingToTeam({
      teamId: leader.teamId,
      teamName: leader.teamName,
      teamLogoUrl: leader.teamLogo,
    }),
  )
  return buildTeamLookup(teams)
})

function getTeamById(id: number | string) {
  return teamLookup.value(id)
}

function roleTitle(leader: LeaderView) {
  return t(`eventsPage.leaders.${leader.roleKey}.title`)
}

function metricLabel(leader: LeaderView) {
  return t(`eventsPage.leaders.${leader.roleKey}.metric`)
}

watch(
  () => ctx.divisionId.value,
  (divisionId) => {
    void load(divisionId)
  },
  { immediate: true },
)
</script>

<template>
  <section class="season-leaders">
    <div class="season-leaders__inner">
      <div class="season-leaders__header">
        <div class="section-head-group">
          <h2 class="cnpa-paper-section__title">{{ t('eventsPage.leaders.title') }}</h2>
          <span class="cnpa-paper-section__subtitle">Season Leaders</span>
        </div>
      </div>

      <div v-if="state === 'loading'" class="season-leaders__state">{{ t('common.loading') }}</div>
      <div v-else-if="state === 'error'" class="season-leaders__state season-leaders__state--error">
        {{ errorMessage || t('eventsPage.leaders.loadFailed') }}
      </div>
      <div v-else-if="state === 'empty'" class="season-leaders__state">{{ t('eventsPage.leaders.empty') }}</div>
      <div v-else class="season-leaders__grid">
        <article v-for="leader in seasonLeaders" :key="leader.id" class="leader-card">
          <div class="leader-card__avatar">
            <img
              v-if="hasPortrait(leader)"
              :src="leader.portrait"
              :alt="leader.playerName"
              @error="handlePortraitError(leader.id)"
            />
            <span v-else class="leader-card__initials">{{ initials(leader.playerName) }}</span>
          </div>

          <div class="leader-card__body">
            <header class="leader-card__role">
              <h3>{{ roleTitle(leader) }}</h3>
              <small>{{ leader.roleEn }}</small>
            </header>

            <div class="leader-card__stat sport-num">
              <strong>{{ leader.value }}</strong>
              <span>{{ leader.unit }}</span>
            </div>

            <p class="leader-card__name">{{ leader.playerName }}</p>

            <TeamEntryLink
              v-if="getTeamById(leader.teamId)"
              :team="getTeamById(leader.teamId)!"
              class="leader-card__team"
            >
              <TeamLogo :team="getTeamById(leader.teamId)!" size="md" />
              <span>{{ getTeamById(leader.teamId)!.name }}</span>
            </TeamEntryLink>
            <div v-else class="leader-card__team leader-card__team--plain">
              <span>{{ leader.teamName || '--' }}</span>
            </div>

            <p class="leader-card__metric">{{ metricLabel(leader) }}</p>
          </div>
        </article>
      </div>
    </div>
  </section>
</template>

<style scoped lang="scss">
.season-leaders {
  margin-top: 0;
}

.season-leaders__inner {
  padding-block: clamp(28px, 2.4vw, 42px) clamp(32px, 2.8vw, 48px);
}

.season-leaders__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 22px;
}

.season-leaders__grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
}

.season-leaders__state {
  padding: 24px 0 8px;
  color: rgba(17, 17, 17, 0.55);
  font-size: 13px;
  font-weight: 700;
  text-align: center;
}

.season-leaders__state--error {
  color: var(--cnpa-red);
}

.leader-card {
  display: grid;
  grid-template-columns: minmax(104px, 38%) minmax(0, 1fr);
  min-height: 0;
  overflow: hidden;
  border: 1px solid rgba(17, 17, 17, 0.08);
  border-radius: 6px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.94) 0%, #f6f4f2 100%),
    repeating-linear-gradient(
      -18deg,
      rgba(17, 17, 17, 0.018) 0,
      rgba(17, 17, 17, 0.018) 1px,
      transparent 1px,
      transparent 10px
    );
  box-shadow: inset 0 3px 0 var(--cnpa-red);
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;

  &:hover {
    transform: translateY(-2px);
    border-color: rgba(225, 6, 19, 0.28);
    box-shadow:
      inset 0 3px 0 var(--cnpa-red),
      0 10px 24px rgba(17, 17, 17, 0.08);
  }
}

.leader-card__avatar {
  position: relative;
  display: grid;
  place-items: center;
  min-height: 100%;
  background:
    linear-gradient(180deg, rgba(225, 6, 19, 0.08) 0%, transparent 42%),
    linear-gradient(180deg, #f3f0ec 0%, #e8e4de 100%);

  &::after {
    content: '';
    position: absolute;
    inset: auto 0 0;
    height: 3px;
    background: linear-gradient(90deg, transparent, rgba(225, 6, 19, 0.35), transparent);
  }

  img {
    width: clamp(100px, 18vw, 150px);
    height: clamp(100px, 18vw, 150px);
    max-width: 86%;
    object-fit: cover;
    object-position: top center;
  }
}

.leader-card__initials {
  display: grid;
  place-items: center;
  width: clamp(100px, 18vw, 150px);
  height: clamp(100px, 18vw, 150px);
  max-width: 86%;
  border: 1px solid rgba(17, 17, 17, 0.08);
  background: #efece8;
  color: #111;
  font-size: clamp(28px, 3vw, 40px);
  font-weight: 800;
  letter-spacing: 0.04em;
}

.leader-card__body {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 18px 16px 16px;
}

.leader-card__role {
  h3 {
    margin: 0;
    font-size: 15px;
    font-weight: 800;
    letter-spacing: 0.04em;
    color: #111;
  }

  small {
    display: block;
    margin-top: 2px;
    color: var(--cnpa-red);
    font-size: 10px;
    font-weight: 800;
    font-style: italic;
    letter-spacing: 0.14em;
  }
}

.leader-card__stat {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  line-height: 1;

  strong {
    font-size: clamp(42px, 4.4vw, 56px);
    font-weight: 900;
    color: var(--cnpa-red);
  }

  span {
    padding-bottom: 6px;
    color: rgba(17, 17, 17, 0.72);
    font-size: clamp(12px, 1.2vw, 16px);
    font-weight: 800;
    letter-spacing: 0.08em;
  }
}

.leader-card__name {
  margin: 2px 0 0;
  overflow: hidden;
  color: #111;
  font-size: clamp(18px, 1.6vw, 22px);
  font-weight: 800;
  line-height: 1.2;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.leader-card__team {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  width: fit-content;
  max-width: 100%;
  min-height: 44px;
  padding: 4px 12px 4px 4px;
  border-radius: 4px;
  background: rgba(17, 17, 17, 0.045);
  color: rgba(17, 17, 17, 0.78);
  text-decoration: none;

  span {
    overflow: hidden;
    font-size: 13px;
    font-weight: 700;
    white-space: nowrap;
    text-overflow: ellipsis;
  }

  :deep(.team-logo-md) {
    width: clamp(32px, 3.2vw, 44px);
    height: clamp(32px, 3.2vw, 44px);
  }
}

.leader-card__team--plain {
  min-height: 36px;
}

.leader-card__metric {
  margin: auto 0 0;
  color: rgba(17, 17, 17, 0.48);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.06em;
}

@media (max-width: 1100px) {
  .leader-card__stat strong {
    font-size: clamp(36px, 4vw, 48px);
  }
}

@media (max-width: 900px) {
  .season-leaders__grid {
    grid-template-columns: 1fr;
  }

  .leader-card {
    grid-template-columns: minmax(120px, 32%) minmax(0, 1fr);
  }
}

@media (max-width: 768px) {
  .season-leaders__header {
    align-items: flex-start;
    flex-direction: column;
  }

  .season-leaders__grid {
    grid-template-columns: 1fr;
  }

  .leader-card {
    grid-template-columns: minmax(104px, 36%) minmax(0, 1fr);
  }
}
</style>
