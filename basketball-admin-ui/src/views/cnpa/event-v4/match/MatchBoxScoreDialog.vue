<template>
  <el-dialog
    :model-value="modelValue"
    title="上传详细数据"
    width="1180px"
    top="4vh"
    append-to-body
    @close="emit('update:modelValue', false)"
  >
    <el-alert type="info" :closable="false" show-icon class="mb8"
      title="和队长上传的详细数据是同一张表。截图上的队名可以和报名队名不一样，按名单里的游戏 ID 认队、认人。对上的直接填好，对不上再手改。" />
    <section class="box-board">
      <div>
        <span>HOME</span>
        <strong>{{ roster?.homeTeamName || match?.homeTeamName || '主队' }}</strong>
      </div>
      <div class="box-board__score">
        <b>{{ sides[0].points }}</b>
        <em>:</em>
        <b>{{ sides[1].points }}</b>
      </div>
      <div class="is-away">
        <span>AWAY</span>
        <strong>{{ roster?.awayTeamName || match?.awayTeamName || '客队' }}</strong>
      </div>
      <p>根据双方球员 PTS 自动计算</p>
    </section>
    <div class="box-toolbar">
      <el-button type="primary" :loading="recognizing" @click="fileInput?.click()">识别截图并填入</el-button>
      <input ref="fileInput" class="box-file" type="file" accept="image/png,image/jpeg,image/webp" @change="onFile" />
      <span v-if="warning" class="box-warn">{{ warning }}</span>
    </div>

    <div v-if="roster?.periodRequired !== false" class="box-periods">
      <div v-for="period in periods" :key="period.periodNo" class="box-q">
        <span>Q{{ period.periodNo }}</span>
        <el-input-number v-model="period.homeScore" :min="0" :max="80" size="small" controls-position="right" />
        <em>:</em>
        <el-input-number v-model="period.awayScore" :min="0" :max="80" size="small" controls-position="right" />
      </div>
    </div>

    <section v-for="side in sides" :key="side.key" class="box-side">
      <header>
        <strong>{{ side.name }} · 球员数据</strong>
        <el-button link type="primary" @click="addRow(side.key)">添加球员</el-button>
      </header>
      <div class="box-table-wrap">
        <table class="box-table">
          <thead>
            <tr>
              <th>球员</th>
              <th>PTS</th><th>REB</th><th>AST</th><th>STL</th><th>BLK</th><th>PF</th><th>TO</th>
              <th>FGM/FGA</th><th>3PM/3PA</th><th>FTM/FTA</th><th></th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(row, index) in side.rows" :key="`${side.key}-${index}`">
              <td class="is-player">
                <el-select v-model="row.eventPlayerId" filterable clearable placeholder="选择游戏 ID" style="width: 100%">
                  <el-option
                    v-for="player in available(side.key, index)"
                    :key="player.eventPlayerId"
                    :label="playerLabel(player)"
                    :value="player.eventPlayerId"
                  />
                </el-select>
                <div v-if="row.recognizedName && !row.eventPlayerId" class="box-miss">图上的游戏 ID 是「{{ row.recognizedName }}」，和名单不一致</div>
                <div v-else-if="formula(row)" class="box-miss">{{ formula(row) }}</div>
              </td>
              <td><el-input-number v-model="row.points" :min="0" :max="99" size="small" :controls="false" /></td>
              <td><el-input-number v-model="row.rebound" :min="0" :max="99" size="small" :controls="false" /></td>
              <td><el-input-number v-model="row.assist" :min="0" :max="99" size="small" :controls="false" /></td>
              <td><el-input-number v-model="row.steal" :min="0" :max="99" size="small" :controls="false" /></td>
              <td><el-input-number v-model="row.block" :min="0" :max="99" size="small" :controls="false" /></td>
              <td><el-input-number v-model="row.foul" :min="0" :max="99" size="small" :controls="false" /></td>
              <td><el-input-number v-model="row.turnover" :min="0" :max="99" size="small" :controls="false" /></td>
              <td class="is-pair">
                <el-input-number v-model="row.fieldGoalMade" :min="0" :max="99" size="small" :controls="false" />
                <span>/</span>
                <el-input-number v-model="row.fieldGoalAttempt" :min="0" :max="99" size="small" :controls="false" />
              </td>
              <td class="is-pair">
                <el-input-number v-model="row.threePointMade" :min="0" :max="99" size="small" :controls="false" />
                <span>/</span>
                <el-input-number v-model="row.threePointAttempt" :min="0" :max="99" size="small" :controls="false" />
              </td>
              <td class="is-pair">
                <el-input-number v-model="row.freeThrowMade" :min="0" :max="99" size="small" :controls="false" />
                <span>/</span>
                <el-input-number v-model="row.freeThrowAttempt" :min="0" :max="99" size="small" :controls="false" />
              </td>
              <td><el-button link type="danger" @click="removeRow(side.key, index)">删除</el-button></td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <el-form label-width="80px" class="box-reason">
      <el-form-item label="原因" required>
        <el-input v-model="reason" type="textarea" :rows="2" maxlength="500" placeholder="例如：队长未交，按结算截图录入" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="emit('update:modelValue', false)">取消</el-button>
      <el-button type="primary" :loading="saving" @click="submit">确认写入正式结果</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  enterV4MatchBoxScore,
  getV4MatchBoxScoreRoster,
  recognizeV4AdminScreenshot,
  type BoxScorePlayer,
  type BoxScoreRoster,
} from '@/api/cnpa/event-v4/stage-exception'

