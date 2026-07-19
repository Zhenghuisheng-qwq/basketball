<script setup lang="ts">
import { computed } from 'vue'
import { standings, playoffCutoffRank } from '../mock'
import { getTeamById } from '../mock/teams.mock'
import TeamEntryLink from '@/components/team/TeamEntryLink.vue'
import TeamLogo from './TeamLogo.vue'

const playoffZone = computed(() => standings.filter((item) => item.rank <= playoffCutoffRank))
const regularZone = computed(() => standings.filter((item) => item.rank > playoffCutoffRank))

function formatWinRate(value: number) {
  return `${value.toFixed(1)}%`
}

function formatPointDiff(value: number) {
  return value > 0 ? `+${value}` : `${value}`
}
</script>

<template>
  <section class="regular-standings">
    <div class="standings-grid">
      <div class="standings-panel">
        <h3>季后赛区（前8名晋级季后赛）</h3>
        <div class="table-wrap">
          <div class="table-head">
            <span>排名</span><span>战队</span><span>胜</span><span>负</span><span>胜率</span><span>净胜分</span><span>近况</span>
          </div>
          <div v-for="row in playoffZone" :key="row.rank" class="table-row">
            <span class="rank rank-playoff">{{ row.rank }}</span>
            <TeamEntryLink
              v-if="getTeamById(row.teamId)"
              :team="getTeamById(row.teamId)!"
              class="team"
            >
              <TeamLogo :team="getTeamById(row.teamId)!" size="sm" />
              {{ getTeamById(row.teamId)!.name }}
            </TeamEntryLink>
            <span>{{ row.wins }}</span>
            <span>{{ row.losses }}</span>
            <span>{{ formatWinRate(row.winRate) }}</span>
            <span>{{ formatPointDiff(row.pointDiff) }}</span>
            <span class="recent">
              <i v-for="(item, index) in row.recent" :key="index" :class="item === 'W' ? 'win' : 'loss'" />
            </span>
          </div>
        </div>
      </div>

      <div class="standings-panel">
        <h3>非季后赛区（9名及以后）</h3>
        <div class="table-wrap">
          <div class="table-head">
            <span>排名</span><span>战队</span><span>胜</span><span>负</span><span>胜率</span><span>净胜分</span><span>近况</span>
          </div>
          <div v-for="row in regularZone" :key="row.rank" class="table-row">
            <span class="rank rank-regular">{{ row.rank }}</span>
            <TeamEntryLink
              v-if="getTeamById(row.teamId)"
              :team="getTeamById(row.teamId)!"
              class="team"
            >
              <TeamLogo :team="getTeamById(row.teamId)!" size="sm" />
              {{ getTeamById(row.teamId)!.name }}
            </TeamEntryLink>
            <span>{{ row.wins }}</span>
            <span>{{ row.losses }}</span>
            <span>{{ formatWinRate(row.winRate) }}</span>
            <span>{{ formatPointDiff(row.pointDiff) }}</span>
            <span class="recent">
              <i v-for="(item, index) in row.recent" :key="index" :class="item === 'W' ? 'win' : 'loss'" />
            </span>
          </div>
        </div>
      </div>
    </div>
    <p class="standings-note">常规赛前8名战队晋级季后赛。</p>
  </section>
</template>

<style scoped lang="scss">
.regular-standings {
  padding-top: 8px;
}

.standings-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.standings-panel {
  border: 1px solid rgba(255, 255, 255, 0.1);
  background: #111;

  h3 {
    margin: 0;
    padding: 16px;
    font-size: 14px;
    border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  }
}

.table-wrap {
  width: 100%;
}

.table-head,
.table-row {
  display: grid;
  grid-template-columns: 52px 1.4fr repeat(4, 64px) 88px;
  gap: 8px;
  align-items: center;
  padding: 0 12px;
}

.table-head {
  min-height: 42px;
  font-size: 12px;
  color: var(--cnpa-text-secondary);
}

.table-row {
  min-height: 52px;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
  font-size: 13px;
}

.rank {
  display: grid;
  place-items: center;
  width: 32px;
  height: 28px;
  font-weight: 900;
}

.rank-playoff {
  background: var(--cnpa-red);
}

.rank-regular {
  background: #333;
}

.team {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 800;
}

.recent {
  display: flex;
  gap: 4px;
}

.recent i {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}

.recent .win {
  background: var(--cnpa-red);
}

.recent .loss {
  background: #444;
}

.standings-note {
  margin: 18px 0 0;
  font-size: 13px;
  color: var(--cnpa-text-secondary);
}

@media (max-width: 992px) {
  .standings-grid {
    grid-template-columns: 1fr;
  }

  .table-head,
  .table-row {
    grid-template-columns: 44px 1.2fr repeat(4, 48px) 72px;
    font-size: 11px;
  }
}
</style>
