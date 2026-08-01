<template>
  <div class="app-container dashboard-page">
    <el-row :gutter="16" class="mb16">
      <el-col :span="24">
        <el-card shadow="never">
          <div class="current-event">
            <span class="label">当前工作赛事</span>
            <span class="value">{{ summary.currentTournamentName || '未配置赛事授权' }}</span>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :xs="24" :sm="12" :md="8" :lg="6" v-for="item in cards" :key="item.key">
        <el-card shadow="hover" class="todo-card" @click="go(item.path)">
          <div class="todo-title">{{ item.title }}</div>
          <div class="todo-count">{{ item.count }}</div>
          <div class="todo-desc">点击进入处理</div>
        </el-card>
      </el-col>
    </el-row>

    <OperationTrendPanel :pending-total="pendingTotal" />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getDashboardSummary } from '@/api/basket/dashboard'
import OperationTrendPanel from './OperationTrendPanel.vue'

const router = useRouter()
const summary = ref<any>({
  pendingTeamAudit: 0,
  pendingPlayerCert: 0,
  pendingRegistration: 0,
  pendingRosterChange: 0,
  pendingPlatformChange: 0,
  pendingMatchDataAudit: 0,
  pendingMatchDispute: 0
})

const cards = computed(() => [
  { key: 'playerCert', title: '球员认证待审', count: summary.value.pendingPlayerCert, path: '/identity/player-certification' },
  { key: 'teamAudit', title: '球队认证待审', count: summary.value.pendingTeamAudit, path: '/identity/team-certification' },
  { key: 'registration', title: '球队报名待审', count: summary.value.pendingRegistration, path: '/participation/team-registration' },
  { key: 'rosterChange', title: '阵容变更待审', count: summary.value.pendingRosterChange, path: '/participation/transfer-audit' },
  { key: 'platformChange', title: '平台变更待审', count: summary.value.pendingPlatformChange, path: '/identity/platform-change' },
  { key: 'matchAudit', title: '比赛数据待审', count: summary.value.pendingMatchDataAudit, path: '/competition/match-data-audit' },
  { key: 'matchDispute', title: '数据争议待处理', count: summary.value.pendingMatchDispute, path: '/competition/match-dispute' }
])

const pendingTotal = computed(() => cards.value.reduce((total, item) => total + (Number(item.count) || 0), 0))

async function loadSummary() {
  const res = await getDashboardSummary()
  summary.value = { ...summary.value, ...(res.data || {}) }
}

function go(path: string) {
  router.push(path)
}

onMounted(loadSummary)
</script>

<style scoped lang="scss">
.dashboard-page {
  .current-event {
    display: flex;
    align-items: center;
    gap: 12px;
    .label { color: #909399; }
    .value { font-size: 18px; font-weight: 600; }
  }
  .todo-card {
    margin-bottom: 16px;
    cursor: pointer;
    .todo-title { color: #606266; margin-bottom: 8px; }
    .todo-count { font-size: 28px; font-weight: 700; color: #409eff; line-height: 1.2; }
    .todo-desc { margin-top: 8px; color: #909399; font-size: 12px; }
  }
}
</style>
