<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { getTeamById } from '@/mock/team'
import PaperTear from '@/components/common/PaperTear.vue'
import TeamHero from '@/components/team/TeamHero.vue'
import TeamSubNav from '@/components/team/TeamSubNav.vue'
import './styles/team.scss'

const route = useRoute()

const teamId = computed(() => route.params.teamId as string)
const team = computed(() => getTeamById(teamId.value))
</script>

<template>
  <div class="team-page">
    <div class="site-shell team-page__shell">
      <TeamHero :team="team" :team-id="teamId" />

      <div class="team-page__paper cnpa-paper-section cnpa-paper-section--light">
        <PaperTear position="top" tone="light" />
        <div class="team-page__paper-body">
          <div class="site-shell-inner team-page__paper-inner">
            <TeamSubNav :team-id="teamId" />
            <div class="team-page__content">
              <router-view />
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
