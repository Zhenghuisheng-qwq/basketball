<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import TeamLogo from '@/components/team/TeamLogo.vue'
import HallPanelHead from './HallPanelHead.vue'
import HallSelect from './HallSelect.vue'
import ChampionRoster from './ChampionRoster.vue'
import HallPlayerSilhouette from './HallPlayerSilhouette.vue'
import {
  getPublicHallHonorDetail,
  getPublicHallTournamentHonorPage,
} from '@/api/publicHallOfFame'
import { getV4PublicSeriesList } from '@/api/v4Public'
import type {
  PublicHonorDetailResp,
  PublicHonorItemResp,
  PublicTournamentItemVo,
} from '@/api/types/public'
import { getPublicPlayerSeasonStat } from '@/api/publicPlayer'
import type { PublicPlayerSeasonStatVo } from '@/api/types/public'
import { goToPlayerDetail } from '@/utils/playerNavigate'
import { displayText, formatDateTime, localizedText } from '@/utils/publicDisplay'
import type { ChampionRosterMember } from '../types'
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
const { t } = useI18n()

const tournaments = ref<PublicTournamentItemVo[]>([])
const honors = ref<PublicHonorItemResp[]>([])
const championDetail = ref<PublicHonorDetailResp | null>(null)
const state = ref<'loading' | 'success' | 'empty' | 'error'>('loading')
const errorMessage = ref('')
let tournamentReq = 0
let honorReq = 0

const tournamentOptions = computed(() =>
  tournaments.value.map((item) => ({
    value: item.tournamentId,
    label: displayText(item.tournamentName || item.shortName, t('publicPages.hall.publicEvent')),
  })),
)

const currentTournament = computed(
  () => tournaments.value.find((item) => item.tournamentId === props.tournamentId) ?? null,
)

const championHonor = computed(
  () =>
    honors.value.find((item) => (item.honorCode || '').toUpperCase() === 'CHAMPION') ??
    honors.value.find((item) => item.teamId) ??
    null,
)

const featuredGroups = computed(() => {
  const groups = [
    { key: 'MVP', title: t('publicPages.hall.mvp') },
    { key: 'ALL_TOURNAMENT', title: t('publicPages.hall.allTournament') },
    { key: 'ALL_DEFENSIVE', title: t('publicPages.hall.allDefensive') },
  ]
  return groups
    .map((group) => ({
      ...group,
      items: honors.value.filter((item) => (item.honorCode || '').toUpperCase() === group.key),
    }))
    .filter((group) => group.items.length > 0)
})

const featuredIds = computed(() => new Set(featuredGroups.value.flatMap((group) => group.items.map((item) => item.honorId))))

const otherHonors = computed(() =>
  honors.value.filter((item) => item.honorId !== championHonor.value?.honorId && !featuredIds.value.has(item.honorId)),
)

const championTeam = computed(() => {
  const record = championDetail.value || championHonor.value
  if (!record?.teamId && !record?.teamName) return null
  return {
    id: String(record.teamId || ''),
    name: displayText(record.teamName, 'CN'),
    shortName: displayText(record.teamName, 'CN'),
    logoUrl: record.teamLogoUrl || '',
  }
})

const mvpHonor = computed(() => featuredGroups.value.find((group) => group.key === 'MVP')?.items[0] ?? null)
const mvpStat = ref<PublicPlayerSeasonStatVo | null>(null)

const mvpStats = computed(() => {
  const row = mvpStat.value
  return [
    { key: 'pts', label: t('publicPages.player.avgPoints'), value: formatAvg(row?.avgPoints) },
    { key: 'reb', label: t('publicPages.player.avgRebounds'), value: formatAvg(row?.avgRebounds) },
    { key: 'ast', label: t('publicPages.player.avgAssists'), value: formatAvg(row?.avgAssists) },
    { key: 'stl', label: t('publicPages.team.avgSteals'), value: formatAvg(row?.avgSteals) },
    { key: 'fg', label: t('publicPages.player.fgPct'), value: formatPct(row?.fieldGoalPct) },
  ]
})

