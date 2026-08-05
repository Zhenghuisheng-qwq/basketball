<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { DocumentCopy } from '@element-plus/icons-vue'
import {
  approveSubmission,
  getSubmissionDetail,
  rejectSubmission,
  type MatchDataAuditDetail,
} from '@/api/basket/matchAudit'
import { parseTime } from '@/utils/ruoyi'
import {
  formatFileSize,
  formatVersion,
  REJECT_REASON_PRESETS,
  startedFlagText,
  submissionStatusTagType,
} from '@/utils/matchDataAuditLabels'

const props = defineProps<{
  modelValue: boolean
  submissionId?: number
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  refreshed: []
}>()

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value),
})

const loading = ref(false)
const approving = ref(false)
const rejecting = ref(false)
const rejectOpen = ref(false)
const rejectReason = ref('')
const activeTab = ref('data')
const detail = ref<MatchDataAuditDetail | null>(null)
const showDiffDetail = ref(false)

const periodSummary = computed(() => {
  const periods = detail.value?.periods || []
  const homeTotal = periods.reduce((sum, item) => sum + (item.homeScore ?? 0), 0)
  const awayTotal = periods.reduce((sum, item) => sum + (item.awayScore ?? 0), 0)
  return { homeTotal, awayTotal, diff: homeTotal - awayTotal }
})

const footerHint = computed(() => {
  const summary = detail.value?.validationSummary
  if (!summary) return detail.value?.submissionStatusName || '-'
  const parts = [detail.value?.submissionStatusName || '-']
  if ((summary.errorCount ?? 0) > 0) parts.push(`${summary.errorCount} 项异常`)
  else if ((summary.warningCount ?? 0) > 0) parts.push(`${summary.warningCount} 项提醒`)
  return parts.join(' · ')
})

watch(
  () => [props.modelValue, props.submissionId] as const,
  ([open, submissionId]) => {
    if (open && submissionId) {
      activeTab.value = 'data'
      showDiffDetail.value = false
      void loadDetail(submissionId)
    }
  },
  { immediate: true },
)

async function loadDetail(submissionId: number) {
  loading.value = true
  try {
    const res = await getSubmissionDetail(submissionId)
    detail.value = res.data
  } finally {
    loading.value = false
  }
}

function closeDialog() {
  visible.value = false
}

async function copySubmissionNo() {
  const text = detail.value?.submissionNo
  if (!text) return
  await navigator.clipboard.writeText(text)
  ElMessage.success('提交编号已复制')
}

function teamInitial(name?: string): string {
  if (!name) return '队'
  return name.trim().slice(0, 1)
}

function appendRejectPreset(text: string) {
  rejectReason.value = rejectReason.value ? `${rejectReason.value}\n${text}` : text
}

function openRejectDialog() {
  rejectReason.value = ''
  rejectOpen.value = true
}

async function confirmApprove() {
  if (!detail.value?.submissionId) return
  if (detail.value.submitSideAbnormal) {
    ElMessage.error('提交方异常，无法审核通过')
    return
  }
  const summary = detail.value.validationSummary
  const errorCount = summary?.errorCount ?? 0
  const warningCount = summary?.warningCount ?? 0
  const lines = [
    `比赛：${detail.value.homeTeamName} VS ${detail.value.awayTeamName}`,
    `提交方：${detail.value.submitterTeamName}（${detail.value.submitSideText}）`,
    `版本：${formatVersion(detail.value.versionNo)}`,
    `提交比分：${detail.value.homeScore} : ${detail.value.awayScore}`,
    `对方状态：${detail.value.comparisonResultText || '-'}`,
  ]
  if (errorCount > 0) lines.push(`数据校验：存在 ${errorCount} 项异常`)
  else if (warningCount > 0) lines.push(`数据校验：存在 ${warningCount} 项提醒`)
  else lines.push('数据校验：通过')

  const message = lines.join('\n')
  const confirmText = errorCount > 0
    ? `当前数据存在 ${errorCount} 项校验异常，仍要审核通过吗？`
    : warningCount > 0
      ? `当前数据存在 ${warningCount} 项提醒，确认审核通过吗？`
      : '确认审核通过该提交吗？'

  await ElMessageBox.confirm(`${message}\n\n${confirmText}`, '审核通过确认', {
    confirmButtonText: '审核通过',
    cancelButtonText: '取消',
    type: errorCount > 0 ? 'warning' : 'success',
  })

  approving.value = true
  try {
    await approveSubmission(detail.value.submissionId, {})
    ElMessage.success('审核通过')
    emit('refreshed')
    await loadDetail(detail.value.submissionId)
  } finally {
    approving.value = false
  }
}

