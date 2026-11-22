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
        <el-input-number v-model="homeScore" :min="0" :max="200" :controls="false" @focus="markScoreFocused(true)" @blur="markScoreFocused(false)" @change="onScoreEdit" />
        <em>:</em>
        <el-input-number v-model="awayScore" :min="0" :max="200" :controls="false" @focus="markScoreFocused(true)" @blur="markScoreFocused(false)" @change="onScoreEdit" />
      </div>
      <div class="is-away">
        <span>AWAY</span>
        <strong>{{ roster?.awayTeamName || match?.awayTeamName || '客队' }}</strong>
      </div>
      <p>
        比分可直接改。有节次时按节次合计，没有节次才按球员 PTS。投篮公式少算的分不要写回 PTS。
        球员合计 {{ playerHome }} : {{ playerAway }}
        <template v-if="playerGap">。{{ playerGap }}</template>
      </p>
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
        <span>PTS 合计 {{ side.points }}</span>
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
import { computed, nextTick, ref, watch } from 'vue'
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
const homeScore = ref(0)
const awayScore = ref(0)
const scoreTouched = ref(false)
const reason = ref('')
const warning = ref('')
const recognizing = ref(false)
const saving = ref(false)
const fileInput = ref<HTMLInputElement | null>(null)

const playerHome = computed(() => sum(homeRows.value))
const playerAway = computed(() => sum(awayRows.value))
const periodHome = computed(() => periods.value.reduce((total, row) => total + (Number(row.homeScore) || 0), 0))
const periodAway = computed(() => periods.value.reduce((total, row) => total + (Number(row.awayScore) || 0), 0))
const periodsActive = computed(() => roster.value?.periodRequired !== false && periods.value.some((row) => (Number(row.homeScore) || 0) + (Number(row.awayScore) || 0) > 0))
const playerGap = computed(() => {
  if (!periodsActive.value) return ''
  const bits: string[] = []
  if (periodHome.value !== playerHome.value) bits.push(`主队节次 ${periodHome.value}，球员合计 ${playerHome.value}`)
  if (periodAway.value !== playerAway.value) bits.push(`客队节次 ${periodAway.value}，球员合计 ${playerAway.value}`)
  return bits.length ? `${bits.join('；')}。比分按节次。少了人就点「添加球员」，不要为了和投篮公式一致去改低 PTS` : ''
})
const sides = computed(() => [
  { key: 'home' as const, name: roster.value?.homeTeamName || props.match?.homeTeamName || '主队', rows: homeRows.value, points: playerHome.value },
  { key: 'away' as const, name: roster.value?.awayTeamName || props.match?.awayTeamName || '客队', rows: awayRows.value, points: playerAway.value },
])

watch([playerHome, playerAway, periodHome, periodAway, periodsActive], () => {
  if (scoreTouched.value) return
  if (periodsActive.value) setScores(periodHome.value, periodAway.value)
  else setScores(playerHome.value, playerAway.value)
})

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
  return rows.reduce((total, row) => total + (Number(row.points) || 0), 0)
}

let applyingAutoScore = false
let scoreFieldFocused = false

function setScores(home, away) {
  const nextHome = Number(home) || 0
  const nextAway = Number(away) || 0
  if (homeScore.value === nextHome && awayScore.value === nextAway) return
  applyingAutoScore = true
  homeScore.value = nextHome
  awayScore.value = nextAway
  nextTick(() => {
    applyingAutoScore = false
  })
}

function applyAutoScore() {
  if (scoreTouched.value) return
  if (periodsActive.value) setScores(periodHome.value, periodAway.value)
  else setScores(playerHome.value, playerAway.value)
}

function markScoreFocused(focused: boolean) {
  scoreFieldFocused = focused
}

function onScoreEdit() {
  if (applyingAutoScore || !scoreFieldFocused) return
  scoreTouched.value = true
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
  const pts = row.points || 0
  const fgm = row.fieldGoalMade || 0
  const tpm = row.threePointMade || 0
  const ftm = row.freeThrowMade || 0
  if (pts === 0 && fgm === 0 && tpm === 0 && ftm === 0) return ''
  const fgIncludesThree = (fgm - tpm) * 2 + tpm * 3 + ftm
  const fgIsTwos = fgm * 2 + tpm * 3 + ftm
  if (pts === fgIncludesThree || pts === fgIsTwos) return ''
  return '投篮数据和 PTS 对不上。比分按 PTS 和节次，不要把 PTS 改成公式算出来的数'
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
  scoreTouched.value = false
  homeScore.value = 0
  awayScore.value = 0
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
    scoreTouched.value = false
    applyAutoScore()
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
  const notes = ['确认后会直接形成这场的正式结果，并重算积分和统计。']
  if (homeScore.value !== playerHome.value || awayScore.value !== playerAway.value) {
    notes.push(`写入比分 ${homeScore.value}:${awayScore.value}，球员合计是 ${playerHome.value}:${playerAway.value}。`)
  }
  if (roster.value?.periodRequired !== false && (homeScore.value !== periodHome.value || awayScore.value !== periodAway.value)) {
    ElMessage.warning(`节次合计是 ${periodHome.value}:${periodAway.value}，和上面的比分不一致，请改节比分或比分`)
    return
  }
  if ([...home, ...away].some((row) => formula(row))) {
    notes.push('有球员的 PTS 和投篮数据对不上，仍按当前填写写入。')
  }
  await ElMessageBox.confirm(notes.join('\n'), '写入正式结果', { type: 'warning' })
  saving.value = true
  try {
    await enterV4MatchBoxScore({
      matchId: props.match.id,
      reason: reason.value.trim(),
      homeScore: homeScore.value,
      awayScore: awayScore.value,
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
.box-board__score { display: flex; align-items: center; gap: 8px; }
.box-board__score :deep(.el-input-number) { width: 88px; }
.box-board__score :deep(.el-input__wrapper) { background: transparent; box-shadow: 0 0 0 1px #5c564f inset; }
.box-board__score :deep(.el-input__inner) { color: #f4efe8; font-size: 32px; font-weight: 700; height: 44px; }
.box-board__score em { color: #b90000; font-style: normal; font-weight: 800; font-size: 32px; }
.box-board p { grid-column: 1 / -1; margin: 0; text-align: center; color: #8d8680; font-size: 12px; }
.box-toolbar { display: flex; align-items: center; gap: 12px; margin-bottom: 12px; }
.box-warn { color: #9a6700; }
.box-periods { display: flex; flex-wrap: wrap; gap: 10px; margin-bottom: 12px; }
.box-q { display: flex; align-items: center; gap: 6px; }
.box-side { margin-bottom: 16px; }
.box-side header { display: flex; align-items: center; gap: 12px; margin-bottom: 6px; }
.box-side header span { color: #9a6700; font-size: 13px; }
.box-side header .el-button { margin-left: auto; }
.box-table-wrap { overflow-x: auto; }
.box-table { width: 100%; border-collapse: collapse; font-size: 12px; }
.box-table th, .box-table td { padding: 6px 4px; border-bottom: 1px solid #eee; text-align: left; vertical-align: top; }
.box-table .is-player { min-width: 220px; }
.box-table .is-pair { white-space: nowrap; }
.box-table :deep(.el-input-number) { width: 64px; }
.box-miss { margin-top: 4px; color: #9a6700; font-size: 12px; line-height: 1.3; }
.box-reason { margin-top: 8px; }
</style>
