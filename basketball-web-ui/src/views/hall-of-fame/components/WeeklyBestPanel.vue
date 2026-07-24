<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import TeamLogo from '@/components/team/TeamLogo.vue'
import HallPanelHead from './HallPanelHead.vue'
import HallPagination from './HallPagination.vue'
import HallPlayerSilhouette from './HallPlayerSilhouette.vue'
import { getPublicHallPlayerHonorPage } from '@/api/publicHallOfFame'
import type { PublicHonorItemResp } from '@/api/types/public'
import { goToPlayerDetail } from '@/utils/playerNavigate'
import { displayText, formatDateTime } from '@/utils/publicDisplay'

const props = defineProps<{
  page: number
}>()

const emit = defineEmits<{
  'update:page': [value: number]
}>()

const router = useRouter()
const pageSize = 8

const items = ref<PublicHonorItemResp[]>([])
const totalPages = ref(1)
const state = ref<'loading' | 'success' | 'empty' | 'error'>('loading')
const errorMessage = ref('')
let reqId = 0

async function load(page: number) {
  const current = ++reqId
  state.value = 'loading'
  errorMessage.value = ''
  try {
    const res = await getPublicHallPlayerHonorPage({
      currentPage: page,
      pageSize,
      sortField: 'awardDate',
      sortOrder: 'desc',
    })
    if (current !== reqId) return
    const data = res.data
    items.value = data?.records ?? []
    totalPages.value = Math.max(1, Number(data?.pages || 1))
    state.value = items.value.length ? 'success' : 'empty'
    if (page > totalPages.value) {
      emit('update:page', totalPages.value)
    }
  } catch (err) {
    if (current !== reqId) return
    items.value = []
    state.value = 'error'
    errorMessage.value = err instanceof Error ? err.message : '加载球员荣誉失败'
  }
}

watch(
  () => props.page,
  (page) => {
    void load(page)
  },
  { immediate: true },
)

function openPlayer(playerId?: string | null) {
  if (!playerId) return
  goToPlayerDetail(router, playerId, 'overview')
}

function openTeam(teamId?: string | null) {
  if (!teamId) return
  router.push(`/tournament/team/${teamId}/overview`)
}

function awardDateText(value?: string | null) {
  const dt = formatDateTime(value)
  return dt.date === '--' ? '--' : dt.date
}
</script>

<template>
  <section class="weekly-best-panel">
    <HallPanelHead title="球员荣誉" subtitle="Player Honors" />

    <div v-if="state === 'loading'" class="hall-empty">加载中...</div>
    <div v-else-if="state === 'error'" class="hall-empty">{{ errorMessage }}</div>
    <div v-else-if="state === 'empty'" class="hall-empty">暂无球员荣誉记录</div>

    <template v-else>
      <div class="player-honor-grid">
        <article
          v-for="item in items"
          :key="item.honorId"
          class="weekly-best-card hall-card-surface"
        >
          <div class="weekly-best-card__left">
            <div class="weekly-best-card__visual">
              <HallPlayerSilhouette
                :name="displayText(item.playerName, '球员')"
                :avatar-url="item.playerAvatarUrl || ''"
                size="feature"
              />
            </div>
          </div>

          <div class="weekly-best-card__body">
            <p class="weekly-best-card__label">{{ displayText(item.honorName, '球员荣誉') }}</p>
            <button
              type="button"
              class="weekly-best-card__name"
              :disabled="!item.playerId"
              @click="openPlayer(item.playerId)"
            >
              {{ displayText(item.playerName, '未知球员') }}
            </button>
            <button
              v-if="item.teamId || item.teamName"
              type="button"
              class="weekly-best-card__team"
              @click="openTeam(item.teamId)"
            >
              <span class="weekly-best-card__logo">
                <TeamLogo
                  :team="{
                    id: String(item.teamId || ''),
                    name: displayText(item.teamName),
                    shortName: displayText(item.teamName),
                    logoUrl: item.teamLogoUrl || '',
                  }"
                  size="sm"
                />
              </span>
              <span>{{ displayText(item.teamName) }}</span>
            </button>
            <p class="weekly-best-card__meta">
              {{ displayText(item.tournamentName, '公开赛事') }}
              ·
              {{ displayText(item.seasonName, '赛季') }}
            </p>
            <p class="weekly-best-card__date">授予日期 {{ awardDateText(item.awardDate) }}</p>
            <p v-if="item.honorValue" class="weekly-best-card__value">{{ item.honorValue }}</p>
          </div>
        </article>
      </div>

      <HallPagination
        :page="page"
        :total-pages="totalPages"
        @update:page="emit('update:page', $event)"
      />
    </template>
  </section>
</template>

<style scoped lang="scss">
.weekly-best-panel {
  width: 100%;
}

.player-honor-grid {
  display: grid;
  gap: 18px;
}

.weekly-best-card {
  display: grid;
  grid-template-columns: 180px minmax(0, 1fr);
  gap: 24px;
  align-items: center;
  padding: 22px 24px;
}

.weekly-best-card__label {
  margin: 0 0 8px;
  color: var(--cnpa-red, #e10613);
  font-size: 13px;
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.weekly-best-card__name {
  display: block;
  margin: 0;
  padding: 0;
  border: 0;
  background: transparent;
  color: #111;
  font: inherit;
  font-size: clamp(26px, 3vw, 34px);
  font-weight: 900;
  text-align: left;
  cursor: pointer;

  &:disabled {
    cursor: default;
  }

  &:not(:disabled):hover {
    color: var(--cnpa-red, #e10613);
  }
}

.weekly-best-card__team {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin-top: 10px;
  padding: 0;
  border: 0;
  background: transparent;
  color: #333;
  font: inherit;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;

  &:hover {
    color: var(--cnpa-red, #e10613);
  }
}

.weekly-best-card__logo {
  width: 28px;
  height: 28px;
}

.weekly-best-card__meta,
.weekly-best-card__date,
.weekly-best-card__value {
  margin: 8px 0 0;
  color: #666;
  font-size: 13px;
  font-weight: 600;
}

.weekly-best-card__value {
  color: #111;
  font-size: 15px;
  font-weight: 800;
}

@media (max-width: 767px) {
  .weekly-best-card {
    grid-template-columns: 1fr;
    padding: 18px 16px;
  }
}
</style>
