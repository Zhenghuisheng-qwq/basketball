<script setup lang="ts">
import SeasonLeaders from './SeasonLeaders.vue'
import LeagueTeams from './LeagueTeams.vue'
import CompetitionFormat from './CompetitionFormat.vue'
import LeagueAbout from './LeagueAbout.vue'
</script>
<template>
  <div class="league-overview">
    <SeasonLeaders />
    <LeagueTeams />
    <CompetitionFormat current-stage="regular" />
    <LeagueAbout />
  </div>
</template>
