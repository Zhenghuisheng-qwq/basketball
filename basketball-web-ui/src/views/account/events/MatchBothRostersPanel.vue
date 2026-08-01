<script setup lang="ts">
import AccountStatusBadge from '@/components/account/AccountStatusBadge.vue'
import PlayerAvatar from '@/components/player/PlayerAvatar.vue'
import type { EventRosterMemberDto } from '@/api/types/eventParticipation'
import { formatPositionLabel } from '@/constants/rosterRules'

defineProps<{
  homeTeamName?: string
  awayTeamName?: string
  homeRoster?: EventRosterMemberDto[]
  awayRoster?: EventRosterMemberDto[]
  matchPlatform?: string
}>()

function isCaptain(member: EventRosterMemberDto) {
  return member.isCaptain === 1 || member.rosterRole?.toUpperCase() === 'CAPTAIN'
}

function formatRole(member: EventRosterMemberDto) {
  if (isCaptain(member)) return '赛事队长'
  return member.rosterRoleText || '队员'
}

function formatPosition(position?: string) {
  if (!position || position === '-') return '-'
  return formatPositionLabel(position)
}

function resolveGameId(member: EventRosterMemberDto, matchPlatform?: string) {
  const gameId = (member.gameId || '').trim()
  if (!gameId || gameId === '-' || gameId === '未绑定') return '-'
  const platform = (member.platform || '').trim()
  if (matchPlatform && platform && platform.toUpperCase() !== matchPlatform.toUpperCase()) {
    return gameId
  }
  if (/^(PS5?|PC|XBOX(?:\sONE)?)\s*[:：]/i.test(gameId)) {
    return gameId
  }
  if (platform && platform !== '-') {
    return `${platform}: ${gameId}`
  }
  return gameId
}

function displayName(member: EventRosterMemberDto) {
  return member.nickname || member.playerNo || '-'
}
</script>

<template>
  <div class="match-rosters-panel">
    <section class="match-rosters-panel__card">
      <header class="match-rosters-panel__head">
        <h3>主队阵容</h3>
        <span>{{ homeTeamName || '主队' }} · {{ homeRoster?.length || 0 }} 人</span>
      </header>
      <div v-if="!homeRoster?.length" class="match-rosters-panel__empty">暂无主队阵容</div>
      <div v-else class="match-rosters-panel__table-wrap">
        <table class="match-rosters-panel__table">
          <thead>
            <tr>
              <th>球员</th>
              <th>CNPA编号</th>
              <th>位置</th>
              <th>游戏 ID</th>
              <th>角色</th>
              <th>状态</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="member in homeRoster" :key="member.rosterId || member.playerId">
              <td>
                <div class="match-rosters-panel__player">
                  <PlayerAvatar
                    :name="displayName(member)"
                    :avatar-url="member.avatarUrl"
                    :player-no="member.playerNo"
                    :player-id="member.playerId"
                    size="sm"
                  />
                  <div>
                    <strong>{{ displayName(member) }}</strong>
                    <AccountStatusBadge v-if="isCaptain(member)" type="danger" text="队长" />
                  </div>
                </div>
              </td>
              <td>{{ member.playerNo || '-' }}</td>
              <td>{{ formatPosition(member.position) }}</td>
              <td class="match-rosters-panel__ellipsis" :title="resolveGameId(member, matchPlatform)">
                {{ resolveGameId(member, matchPlatform) }}
              </td>
              <td>{{ formatRole(member) }}</td>
              <td>{{ member.rosterStatusText || '生效中' }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <section class="match-rosters-panel__card">
      <header class="match-rosters-panel__head">
        <h3>客队阵容</h3>
        <span>{{ awayTeamName || '客队' }} · {{ awayRoster?.length || 0 }} 人</span>
      </header>
      <div v-if="!awayRoster?.length" class="match-rosters-panel__empty">暂无客队阵容</div>
      <div v-else class="match-rosters-panel__table-wrap">
        <table class="match-rosters-panel__table">
          <thead>
            <tr>
              <th>球员</th>
              <th>CNPA编号</th>
              <th>位置</th>
              <th>游戏 ID</th>
              <th>角色</th>
              <th>状态</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="member in awayRoster" :key="member.rosterId || member.playerId">
              <td>
                <div class="match-rosters-panel__player">
                  <PlayerAvatar
                    :name="displayName(member)"
                    :avatar-url="member.avatarUrl"
                    :player-no="member.playerNo"
                    :player-id="member.playerId"
                    size="sm"
                  />
                  <div>
                    <strong>{{ displayName(member) }}</strong>
                    <AccountStatusBadge v-if="isCaptain(member)" type="danger" text="队长" />
                  </div>
                </div>
              </td>
              <td>{{ member.playerNo || '-' }}</td>
              <td>{{ formatPosition(member.position) }}</td>
              <td class="match-rosters-panel__ellipsis" :title="resolveGameId(member, matchPlatform)">
                {{ resolveGameId(member, matchPlatform) }}
              </td>
              <td>{{ formatRole(member) }}</td>
              <td>{{ member.rosterStatusText || '生效中' }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
  </div>
</template>

<style scoped lang="scss">
.match-rosters-panel {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 20px;

  &__card {
    min-width: 0;
    padding: 16px;
    border: 1px solid rgba(0, 0, 0, 0.08);
    border-radius: 2px;
    background: #fff;
    box-shadow: 0 1px 0 rgba(0, 0, 0, 0.03);
  }

  &__head {
    display: flex;
    flex-wrap: wrap;
    justify-content: space-between;
    gap: 8px;
    margin-bottom: 12px;
    padding-bottom: 10px;
    border-bottom: 1px solid rgba(0, 0, 0, 0.06);

    h3 {
      margin: 0;
      font-size: 16px;
      font-weight: 800;
      color: #111;
    }

    span {
      font-size: 13px;
      color: #555;
    }
  }

  &__empty {
    padding: 32px 0;
    text-align: center;
    color: #666;
    font-size: 14px;
  }

  &__table-wrap {
    overflow-x: auto;
  }

  &__table {
    width: 100%;
    min-width: 520px;
    border-collapse: collapse;
    font-size: 13px;

    th,
    td {
      padding: 10px 8px;
      border-bottom: 1px solid rgba(0, 0, 0, 0.06);
      text-align: left;
      vertical-align: middle;
      color: #111;
    }

    th {
      font-size: 12px;
      font-weight: 800;
      color: #555;
      background: rgba(0, 0, 0, 0.03);
    }

    tbody tr:last-child td {
      border-bottom: none;
    }
  }

  &__player {
    display: flex;
    align-items: center;
    gap: 10px;

    strong {
      display: block;
      margin-bottom: 4px;
    }
  }

  &__ellipsis {
    max-width: 160px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

@media (max-width: 900px) {
  .match-rosters-panel {
    grid-template-columns: 1fr;
  }
}
</style>
