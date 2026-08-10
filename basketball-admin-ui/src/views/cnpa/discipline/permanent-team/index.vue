<template>
  <div class="app-container">
    <el-alert
      type="info"
      :closable="false"
      show-icon
      class="mb8"
      title="历史球队档案"
      description="查看某届赛事球队（含 ARCHIVED / DISSOLVED / Legacy）的历史资料、成员与处罚荣誉记录。该页只读，不代表永久俱乐部，也不能把旧球队复活到新赛事。"
    />

    <el-form :inline="true" class="mb8">
      <el-form-item label="球队ID">
        <el-input-number v-model="teamId" :min="1" controls-position="right" style="width: 160px" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="loadArchive" v-hasPermi="['basket:permanentTeam:list']">查看档案</el-button>
      </el-form-item>
    </el-form>

    <el-empty v-if="searched && !archive" description="未找到该历史球队档案，请确认球队 ID 是否正确" />

    <template v-if="archive">
      <el-card class="mb8">
        <template #header>球队基础资料</template>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="球队编号">{{ archive.team?.teamNo ?? '-' }}</el-descriptions-item>
          <el-descriptions-item label="球队名称">{{ archive.team?.teamName ?? '-' }}</el-descriptions-item>
          <el-descriptions-item label="球队简称">{{ archive.team?.teamShortName ?? '-' }}</el-descriptions-item>
          <el-descriptions-item label="认证状态">{{ archive.team?.auditStatus ?? '-' }}</el-descriptions-item>
          <el-descriptions-item label="简介" :span="2">{{ archive.team?.teamIntro ?? '-' }}</el-descriptions-item>
        </el-descriptions>
      </el-card>

      <el-card class="mb8">
        <template #header>认证与变更历史</template>
        <el-table :data="archive.auditHistory || []" border>
          <el-table-column label="操作动作" prop="auditAction" />
          <el-table-column label="备注" prop="auditRemark" show-overflow-tooltip />
          <el-table-column label="操作时间" prop="auditTime" width="170">
            <template #default="scope">{{ parseTime(scope.row.auditTime) }}</template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!(archive.auditHistory || []).length" description="暂无认证与变更历史" />
      </el-card>

      <el-card class="mb8">
        <template #header>处罚记录</template>
        <el-table :data="archive.disciplineRecords || []" border>
          <el-table-column label="处罚编号" prop="recordNo" />
          <el-table-column label="标题" prop="title" />
          <el-table-column label="状态" prop="recordStatus" width="100" />
        </el-table>
        <el-empty v-if="!(archive.disciplineRecords || []).length" description="暂无处罚记录" />
      </el-card>

      <el-card>
        <template #header>荣誉记录</template>
        <el-table :data="archive.honors || []" border>
          <el-table-column label="荣誉名称" prop="honorName" />
          <el-table-column label="赛事" prop="tournamentName" />
          <el-table-column label="排名" prop="ranking" width="80" />
        </el-table>
        <el-empty v-if="!(archive.honors || []).length" description="暂无荣誉记录" />
      </el-card>
    </template>

    <el-empty v-else-if="!searched" description="请输入球队 ID 后查看历史球队档案" />
  </div>
</template>

<script setup lang="ts" name="BasketPermanentTeam">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getPermanentTeamArchive } from '@/api/basket/permanentArchive'
import { parseTime } from '@/utils/ruoyi'

const teamId = ref<number>()
const archive = ref<any>(null)
const searched = ref(false)

async function loadArchive() {
  if (!teamId.value) {
    ElMessage.warning('请输入球队 ID')
    return
  }
  searched.value = true
  archive.value = null
  const res = await getPermanentTeamArchive(teamId.value)
  archive.value = res.data
}
</script>
