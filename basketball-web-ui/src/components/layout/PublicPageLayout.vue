<script setup lang="ts">
import PaperTear from '@/components/common/PaperTear.vue'
</script>

<template>
  <div class="public-page">
    <div v-if="$slots.hero" class="public-page__hero">
      <slot name="hero" />
    </div>

    <section class="public-page__main">
      <div class="site-shell">
        <div class="public-page__panel">
          <div v-if="$slots.hero" class="public-page__panel-tear" aria-hidden="true">
            <PaperTear position="top" tone="light" />
          </div>

          <div class="public-page__panel-shell">
            <div class="public-page__panel-body">
              <slot />
            </div>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>
