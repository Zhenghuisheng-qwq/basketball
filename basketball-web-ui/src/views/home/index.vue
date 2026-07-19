<script setup lang="ts">
import { useRouter } from 'vue-router'
import TeamBadge from '@/components/home/TeamBadge.vue'
import TornEdge from '@/components/home/TornEdge.vue'
import homeTop from '@/assets/icon/home_top.png'
import {
  mockMatches,
  mockTeams,
  mockTeamRanks,
  mockPlayerRanks,
  mockStats,
} from '@/mock/home'

const router = useRouter()

function handleMoreMatches() {
  router.push('/tournament')
}

function handleMoreTeams() {
  console.log('[Home] handleMoreTeams')
}

function handleWatchMatch(matchId: number) {
  console.log('[Home] handleWatchMatch', matchId)
}

function handleTeamClick(teamKey: string) {
  console.log('[Home] handleTeamClick', teamKey)
}
</script>

<template>
  <div class="home-page">
    <section class="home-hero-section">
      <div class="site-shell">
        <img class="hero-image" :src="homeTop" alt="CNPA 篮球赛事平台" />
      </div>
    </section>

    <section id="matches" class="match-section">
      <div class="site-shell">
        <div class="paper-band paper-band-light">
          <TornEdge position="top" tone="light" />

          <div class="site-shell-inner paper-band-inner">
            <div class="section-head section-head-dark">
              <h2>近期赛事</h2>
              <button type="button" class="section-more section-more-dark" @click="handleMoreMatches">
                查看全部赛事
                <span>›</span>
              </button>
            </div>
            <div class="match-grid">
              <article v-for="match in mockMatches" :key="match.id" class="match-card">
                <div class="match-card-header">
                  <span>{{ match.date }}</span>
                  <span>{{ match.round }}</span>
                </div>
                <div class="match-versus">
                  <div class="match-team">
                    <div class="match-logo">
                      <TeamBadge :type="match.homeTeam.key" />
                    </div>
                    <span class="match-team-name">{{ match.homeTeam.name }}</span>
                  </div>
                  <div class="match-vs">VS</div>
                  <div class="match-team">
                    <div class="match-logo">
                      <TeamBadge :type="match.awayTeam.key" />
                    </div>
                    <span class="match-team-name">{{ match.awayTeam.name }}</span>
                  </div>
                </div>
                <button type="button" class="watch-btn" @click="handleWatchMatch(match.id)">
                  预约观赛
                </button>
              </article>
            </div>
          </div>
        </div>
      </div>
    </section>

    <section id="teams" class="team-section">
      <div class="site-shell">
        <div class="paper-band paper-band-dark">
          <TornEdge position="top" tone="dark" />

          <div class="site-shell-inner paper-band-inner">
            <div class="section-head section-head-light">
              <h2>参赛战队</h2>
              <button type="button" class="section-more section-more-light" @click="handleMoreTeams">
                查看全部战队
                <span>›</span>
              </button>
            </div>
            <div class="team-grid">
              <article
                v-for="team in mockTeams"
                :key="team.key"
                class="team-card"
                @click="handleTeamClick(team.key)"
              >
                <div class="team-logo">
                  <TeamBadge :type="team.key" />
                </div>
                <span class="team-name">{{ team.name }}</span>
              </article>
            </div>
          </div>
        </div>
      </div>
    </section>

    <section class="about-paper-section">
      <div class="site-shell">
        <div class="paper-band paper-band-light">
          <TornEdge position="top" tone="light" />

          <div class="site-shell-inner paper-band-inner about-paper-inner">
            <div class="about-text">
              <h2>关于 CNPA <span>联赛体系</span></h2>
              <p>
                CNPA 中国职业电竞篮球联赛，致力于打造专业、公平、热血的线上篮球竞技舞台。
                通过全年多级赛事体系，推动电竞篮球运动的发展与普及。
              </p>
            </div>
            <div class="about-stats">
              <div v-for="item in mockStats" :key="item.label" class="stat-item">
                <strong>{{ item.value }}</strong>
                <span>{{ item.label }}</span>
              </div>
            </div>
          </div>

          <TornEdge position="bottom" tone="light" />
        </div>
      </div>
    </section>

    <section id="ranking" class="ranking-section">
      <div class="site-shell">
        <div class="site-shell-inner">
          <div class="section-head">
            <h2>数据榜单</h2>
          </div>
          <div class="ranking-panel">
            <div class="ranking-card">
              <h3>球队排行</h3>
              <table>
                <thead>
                  <tr>
                    <th>排名</th>
                    <th>战队</th>
                    <th>胜场</th>
                    <th>负场</th>
                    <th>胜率</th>
                    <th>连胜</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="row in mockTeamRanks" :key="row.rank">
                    <td class="rank-num">{{ row.rank }}</td>
                    <td>
                      <span class="team-name-cell">
                        <span class="tiny-logo">
                          <TeamBadge :type="row.teamKey" tiny />
                        </span>
                        {{ row.teamName }}
                      </span>
                    </td>
                    <td>{{ row.wins }}</td>
                    <td>{{ row.losses }}</td>
                    <td>{{ row.winRate }}</td>
                    <td>{{ row.streak }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
            <div class="ranking-card">
              <h3>球员得分榜 TOP5</h3>
              <div class="player-row head">
                <span>排名</span>
                <span>球员</span>
                <span>战队</span>
                <span>场均得分 PPG</span>
              </div>
              <div v-for="player in mockPlayerRanks" :key="player.rank" class="player-row">
                <span class="rank-num">{{ player.rank }}</span>
                <span class="player-name">
                  <i class="avatar" />
                  {{ player.name }}
                </span>
                <span>{{ player.team }}</span>
                <span>{{ player.ppg }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped lang="scss">
@use '@/assets/styles/home-page.scss';
</style>
