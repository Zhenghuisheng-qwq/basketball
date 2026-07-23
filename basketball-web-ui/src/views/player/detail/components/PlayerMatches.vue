<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import PlayerMatchTable from './PlayerMatchTable.vue'
import type { PlayerDetailData } from '../mock/playerDetail'

const props = defineProps<{
  detail: PlayerDetailData
}>()

const pageSize = 10
const currentPage = ref(1)
const seasonFilter = ref('全部赛季')
const tournamentFilter = ref('全部赛事')
const teamFilter = ref('全部战队')
const resultFilter = ref('全部结果')

const seasonOptions = ['全部赛季', '2026赛季', '2025赛季', '2024赛季']
const tournamentOptions = computed(() => [
  '全部赛事',
  ...Array.from(new Set(props.detail.allMatches.map((item) => item.tournamentName))),
])
const teamOptions = computed(() => [
  '全部战队',
  ...Array.from(new Set(props.detail.allMatches.map((item) => item.teamName))),
])
const resultOptions = ['全部结果', '胜利', '失败']

const filteredMatches = computed(() => {
  return props.detail.allMatches.filter((row) => {
    if (seasonFilter.value !== '全部赛季' && !row.date.startsWith(seasonFilter.value.slice(0, 4))) return false
    if (tournamentFilter.value !== '全部赛事' && row.tournamentName !== tournamentFilter.value) return false
    if (teamFilter.value !== '全部战队' && row.teamName !== teamFilter.value) return false
    if (resultFilter.value === '胜利' && row.result !== 'WIN') return false
    if (resultFilter.value === '失败' && row.result !== 'LOSS') return false
    return true
  })
})

const totalPages = computed(() => Math.max(1, Math.ceil(filteredMatches.value.length / pageSize)))

const pagedMatches = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return filteredMatches.value.slice(start, start + pageSize)
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

    <PlayerMatchTable :rows="pagedMatches" />

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
  </div>
</template>

<style scoped lang="scss">
@use '../styles/player-detail.scss';
</style>
