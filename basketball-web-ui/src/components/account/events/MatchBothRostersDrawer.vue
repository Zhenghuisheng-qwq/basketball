<script setup lang="ts">
import AccountStatusBadge from '@/components/account/AccountStatusBadge.vue'
import type { EventRosterMemberDto } from '@/api/types/eventParticipation'
import { formatPositionLabel } from '@/constants/rosterRules'

defineProps<{
  visible: boolean
  homeTeamName?: string
  awayTeamName?: string
  homeRoster?: EventRosterMemberDto[]
  awayRoster?: EventRosterMemberDto[]
  matchPlatform?: string
}>()

const emit = defineEmits<{
  close: []
}>()

function isCaptain(member: EventRosterMemberDto) {
  return member.isCaptain === 1 || member.rosterRole?.toUpperCase() === 'CAPTAIN'
}

function formatRole(member: EventRosterMemberDto) {
  return member.rosterRoleText || (isCaptain(member) ? '赛事队长' : '队员')
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
</script>

<template>
  <el-drawer
    :model-value="visible"
    title="本场阵容"
    size="min(80vw, 960px)"
    append-to-body
    destroy-on-close
    class="match-rosters-drawer"
    @close="emit('close')"
  >
    <div class="match-rosters-drawer__grid">
      <section class="match-rosters-drawer__team">
        <header class="match-rosters-drawer__team-head">
          <h4>主队阵容</h4>
          <span>{{ homeTeamName || '主队' }} · {{ homeRoster?.length || 0 }} 人</span>
        </header>
        <div v-if="!homeRoster?.length" class="match-rosters-drawer__empty">暂无阵容数据</div>
        <div v-else class="match-rosters-drawer__table-wrap">
          <table class="match-rosters-drawer__table">
            <thead>
              <tr>
                <th>球员</th>
                <th>编号</th>
                <th>位置</th>
                <th>游戏 ID</th>
                <th>角色</th>
                <th>状态</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="member in homeRoster" :key="member.rosterId || member.playerId">
                <td>
                  <div class="match-rosters-drawer__player">
                    <strong>{{ member.nickname || member.playerNo || '-' }}</strong>
                    <AccountStatusBadge v-if="isCaptain(member)" type="danger" text="队长" />
                  </div>
                </td>
                <td>{{ member.playerNo || '-' }}</td>
                <td>{{ formatPosition(member.position) }}</td>
                <td>{{ resolveGameId(member, matchPlatform) }}</td>
                <td>{{ formatRole(member) }}</td>
                <td>{{ member.rosterStatusText || '-' }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

      <section class="match-rosters-drawer__team">
        <header class="match-rosters-drawer__team-head">
          <h4>客队阵容</h4>
          <span>{{ awayTeamName || '客队' }} · {{ awayRoster?.length || 0 }} 人</span>
        </header>
        <div v-if="!awayRoster?.length" class="match-rosters-drawer__empty">暂无阵容数据</div>
        <div v-else class="match-rosters-drawer__table-wrap">
          <table class="match-rosters-drawer__table">
            <thead>
              <tr>
                <th>球员</th>
                <th>编号</th>
                <th>位置</th>
                <th>游戏 ID</th>
                <th>角色</th>
                <th>状态</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="member in awayRoster" :key="member.rosterId || member.playerId">
                <td>
                  <div class="match-rosters-drawer__player">
                    <strong>{{ member.nickname || member.playerNo || '-' }}</strong>
                    <AccountStatusBadge v-if="isCaptain(member)" type="danger" text="队长" />
                  </div>
                </td>
                <td>{{ member.playerNo || '-' }}</td>
                <td>{{ formatPosition(member.position) }}</td>
                <td>{{ resolveGameId(member, matchPlatform) }}</td>
                <td>{{ formatRole(member) }}</td>
                <td>{{ member.rosterStatusText || '-' }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>
    </div>
  </el-drawer>
</template>

<style scoped lang="scss">
.match-rosters-drawer {
  :deep(.el-drawer__header) {
    margin-bottom: 0;
    padding-bottom: 12px;
    border-bottom: 1px solid rgba(0, 0, 0, 0.08);
    color: #111;
    font-weight: 800;
  }

  :deep(.el-drawer__body) {
    padding: 16px 20px 24px;
    color: #111;
  }

  &__grid {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 20px;
  }

  &__team-head {
    display: flex;
    flex-wrap: wrap;
    justify-content: space-between;
    gap: 8px;
    margin-bottom: 12px;

    h4 {
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
    padding: 24px 0;
    color: #666;
    font-size: 14px;
  }

  &__table-wrap {
    overflow-x: auto;
  }

  &__table {
    width: 100%;
    border-collapse: collapse;
    font-size: 13px;

    th,
    td {
      padding: 10px 8px;
      border-bottom: 1px solid rgba(0, 0, 0, 0.08);
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
  }

  &__player {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 6px;
  }
}

@media (max-width: 900px) {
  .match-rosters-drawer__grid {
    grid-template-columns: 1fr;
  }
}
</style>
