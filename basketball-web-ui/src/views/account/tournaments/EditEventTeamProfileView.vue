<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AccountPageHeader from '@/components/account/AccountPageHeader.vue'
import AccountPanel from '@/components/account/AccountPanel.vue'
import EventTeamHeroStrip from '@/components/account/tournament/EventTeamHeroStrip.vue'
import EventTeamLogo from '@/components/account/tournament/EventTeamLogo.vue'
import AccountStatusBadge from '@/components/account/AccountStatusBadge.vue'
import { useEventTeamPage } from '@/composables/useEventTeamPage'

const router = useRouter()
const { breadcrumbRoot, teamPath, eventTeamStore } = useEventTeamPage({ requireCaptain: true })

const draftName = ref(eventTeamStore.eventTeam.teamName)
const draftLogoUrl = ref(eventTeamStore.eventTeam.teamLogoUrl)

function cancelEdit() {
  draftName.value = eventTeamStore.eventTeam.teamName
  draftLogoUrl.value = eventTeamStore.eventTeam.teamLogoUrl
  router.push(teamPath())
}

function submitReview() {
  ElMessage.info('本届球队资料修改将在后续版本开放')
}
</script>

<template>
  <div class="account-view">
    <AccountPageHeader
      zh-title="修改本届球队资料"
      en-title="EDIT EVENT TEAM PROFILE"
      :breadcrumb-items="[...breadcrumbRoot, { label: '球队管理', to: teamPath() }, { label: '修改球队资料' }]"
    />

    <EventTeamHeroStrip
      :tournament-name="eventTeamStore.tournament.tournamentName"
      :team-code="eventTeamStore.eventTeam.teamCode"
      description="仅修改本届赛事公开资料"
      status-text="修改后需重新审核"
      status-type="warning"
    />

    <div class="event-team-grid event-team-grid--2">
      <AccountPanel title="当前审核通过版本">
        <div class="profile-version">
          <EventTeamLogo
            :team-code="eventTeamStore.eventTeam.teamCode"
            :logo-url="eventTeamStore.eventTeam.teamLogoUrl"
            size="lg"
          />
          <div class="account-info-list">
            <div class="account-info-row"><span class="account-info-row__label">本届队名</span><span>{{ eventTeamStore.eventTeam.teamName }}</span></div>
            <div class="account-info-row"><span class="account-info-row__label">比赛平台</span><span>{{ eventTeamStore.eventTeam.platform }}</span></div>
            <div class="account-info-row"><span class="account-info-row__label">永久球队 ID</span><span>{{ eventTeamStore.eventTeam.permanentTeamNo }}</span></div>
            <div class="account-info-row"><span class="account-info-row__label">版本状态</span><AccountStatusBadge type="success" :text="eventTeamStore.eventTeam.teamProfileStatusText" /></div>
          </div>
          <button type="button" class="account-btn-secondary" disabled>当前公开页使用版本</button>
        </div>
      </AccountPanel>

      <AccountPanel title="拟修改版本">
        <div class="profile-version">
          <EventTeamLogo
            :team-code="eventTeamStore.eventTeam.teamCode"
            :logo-url="draftLogoUrl"
            size="lg"
          />
          <label class="identity-apply-form__field">
            <span>本届队名</span>
            <input v-model="draftName" type="text" class="identity-apply-form__input" disabled />
          </label>
          <label class="account-btn-secondary identity-apply-form__upload-btn">
            上传新 Logo
            <input type="file" accept="image/*" hidden disabled />
          </label>
          <label class="identity-apply-form__field">
            <span>比赛平台</span>
            <input :value="eventTeamStore.eventTeam.platform" type="text" class="identity-apply-form__input" disabled />
          </label>
          <div class="profile-version__actions">
            <button type="button" class="account-btn-secondary" @click="cancelEdit">返回</button>
            <button type="button" class="account-btn-primary" disabled @click="submitReview">提交重新审核</button>
          </div>
          <p class="event-team-muted">球队资料修改 API 尚未接入，当前仅展示本届球队信息。</p>
        </div>
      </AccountPanel>
    </div>
  </div>
</template>

<style scoped lang="scss">
@import '../styles/account-center.scss';

.profile-version {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.profile-version__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}
</style>
