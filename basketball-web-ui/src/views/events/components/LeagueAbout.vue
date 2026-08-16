<script setup lang="ts">
import { computed, inject } from 'vue'
import tournamentFoot from '@/assets/icon/tournament_foot.png'
import type { usePublicSeasonContext } from '../composables/usePublicSeasonContext'

const ctx = inject<ReturnType<typeof usePublicSeasonContext> | null>('publicSeasonContext', null)
const seriesLabel = computed(
  () => ctx?.currentTournament.value?.tournamentName || ctx?.divisionDetail.value?.seriesName || '赛事',
)
</script>
<template>
  <section class="league-about" :aria-label="`关于 ${seriesLabel}`">
    <div class="site-shell">
      <img
        class="league-about__image"
        :src="tournamentFoot"
        :alt="`关于 ${seriesLabel}`"
      />
    </div>
  </section>
</template>
<style scoped lang="scss">
.league-about {
  width: 100%;
  background: #050505;
  overflow: hidden;
}

.league-about__image {
  display: block;
  width: 100%;
  height: auto;
}
</style>