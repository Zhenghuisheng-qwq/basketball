<template>
  <div class="app-container">
    <el-form :inline="true" class="mb8">
      <el-form-item label="球队ID">
        <el-input-number v-model="teamId" :min="1" controls-position="right" style="width: 160px" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="loadArchive" v-hasPermi="['basket:permanentTeam:list']">查询档案</el-button>
      </el-form-item>
    </el-form>

    <template v-if="archive">
      <el-card class="mb8">
        <template #header>球队基本信息</template>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="球队编号">{{ archive.team?.teamNo }}</el-descriptions-item>
          <el-descriptions-item label="名称">{{ archive.team?.teamName }}</el-descriptions-item>
          <el-descriptions-item label="简称">{{ archive.team?.teamShortName }}</el-descriptions-item>
          <el-descriptions-item label="认证状态">{{ archive.team?.auditStatus }}</el-descriptions-item>
          <el-descriptions-item label="简介" :span="2">{{ archive.team?.teamIntro }}</el-descriptions-item>
        </el-descriptions>
      </el-card>

      <el-card class="mb8">
        <template #header>认证/变更历史</template>
        <el-table :data="archive.auditHistory || []" border>
          <el-table-column label="动作" prop="auditAction" />
          <el-table-column label="备注" prop="auditRemark" show-overflow-tooltip />
          <el-table-column label="时间" prop="auditTime" width="170">
            <template #default="scope">{{ parseTime(scope.row.auditTime) }}</template>
          </el-table-column>
        </el-table>
      </el-card>

      <el-card class="mb8">
        <template #header>处罚历史</template>
        <el-table :data="archive.disciplineRecords || []" border>
          <el-table-column label="编号" prop="recordNo" />
          <el-table-column label="标题" prop="title" />
          <el-table-column label="状态" prop="recordStatus" width="100" />
        </el-table>
      </el-card>

      <el-card>
        <template #header>荣誉</template>
        <el-table :data="archive.honors || []" border>
          <el-table-column label="荣誉名称" prop="honorName" />
          <el-table-column label="赛事" prop="tournamentName" />
          <el-table-column label="排名" prop="ranking" width="80" />
        </el-table>
      </el-card>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getPermanentTeamArchive } from '@/api/basket/permanentArchive'
import { parseTime } from '@/utils/ruoyi'

const teamId = ref<number>()
const archive = ref<any>(null)

async function loadArchive() {
  if (!teamId.value) {
    ElMessage.warning('请输入球队ID')
    return
  }
  const res = await getPermanentTeamArchive(teamId.value)
  archive.value = res.data
}
</script>
