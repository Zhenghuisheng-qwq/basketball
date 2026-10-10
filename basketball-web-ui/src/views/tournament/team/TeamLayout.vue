<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import TeamHero from '@/components/team/TeamHero.vue'
import TeamSubNav from '@/components/team/TeamSubNav.vue'
import { usePublicTeamPage } from './composables/usePublicTeamPage'
import './styles/team.scss'

const ctx = usePublicTeamPage()
const { t } = useI18n()
</script>

<template>
  <div class="team-page">
    <div class="site-shell team-page__shell">
      <div v-if="ctx.state.value === 'loading'" class="team-page__state">{{ t('publicPages.team.loading') }}</div>
      <div v-else-if="ctx.state.value === 'error'" class="team-page__state is-error">
        {{ ctx.errorMessage.value || t('publicPages.team.loadFailed') }}
        <button type="button" class="team-page__retry" @click="ctx.reload()">{{ t('publicPages.team.retry') }}</button>
      </div>
      <div v-else-if="ctx.state.value === 'empty' || !ctx.teamView.value" class="team-page__state">
        {{ t('publicPages.team.notPublic') }}
      </div>
      <template v-else>
        <TeamHero :team="ctx.teamView.value" :team-id="ctx.teamId.value" />

        <div class="team-page__paper">
          <div class="team-page__paper-body">
            <div class="site-shell-inner team-page__paper-inner">
              <TeamSubNav :team-id="ctx.teamId.value" />
              <div class="team-page__content">
                <router-view />
              </div>
            </div>
          </div>
        </div>
      </template>
    </div>
  </div>
</template>

<style scoped lang="scss">
.team-page__state {
  padding: 64px 16px;
  text-align: center;
  color: rgba(255, 255, 255, 0.65);
  font-weight: 700;

  &.is-error {
    color: var(--cnpa-red);
  }
}

.team-page__retry {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin-left: 12px;
  min-height: 40px;
  padding: 0 14px;
  border: 1px solid var(--cnpa-red);
  border-radius: 2px;
  background: var(--cnpa-red);
  color: #fff;
  font-weight: 700;
  cursor: pointer;
  transition: background 0.2s ease, border-color 0.2s ease;

  &:hover,
  &:focus-visible {
    border-color: var(--cnpa-red-dark);
    background: var(--cnpa-red-dark);
  }

  &:focus-visible {
    outline: 2px solid rgba(225, 6, 19, 0.4);
    outline-offset: 2px;
  }
}
</style>
