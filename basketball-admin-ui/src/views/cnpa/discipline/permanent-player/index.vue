<template>
  <div class="app-container">
    <el-form :inline="true" class="mb8">
      <el-form-item label="球员ID">
        <el-input-number v-model="playerId" :min="1" controls-position="right" style="width: 160px" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="loadArchive" v-hasPermi="['basket:permanentPlayer:list']">查询档案</el-button>
      </el-form-item>
    </el-form>

    <template v-if="archive">
      <el-card class="mb8">
        <template #header>球员基本信息</template>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="球员编号">{{ archive.profile?.playerNo }}</el-descriptions-item>
          <el-descriptions-item label="昵称">{{ archive.profile?.gameNickname }}</el-descriptions-item>
          <el-descriptions-item label="认证状态">{{ archive.profile?.certificationStatus }}</el-descriptions-item>
          <el-descriptions-item label="平台">{{ archive.profile?.gameTerminal }}</el-descriptions-item>
          <el-descriptions-item label="游戏ID">{{ archive.profile?.gameAccount }}</el-descriptions-item>
          <el-descriptions-item label="邮箱">{{ archive.account?.email }}</el-descriptions-item>
        </el-descriptions>
      </el-card>

      <el-card class="mb8">
        <template #header>球队关系历史</template>
        <el-table :data="archive.teamHistory || []" border>
          <el-table-column label="球队" prop="teamName" />
          <el-table-column label="是否队长" prop="isCaptain" width="90" />
          <el-table-column label="加入时间" prop="joinTime" width="170">
            <template #default="scope">{{ parseTime(scope.row.joinTime) }}</template>
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
import { getPermanentPlayerArchive } from '@/api/basket/permanentArchive'
import { parseTime } from '@/utils/ruoyi'

const playerId = ref<number>()
const archive = ref<any>(null)

async function loadArchive() {
  if (!playerId.value) {
    ElMessage.warning('请输入球员ID')
    return
  }
  const res = await getPermanentPlayerArchive(playerId.value)
  archive.value = res.data
}
</script>
