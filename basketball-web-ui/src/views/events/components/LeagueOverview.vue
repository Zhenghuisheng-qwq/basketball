<script setup lang="ts">
import SeasonLeaders from './SeasonLeaders.vue'
import LeagueTeams from './LeagueTeams.vue'
import CompetitionFormat from './CompetitionFormat.vue'
import LeagueAbout from './LeagueAbout.vue'
import CommunitySection from './CommunitySection.vue'
</script>
<template>
  <div class="league-overview">
    <SeasonLeaders />
    <LeagueTeams />
    <CompetitionFormat current-stage="regular" />
    <LeagueAbout />
    <CommunitySection />
  </div>
</template>