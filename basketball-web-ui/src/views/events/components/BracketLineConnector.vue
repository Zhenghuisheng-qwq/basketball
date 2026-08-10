<script setup lang="ts">
withDefaults(
  defineProps<{
    direction?: 'ltr' | 'rtl'
  }>(),
  {
    direction: 'ltr',
  },
)
</script>
<template>
  <div
    class="bracket-connector bracket-connector--line"
    :class="`bracket-connector--${direction}`"
    aria-hidden="true"
  >
    <div class="bracket-connector__row bracket-connector__row--mid">
      <span class="bracket-connector__line" />
    </div>
  </div>
</template>
<style scoped lang="scss">
.bracket-connector--line {
  display: grid;
  grid-template-rows: subgrid;
  grid-row: 1 / -1;
  height: 100%;
  min-height: 0;
}

.bracket-connector__row--mid {
  grid-row: 2;
  position: relative;
}

.bracket-connector__line {
  position: absolute;
  top: 50%;
  left: 0;
  right: 0;
  height: var(--bracket-line-width, 1px);
  background: var(--bracket-line-color, rgba(225, 6, 19, 0.78));
  transform: translateY(-50%);
}
</style>