async function submitReject() {
  const reason = rejectReason.value.trim()
  if (reason.length < 5) {
    ElMessage.warning('驳回原因至少 5 个字')
    return
  }
  if (reason.length > 500) {
    ElMessage.warning('驳回原因不能超过 500 字')
    return
  }
  if (!detail.value?.submissionId) return
  rejecting.value = true
  try {
    await rejectSubmission(detail.value.submissionId, { rejectReason: reason })
    ElMessage.success('已驳回')
    rejectOpen.value = false
    emit('refreshed')
    await loadDetail(detail.value.submissionId)
  } finally {
    rejecting.value = false
  }
}
</script>

<template>
  <el-dialog
    v-model="visible"
    width="86%"
    top="6vh"
    class="audit-detail-dialog"
    destroy-on-close
    append-to-body
    :close-on-click-modal="false"
    @closed="detail = null"
  >
    <template #header>
      <div class="match-audit-dialog__header">
        <div>
          <div class="match-audit-dialog__title">比赛数据提交审核</div>
          <div v-if="detail" class="match-audit-dialog__meta">
            <span class="match-audit-dialog__copy" @click="copySubmissionNo">
              提交编号：{{ detail.submissionNo }}
              <el-icon><DocumentCopy /></el-icon>
            </span>
            <span>{{ formatVersion(detail.versionNo) }}</span>
            <el-tag :type="submissionStatusTagType(detail.submissionStatus, detail.auditStatus)" size="small">
              {{ detail.submissionStatusName || detail.auditStatusText }}
            </el-tag>
          </div>
        </div>
      </div>
    </template>

    <div v-loading="loading" class="audit-dialog-body">
      <template v-if="detail">
        <div class="audit-dialog-top">
        <section class="match-audit-dialog__summary">
          <div class="match-audit-dialog__match-card" :class="{ 'is-submit-side': detail.homeTeam?.currentSubmissionSide }">
            <el-avatar :size="56" :src="detail.homeTeam?.logoUrl || undefined" class="match-audit-dialog__avatar">
              {{ teamInitial(detail.homeTeam?.teamName) }}
            </el-avatar>
            <div class="match-audit-dialog__team-name" :title="detail.homeTeam?.teamName">{{ detail.homeTeam?.teamName || detail.homeTeamName }}</div>
            <div class="match-audit-dialog__team-side">主队</div>
            <el-tag v-if="detail.homeTeam?.currentSubmissionSide" type="danger" size="small">本次提交方</el-tag>
            <div class="match-audit-dialog__side-status">{{ detail.homeTeam?.submissionStatusText || '未提交' }}</div>
          </div>

          <div class="match-audit-dialog__score-block">
            <div class="match-audit-dialog__score-label">提交比分</div>
            <div class="match-audit-dialog__score">{{ detail.homeScore ?? '-' }} : {{ detail.awayScore ?? '-' }}</div>
            <div class="match-audit-dialog__score-sub">
              {{ detail.stageName || '-' }} · {{ detail.roundName || '-' }}
            </div>
            <div class="match-audit-dialog__score-sub">{{ parseTime(detail.matchTime) }}</div>
            <div class="match-audit-dialog__score-sub">比赛编号：{{ detail.matchNo }}</div>
            <div v-if="detail.officialScore?.confirmed" class="match-audit-dialog__official-score">
              正式比分 {{ detail.officialScore.homeScore }} : {{ detail.officialScore.awayScore }}
            </div>
            <el-alert
              v-if="detail.submitSideAbnormal"
              title="提交方不属于本场比赛主客队，禁止审核通过"
              type="error"
              :closable="false"
              show-icon
              class="mt8"
            />
          </div>

          <div class="match-audit-dialog__match-card" :class="{ 'is-submit-side': detail.awayTeam?.currentSubmissionSide }">
            <el-avatar :size="56" :src="detail.awayTeam?.logoUrl || undefined" class="match-audit-dialog__avatar">
              {{ teamInitial(detail.awayTeam?.teamName) }}
            </el-avatar>
            <div class="match-audit-dialog__team-name" :title="detail.awayTeam?.teamName">{{ detail.awayTeam?.teamName || detail.awayTeamName }}</div>
            <div class="match-audit-dialog__team-side">客队</div>
            <el-tag v-if="detail.awayTeam?.currentSubmissionSide" type="danger" size="small">本次提交方</el-tag>
            <div class="match-audit-dialog__side-status">{{ detail.awayTeam?.submissionStatusText || '未提交' }}</div>
          </div>
        </section>

        <section class="match-audit-dialog__info-grid">
          <div class="match-audit-dialog__info-card">
            <div class="match-audit-dialog__info-title">提交信息</div>
            <div class="match-audit-dialog__submitter">
              <el-avatar :size="44" :src="detail.submitter?.avatarUrl || undefined">
                {{ (detail.submitter?.displayName || '提').slice(0, 1) }}
              </el-avatar>
              <div class="match-audit-dialog__submitter-main">
                <div class="match-audit-dialog__submitter-name">{{ detail.submitter?.displayName || '提交人信息缺失' }}</div>
                <div class="match-audit-dialog__submitter-sub">{{ detail.submitter?.playerCode || '-' }}</div>
                <div class="match-audit-dialog__submitter-sub">{{ detail.submitter?.roleText || detail.submitterRoleText || '-' }}</div>
              </div>
            </div>
            <div class="match-audit-dialog__desc-grid">
              <div class="match-audit-dialog__desc-item"><span>所属球队</span><span>{{ detail.submitter?.teamName || detail.submitterTeamName || '-' }}</span></div>
              <div class="match-audit-dialog__desc-item"><span>提交时间</span><span>{{ parseTime(detail.submittedTime) }}</span></div>
              <div class="match-audit-dialog__desc-item"><span>提交账号</span><span>{{ detail.submitter?.displayEmail || '-' }}</span></div>
              <div class="match-audit-dialog__desc-item"><span>游戏 ID</span><span>{{ detail.submitter?.gameId || '-' }}</span></div>
            </div>
          </div>

          <div class="match-audit-dialog__info-card">
            <div class="match-audit-dialog__info-title">双方提交状态</div>
            <div class="match-audit-dialog__status-list">
              <div class="match-audit-dialog__status-item">
                <span>主队数据</span>
                <span>
                  {{ detail.homeTeam?.submissionStatusText || '未提交' }}
                  <template v-if="detail.homeTeam?.submissionVersion"> · {{ formatVersion(detail.homeTeam.submissionVersion) }}</template>
                </span>
              </div>
              <div class="match-audit-dialog__status-item">
                <span>客队数据</span>
                <span>
                  {{ detail.awayTeam?.submissionStatusText || '未提交' }}
                  <template v-if="detail.awayTeam?.submissionVersion"> · {{ formatVersion(detail.awayTeam.submissionVersion) }}</template>
                </span>
              </div>
              <div class="match-audit-dialog__status-item"><span>本次提交方</span><span>{{ detail.submitterTeamName }}（{{ detail.submitSideText }}）</span></div>
              <div class="match-audit-dialog__status-item"><span>核对结果</span><span>{{ detail.comparisonResultText || '-' }}</span></div>
              <div class="match-audit-dialog__status-item"><span>赛事</span><span>{{ detail.seasonName || '-' }}</span></div>
              <div class="match-audit-dialog__status-item"><span>比赛状态</span><span>{{ detail.matchStatusText || '-' }}</span></div>
            </div>
          </div>
        </section>
        </div>

        <el-tabs v-model="activeTab" class="match-audit-dialog__tabs">
          <el-tab-pane label="比赛数据" name="data">
            <div class="match-audit-dialog__tab-scroll">
              <div v-if="detail.validationSummary" class="match-audit-dialog__validation">
                <div class="match-audit-dialog__validation-head">
                  <span class="match-audit-dialog__info-title">数据校验摘要</span>
                  <el-tag :type="detail.validationSummary.allPassed ? 'success' : 'danger'" size="small">
                    {{ detail.validationSummary.allPassed ? '通过' : `${detail.validationSummary.errorCount || 0} 项异常` }}
                  </el-tag>
                </div>
                <div class="match-audit-dialog__validation-grid">
                  <div
                    v-for="(item, index) in detail.validationSummary.items || []"
                    :key="index"
                    class="match-audit-dialog__validation-item"
                    :class="`is-${(item.level || 'ok').toLowerCase()}`"
                  >
                    {{ item.passed ? '✓' : '×' }} {{ item.message }}
                  </div>
                </div>
              </div>

              <div class="match-audit-dialog__period-row">
                <div class="match-audit-dialog__period-main">
                  <div class="match-audit-dialog__section-title">分节比分</div>
                  <div class="match-audit-dialog__period-table-wrap">
                    <el-table :data="detail.periods || []" border size="small" empty-text="暂无分节比分">
                      <el-table-column label="节次" prop="periodNo" width="72">
                        <template #default="{ row }">{{ row.periodType === 'OVERTIME' ? `加时${row.periodNo}` : `第${row.periodNo}节` }}</template>
                      </el-table-column>
                      <el-table-column label="类型" prop="periodTypeName" width="88" />
                      <el-table-column label="主队" prop="homeScore" width="64" align="center" />
                      <el-table-column label="客队" prop="awayScore" width="64" align="center" />
                      <el-table-column label="差值" width="64" align="center">
                        <template #default="{ row }">{{ row.scoreDiff >= 0 ? `+${row.scoreDiff}` : row.scoreDiff }}</template>
                      </el-table-column>
                      <el-table-column label="校验" prop="validationStatus" width="64" align="center" />
                    </el-table>
                  </div>
                </div>
                <div v-if="detail.periods?.length" class="match-audit-dialog__period-side">
                  <div class="match-audit-dialog__section-title">比分合计</div>
                  <div class="match-audit-dialog__period-total-box">
                    <div>{{ periodSummary.homeTotal }} : {{ periodSummary.awayTotal }}</div>
                    <div class="match-audit-dialog__period-total-sub">差值 {{ periodSummary.diff >= 0 ? '+' : '' }}{{ periodSummary.diff }}</div>
                  </div>
                </div>
              </div>

              <div class="match-audit-dialog__section-title">球队统计</div>
              <div v-if="!(detail.teamStats || []).length" class="match-audit-dialog__empty">暂无球队统计</div>
              <div v-else class="match-audit-dialog__team-stats">
                <div v-for="team in detail.teamStats" :key="String(team.teamId)" class="match-audit-dialog__team-stat-card">
                  <el-avatar :size="48" :src="(team.logoUrl as string) || undefined">{{ teamInitial(team.teamName as string) }}</el-avatar>
                  <div class="match-audit-dialog__team-stat-name">{{ team.teamName || '-' }}（{{ team.sideText || '-' }}）</div>
                  <div>得分：{{ team.points ?? '-' }}</div>
                  <div>篮板：{{ team.rebounds ?? '-' }}</div>
                  <div>助攻：{{ team.assists ?? '-' }}</div>
                </div>
              </div>

              <div class="match-audit-dialog__section-title">球员统计</div>
              <div class="match-audit-dialog__player-table-wrap">
              <el-table :data="detail.playerStats || []" border size="small" height="320" empty-text="暂无球员数据">
                <el-table-column label="球员" min-width="180">
                  <template #default="{ row }">
                    <div class="match-audit-dialog__player-cell">
                      <el-avatar :size="32" :src="row.avatarUrl || undefined">{{ (row.playerName || '球').slice(0, 1) }}</el-avatar>
                      <div>
                        <div>{{ row.playerName || '-' }}</div>
                        <div class="match-audit-dialog__player-sub">{{ row.playerCode || '-' }}</div>
                      </div>
                    </div>
                  </template>
                </el-table-column>
                <el-table-column label="游戏 ID" prop="gameId" min-width="120" show-overflow-tooltip />
                <el-table-column label="位置" prop="positionName" width="90" />
                <el-table-column label="得分" prop="points" width="70" />
                <el-table-column label="篮板" prop="rebounds" width="70" />
                <el-table-column label="助攻" prop="assists" width="70" />
                <el-table-column label="抢断" prop="steals" width="70" />
                <el-table-column label="盖帽" prop="blocks" width="70" />
                <el-table-column label="出场" width="80">
                  <template #default="{ row }">{{ startedFlagText(row.startedFlag as number) }}</template>
                </el-table-column>
              </el-table>
              </div>

              <div class="match-audit-dialog__section-title">双方数据差异</div>
              <div
                v-if="!detail.comparisonSummary?.opponentSubmitted"
                class="match-audit-dialog__compact-empty"
              >
                对方尚未提交比赛数据，暂无法进行双方核对
              </div>
              <template v-else>
                <el-alert :title="detail.comparisonSummary.summaryText || '-'" :type="(detail.comparisonSummary.diffCount || 0) > 0 ? 'warning' : 'success'" :closable="false" show-icon />
                <el-button v-if="(detail.comparisonSummary.diffs || []).length" link type="primary" @click="showDiffDetail = !showDiffDetail">
                  {{ showDiffDetail ? '收起差异明细' : `查看 ${detail.comparisonSummary.diffCount} 项差异` }}
                </el-button>
                <el-table v-if="showDiffDetail" :data="detail.comparisonSummary.diffs || []" border size="small" class="mt8">
                  <el-table-column label="字段" prop="fieldLabel" width="120" />
                  <el-table-column label="本方" prop="selfValue" />
                  <el-table-column label="对方" prop="opponentValue" />
                </el-table>
              </template>
            </div>
          </el-tab-pane>

          <el-tab-pane :label="`证明材料（${detail.materials?.length || 0}）`" name="materials">
            <div class="match-audit-dialog__tab-scroll">
              <div v-if="!(detail.materials || []).length" class="match-audit-dialog__compact-empty">暂无截图、录像或其他证明材料</div>
              <div v-else class="match-audit-dialog__material-grid">
                <div v-for="material in detail.materials" :key="String(material.id || material.fileId)" class="match-audit-dialog__material-item">
                  <div class="match-audit-dialog__material-title">{{ material.materialTypeName || material.materialType }}</div>
                  <div class="match-audit-dialog__material-meta">{{ material.materialName || '-' }}</div>
                  <div class="match-audit-dialog__material-meta">{{ formatFileSize(material.fileSize as number) }} · {{ parseTime(material.uploadTime as string) }}</div>
                  <el-image
                    v-if="material.fileUrl"
                    :src="material.fileUrl as string"
                    :preview-src-list="(detail.materials || []).map(item => item.fileUrl).filter(Boolean) as string[]"
                    fit="cover"
                    class="match-audit-dialog__material-image"
                  />
                  <div v-else class="match-audit-dialog__empty-inline">文件不可用</div>
                  <el-link v-if="material.fileUrl" :href="material.fileUrl as string" target="_blank" type="primary">下载/打开</el-link>
                </div>
              </div>
            </div>
          </el-tab-pane>

          <el-tab-pane :label="`版本记录（${detail.versions?.length || 0}）`" name="versions">
            <div class="match-audit-dialog__tab-scroll">
              <el-table :data="detail.versions || []" border size="small" empty-text="暂无版本历史">
                <el-table-column label="版本" width="80">
                  <template #default="{ row }">{{ formatVersion(row.versionNo as number) }}</template>
                </el-table-column>
                <el-table-column label="状态" prop="submissionStatusName" width="100" />
                <el-table-column label="提交人" prop="submitterMemberName" min-width="120" />
                <el-table-column label="提交时间" width="170">
                  <template #default="{ row }">{{ parseTime(row.submittedTime as string) }}</template>
                </el-table-column>
                <el-table-column label="驳回原因" prop="rejectReason" min-width="180" show-overflow-tooltip />
                <el-table-column label="标识" width="100">
                  <template #default="{ row }">
                    <el-tag v-if="row.isCurrent" type="warning" size="small">当前版本</el-tag>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </el-tab-pane>

          <el-tab-pane :label="`审核记录（${detail.auditLogs?.length || 0}）`" name="logs">
            <div class="match-audit-dialog__tab-scroll">
              <div v-if="!(detail.auditLogs || []).length" class="match-audit-dialog__compact-empty">暂无审核记录</div>
              <el-timeline v-else>
                <el-timeline-item
                  v-for="log in detail.auditLogs"
                  :key="String(log.id)"
                  :timestamp="parseTime(log.auditTime as string)"
                  placement="top"
                >
                  <div class="match-audit-dialog__timeline-title">
                    {{ log.auditActionName || log.auditAction }}
                    <span v-if="log.versionNo"> · {{ formatVersion(log.versionNo as number) }}</span>
                  </div>
                  <div>{{ log.auditorName || '系统操作' }}</div>
                  <div v-if="log.auditStatusText">状态：{{ log.auditStatusText }}</div>
                  <div v-if="log.rejectReason">原因：{{ log.rejectReason }}</div>
                </el-timeline-item>
              </el-timeline>
            </div>
          </el-tab-pane>
        </el-tabs>

        <el-alert v-if="!detail.canAudit && detail.cannotAuditReason" :title="detail.cannotAuditReason" type="info" show-icon :closable="false" class="mt8" />
      </template>
    </div>

    <template #footer>
      <div class="match-audit-dialog__footer">
        <div class="match-audit-dialog__footer-left">{{ footerHint }} · {{ formatVersion(detail?.versionNo) }}</div>
        <div class="match-audit-dialog__footer-actions">
          <el-button @click="closeDialog">关闭</el-button>
          <el-button
            v-hasPermi="['basket:matchAudit:reject']"
            type="danger"
            :disabled="!detail?.permissions?.canReject"
            :loading="rejecting"
            @click="openRejectDialog"
          >
            驳回
          </el-button>
          <el-button
            v-hasPermi="['basket:matchAudit:approve']"
            type="success"
            :disabled="!detail?.permissions?.canApprove || detail?.submitSideAbnormal"
            :loading="approving"
            @click="confirmApprove"
          >
            审核通过
          </el-button>
        </div>
      </div>
    </template>
  </el-dialog>

  <el-dialog v-model="rejectOpen" title="驳回比赛数据" width="520px" append-to-body>
    <div class="match-audit-dialog__reject-presets">
      <el-tag v-for="item in REJECT_REASON_PRESETS" :key="item" class="match-audit-dialog__preset" @click="appendRejectPreset(item)">{{ item }}</el-tag>
    </div>
    <el-input v-model="rejectReason" type="textarea" :rows="5" maxlength="500" show-word-limit placeholder="请填写驳回原因（5-500字）" />
    <template #footer>
      <el-button @click="rejectOpen = false">取消</el-button>
      <el-button type="danger" :loading="rejecting" @click="submitReject">确认驳回</el-button>
    </template>
  </el-dialog>
