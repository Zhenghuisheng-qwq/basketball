<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AccountPageHeader from '@/components/account/AccountPageHeader.vue'
import AccountPanel from '@/components/account/AccountPanel.vue'
import { pageMyMatches, type PlayerMatchTaskItem } from '@/api/matchData'
import { useEventTeamPage } from '@/composables/useEventTeamPage'

const route = useRoute()
const router = useRouter()
const { breadcrumbRoot, teamPath, eventTeamStore } = useEventTeamPage()

const loading = ref(false)
const rows = ref<PlayerMatchTaskItem[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)

const seasonId = computed(() => eventTeamStore.seasonId || route.params.tournamentId)

async function loadList() {
  loading.value = true
  try {
    const res = await pageMyMatches({
      seasonId: seasonId.value,
      pageNum: pageNum.value,
      pageSize: pageSize.value
    })
    rows.value = res.data?.records || []
    total.value = res.data?.total || 0
    pendingCount.value = rows.value.filter((item) => item.canSubmit).length
  } catch (e: any) {
    ElMessage.error(e?.message || '加载失败')
  } finally {
    loading.value = false
  }
}

const pendingCount = ref(0)

function goAction(row: PlayerMatchTaskItem) {
  const base = teamPath('/match-data')
  if (row.canSubmit) {
    router.push(`${base}/${row.matchId}/submit`)
    return
  }
  if (row.canResubmit) {
    router.push(`${base}/${row.matchId}/resubmit`)
    return
  }
  router.push(`${base}/${row.matchId}/status`)
}

onMounted(async () => {
  await eventTeamStore.ensureLoaded(String(route.params.tournamentId))
  loadList()
})
</script>

<template>
  <div class="account-view">
    <AccountPageHeader
      zh-title="我的比赛"
      en-title="MY MATCHES"
      :breadcrumb-items="[...breadcrumbRoot, { label: '比赛数据' }]"
    />

    <AccountPanel title="比赛列表">
      <p class="event-team-muted">{{ pendingCount }} 场待提交比赛数据</p>
      <div v-loading="loading" class="match-data-list">
        <div v-if="!rows.length && !loading" class="event-team-muted">暂无相关比赛</div>
        <div v-for="item in rows" :key="item.matchId" class="match-data-row">
          <div>
            <strong>{{ item.homeTeamName }} vs {{ item.awayTeamName }}</strong>
            <p class="event-team-muted">{{ item.scheduledStartTime }} · {{ item.statusText }}</p>
          </div>
          <button type="button" class="account-btn-primary" @click="goAction(item)">
            {{ item.actionText || '查看' }}
          </button>
        </div>
      </div>
    </AccountPanel>
  </div>
</template>

<style scoped lang="scss">
@import '../styles/account-center.scss';

.match-data-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.match-data-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  padding: 12px 0;
  border-bottom: 1px solid var(--account-border, #eee);
}
</style>
