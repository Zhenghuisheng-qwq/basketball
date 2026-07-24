<script setup lang="ts">
import PaperTear from '@/components/common/PaperTear.vue'
import TeamHero from '@/components/team/TeamHero.vue'
import TeamSubNav from '@/components/team/TeamSubNav.vue'
import { usePublicTeamPage } from './composables/usePublicTeamPage'
import './styles/team.scss'

const ctx = usePublicTeamPage()
</script>

<template>
  <div class="team-page">
    <div class="site-shell team-page__shell">
      <div v-if="ctx.state.value === 'loading'" class="team-page__state">加载中...</div>
      <div v-else-if="ctx.state.value === 'error'" class="team-page__state is-error">
        {{ ctx.errorMessage.value || '加载球队失败' }}
        <button type="button" class="team-page__retry" @click="ctx.reload()">重试</button>
      </div>
      <div v-else-if="ctx.state.value === 'empty' || !ctx.teamView.value" class="team-page__state">
        球队不存在或未公开
      </div>
      <template v-else>
        <TeamHero :team="ctx.teamView.value" :team-id="ctx.teamId.value" />

        <div class="team-page__paper">
          <PaperTear position="top" tone="light" />
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
    color: #e10613;
  }
}

.team-page__retry {
  display: inline-block;
  margin-left: 12px;
  padding: 8px 14px;
  border: 0;
  border-radius: 4px;
  background: #e10613;
  color: #fff;
  font-weight: 700;
  cursor: pointer;
}
</style>
