<template>
  <div class="public-paper-divider" aria-hidden="true" />
</template>

<style scoped lang="scss">
.public-paper-divider {
  position: relative;
  z-index: 3;
  flex-shrink: 0;
  width: 100%;
  height: clamp(18px, 2vw, 28px);
  margin-bottom: -1px;
  background:
    linear-gradient(175deg, transparent 46%, var(--paper-bg, var(--cnpa-paper-bg)) 46%),
    linear-gradient(185deg, transparent 52%, var(--paper-bg, var(--cnpa-paper-bg)) 52%);
  background-size: 100% 100%, 100% 100%;
  filter: drop-shadow(0 -2px 0 rgba(0, 0, 0, 0.12));
}
</style>
