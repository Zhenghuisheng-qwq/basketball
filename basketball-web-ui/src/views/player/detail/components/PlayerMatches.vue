<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import PlayerMatchTable from './PlayerMatchTable.vue'
import type { PlayerDetailData } from '../types'

const props = defineProps<{
  detail: PlayerDetailData
  playerId: string
}>()

const pageSize = 10
const currentPage = ref(1)
const resultFilter = ref<'all' | 'WIN' | 'LOSS'>('all')
const { t } = useI18n()

const resultOptions = computed(() => [
  { value: 'all' as const, label: t('publicPages.player.allResults') },
  { value: 'WIN' as const, label: t('publicPages.player.win') },
  { value: 'LOSS' as const, label: t('publicPages.player.loss') },
])
const scopeLabel = computed(() => {
  const profile = props.detail.profile
  const season = String(profile.seasonName || '').trim()
  const division = String(profile.divisionName || '').trim()
  const platform = String(profile.platform || '').trim()
  const parts: string[] = []
  if (season) parts.push(season)
  if (division) parts.push(division)
  else if (platform && platform !== '--') parts.push(platform)
  const unique: string[] = []
  for (const part of parts) {
    const covered = unique.some((item) => item.includes(part) || part.includes(item))
    if (!covered) unique.push(part)
  }
  return unique.join(' · ') || t('publicPages.player.currentScope')
})

const filteredMatches = computed(() => {
  return props.detail.allMatches.filter((row) => {
    if (resultFilter.value !== 'all' && row.result !== resultFilter.value) return false
    return true
  })
})

const totalPages = computed(() => Math.max(1, Math.ceil(filteredMatches.value.length / pageSize)))
const pageMatches = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return filteredMatches.value.slice(start, start + pageSize)
})

watch([resultFilter, () => props.detail.allMatches], () => {
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
        <h2 class="player-detail-section-head__title">{{ t('publicPages.player.matchHistory') }}</h2>
        <span class="player-detail-section-head__sub">Match History</span>
      </div>
    </div>

    <div class="player-detail-filter-row">
      <div class="player-detail-scope">
        <span>{{ t('publicPages.player.currentScope') }}</span>
        <strong>{{ scopeLabel }}</strong>
      </div>
      <select v-model="resultFilter">
        <option v-for="item in resultOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
      </select>
    </div>

    <div v-if="filteredMatches.length === 0" class="player-detail-empty-inline">{{ t('publicPages.player.noMatchRecords') }}</div>

    <template v-else>
      <PlayerMatchTable :rows="pageMatches" />

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
  padding: 8px 0 0;
  box-sizing: border-box;
}

.player-detail-scope {
  display: flex;
  flex: 1 1 auto;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
  color: var(--cnpa-paper-text-muted);
  font-size: 12px;
  font-weight: 700;
}

.player-detail-scope strong {
  overflow: hidden;
  color: var(--cnpa-paper-text);
  font-size: 13px;
  font-weight: 800;
  text-overflow: ellipsis;
  white-space: nowrap;
}

@media (max-width: 767px) {
  .player-detail-matches {
    padding: 20px 16px;
  }
}
</style>
