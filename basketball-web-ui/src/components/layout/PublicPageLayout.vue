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
        <div v-if="$slots.before" class="public-page__before">
          <slot name="before" />
        </div>

        <div class="public-page__panel">
          <div v-if="$slots.hero" class="public-page__panel-tear" aria-hidden="true">
            <PaperTear position="top" tone="light" />
          </div>

          <div class="public-page__panel-shell">
            <div class="public-page__panel-body">
              <slot />
            </div>
          </div>

          <div class="public-page__panel-edge-bottom" aria-hidden="true" />
        </div>
      </div>
    </section>

    <div class="public-page__red-tear" aria-hidden="true">
      <svg
        class="public-page__red-tear-svg"
        viewBox="0 0 1440 14"
        preserveAspectRatio="none"
        focusable="false"
      >
        <path
          d="M0 8.2
             C18 3.1 34 11.4 52 6.8
             C71 1.8 88 10.6 109 5.4
             C128 0.8 146 9.9 167 4.6
             C189 -0.4 208 10.2 231 6.1
             C252 2.4 271 11.8 295 7.2
             C318 2.7 338 12.1 362 6.4
             C384 1.2 404 10.8 428 5.9
             C451 1.4 471 11.6 496 7.0
             C519 2.8 540 12.4 565 6.6
             C588 1.1 609 10.9 634 5.2
             C656 0.4 677 11.2 702 6.8
             C725 2.9 746 12.6 771 7.4
             C794 2.5 815 11.0 840 5.7
             C862 1.0 883 10.4 908 6.2
             C931 2.3 952 12.0 977 7.1
             C999 2.6 1020 11.5 1045 5.8
             C1067 0.9 1088 10.7 1113 6.5
             C1135 2.7 1156 12.2 1181 7.3
             C1203 2.9 1224 11.1 1249 5.6
             C1271 0.7 1292 10.5 1317 6.3
             C1338 2.4 1358 11.8 1382 7.0
             C1401 3.2 1418 10.2 1440 6.1
             L1440 14 L0 14 Z"
        />
      </svg>
    </div>
  </div>
</template>
