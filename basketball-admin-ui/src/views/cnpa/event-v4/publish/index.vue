<template>
  <div class="app-container">
    <el-alert
      title="确定之前，规则、结构、日期、赛程都能改。发布只是让当前这一步生效，没开赛时可以退回再改。"
      type="info"
      :closable="false"
      show-icon
      class="mb8 page-tip-alert"
    >
      <template #default>
        <ul class="page-tip-list">
          <li>1 规则：本页。没点「发布规则」之前直接改。已发布后，点「退回修改」。</li>
          <li>2 结构：谁和谁打。在「2 结构」生成，没开赛可以作废重来。</li>
          <li>3 日期：什么时候打。在「3 日期」改。赛程撤回后也能改。</li>
          <li>4 赛程：公开给官网。在「4 赛程」发布，也可以撤回后再改日期。</li>
        </ul>
      </template>
    </el-alert>

    <DivisionEventSelector
      v-if="!workspaceDivisionId"
      ref="selectorRef"
      entry="publish"
      show-more
      @select="row => selectDivision(row.divisionId)"
      @more="onSelectorMore"
    >
      <template #actions>
        <el-button type="primary" icon="Plus" v-hasPermi="['basket:v4Season:add']" @click="openCreateWizard">新建赛事</el-button>
        <el-button v-hasPermi="['basket:v4Season:list']" @click="goSeasonManage()">赛季管理</el-button>
      </template>
    </DivisionEventSelector>

    <template v-else>
      <DivisionContextHeader :overview="overview" show-switch @switch="clearDivision" />
      <DivisionWorkspaceNav />
      <DivisionFormatRulesPanel
        v-if="workspaceDivision"
        :key="workspaceDivisionId"
        :division-id="workspaceDivisionId"
        :max-team-count="workspaceDivision.maxTeamCount || 16"
        :format-template-id="workspaceDivision.formatTemplateId"
        :division-meta="workspaceDivision"
        :series-id="overview?.context?.seriesId"
        :series-code="overview?.context?.seriesCode"
        :series-name="overview?.context?.seriesName"
        @update:format-template-id="id => { if (workspaceDivision) workspaceDivision.formatTemplateId = id }"
        @snapshot-change="snap => { if (workspaceDivision && snap?.sourceTemplateId) workspaceDivision.formatTemplateId = snap.sourceTemplateId }"
        @published="loadOverview"
      />
    </template>

    <PublishWizardDialog
      v-model="wizardOpen"
      :series-list="seriesList"
      :series-id="seriesList[0]?.id"
      @success="onWizardSuccess"
    />
  </div>
</template>

<script setup lang="ts" name="CnpaEventV4Publish">
import { onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import DivisionFormatRulesPanel from './components/DivisionFormatRulesPanel.vue'
import PublishWizardDialog, { type PublishWizardSuccessPayload } from './components/PublishWizardDialog.vue'
import DivisionEventSelector from '../components/DivisionEventSelector.vue'
import DivisionContextHeader from '../components/DivisionContextHeader.vue'
import DivisionWorkspaceNav from '../components/DivisionWorkspaceNav.vue'
import { useDivisionWorkspace } from '../composables/useDivisionWorkspace'
import { getV4Division, type BasketDivisionListVo } from '@/api/cnpa/event-v4/division'
import { listV4Series, type BasketSeriesVo } from '@/api/cnpa/event-v4/series'
import type { DivisionSelectorItemVo } from '@/api/cnpa/event-v4/overview'

const router = useRouter()
const { divisionId: workspaceDivisionId, overview, selectDivision, clearDivision, loadOverview } = useDivisionWorkspace('publish')
const workspaceDivision = ref<BasketDivisionListVo>()
const selectorRef = ref<{ reload: () => void }>()
const wizardOpen = ref(false)
const seriesList = ref<BasketSeriesVo[]>([])

watch(workspaceDivisionId, (id) => {
  workspaceDivision.value = undefined
  if (!id) return
  getV4Division(id).then(res => { workspaceDivision.value = res.data })
}, { immediate: true })

function openCreateWizard() {
  wizardOpen.value = true
}

function goSeasonManage(query?: Record<string, string>) {
  router.push({ path: '/event/event-v4-season-manage', query })
}

function onSelectorMore(command: string, row: DivisionSelectorItemVo) {
  if (!row.seasonId) {
    ElMessage.warning('当前行没有关联赛季，请到赛季管理查看')
    return
  }
  if (command === 'edit-season') {
    goSeasonManage({ seasonId: String(row.seasonId), action: 'edit' })
    return
  }
  if (command === 'view-divisions') {
    goSeasonManage({ seasonId: String(row.seasonId) })
  }
}

function onWizardSuccess(payload?: PublishWizardSuccessPayload) {
  selectorRef.value?.reload()
  if (!payload?.finished) return
  const divisionIds = payload.divisionIds || []
  if (divisionIds.length) {
    selectDivision(divisionIds[0])
    return
  }
  ElMessage.info('赛事基础资料已创建，请继续创建/配置赛事赛区。')
  goSeasonManage(payload.seasonId ? { seasonId: String(payload.seasonId) } : undefined)
}

onMounted(() => {
  listV4Series({ enabled: 1 }).then(res => { seriesList.value = res.data || [] })
})
</script>

<style scoped>
.mb8 {
  margin-bottom: 8px;
}
.page-tip-alert :deep(.el-alert__content) {
  padding-top: 0;
}
.page-tip-list {
  margin: 0;
  padding-left: 18px;
  line-height: 1.7;
  font-size: 13px;
}
.page-tip-list li + li {
  margin-top: 2px;
}
</style>
