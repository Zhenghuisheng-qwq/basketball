<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import MyEventTeamMatchesView from '@/views/account/events/MyEventTeamMatchesView.vue'

const route = useRoute()
const legacySeasonId = computed(() => String(route.params.tournamentId || ''))
</script>

<template>
  <MyEventTeamMatchesView :legacy-season-id="legacySeasonId" />
</template>
