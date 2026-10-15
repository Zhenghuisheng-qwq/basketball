<template>
  <div v-if="divisionId" class="mt16">
    <el-divider content-position="left">待审核名单变更</el-divider>
    <el-table v-loading="loading" :data="rows" border>
      <el-table-column label="变更ID" prop="id" width="90" />
      <el-table-column label="报名ID" prop="registrationId" width="90" />
      <el-table-column label="球队" prop="teamName" min-width="120" />
      <el-table-column label="移除球员" min-width="160">
        <template #default="scope">{{ (scope.row.removedNames || []).join('、') || '-' }}</template>
      </el-table-column>
      <el-table-column label="新增球员" min-width="160">
        <template #default="scope">{{ (scope.row.addedNames || []).join('、') || '-' }}</template>
      </el-table-column>
      <el-table-column label="版本" width="120">
        <template #default="scope">{{ scope.row.baseLineupVersion }} / {{ scope.row.currentLineupVersion }}</template>
      </el-table-column>
      <el-table-column label="操作" width="160">
        <template #default="scope">
          <el-button link type="success" v-hasPermi="['basket:v4RegistrationAudit:approve']" @click="approve(scope.row)">通过</el-button>
          <el-button link type="danger" v-hasPermi="['basket:v4RegistrationAudit:reject']" @click="reject(scope.row)">驳回</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { getCurrentInstance, ref, watch } from 'vue'
import { approveV4RosterChange, listV4RosterChanges, rejectV4RosterChange } from '@/api/cnpa/event-v4/registration-audit'

const props = defineProps<{ divisionId?: number }>()
const { proxy } = getCurrentInstance() as any
const loading = ref(false)
const rows = ref<any[]>([])

function load() {
  if (!props.divisionId) {
    rows.value = []
    return
  }
  loading.value = true
  listV4RosterChanges(props.divisionId).then(res => {
    rows.value = res.data || []
  }).finally(() => { loading.value = false })
}

function approve(row: any) {
  proxy.$modal.confirm('确认通过该名单变更？通过后将替换该队当前有效参赛名单，报名仍保持已通过。').then(() => {
    return approveV4RosterChange(row.id)
  }).then(() => {
    proxy.$modal.msgSuccess('名单变更已通过')
    load()
  }).catch(() => undefined)
}

function reject(row: any) {
  proxy.$modal.prompt('请输入驳回原因').then((result: { value: string }) => {
    if (!result?.value) return
    return rejectV4RosterChange(row.id, { rejectReason: result.value })
  }).then(() => {
    proxy.$modal.msgSuccess('已驳回，原名单未变化')
    load()
  }).catch(() => undefined)
}

watch(() => props.divisionId, load, { immediate: true })
</script>
