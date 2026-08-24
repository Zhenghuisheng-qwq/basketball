<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { getPublicPlayerMatchPage } from '@/api/publicPlayer'
import { mapPublicPlayerMatch } from '../mapPlayerDetail'
import PlayerMatchTable from './PlayerMatchTable.vue'
import type { PlayerDetailData, PlayerMatchRecord } from '../types'

const props = defineProps<{
  detail: PlayerDetailData
  playerId: string
}>()

const pageSize = 10
const currentPage = ref(1)
const totalCount = ref(0)
const seasonFilter = ref('全部赛季')
const tournamentFilter = ref('全部赛事')
const teamFilter = ref('全部战队')
const resultFilter = ref('全部结果')
const pageMatches = ref<PlayerMatchRecord[]>([])
const loadState = ref<'loading' | 'success' | 'empty' | 'error'>('loading')
const loadError = ref('')
let reqId = 0

const seasonOptions = computed(() => {
  const years = Array.from(
    new Set(
      props.detail.allMatches
        .map((item) => item.date.slice(0, 4))
        .filter(Boolean),
    ),
  )
  return ['全部赛季', ...years.map((year) => `${year}赛季`)]
})

const tournamentOptions = computed(() => [
  '全部赛事',
  ...Array.from(new Set(pageMatches.value.map((item) => item.tournamentName))),
])

const teamOptions = computed(() => [
  '全部战队',
  ...Array.from(new Set(pageMatches.value.map((item) => item.teamName))),
])

const resultOptions = ['全部结果', '胜利', '失败']

const filteredMatches = computed(() => {
  return pageMatches.value.filter((row) => {
    if (seasonFilter.value !== '全部赛季' && !row.date.startsWith(seasonFilter.value.slice(0, 4))) {
      return false
    }
    if (tournamentFilter.value !== '全部赛事' && row.tournamentName !== tournamentFilter.value) return false
    if (teamFilter.value !== '全部战队' && row.teamName !== teamFilter.value) return false
    if (resultFilter.value === '胜利' && row.result !== 'WIN') return false
    if (resultFilter.value === '失败' && row.result !== 'LOSS') return false
    return true
  })
})

const totalPages = computed(() => Math.max(1, Math.ceil(totalCount.value / pageSize)))

async function loadMatches() {
  const current = ++reqId
  loadState.value = 'loading'
  loadError.value = ''
  try {
    const res = await getPublicPlayerMatchPage(props.playerId, {
      currentPage: currentPage.value,
      pageSize,
    })
    if (current !== reqId) return
    const page = res.data
    pageMatches.value = (page?.records ?? []).map((item) =>
      mapPublicPlayerMatch(item, props.detail.profile.currentTeamId, props.detail.profile.currentTeamName),
    )
    totalCount.value = page?.total ?? pageMatches.value.length
    loadState.value = pageMatches.value.length ? 'success' : 'empty'
  } catch (err) {
    if (current !== reqId) return
    pageMatches.value = []
    totalCount.value = 0
    loadState.value = 'error'
    loadError.value = err instanceof Error ? err.message : '加载比赛失败'
  }
}

watch(
  () => props.playerId,
  () => {
    currentPage.value = 1
    void loadMatches()
  },
  { immediate: true },
)

watch(currentPage, () => {
  void loadMatches()
})

watch([seasonFilter, tournamentFilter, teamFilter, resultFilter], () => {
  currentPage.value = 1
})

function goPage(page: number) {
  if (page < 1 || page > totalPages.value) return
  currentPage.value = page
}
</script>

<template>
  <div class="player-detail-matches">
    <div class="player-detail-section-head">
      <div class="player-detail-section-head__group">
        <h2 class="player-detail-section-head__title">比赛记录</h2>
        <span class="player-detail-section-head__sub">Match History</span>
      </div>
    </div>

    <div class="player-detail-filter-row">
      <select v-model="seasonFilter">
        <option v-for="item in seasonOptions" :key="item" :value="item">{{ item }}</option>
      </select>
      <select v-model="tournamentFilter">
        <option v-for="item in tournamentOptions" :key="item" :value="item">{{ item }}</option>
      </select>
      <select v-model="teamFilter">
        <option v-for="item in teamOptions" :key="item" :value="item">{{ item }}</option>
      </select>
      <select v-model="resultFilter">
        <option v-for="item in resultOptions" :key="item" :value="item">{{ item }}</option>
      </select>
    </div>

    <div v-if="loadState === 'loading'" class="player-detail-empty-inline">加载中...</div>
    <div v-else-if="loadState === 'error'" class="player-detail-empty-inline is-error">
      {{ loadError || '加载比赛失败' }}
    </div>
    <div v-else-if="filteredMatches.length === 0" class="player-detail-empty-inline">暂无比赛记录</div>

    <template v-else>
      <PlayerMatchTable :rows="filteredMatches" />

      <div class="player-detail-pagination">
        <button
          type="button"
          class="player-detail-pagination__btn"
          :disabled="currentPage <= 1"
          @click="goPage(currentPage - 1)"
        >
          ‹
        </button>
        <button
          v-for="page in totalPages"
          :key="page"
          type="button"
          class="player-detail-pagination__btn"
          :class="{ 'is-active': page === currentPage }"
          @click="goPage(page)"
        >
          {{ page }}
        </button>
        <button
          type="button"
          class="player-detail-pagination__btn"
          :disabled="currentPage >= totalPages"
          @click="goPage(currentPage + 1)"
        >
          ›
        </button>
      </div>
    </template>
  </div>
</template>

<style scoped lang="scss">
@use '../styles/player-detail.scss';

.player-detail-empty-inline {
  padding: 32px 16px;
  text-align: center;
  color: var(--cnpa-paper-text-muted);
  font-size: 14px;

  &.is-error {
    color: #e10613;
  }
}

.player-detail-matches {
  padding: clamp(22px, 2.6vw, 38px);
  border: 1px solid var(--cnpa-public-border);
  background: var(--cnpa-public-card-bg);
  box-shadow: var(--cnpa-public-shadow-soft);
  box-sizing: border-box;
}

@media (max-width: 767px) {
  .player-detail-matches {
    padding: 20px 16px;
  }
}
</style>