const rosterMembers = computed<ChampionRosterMember[]>(() => {
  const detail = championDetail.value
  if (!detail?.lineup?.length) return []
  return detail.lineup.map((item, index) => {
    const roleRaw = (item.roleType || '').toUpperCase()
    const role = roleRaw.includes('START') ? 'STARTER' : 'SUBSTITUTE'
    return {
      id: String(item.lineupId || `${item.playerId || index}`),
      tournamentId: String(detail.tournamentId || props.tournamentId),
      teamId: String(item.teamId || detail.teamId || ''),
      playerId: item.playerId || '',
      playerName: displayText(item.playerName, t('publicPages.hall.player')),
      playerAvatarUrl: item.avatarUrl || '',
      position: displayText(item.positionCode, '--'),
      role,
      captain: roleRaw.includes('CAPTAIN'),
      appearances: 0,
      sortOrder: item.sortNo ?? index,
    }
  })
})

async function loadTournaments() {
  const current = ++tournamentReq
  try {
    const res = await getV4PublicSeriesList()
    if (current !== tournamentReq) return
    const records: PublicTournamentItemVo[] = (res.data || []).map((item) => ({
      tournamentId: String(item.seriesId),
      tournamentName: item.seriesName,
      shortName: item.seriesCode || item.seriesName,
    }))
    tournaments.value = records
    if (!records.length) {
      state.value = 'empty'
      return
    }
    const exists = records.some((item) => item.tournamentId === props.tournamentId)
    if (!exists) {
      emit('update:tournamentId', records[0]!.tournamentId)
    }
  } catch (err) {
    if (current !== tournamentReq) return
    tournaments.value = []
    state.value = 'error'
    errorMessage.value = err instanceof Error ? err.message : t('publicPages.hall.loadEventsFailed')
  }
}

async function loadHonors(tournamentId: string) {
  const current = ++honorReq
  if (!tournamentId) {
    honors.value = []
    championDetail.value = null
    state.value = 'empty'
    return
  }
  state.value = 'loading'
  errorMessage.value = ''
  championDetail.value = null
  try {
    const res = await getPublicHallTournamentHonorPage({
      currentPage: 1,
      pageSize: 50,
      tournamentId,
      sortField: 'awardDate',
      sortOrder: 'desc',
    })
    if (current !== honorReq) return
    honors.value = res.data?.records ?? []
    if (!honors.value.length) {
      state.value = 'empty'
      return
    }
    state.value = 'success'
    const champion = honors.value.find((item) => (item.honorCode || '').toUpperCase() === 'CHAMPION')
    if (champion?.honorId) {
      try {
        const detailRes = await getPublicHallHonorDetail(champion.honorId)
        if (current !== honorReq) return
        championDetail.value = detailRes.data ?? null
      } catch {
        if (current !== honorReq) return
        championDetail.value = null
      }
    }
  } catch (err) {
    if (current !== honorReq) return
    honors.value = []
    state.value = 'error'
    errorMessage.value = err instanceof Error ? err.message : t('publicPages.hall.loadEventHonorsFailed')
  }
}

watch(
  () => props.tournamentId,
  (id) => {
    void loadHonors(id)
  },
)

watch(
  () => true,
  () => {
    void loadTournaments()
  },
  { immediate: true },
)

function updateTournament(value: string | number) {
  emit('update:tournamentId', String(value))
  emit('update:rosterExpanded', false)
}

function toggleRoster() {
  emit('update:rosterExpanded', !props.rosterExpanded)
}

function openTeam(teamId?: string | null) {
  if (!teamId) return
  router.push(`/tournament/team/${teamId}/overview`)
}

function openPlayer(playerId?: string | null) {
  if (!playerId) return
  goToPlayerDetail(router, playerId, 'overview')
}

function awardDateText(value?: string | null) {
  const dt = formatDateTime(value)
  return dt.date === '--' ? '--' : dt.date
}

function formatAvg(value?: number | string | null) {
  if (value == null || value === '') return '--'
  const num = Number(value)
  if (Number.isNaN(num)) return String(value)
  return Number.isInteger(num) ? String(num) : num.toFixed(1)
}

function formatPct(value?: number | string | null) {
  if (value == null || value === '') return '--'
  const num = Number(value)
  if (Number.isNaN(num)) return String(value)
  const pct = num <= 1 ? num * 100 : num
  return `${pct.toFixed(1)}%`
}

watch(mvpHonor, async (item) => {
  mvpStat.value = null
  if (!item?.playerId) return
  try {
    const res = await getPublicPlayerSeasonStat(item.playerId, { seasonId: item.seasonId || undefined })
    const rows = res.data ?? []
    mvpStat.value = rows.find((row) => String(row.seasonId || '') === String(item.seasonId || '')) || rows[0] || null
  } catch {
    mvpStat.value = null
  }
})
</script>

