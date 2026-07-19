<script setup lang="ts">
defineProps<{
  direction: 'ltr' | 'rtl'
}>()
</script>

<template>
  <div
    class="bracket-connector bracket-connector--merge"
    :class="`bracket-connector--${direction}`"
    aria-hidden="true"
  >
    <div class="bracket-connector__row bracket-connector__row--top">
      <span class="bracket-connector__horiz bracket-connector__horiz--in" />
      <span class="bracket-connector__vert bracket-connector__vert--down" />
    </div>

    <div class="bracket-connector__row bracket-connector__row--mid">
      <span class="bracket-connector__vert bracket-connector__vert--through" />
      <span class="bracket-connector__horiz bracket-connector__horiz--out" />
    </div>

    <div class="bracket-connector__row bracket-connector__row--bottom">
      <span class="bracket-connector__vert bracket-connector__vert--up" />
      <span class="bracket-connector__horiz bracket-connector__horiz--in" />
    </div>
  </div>
</template>

<style scoped lang="scss">
.bracket-connector--merge {
  --merge-rail: 55%;
  display: grid;
  grid-template-rows: subgrid;
  grid-row: 1 / -1;
  height: 100%;
  min-height: 0;
}

.bracket-connector__row {
  position: relative;
  min-width: 0;
}

.bracket-connector__row--top {
  grid-row: 1;
}

.bracket-connector__row--mid {
  grid-row: 2;
}

.bracket-connector__row--bottom {
  grid-row: 3;
}

.bracket-connector__horiz,
.bracket-connector__vert {
  position: absolute;
  background: var(--bracket-line-color, rgba(225, 6, 19, 0.78));
}

.bracket-connector__horiz {
  height: var(--bracket-line-width, 1px);
}

.bracket-connector__vert {
  width: var(--bracket-line-width, 1px);
}

.bracket-connector--ltr {
  .bracket-connector__horiz--in {
    top: 50%;
    left: 0;
    width: var(--merge-rail);
    transform: translateY(-50%);
  }

  .bracket-connector__horiz--out {
    top: 50%;
    left: var(--merge-rail);
    right: 0;
    transform: translateY(-50%);
  }

  .bracket-connector__vert--down {
    top: 50%;
    left: var(--merge-rail);
    bottom: 0;
    transform: translateX(-50%);
  }

  .bracket-connector__vert--through {
    top: 0;
    bottom: 0;
    left: var(--merge-rail);
    transform: translateX(-50%);
  }

  .bracket-connector__vert--up {
    top: 0;
    left: var(--merge-rail);
    height: 50%;
    transform: translateX(-50%);
  }
}

.bracket-connector--rtl {
  .bracket-connector__horiz--in {
    top: 50%;
    right: 0;
    width: var(--merge-rail);
    transform: translateY(-50%);
  }

  .bracket-connector__horiz--out {
    top: 50%;
    right: var(--merge-rail);
    left: 0;
    transform: translateY(-50%);
  }

  .bracket-connector__vert--down {
    top: 50%;
    right: var(--merge-rail);
    bottom: 0;
    transform: translateX(50%);
  }

  .bracket-connector__vert--through {
    top: 0;
    bottom: 0;
    right: var(--merge-rail);
    transform: translateX(50%);
  }

  .bracket-connector__vert--up {
    top: 0;
    right: var(--merge-rail);
    height: 50%;
    transform: translateX(50%);
  }
}
</style>
