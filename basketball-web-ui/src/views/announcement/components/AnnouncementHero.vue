<script setup lang="ts">
import ssggTopBackground from '@/assets/icon/ssgg/ssgg_top_background.png'

withDefaults(
  defineProps<{
    variant?: 'list' | 'detail'
  }>(),
  {
    variant: 'list',
  },
)
</script>

<template>
  <section
    class="announcement-hero-section"
    :aria-label="variant === 'detail' ? 'CNPA 公告详情' : 'CNPA 公告中心'"
  >
    <div class="site-shell">
      <div class="announcement-hero__frame cnpa-public-hero-frame">
        <img
          class="announcement-hero__image"
          :src="ssggTopBackground"
          alt="CNPA 公告中心 ANNOUNCEMENTS"
          draggable="false"
        />
      </div>
    </div>
  </section>
</template>

<style scoped lang="scss">
.announcement-hero-section {
  width: 100%;
  background: #000;
  overflow: hidden;
}

.announcement-hero__frame {
  background: #000;
}
</style>
