<script setup lang="ts">
import tournamentTop from '@/assets/icon/tournament_top.png'
</script>

<template>
  <section class="league-hero" aria-label="CNPA League">
    <!-- 后续接入赛事动态数据后，在此增加 LeagueMeta 组件 -->
    <img
      class="league-hero__image"
      :src="tournamentTop"
      alt="CNPA League 官方职业联赛"
    />
  </section>
</template>

<style scoped lang="scss">
.league-hero {
  position: relative;
  width: 100%;
  overflow: hidden;
  background: #050505;
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 2px 2px 0 0;
  box-sizing: border-box;
}

.league-hero__image {
  display: block;
  width: 100%;
  height: auto;
  aspect-ratio: 2172 / 724;
  object-fit: contain;
}
</style>