</template>

<style lang="scss">
.audit-detail-dialog.el-dialog {
  width: 86% !important;
  max-width: 1680px;
  height: 86vh;
  max-height: 88vh;
  margin-top: 6vh !important;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.audit-detail-dialog .el-dialog__header {
  flex: 0 0 auto;
  padding: 12px 16px 8px;
  margin-right: 0;
}

.audit-detail-dialog .el-dialog__body {
  flex: 1 1 auto;
  min-height: 0;
  overflow: hidden;
  padding: 0 16px 8px;
  display: flex;
  flex-direction: column;
}

.audit-detail-dialog .el-dialog__footer {
  flex: 0 0 auto;
  padding: 10px 16px;
  border-top: 1px solid #ebeef5;
}

@media (max-width: 1500px) {
  .audit-detail-dialog.el-dialog {
    width: 91% !important;
  }
}

@media (max-width: 1280px) {
  .audit-detail-dialog.el-dialog {
    width: 94% !important;
  }
}
</style>

<style scoped lang="scss">
.audit-dialog-body {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.audit-dialog-top {
  flex: 0 0 auto;
}

.match-audit-dialog__header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.match-audit-dialog__title {
  font-size: 17px;
  font-weight: 700;
}

.match-audit-dialog__meta {
  display: flex;
  gap: 10px;
  align-items: center;
  margin-top: 6px;
  color: #666;
  font-size: 12px;
}

.match-audit-dialog__copy {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  cursor: pointer;
}

.match-audit-dialog__summary {
  display: grid;
  grid-template-columns: minmax(0, 1fr) clamp(230px, 18%, 300px) minmax(0, 1fr);
  gap: 12px;
  margin-bottom: 12px;
}

.match-audit-dialog__match-card {
  border: 1px solid #ebeef5;
  border-radius: 6px;
  padding: 12px;
  min-height: 140px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  background: #fafafa;

  &.is-submit-side {
    border-color: #f56c6c;
    background: #fff7f7;
  }
}

.match-audit-dialog__avatar {
  margin-bottom: 6px;
}

.match-audit-dialog__team-name {
  font-weight: 700;
  font-size: 14px;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.match-audit-dialog__team-side,
.match-audit-dialog__side-status {
  color: #666;
  font-size: 12px;
  margin-top: 2px;
  line-height: 1.4;
}

.match-audit-dialog__score-block {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  padding: 12px;
  min-height: 140px;
}

.match-audit-dialog__score-label {
  color: #666;
  font-size: 12px;
}

.match-audit-dialog__score {
  font-size: 38px;
  font-weight: 800;
  line-height: 1.15;
  margin: 6px 0;
}

.match-audit-dialog__score-sub,
.match-audit-dialog__official-score {
  color: #666;
  font-size: 12px;
  margin-top: 2px;
  line-height: 1.4;
  text-align: center;
}

.match-audit-dialog__info-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 12px;
}

.match-audit-dialog__info-card {
  border: 1px solid #ebeef5;
  border-radius: 6px;
  padding: 12px;
  min-height: 132px;
}

.match-audit-dialog__info-title {
  font-weight: 700;
  font-size: 14px;
  margin-bottom: 8px;
}

.match-audit-dialog__submitter {
  display: flex;
  gap: 10px;
  margin-bottom: 8px;
  align-items: center;
}

.match-audit-dialog__submitter-main {
  min-width: 0;
}

.match-audit-dialog__submitter-name {
  font-weight: 700;
  font-size: 14px;
  line-height: 1.3;
}

.match-audit-dialog__submitter-sub,
.match-audit-dialog__player-sub {
  color: #666;
  font-size: 12px;
  line-height: 1.35;
}

.match-audit-dialog__desc-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 6px 12px;
}

.match-audit-dialog__desc-item {
  display: grid;
  grid-template-columns: 68px minmax(0, 1fr);
  gap: 8px;
  font-size: 12px;
  line-height: 1.4;

  span:first-child {
    color: #666;
  }

  span:last-child {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.match-audit-dialog__status-list {
  display: grid;
  gap: 4px;
}

.match-audit-dialog__status-item {
  display: grid;
  grid-template-columns: 88px minmax(0, 1fr);
  gap: 8px;
  font-size: 12px;
  line-height: 1.45;

  span:first-child {
    color: #666;
  }

  span:last-child {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.match-audit-dialog__tabs {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;

  :deep(.el-tabs__header) {
    flex: 0 0 auto;
    margin-bottom: 0;
  }

  :deep(.el-tabs__nav-wrap) {
    height: 42px;
  }

  :deep(.el-tabs__content) {
    flex: 1;
    min-height: 0;
    overflow: hidden;
  }

  :deep(.el-tab-pane) {
    height: 100%;
  }
}

.match-audit-dialog__tab-scroll {
  height: 100%;
  overflow-y: auto;
  overflow-x: hidden;
  padding-top: 8px;
  padding-right: 2px;
}

.match-audit-dialog__section-title {
  font-weight: 700;
  font-size: 14px;
  margin: 12px 0 8px;

  &:first-child {
    margin-top: 0;
  }
}

.match-audit-dialog__validation {
  border: 1px solid #ebeef5;
  border-radius: 6px;
  padding: 10px 12px;
  margin-bottom: 12px;
}

.match-audit-dialog__validation-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 8px;

  .match-audit-dialog__info-title {
    margin-bottom: 0;
  }
}

.match-audit-dialog__validation-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 4px 12px;
}

.match-audit-dialog__validation-item {
  font-size: 12px;
  line-height: 1.5;
  min-height: 24px;

  &.is-error { color: #f56c6c; }
  &.is-warn { color: #e6a23c; }
  &.is-ok { color: #67c23a; }
}

.match-audit-dialog__period-row {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(180px, 0.6fr);
  gap: 12px;
  align-items: start;
}

.match-audit-dialog__period-table-wrap {
  width: fit-content;
  max-width: 100%;
  overflow-x: auto;
}

.match-audit-dialog__period-side {
  min-width: 0;
}

.match-audit-dialog__period-total-box {
  border: 1px solid #ebeef5;
  border-radius: 6px;
  padding: 12px;
  font-size: 20px;
  font-weight: 700;
  line-height: 1.3;
}

.match-audit-dialog__period-total-sub {
  margin-top: 6px;
  font-size: 12px;
  font-weight: 400;
  color: #666;
}

.match-audit-dialog__team-stats {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.match-audit-dialog__team-stat-card {
  border: 1px solid #ebeef5;
  border-radius: 6px;
  padding: 10px 12px;
  font-size: 13px;
  line-height: 1.5;
}

.match-audit-dialog__team-stat-name {
  font-weight: 700;
  margin: 6px 0 4px;
}

.match-audit-dialog__player-table-wrap {
  width: 100%;
  overflow-x: auto;
}

.match-audit-dialog__player-cell {
  display: flex;
  gap: 8px;
  align-items: center;
}

.match-audit-dialog__material-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 12px;
}

.match-audit-dialog__material-item {
  border: 1px solid #ebeef5;
  border-radius: 6px;
  padding: 10px 12px;
}

.match-audit-dialog__material-title {
  font-weight: 700;
  font-size: 13px;
}

.match-audit-dialog__material-meta {
  color: #666;
  font-size: 12px;
  margin-top: 2px;
}

.match-audit-dialog__material-image {
  width: 100%;
  height: 120px;
  margin: 8px 0;
  border-radius: 4px;
}

.match-audit-dialog__timeline-title {
  font-weight: 700;
}

.match-audit-dialog__footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  gap: 12px;
  min-height: 44px;
}

.match-audit-dialog__footer-left {
  color: #666;
  font-size: 13px;
}

.match-audit-dialog__footer-actions {
  display: flex;
  gap: 8px;
}

.match-audit-dialog__empty,
.match-audit-dialog__empty-inline {
  color: #999;
  font-size: 13px;
  padding: 8px 0;
}

.match-audit-dialog__compact-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 96px;
  max-height: 120px;
  padding: 12px;
  border: 1px dashed #dcdfe6;
  border-radius: 6px;
  color: #909399;
  font-size: 13px;
  text-align: center;
  background: #fafafa;
}

.match-audit-dialog__reject-presets {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}

.match-audit-dialog__preset {
  cursor: pointer;
}

.mt8 { margin-top: 8px; }

@media (max-width: 1500px) {
  .match-audit-dialog__validation-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 1280px) {
  .match-audit-dialog__summary {
    grid-template-columns: minmax(0, 1fr) clamp(210px, 22%, 260px) minmax(0, 1fr);
  }

  .match-audit-dialog__desc-grid {
    grid-template-columns: 1fr;
  }

  .match-audit-dialog__period-row {
    grid-template-columns: 1fr;
  }

  .match-audit-dialog__validation-grid {
    grid-template-columns: 1fr;
  }
}
</style>
