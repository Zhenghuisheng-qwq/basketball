<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import HonorPlaque from '@/components/honor/HonorPlaque.vue'
import type { PlayerDetailData, PlayerHonor } from '../types'

const props = defineProps<{
  detail: PlayerDetailData
}>()

const router = useRouter()
const { t } = useI18n()

const honors = computed(() => {
  const seasonId = props.detail.profile.seasonId
  if (!seasonId) return props.detail.honors
  const scoped = props.detail.honors.filter((item) => !item.seasonId || item.seasonId === seasonId)
  return scoped
})

function honorTone(honor: PlayerHonor): 'champion' | 'runner' | 'third' | 'award' {
  const code = (honor.honorCode || '').toUpperCase()
  if (code === 'CHAMPION') return 'champion'
  if (code === 'RUNNER_UP') return 'runner'
  if (code === 'THIRD_PLACE') return 'third'
  return 'award'
}

function honorMark(honor: PlayerHonor): 'trophy' | 'shield' | 'star' {
  const code = (honor.honorCode || '').toUpperCase()
  if (code === 'WEEKLY_BEST') return 'star'
  if (code === 'ALL_TOURNAMENT' || code === 'ALL_DEFENSIVE' || code === 'MVP') return 'shield'
  return 'trophy'
}

function honorDate(honor: PlayerHonor): string {
  if (honor.honorDate) return honor.honorDate
  return /^\d{4}/.test(honor.honorValue || '') ? honor.honorValue || '' : ''
}

function openHonor(tournamentId?: string) {
  if (!tournamentId) return
  router.push({ path: '/hall-of-fame', query: { tab: 'event', tournamentId } })
}
</script>

<template>
  <section class="player-honors" :aria-label="t('publicPages.player.honors')">
    <header class="player-honors__head">
      <h2>{{ t('publicPages.player.honors') }}</h2>
      <p>{{ t('publicPages.player.honorCount', { count: honors.length }) }}</p>
    </header>
    <div v-if="!honors.length" class="player-detail-empty-inline">{{ t('publicPages.player.noHonors') }}</div>
    <div v-else class="player-honors__list">
      <HonorPlaque
        v-for="honor in honors"
        :key="honor.id"
        :season="honor.seasonName"
        :title="honor.honorName"
        :date="honorDate(honor)"
        :tone="honorTone(honor)"
        :mark="honorMark(honor)"
        :icon-url="honor.imageUrl"
        :clickable="Boolean(honor.tournamentId)"
        @select="openHonor(honor.tournamentId)"
      />
    </div>
    <p class="player-honors__hint">{{ t('publicPages.player.honorsDesc') }}</p>
  </section>
</template>

<style scoped lang="scss">
.player-honors__head {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: 12px;
  margin-bottom: 16px;

  h2 {
    margin: 0;
    font-size: 20px;
  }

  p {
    margin: 0;
    color: #888;
    font-size: 12px;
  }
}

.player-honors__list {
  display: grid;
  gap: 12px;
}

.player-honors__hint {
  margin: 14px 0 0;
  color: #8b8985;
  font-size: 12px;
}
</style>