const props = defineProps<{ modelValue: boolean; match?: { id?: number; homeTeamName?: string; awayTeamName?: string } | null }>()
const emit = defineEmits<{ 'update:modelValue': [boolean]; saved: [] }>()

const roster = ref<BoxScoreRoster | null>(null)
const homeRows = ref<BoxScorePlayer[]>([])
const awayRows = ref<BoxScorePlayer[]>([])
const periods = ref<Array<{ periodNo: number; homeScore: number; awayScore: number }>>([])
const reason = ref('')
const warning = ref('')
const recognizing = ref(false)
const saving = ref(false)
const fileInput = ref<HTMLInputElement | null>(null)

const sides = computed(() => [
  { key: 'home' as const, name: roster.value?.homeTeamName || props.match?.homeTeamName || '主队', rows: homeRows.value, points: sum(homeRows.value) },
  { key: 'away' as const, name: roster.value?.awayTeamName || props.match?.awayTeamName || '客队', rows: awayRows.value, points: sum(awayRows.value) },
])

watch(() => props.modelValue, (open) => {
  if (open) void load()
})

function emptyRow(): BoxScorePlayer {
  return {
    points: 0, rebound: 0, assist: 0, steal: 0, block: 0, turnover: 0, foul: 0,
    fieldGoalMade: 0, fieldGoalAttempt: 0, threePointMade: 0, threePointAttempt: 0, freeThrowMade: 0, freeThrowAttempt: 0,
  }
}

function sum(rows: BoxScorePlayer[]) {
  return rows.reduce((total, row) => total + (row.points || 0), 0)
}

function playerLabel(player: BoxScorePlayer) {
  const game = player.gameId?.trim() || ''
  const name = player.playerName?.trim() || ''
  const nickname = name && name !== game ? name : ''
  const title = game ? (nickname ? `${game}（${nickname}）` : game) : (nickname || '未命名')
  return player.jerseyNo ? `#${player.jerseyNo} ${title}` : title
}

function available(side: 'home' | 'away', index: number) {
  const pool = side === 'home' ? roster.value?.homePlayers || [] : roster.value?.awayPlayers || []
  const rows = side === 'home' ? homeRows.value : awayRows.value
  const selected = new Set(rows.map((row, i) => (i === index ? undefined : row.eventPlayerId)).filter(Boolean))
  return pool.filter((player) => !selected.has(player.eventPlayerId))
}

function addRow(side: 'home' | 'away') {
  const rows = side === 'home' ? homeRows : awayRows
  rows.value = [...rows.value, emptyRow()]
}

function removeRow(side: 'home' | 'away', index: number) {
  const rows = side === 'home' ? homeRows : awayRows
  if (rows.value.length <= 1) return
  rows.value = rows.value.filter((_, i) => i !== index)
}

function formula(row: BoxScorePlayer) {
  const fgm = row.fieldGoalMade || 0
  const tpm = row.threePointMade || 0
  const ftm = row.freeThrowMade || 0
  const expected = (fgm - tpm) * 2 + tpm * 3 + ftm
  if ((row.points || 0) === 0 && fgm === 0 && ftm === 0) return ''
  if ((row.points || 0) !== expected) return `PTS 应按公式等于 ${expected}`
  return ''
}

function fromRecognition(players?: BoxScorePlayer[]) {
  const rows = (players || []).map((player) => ({
    ...emptyRow(),
    ...player,
    eventPlayerId: player.eventPlayerId || undefined,
    recognizedName: player.matched ? '' : (player.recognizedName || ''),
  }))
  return rows.length ? rows : [emptyRow()]
}

async function load() {
  warning.value = ''
  reason.value = ''
  roster.value = null
  homeRows.value = [emptyRow()]
  awayRows.value = [emptyRow()]
  periods.value = [1, 2, 3, 4].map((periodNo) => ({ periodNo, homeScore: 0, awayScore: 0 }))
  if (!props.match?.id) return
  const res = await getV4MatchBoxScoreRoster(props.match.id)
  roster.value = res.data || null
}

