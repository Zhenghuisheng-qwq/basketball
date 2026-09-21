<script setup lang="ts">
import mrtTopBackground from '@/assets/icon/mrt/mrt_top_background.png'
</script>

<template>
  <section class="hall-hero-section" aria-label="CNPA 名人堂">
    <div class="site-shell">
      <div class="hall-hero__frame cnpa-public-hero-frame">
        <img
          class="hall-hero__image"
          :src="mrtTopBackground"
          alt="CNPA 名人堂 HALL OF HONORS"
          draggable="false"
        />
      </div>
    </div>
  </section>
</template>

<style scoped lang="scss">
.hall-hero-section {
  width: 100%;
  background: #000;
  overflow: hidden;
}

.hall-hero__frame {
  background: #000;
}
</style>
