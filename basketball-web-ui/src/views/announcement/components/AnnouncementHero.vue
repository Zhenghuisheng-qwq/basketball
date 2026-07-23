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
      <div class="announcement-hero__frame">
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
  width: 100%;
  height: clamp(300px, 32vw, 510px);
  overflow: hidden;
  background: #000;
}

.announcement-hero__image {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center;
  user-select: none;
  -webkit-user-drag: none;
}
</style>
