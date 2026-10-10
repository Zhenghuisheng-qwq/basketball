<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'

defineProps<{
  teamId: string
}>()

const route = useRoute()
const { t } = useI18n()
const keepQuery = () => {
  const query: Record<string, string> = {}
  if (route.query.divisionId) query.divisionId = String(route.query.divisionId)
  if (route.query.seasonId) query.seasonId = String(route.query.seasonId)
  return query
}

const tabs = computed(() => [
  { label: t('publicPages.team.tabOverview'), path: 'overview' },
  { label: t('publicPages.team.tabMatches'), path: 'matches' },
  { label: t('publicPages.team.tabData'), path: 'stats' },
  { label: t('publicPages.team.tabArchive'), path: 'archive' },
])
</script>

<template>
  <nav class="team-sub-nav" :aria-label="t('publicPages.team.navAria')">
    <router-link
      v-for="tab in tabs"
      :key="tab.path"
      :to="{ path: `/tournament/team/${teamId}/${tab.path}`, query: keepQuery() }"
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
      left: 12%;
      right: 12%;
      bottom: -1px;
      height: 4px;
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

.team-sub-nav {
  display: flex;
  width: 100%;
  overflow-x: auto;
  border-bottom: 1px solid var(--cnpa-public-border);
  scrollbar-width: none;
}

.team-sub-nav::-webkit-scrollbar {
  display: none;
}

.team-sub-nav__link {
  min-height: 52px;
  letter-spacing: 0;
}

.team-sub-nav__link:focus-visible {
  color: var(--cnpa-red);
  outline: 2px solid rgba(225, 6, 19, 0.35);
  outline-offset: -2px;
}
</style>
