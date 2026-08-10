<script setup lang="ts">
import dataHeroBg from '@/assets/icon/data_hero_bg.png'
import type { TournamentGroup } from '../types'
import TournamentSelector from './TournamentSelector.vue'

defineProps<{
  tournamentId: string
  groups: TournamentGroup[]
  loading?: boolean
  error?: string
}>()

const emit = defineEmits<{
  'update:tournamentId': [value: string]
}>()

function handleTournamentChange(value: string) {
  emit('update:tournamentId', value)
}
</script>
<template>
  <section class="data-hero" aria-label="CNPA 数据中心">
    <div class="site-shell data-hero__inner">
      <div class="data-hero__visual">
        <img
          class="data-hero__image"
          :src="dataHeroBg"
          alt="CNPA 数据中心"
          draggable="false"
        />
        <div class="data-hero__selector">
          <TournamentSelector
            :model-value="tournamentId"
            :groups="groups"
            :loading="loading"
            :error="error"
            @update:model-value="handleTournamentChange"
          />
        </div>
      </div>
    </div>
  </section>
</template>
<style scoped lang="scss">
.data-hero {
  width: 100%;
  background: #000;
  overflow: visible;
}

.data-hero__inner {
  position: relative;
  overflow: visible;
}

.data-hero__visual {
  position: relative;
  width: 100%;
  overflow: visible;
  background: #000;
}

.data-hero__image {
  display: block;
  width: 100%;
  height: auto;
  user-select: none;
  -webkit-user-drag: none;
  pointer-events: none;
}

.data-hero__selector {
  position: absolute;
  top: clamp(18px, 3.2vw, 30px);
  right: clamp(12px, 2.4vw, 28px);
  z-index: 40;
  width: min(100%, 254px);
}

@media (max-width: 767px) {
  .data-hero__selector {
    position: absolute;
    top: 12px;
    right: 10px;
    left: auto;
    width: min(68%, 220px);
  }
}
</style>