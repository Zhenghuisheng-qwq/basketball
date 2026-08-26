<script setup lang="ts">
import PaperSection from '@/components/common/PaperSection.vue'
import SeasonLeaders from './SeasonLeaders.vue'
import LeagueTeams from './LeagueTeams.vue'
import CompetitionFormat from './CompetitionFormat.vue'
import LeagueAbout from './LeagueAbout.vue'
</script>
<template>
  <div class="league-overview">
    <div class="site-shell">
      <PaperSection edge="none" tone="gray" class="league-overview__paper">
        <div class="league-overview__inner">
          <SeasonLeaders />
          <LeagueTeams />
          <CompetitionFormat embedded current-stage="regular" />
        </div>
      </PaperSection>
    </div>
    <LeagueAbout />
  </div>
</template>
<style scoped lang="scss">
.league-overview__paper {
  margin-top: 0;
}

.league-overview__inner {
  padding-inline: var(--cnpa-public-content-gutter);
}
</style>
