<script setup lang="ts">
defineProps<{
  teamId: string
}>()

const tabs = [
  { label: '总览', path: 'overview' },
  { label: '比赛', path: 'matches' },
  { label: '数据', path: 'stats' },
  { label: '动态', path: 'archive' },
] as const
</script>

<template>
  <nav class="team-sub-nav" aria-label="球队二级导航">
    <router-link
      v-for="tab in tabs"
      :key="tab.path"
      :to="`/tournament/team/${teamId}/${tab.path}`"
      class="team-sub-nav__link"
      active-class="team-sub-nav__link--active"
    >
      {{ tab.label }}
    </router-link>
  </nav>
</template>

<style scoped lang="scss">
.team-sub-nav {
  display: inline-flex;
  align-items: stretch;
  width: fit-content;
  max-width: 100%;
  min-height: 56px;
  margin: 0 0 clamp(4px, 0.6vw, 8px);
  background: transparent;
  box-sizing: border-box;
}

.team-sub-nav__link {
  position: relative;
  flex: 0 0 auto;
  min-width: clamp(96px, 9vw, 132px);
  padding: 0 clamp(24px, 2.2vw, 40px);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: rgba(17, 17, 17, 0.58);
  font-size: clamp(18px, 1.4vw, 20px);
  font-weight: 900;
  letter-spacing: 0.04em;
  transition: color 0.2s ease;

  &:hover {
    color: var(--cnpa-red);
  }

  &--active {
    color: var(--cnpa-red);

    &::after {
      content: '';
      position: absolute;
      left: 0;
      right: 0;
      bottom: 0;
      height: 3px;
      background: var(--cnpa-red);
    }
  }
}

@media (max-width: 480px) {
  .team-sub-nav {
    min-height: 46px;
  }

  .team-sub-nav__link {
    min-width: 80px;
    padding: 0 18px;
    font-size: 16px;
  }
}
</style>
