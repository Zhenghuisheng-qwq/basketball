<template>
  <div class="app-container">
    <el-alert type="info" show-icon :closable="false" class="mb8"
      title="异常比赛处理复用赛程管理与比分录入中的延期、取消、判负能力；本页提供快捷入口。" />
    <el-form :inline="true" class="mb8">
      <el-form-item label="比赛ID">
        <el-input-number v-model="matchId" :min="1" controls-position="right" style="width: 160px" />
      </el-form-item>
    </el-form>
    <el-row :gutter="12">
      <el-col :span="8">
        <el-card shadow="never">
          <template #header>赛程调整</template>
          <el-button type="primary" link @click="go('/competition-exec/schedule')">打开赛程管理</el-button>
          <p class="hint">支持延期、取消、重赛关联</p>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="never">
          <template #header>弃赛 / 判负</template>
          <el-button type="warning" link @click="goScore" :disabled="!matchId">打开比分录入（判负）</el-button>
          <p class="hint">在比分页使用判负结束，形成 FORFEIT 正式结果</p>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="never">
          <template #header>结果重开 / 修订</template>
          <el-button type="danger" link @click="goEntry" :disabled="!matchId">打开数据录入（重开）</el-button>
          <p class="hint">已确认比赛可通过重开进入修订流程</p>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const matchId = ref<number>()

function go(path: string) {
  router.push(path)
}

function goScore() {
  router.push({ path: '/competition-exec/score', query: { matchId: String(matchId.value) } })
}

function goEntry() {
  router.push({ path: '/competition-exec/match-data-entry', query: { matchId: String(matchId.value) } })
}
</script>

<style scoped>
.mb8 { margin-bottom: 8px; }
.hint { color: #909399; font-size: 13px; margin-top: 8px; }
</style>