<template>
  <section class="event-honors-panel">
    <HallPanelHead :title="t('publicPages.hall.eventHonors')" subtitle="EVENT HONORS" layout="inline">
      <template #filters>
        <HallSelect
          :model-value="tournamentId || tournamentOptions[0]?.value || ''"
          :options="tournamentOptions"
          :disabled="!tournamentOptions.length"
          @update:model-value="updateTournament"
        />
      </template>
    </HallPanelHead>

    <div v-if="state === 'loading'" class="hall-empty">{{ t('publicPages.hall.loading') }}</div>
    <div v-else-if="state === 'error'" class="hall-empty">{{ errorMessage }}</div>
    <div v-else-if="state === 'empty'" class="hall-empty">{{ t('publicPages.hall.noEventHonors') }}</div>

    <template v-else>
      <article
        v-if="championHonor"
        class="event-champion-card"
        :style="championCardStyle"
      >
        <button
          type="button"
          class="event-champion-card__team"
          :disabled="!championHonor.eventTeamId"
          @click="openTeam(championHonor.eventTeamId)"
        >
          <p class="event-champion-card__label">
            {{ displayText(championHonor.honorName, t('publicPages.hall.champion')) }}
          </p>
          <span class="event-champion-card__label-en">CHAMPION</span>
          <span class="event-champion-card__logo">
            <TeamLogo v-if="championTeam" :team="championTeam" size="xl" />
          </span>
          <strong>{{ displayText(championHonor.teamName, t('publicPages.hall.championTeam')) }}</strong>
        </button>

        <div class="event-champion-card__info">
          <p class="event-champion-card__tournament">
            {{ displayText(championHonor.seasonName || currentTournament?.tournamentName || championHonor.tournamentName) }}
          </p>
          <p class="event-champion-card__desc">
            {{ localizedText(championHonor.honorSubtitle || championHonor.honorTitle, championHonor.honorSubtitleEn || championHonor.honorTitleEn, t('publicPages.hall.publishedHonor')) }}
          </p>
          <div class="event-champion-card__date-block">
            <span>{{ t('publicPages.hall.awardDate') }}</span>
            <strong>{{ awardDateText(championHonor.awardDate) }}</strong>
          </div>
          <button
            v-if="rosterMembers.length"
            type="button"
            class="event-champion-card__btn"
            @click="toggleRoster"
          >
            {{ rosterExpanded ? t('publicPages.hall.collapseRoster') : t('publicPages.hall.viewRoster') }}
            <span aria-hidden="true">›</span>
          </button>
        </div>

        <div class="event-champion-card__visual" aria-hidden="true" />
      </article>

      <ChampionRoster
        v-if="rosterExpanded && rosterMembers.length"
        :members="rosterMembers"
      />

      <template v-for="group in featuredGroups" :key="group.key">
        <section v-if="group.key === 'MVP'" class="event-mvp">
          <h3 class="event-other-honors__title">{{ group.title }}</h3>
          <article
            v-for="item in group.items"
            :key="item.honorId"
            class="event-mvp__card hall-card-surface"
          >
            <div class="event-mvp__photo">
              <HallPlayerSilhouette
                :name="displayText(item.playerName, t('publicPages.hall.player'))"
                :avatar-url="item.playerAvatarUrl || ''"
                size="mvp"
              />
            </div>
            <div class="event-mvp__body">
              <button type="button" class="event-mvp__name" :disabled="!item.playerId" @click="openPlayer(item.playerId)">
                {{ displayText(item.playerName, t('publicPages.hall.unknownPlayer')) }}
              </button>
              <p v-if="item.teamName" class="event-mvp__team">{{ displayText(item.teamName) }}</p>
              <p class="event-mvp__meta">{{ localizedText(item.seasonName, item.seasonNameEn, t('publicPages.hall.season')) }}</p>
              <dl class="event-mvp__stats">
                <div v-for="box in mvpStats" :key="box.key">
                  <dt>{{ box.label }}</dt>
                  <dd>{{ box.value }}</dd>
                </div>
              </dl>
            </div>
          </article>
        </section>
        <section v-else class="event-lineup">
          <h3 class="event-lineup__title">
            {{ group.title }}
            <span>{{ group.key === 'ALL_DEFENSIVE' ? 'ALL-DEFENSIVE TEAM' : 'ALL-TOURNAMENT TEAM' }}</span>
          </h3>
          <div class="event-lineup__grid">
            <article v-for="item in group.items" :key="item.honorId" class="event-lineup__card">
              <div class="event-lineup__photo">
                <HallPlayerSilhouette
                  :name="displayText(item.playerName, t('publicPages.hall.player'))"
                  :avatar-url="item.playerAvatarUrl || ''"
                  size="lineup"
                />
              </div>
              <button type="button" :disabled="!item.playerId" @click="openPlayer(item.playerId)">
                {{ displayText(item.playerName, t('publicPages.hall.unknownPlayer')) }}
              </button>
              <p>
                <TeamLogo
                  v-if="item.teamName || item.teamLogoUrl"
                  :team="{
                    id: String(item.eventTeamId || item.teamId || item.honorId),
                    name: displayText(item.teamName, 'CN'),
                    shortName: displayText(item.teamName, 'CN'),
                    logoUrl: item.teamLogoUrl || '',
                  }"
                  size="xs"
                />
                {{ displayText(item.teamName) }}
              </p>
            </article>
          </div>
        </section>
      </template>

      <div v-if="otherHonors.length" class="event-other-honors">
        <h3 class="event-other-honors__title">{{ t('publicPages.hall.otherHonors') }}</h3>
        <div class="event-other-honors__grid">
          <article
            v-for="item in otherHonors"
            :key="item.honorId"
            class="event-other-honor hall-card-surface"
          >
            <div v-if="item.playerId" class="event-other-honor__avatar">
              <HallPlayerSilhouette
                :name="displayText(item.playerName, t('publicPages.hall.player'))"
                :avatar-url="item.playerAvatarUrl || ''"
                size="roster"
              />
            </div>
            <div v-else-if="item.teamId" class="event-other-honor__logo">
              <TeamLogo
                :team="{
                  id: String(item.teamId),
                  name: displayText(item.teamName, 'CN'),
                  shortName: displayText(item.teamName, 'CN'),
                  logoUrl: item.teamLogoUrl || '',
                }"
                size="md"
              />
            </div>
            <div class="event-other-honor__body">
              <p class="event-other-honor__code">{{ displayText(item.honorName) }}</p>
              <button
                v-if="item.playerId"
                type="button"
                class="event-other-honor__name"
                @click="openPlayer(item.playerId)"
              >
                {{ displayText(item.playerName) }}
              </button>
              <button
                v-else-if="item.teamId"
                type="button"
                class="event-other-honor__name"
                @click="openTeam(item.eventTeamId)"
              >
                {{ displayText(item.teamName) }}
              </button>
              <p class="event-other-honor__meta">
                {{ localizedText(item.seasonName, item.seasonNameEn, t('publicPages.hall.season')) }} · {{ awardDateText(item.awardDate) }}
              </p>
            </div>
          </article>
        </div>
      </div>
    </template>
  </section>
