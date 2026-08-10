<script setup lang="ts">
import tournamentFoot from '@/assets/icon/tournament_foot.png'
</script>
<template>
  <section class="league-about" aria-label="关于 CNPA League">
    <div class="site-shell">
      <img
        class="league-about__image"
        :src="tournamentFoot"
        alt="关于 CNPA League"
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