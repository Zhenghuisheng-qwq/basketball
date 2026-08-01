<template>
  <div class="app-container">
    <el-alert type="info" show-icon :closable="false" class="mb8"
      title="异常比赛处理"
      description="处理弃赛、异常比赛与判负等场景；可快捷跳转至赛程管理、比赛数据录入完成操作。" />
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
          <el-button type="warning" link @click="goScore" :disabled="!matchId">打开比分录入（判负，菜单已隐藏）</el-button>
          <p class="hint">判负能力暂保留在比分录入页，后续将合并至比赛数据录入</p>
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