</template>

<style scoped lang="scss">
.event-honors-panel {
  width: 100%;
}

.event-champion-card {
  display: grid;
  grid-template-columns: 220px minmax(0, 1fr) 180px;
  gap: 24px;
  min-height: 240px;
  padding: 28px 30px;
  border-radius: 2px;
  background-size: cover;
  background-position: center right;
  color: #fff;
}

.event-champion-card__team {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 10px;
  padding: 0;
  border: 0;
  background: transparent;
  color: inherit;
  text-align: left;
  cursor: pointer;

  &:disabled {
    cursor: default;
  }

  strong {
    font-size: 28px;
    font-weight: 900;
  }
}

.event-champion-card__label {
  margin: 0;
  font-size: 22px;
  font-weight: 900;
}

.event-champion-card__label-en {
  margin-top: -6px;
  color: var(--cnpa-red, #e10613);
  font-size: 16px;
  font-style: italic;
  font-weight: 800;
  letter-spacing: 0.08em;
}

.event-champion-card__logo {
  width: 132px;
  height: 132px;
}

.event-champion-card__info {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 10px;
}

.event-champion-card__tournament {
  margin: 0;
  font-size: clamp(26px, 2.4vw, 36px);
  font-weight: 900;
  line-height: 1.15;
}

.event-champion-card__desc {
  margin: 0;
  color: rgba(255, 255, 255, 0.78);
  font-size: 14px;
  line-height: 1.6;
}

.event-champion-card__date-block {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-top: 6px;

  span {
    color: rgba(255, 255, 255, 0.6);
    font-size: 12px;
  }

  strong {
    font-size: 18px;
  }
}

.event-champion-card__btn {
  align-self: flex-start;
  margin-top: 8px;
  padding: 10px 14px;
  border: 1px solid rgba(255, 255, 255, 0.45);
  background: rgba(0, 0, 0, 0.28);
  color: #fff;
  font: inherit;
  font-size: 13px;
  font-weight: 800;
  cursor: pointer;
}

.event-other-honors {
  margin-top: 28px;
}

.event-other-honors__title {
  margin: 0 0 14px;
  color: #111;
  font-size: 18px;
  font-weight: 900;
}

.event-other-honors__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.event-other-honor {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px 18px;
}

.event-other-honor__avatar,
.event-other-honor__logo {
  width: 64px;
  height: 64px;
  flex-shrink: 0;
}

.event-other-honor__code {
  margin: 0;
  color: var(--cnpa-red, #e10613);
  font-size: 12px;
  font-weight: 800;
}

.event-other-honor__name {
  display: block;
  margin: 6px 0 0;
  padding: 0;
  border: 0;
  background: transparent;
  color: #fff;
  font: inherit;
  font-size: 18px;
  font-weight: 900;
  text-align: left;
  cursor: pointer;

  &:hover {
    color: var(--cnpa-red, #e10613);
  }
}

.event-other-honor__meta {
  margin: 6px 0 0;
  color: rgba(255, 255, 255, 0.62);
  font-size: 13px;
}

.event-mvp {
  margin-top: 28px;
}

.event-mvp__card {
  display: grid;
  grid-template-columns: 220px minmax(0, 1fr);
  min-height: 220px;
  margin-top: 12px;
}

.event-mvp__photo {
  min-height: 220px;
}

.event-mvp__body {
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 24px;
}

.event-mvp__name {
  padding: 0;
  border: 0;
  background: transparent;
  color: #fff;
  font: inherit;
  font-size: clamp(28px, 3vw, 40px);
  font-weight: 900;
  text-align: left;
  cursor: pointer;

  &:disabled {
    cursor: default;
  }
}

.event-mvp__team,
.event-mvp__meta,
.event-mvp__value {
  margin: 8px 0 0;
  color: rgba(255, 255, 255, 0.72);
  font-size: 14px;
  font-weight: 700;
}

.event-mvp__stats {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 12px;
  margin: 18px 0 0;

  dt {
    color: rgba(255, 255, 255, 0.62);
    font-size: 12px;
    font-weight: 700;
  }

  dd {
    margin: 6px 0 0;
    color: var(--cnpa-red, #e10613);
    font-size: 28px;
    font-weight: 900;
    line-height: 1;
  }
}

.event-lineup {
  margin-top: 28px;
}

.event-lineup__title {
  display: flex;
  align-items: baseline;
  gap: 12px;
  margin: 0 0 14px;
  color: #111;
  font-size: 22px;
  font-weight: 900;

  span {
    color: var(--cnpa-red, #e10613);
    font-size: 13px;
    font-style: italic;
    letter-spacing: 0.06em;
  }
}

.event-lineup__grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 12px;
}

.event-lineup__card {
  background: #111;
  color: #fff;

  button,
  p {
    margin: 0;
  }

  button {
    display: block;
    width: 100%;
    padding: 12px 12px 0;
    border: 0;
    background: transparent;
    color: #fff;
    font: inherit;
    font-size: 16px;
    font-weight: 900;
    text-align: left;
    cursor: pointer;

    &:disabled {
      cursor: default;
    }
  }

  p {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 8px 12px 14px;
    color: rgba(255, 255, 255, 0.72);
    font-size: 12px;
    font-weight: 800;
  }
}

.event-lineup__photo {
  height: 168px;
}

@media (max-width: 1023px) {
  .event-champion-card {
    grid-template-columns: 1fr;
  }

  .event-other-honors__grid,
  .event-lineup__grid,
  .event-mvp__card {
    grid-template-columns: 1fr;
  }
}
</style>