async function onFile(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file || !props.match?.id) return
  if (!file.type.startsWith('image/') || file.size > 8 * 1024 * 1024) {
    ElMessage.warning('请上传 8MB 以内的 jpg、png 或 webp 截图')
    return
  }
  recognizing.value = true
  try {
    const res = await recognizeV4AdminScreenshot(props.match.id, file)
    const data = res.data
    homeRows.value = fromRecognition(data?.homePlayers)
    awayRows.value = fromRecognition(data?.awayPlayers)
    if (roster.value?.periodRequired && data?.periods?.length) {
      const incoming = new Map(data.periods.map((row) => [row.periodNo, row]))
      const max = Math.max(4, ...data.periods.map((row) => row.periodNo || 0))
      periods.value = Array.from({ length: Math.min(max, 10) }, (_, index) => ({
        periodNo: index + 1,
        homeScore: incoming.get(index + 1)?.homeScore ?? 0,
        awayScore: incoming.get(index + 1)?.awayScore ?? 0,
      }))
    }
    const unmatched = data?.unmatchedCount || 0
    warning.value = unmatched > 0 ? `还有 ${unmatched} 个游戏 ID 没对上名单，请在下拉框里改` : ''
    ElMessage.success(unmatched > 0 ? warning.value : '已按游戏 ID 填入，对上的不用再改')
  } finally {
    recognizing.value = false
  }
}

function used(rows: BoxScorePlayer[]) {
  return rows.filter((row) => row.eventPlayerId || (row.points || 0) + (row.rebound || 0) + (row.fieldGoalMade || 0) > 0)
}

async function submit() {
  if (!props.match?.id) return
  if (!reason.value.trim()) {
    ElMessage.warning('请填写原因')
    return
  }
  const home = used(homeRows.value)
  const away = used(awayRows.value)
  if (!home.length || !away.length) {
    ElMessage.warning('双方都要有球员数据')
    return
  }
  if (home.some((row) => !row.eventPlayerId) || away.some((row) => !row.eventPlayerId)) {
    ElMessage.warning('还有球员没对上名单')
    return
  }
  if ([...home, ...away].some((row) => formula(row))) {
    ElMessage.warning('有球员的 PTS 和投篮数据对不上，请先改')
    return
  }
  await ElMessageBox.confirm('确认后会直接形成这场的正式结果，并重算积分和统计。', '写入正式结果', { type: 'warning' })
  saving.value = true
  try {
    await enterV4MatchBoxScore({
      matchId: props.match.id,
      reason: reason.value.trim(),
      homePlayers: home,
      awayPlayers: away,
      periods: roster.value?.periodRequired ? periods.value : [],
    })
    ElMessage.success('已写入正式结果')
    emit('update:modelValue', false)
    emit('saved')
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.box-file { display: none; }
.box-board {
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  gap: 8px 16px;
  align-items: center;
  margin-bottom: 12px;
  padding: 16px 18px 12px;
  background: #141414;
  color: #f4efe8;
  border-radius: 8px;
}
.box-board span { display: block; color: #9a9188; font-size: 11px; letter-spacing: .08em; }
.box-board strong { font-size: 18px; }
.box-board .is-away { text-align: right; }
.box-board__score { display: flex; align-items: baseline; gap: 10px; }
.box-board__score b { font-size: 40px; line-height: 1; }
.box-board__score em { color: #b90000; font-style: normal; font-weight: 800; }
.box-board p { grid-column: 1 / -1; margin: 0; text-align: center; color: #8d8680; font-size: 12px; }
.box-toolbar { display: flex; align-items: center; gap: 12px; margin-bottom: 12px; }
.box-warn { color: #9a6700; }
.box-periods { display: flex; flex-wrap: wrap; gap: 10px; margin-bottom: 12px; }
.box-q { display: flex; align-items: center; gap: 6px; }
.box-side { margin-bottom: 16px; }
.box-side header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 6px; }
.box-table-wrap { overflow-x: auto; }
.box-table { width: 100%; border-collapse: collapse; font-size: 12px; }
.box-table th, .box-table td { padding: 6px 4px; border-bottom: 1px solid #eee; text-align: left; vertical-align: top; }
.box-table .is-player { min-width: 220px; }
.box-table .is-pair { white-space: nowrap; }
.box-table :deep(.el-input-number) { width: 64px; }
.box-miss { margin-top: 4px; color: #9a6700; font-size: 12px; line-height: 1.3; }
.box-reason { margin-top: 8px; }
</style>
