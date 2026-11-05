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
      <el-form-item label="球员">
        <el-select
          v-model="playerSelectId"
          filterable
          remote
          allow-create
          default-first-option
          reserve-keyword
          clearable
          :remote-method="searchPlayers"
          :loading="playerLoading"
          placeholder="搜索或输入球员姓名 / 编号 / ID"
          style="width: 360px"
          @change="onPlayerChange"
          @visible-change="onPlayerVisible"
        >
          <el-option v-for="item in playerOptions" :key="item.id" :label="item.label" :value="item.id">
            <div class="selector-option">
              <div>{{ item.label }}</div>
              <div v-if="item.secondaryLabel" class="selector-option__sub">{{ item.secondaryLabel }}</div>
            </div>
          </el-option>
        </el-select>
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

    <el-empty v-else-if="!searched" description="搜索或输入球员后查看永久档案" />
  </div>
</template>

<script setup lang="ts" name="BasketPermanentPlayer">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { DisciplineSelectorOption } from '@/api/basket/discipline'
import { getPermanentPlayerArchive, searchPermanentPlayers } from '@/api/basket/permanentArchive'
import { parseTime } from '@/utils/ruoyi'

const playerSelectId = ref<number | string>()
const playerId = ref<number>()
const playerOptions = ref<DisciplineSelectorOption[]>([])
const playerLoading = ref(false)
const archive = ref<any>(null)
const searched = ref(false)
let playerTimer: ReturnType<typeof setTimeout> | undefined

function sameText(item: DisciplineSelectorOption, keyword: string) {
  return [item.id, item.playerNo, item.gameId, item.label, item.teamName]
    .some(value => value != null && String(value).trim() === keyword)
}

async function fetchPlayers(keyword = '') {
  playerLoading.value = true
  try {
    const res = await searchPermanentPlayers({ keyword: keyword || undefined, pageNum: 1, pageSize: 20 })
    playerOptions.value = res.rows || []
  } finally {
    playerLoading.value = false
  }
}

function searchPlayers(keyword: string) {
  clearTimeout(playerTimer)
  playerTimer = setTimeout(() => fetchPlayers(keyword), 300)
}

function onPlayerVisible(visible: boolean) {
  if (visible && !playerOptions.value.length) fetchPlayers('')
}

async function onPlayerChange(selectKey?: number | string) {
  if (selectKey == null || selectKey === '') {
    playerId.value = undefined
    return
  }
  let selected = playerOptions.value.find(item => String(item.id) === String(selectKey))
  if (!selected) {
    await fetchPlayers(String(selectKey))
    const text = String(selectKey).trim()
    const exact = playerOptions.value.filter(item => sameText(item, text))
    selected = playerOptions.value.find(item => String(item.id) === text)
      || (exact.length === 1 ? exact[0] : undefined)
      || (playerOptions.value.length === 1 ? playerOptions.value[0] : undefined)
    if (!selected) {
      playerSelectId.value = undefined
      playerId.value = undefined
      ElMessage.warning(playerOptions.value.length ? '找到多个球员，请从下拉列表选择' : '没有匹配的球员')
      return
    }
    if (String(playerSelectId.value) !== String(selected.id)) {
      playerSelectId.value = selected.id
    }
  }
  playerId.value = Number(selected.id)
  await loadArchive()
}

async function loadArchive() {
  if (!playerId.value) {
    ElMessage.warning('请搜索或输入球员')
    return
  }
  searched.value = true
  archive.value = null
  const res = await getPermanentPlayerArchive(playerId.value)
  archive.value = res.data
}
</script>

<style scoped>
.selector-option {
  line-height: 1.3;
  padding: 4px 0;
}
.selector-option__sub {
  margin-top: 2px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}
</style>
