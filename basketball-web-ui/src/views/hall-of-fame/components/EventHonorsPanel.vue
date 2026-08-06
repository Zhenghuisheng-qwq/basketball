<script setup lang="ts">
import { computed, ref, watch } from 'vue'
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
import { goToPlayerDetail } from '@/utils/playerNavigate'
import { displayText, formatDateTime } from '@/utils/publicDisplay'
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
    label: displayText(item.tournamentName || item.shortName, '公开赛事'),
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

const otherHonors = computed(() =>
  honors.value.filter((item) => item.honorId !== championHonor.value?.honorId),
)

const championTeam = computed(() => {
  const record = championDetail.value || championHonor.value
  if (!record?.teamId && !record?.teamName) return null
  return {
    id: String(record.teamId || ''),
    name: displayText(record.teamName),
    shortName: displayText(record.teamName),
    logoUrl: record.teamLogoUrl || '',
  }
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
      playerName: displayText(item.playerName, '球员'),
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
    errorMessage.value = err instanceof Error ? err.message : '加载赛事失败'
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
    errorMessage.value = err instanceof Error ? err.message : '加载赛事荣誉失败'
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
</script>

<template>
  <section class="event-honors-panel">
    <HallPanelHead title="赛事荣誉" subtitle="EVENT HONORS" layout="inline">
      <template #filters>
        <HallSelect
          :model-value="tournamentId || tournamentOptions[0]?.value || ''"
          :options="tournamentOptions"
          :disabled="!tournamentOptions.length"
          @update:model-value="updateTournament"
        />
      </template>
    </HallPanelHead>

    <div v-if="state === 'loading'" class="hall-empty">加载中...</div>
    <div v-else-if="state === 'error'" class="hall-empty">{{ errorMessage }}</div>
    <div v-else-if="state === 'empty'" class="hall-empty">该赛事暂无已发布荣誉</div>

    <template v-else>
      <article
        v-if="championHonor"
        class="event-champion-card"
        :style="championCardStyle"
      >
        <button
          type="button"
          class="event-champion-card__team"
          :disabled="!championHonor.teamId"
          @click="openTeam(championHonor.teamId)"
        >
          <p class="event-champion-card__label">
            {{ displayText(championHonor.honorName, '赛事冠军') }}
          </p>
          <span class="event-champion-card__label-en">CHAMPION</span>
          <span class="event-champion-card__logo">
            <TeamLogo v-if="championTeam" :team="championTeam" size="xl" />
          </span>
          <strong>{{ displayText(championHonor.teamName, '冠军球队') }}</strong>
        </button>

        <div class="event-champion-card__info">
          <p class="event-champion-card__tournament">
            {{ displayText(currentTournament?.tournamentName || championHonor.tournamentName) }}
          </p>
          <p class="event-champion-card__desc">
            {{ displayText(championHonor.honorSubtitle || championHonor.honorTitle, '已发布赛事荣誉') }}
          </p>
          <div class="event-champion-card__date-block">
            <span>授予日期</span>
            <strong>{{ awardDateText(championHonor.awardDate) }}</strong>
          </div>
          <button
            v-if="rosterMembers.length"
            type="button"
            class="event-champion-card__btn"
            @click="toggleRoster"
          >
            {{ rosterExpanded ? '收起冠军阵容' : '查看冠军阵容' }}
            <span aria-hidden="true">›</span>
          </button>
        </div>

        <div class="event-champion-card__visual" aria-hidden="true" />
      </article>

      <ChampionRoster
        v-if="rosterExpanded && rosterMembers.length"
        :members="rosterMembers"
      />

      <div v-if="otherHonors.length" class="event-other-honors">
        <h3 class="event-other-honors__title">其他赛事荣誉</h3>
        <div class="event-other-honors__grid">
          <article
            v-for="item in otherHonors"
            :key="item.honorId"
            class="event-other-honor hall-card-surface"
          >
            <div v-if="item.playerId" class="event-other-honor__avatar">
              <HallPlayerSilhouette
                :name="displayText(item.playerName, '球员')"
                :avatar-url="item.playerAvatarUrl || ''"
                size="roster"
              />
            </div>
            <div v-else-if="item.teamId" class="event-other-honor__logo">
              <TeamLogo
                :team="{
                  id: String(item.teamId),
                  name: displayText(item.teamName),
                  shortName: displayText(item.teamName),
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
                @click="openTeam(item.teamId)"
              >
                {{ displayText(item.teamName) }}
              </button>
              <p class="event-other-honor__meta">
                {{ displayText(item.seasonName, '赛季') }} · {{ awardDateText(item.awardDate) }}
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
  font-size: 14px;
  font-weight: 800;
}

.event-champion-card__label-en {
  color: rgba(255, 255, 255, 0.65);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.12em;
}

.event-champion-card__logo {
  width: 88px;
  height: 88px;
}

.event-champion-card__info {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 10px;
}

.event-champion-card__tournament {
  margin: 0;
  font-size: 18px;
  font-weight: 800;
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
  color: #111;
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
  color: #666;
  font-size: 13px;
}

@media (max-width: 1023px) {
  .event-champion-card {
    grid-template-columns: 1fr;
  }

  .event-other-honors__grid {
    grid-template-columns: 1fr;
  }
}
</style>
