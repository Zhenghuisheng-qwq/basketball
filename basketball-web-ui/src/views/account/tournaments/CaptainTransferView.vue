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
const { breadcrumbRoot, teamPath, eventTeamStore } = useEventTeamPage({ requireCaptain: true })

const selectedPlayerId = ref('')
const applyReason = ref('')
const confirmed = ref(false)
const submitting = ref(false)

const captainMember = computed(() =>
  eventTeamStore.roster.find((item) => item.role === 'CAPTAIN'),
)

const selectedMember = computed(() =>
  eventTeamStore.captainCandidates.find((item) => item.playerId === selectedPlayerId.value),
)

async function submitTransfer() {
  if (!selectedPlayerId.value) {
    ElMessage.warning('请选择新队长')
    return
  }
  if (!confirmed.value) {
    ElMessage.warning('请勾选移交确认')
    return
  }
  submitting.value = true
  try {
    const ok = await eventTeamStore.submitCaptainTransfer(
      selectedPlayerId.value,
      applyReason.value.trim() || undefined,
    )
    if (ok) {
      ElMessage.success('队长移交申请已提交')
      router.push(teamPath())
    } else {
      ElMessage.warning('提交失败')
    }
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '提交失败')
  } finally {
    submitting.value = false
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
        </div>
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
              </tr>
            </tbody>
          </table>
        </div>
      </AccountPanel>
    </div>

    <AccountPanel title="移交确认">
      <label>
        <span>申请原因</span>
        <textarea v-model="applyReason" rows="2" class="identity-apply-form__input" />
      </label>
      <label class="registration-confirm-card__item">
        <input v-model="confirmed" type="checkbox" />
        <span>
          本人确认将本届赛事队长权限移交给
          {{ selectedMember?.nickname || '所选队员' }}
        </span>
      </label>
      <div class="profile-version__actions">
        <button type="button" class="account-btn-primary" :disabled="submitting" @click="submitTransfer">
          提交移交申请
        </button>
        <button type="button" class="account-btn-secondary" @click="router.push(teamPath())">返回球队管理</button>
      </div>
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

.profile-version__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 12px;
}
</style>
