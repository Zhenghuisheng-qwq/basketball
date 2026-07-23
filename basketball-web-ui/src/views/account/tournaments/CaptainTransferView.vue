<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AccountPageHeader from '@/components/account/AccountPageHeader.vue'
import AccountPanel from '@/components/account/AccountPanel.vue'
import AccountStatusBadge from '@/components/account/AccountStatusBadge.vue'
import EventTeamHeroStrip from '@/components/account/tournament/EventTeamHeroStrip.vue'
import { useEventTeamPage } from '@/composables/useEventTeamPage'

const router = useRouter()
const { breadcrumbRoot, teamPath, eventTeamStore } = useEventTeamPage()

const selectedPlayerId = ref('')
const confirmed = ref(false)

const captainMember = computed(() =>
  eventTeamStore.roster.find((item) => item.role === 'CAPTAIN'),
)

const selectedMember = computed(() =>
  eventTeamStore.captainCandidates.find((item) => item.playerId === selectedPlayerId.value),
)

function submitTransfer() {
  if (!selectedPlayerId.value) {
    ElMessage.warning('请选择新队长')
    return
  }
  if (!confirmed.value) {
    ElMessage.warning('请勾选移交确认')
    return
  }
  if (eventTeamStore.submitCaptainTransfer(selectedPlayerId.value, confirmed.value)) {
    ElMessage.success('队长移交申请已提交（Mock）')
    router.push(teamPath())
  }
}
</script>

<template>
  <div class="account-view">
    <AccountPageHeader
      zh-title="队长移交"
      en-title="CAPTAIN TRANSFER"
      :breadcrumb-items="[...breadcrumbRoot, { label: '球队管理', to: teamPath() }, { label: '队长移交' }]"
    />

    <EventTeamHeroStrip
      :tournament-name="eventTeamStore.tournament.tournamentName"
      :team-code="eventTeamStore.eventTeam.teamCode"
      description="在本届有效阵容内转移赛事队长权限"
      status-text="未提交"
      status-type="warning"
    />

    <div class="event-team-grid event-team-grid--2">
      <AccountPanel title="当前赛事队长">
        <div v-if="captainMember" class="captain-current">
          <div class="captain-current__avatar" aria-hidden="true">
            <svg viewBox="0 0 24 24"><path fill="currentColor" d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/></svg>
          </div>
          <div>
            <strong>{{ captainMember.nickname }}</strong>
            <AccountStatusBadge type="warning" text="赛事队长" />
          </div>
        </div>
        <div class="account-info-list">
          <div class="account-info-row"><span class="account-info-row__label">永久球员 ID</span><span>{{ captainMember?.playerNo }}</span></div>
          <div class="account-info-row"><span class="account-info-row__label">平台</span><span>{{ captainMember?.platform }}</span></div>
          <div class="account-info-row"><span class="account-info-row__label">游戏 ID</span><span>{{ captainMember?.gameId }}</span></div>
          <div class="account-info-row"><span class="account-info-row__label">当前状态</span><span>{{ eventTeamStore.eventTeam.captainPermissionText }}</span></div>
        </div>
        <p class="event-team-muted">审核期间原队长继续管理</p>
      </AccountPanel>

      <AccountPanel title="选择新队长">
        <div class="platform-table-wrap">
          <table class="platform-table">
            <thead>
              <tr>
                <th />
                <th>球员</th>
                <th>位置</th>
                <th>平台</th>
                <th>游戏 ID</th>
                <th>身份</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="member in eventTeamStore.captainCandidates" :key="member.playerId">
                <td>
                  <input v-model="selectedPlayerId" type="radio" :value="member.playerId" />
                </td>
                <td>{{ member.nickname }}</td>
                <td>{{ member.position }}</td>
                <td>{{ member.platform }}</td>
                <td>{{ member.gameId }}</td>
                <td>{{ member.roleText }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </AccountPanel>
    </div>

    <AccountPanel title="移交确认">
      <label class="registration-confirm-card__item">
        <input v-model="confirmed" type="checkbox" />
        <span>
          本人确认将本届赛事队长权限移交给
          {{ selectedMember?.nickname || '所选队员' }}
        </span>
      </label>
      <ol class="captain-transfer-steps">
        <li class="captain-transfer-steps__item captain-transfer-steps__item--current">01 本人确认</li>
        <li class="captain-transfer-steps__item">02 管理员审核</li>
      </ol>
      <div class="profile-version__actions">
        <button type="button" class="account-btn-primary" @click="submitTransfer">提交移交申请</button>
        <button type="button" class="account-btn-secondary" @click="router.push(teamPath())">返回球队管理</button>
      </div>
      <p class="event-team-muted">
        审核通过后，{{ selectedMember?.nickname || '新队长' }} 获得本届赛事管理权限，{{ captainMember?.nickname }} 变为普通队员；阵容、球队和历史关联不改变。
        原队长失联时由赛事方直接指定。
      </p>
    </AccountPanel>
  </div>
</template>

<style scoped lang="scss">
@import '../styles/account-center.scss';

.captain-current {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-bottom: 12px;

  &__avatar {
    width: 56px;
    height: 56px;
    border-radius: 50%;
    background: #ececec;
    display: grid;
    place-items: center;
    color: #777;
  }
}

.captain-transfer-steps {
  list-style: none;
  margin: 12px 0;
  padding: 0;
  display: flex;
  gap: 12px;
  flex-wrap: wrap;

  &__item {
    padding: 8px 12px;
    border: 1px solid rgba(0, 0, 0, 0.12);
    font-size: 13px;
    font-weight: 700;

    &--current {
      border-color: var(--account-red, #d30910);
      color: var(--account-red, #d30910);
    }
  }
}

.profile-version__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 12px;
}
</style>
