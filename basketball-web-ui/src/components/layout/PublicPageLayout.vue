<script setup lang="ts">
import PublicPaperDivider from './PublicPaperDivider.vue'
</script>

<template>
  <div class="public-page">
    <div class="public-page__main">
      <div v-if="$slots.hero" class="public-page__hero">
        <slot name="hero" />
      </div>
      <PublicPaperDivider v-if="$slots.hero" />
      <section class="public-paper-content">
        <div class="public-paper-content__inner">
          <slot />
        </div>
      </section>
    </div>
  </div>
</template>
