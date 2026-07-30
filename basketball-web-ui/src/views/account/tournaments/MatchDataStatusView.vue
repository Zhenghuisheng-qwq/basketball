<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AccountPageHeader from '@/components/account/AccountPageHeader.vue'
import AccountPanel from '@/components/account/AccountPanel.vue'
import { getMatchDataStatus } from '@/api/matchData'
import { useEventTeamPage } from '@/composables/useEventTeamPage'

const route = useRoute()
const router = useRouter()
const { breadcrumbRoot, teamPath, eventTeamStore } = useEventTeamPage()

const matchId = computed(() => Number(route.params.matchId))
const loading = ref(false)
const status = ref<any>(null)

async function loadStatus() {
  loading.value = true
  try {
    const res = await getMatchDataStatus(matchId.value)
    status.value = res.data
  } catch (e: any) {
    ElMessage.error(e?.message || '加载失败')
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await eventTeamStore.ensureLoaded(String(route.params.tournamentId))
  loadStatus()
})
</script>

<template>
  <div class="account-view">
    <AccountPageHeader
      zh-title="比赛数据状态"
      en-title="MATCH DATA STATUS"
      :breadcrumb-items="[...breadcrumbRoot, { label: '比赛数据', to: teamPath('/match-data') }, { label: '状态' }]"
    />

    <AccountPanel v-loading="loading" title="审核进度">
      <template v-if="status">
        <p><strong>状态：</strong>{{ status.statusText || status.submissionStatusText }}</p>
        <p v-if="status.submittedTime"><strong>提交时间：</strong>{{ status.submittedTime }}</p>
        <p v-if="status.reviewedTime"><strong>审核时间：</strong>{{ status.reviewedTime }}</p>
        <p v-if="status.rejectReason"><strong>驳回原因：</strong>{{ status.rejectReason }}</p>
        <div class="account-panel__action">
          <button type="button" class="account-btn-secondary" @click="router.push(teamPath('/match-data'))">返回列表</button>
          <button
            v-if="status.canResubmit"
            type="button"
            class="account-btn-primary"
            @click="router.push(teamPath(`/match-data/${matchId}/resubmit`))"
          >
            修改并重新提交
          </button>
        </div>
      </template>
    </AccountPanel>
  </div>
</template>

<style scoped lang="scss">
@import '../styles/account-center.scss';
</style>
