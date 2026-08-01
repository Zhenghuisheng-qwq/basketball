<template>
  <div class="app-container">
    <el-alert
      type="info"
      :closable="false"
      show-icon
      class="mb8"
      title="永久球员档案"
      description="查看球员永久档案、认证信息、平台游戏 ID 历史、所属球队历史、参赛历史、荣誉记录、处罚记录和数据汇总。该页面用于档案追溯，不处理球员认证审核、平台游戏 ID 审核或队长授权。"
    />

    <el-form :inline="true" class="mb8">
      <el-form-item label="球员ID">
        <el-input-number v-model="playerId" :min="1" controls-position="right" style="width: 160px" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="loadArchive" v-hasPermi="['basket:permanentPlayer:list']">查看档案</el-button>
      </el-form-item>
    </el-form>

    <el-empty v-if="searched && !archive" description="未找到该球员的永久档案，请确认球员 ID 是否正确" />

    <template v-if="archive">
      <el-card class="mb8">
        <template #header>基础资料</template>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="永久球员编号">{{ archive.profile?.playerNo ?? '-' }}</el-descriptions-item>
          <el-descriptions-item label="用户昵称">{{ archive.profile?.gameNickname ?? '-' }}</el-descriptions-item>
          <el-descriptions-item label="认证状态">{{ archive.profile?.certificationStatus ?? '-' }}</el-descriptions-item>
          <el-descriptions-item label="平台">{{ archive.profile?.gameTerminal ?? '-' }}</el-descriptions-item>
          <el-descriptions-item label="平台游戏 ID">{{ archive.profile?.gameAccount ?? '-' }}</el-descriptions-item>
          <el-descriptions-item label="邮箱">{{ archive.account?.email ?? '-' }}</el-descriptions-item>
        </el-descriptions>
      </el-card>

      <el-card class="mb8">
        <template #header>球队归属历史</template>
        <el-table :data="archive.teamHistory || []" border>
          <el-table-column label="球队" prop="teamName" />
          <el-table-column label="是否队长" prop="isCaptain" width="90" />
          <el-table-column label="加入时间" prop="joinTime" width="170">
            <template #default="scope">{{ parseTime(scope.row.joinTime) }}</template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!(archive.teamHistory || []).length" description="暂无球队归属历史" />
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

    <el-empty v-else-if="!searched" description="请输入球员 ID 后查看永久档案" />
  </div>
</template>

<script setup lang="ts" name="BasketPermanentPlayer">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getPermanentPlayerArchive } from '@/api/basket/permanentArchive'
import { parseTime } from '@/utils/ruoyi'

const playerId = ref<number>()
const archive = ref<any>(null)
const searched = ref(false)

async function loadArchive() {
  if (!playerId.value) {
    ElMessage.warning('请输入球员 ID')
    return
  }
  searched.value = true
  archive.value = null
  const res = await getPermanentPlayerArchive(playerId.value)
  archive.value = res.data
}
</script>
