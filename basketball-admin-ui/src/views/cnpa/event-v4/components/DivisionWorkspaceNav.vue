<template>
  <div v-if="divisionId" class="workspace-nav">
    <el-radio-group :model-value="activeKey" size="default" @change="onChange">
      <el-radio-button
        v-for="item in visibleItems"
        :key="item.key"
        :label="item.key"
      >
        {{ item.label }}
      </el-radio-button>
    </el-radio-group>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { checkPermi } from '@/utils/permission'

export type WorkspaceNavKey = 'overview' | 'rule' | 'structure' | 'schedule' | 'publish'

interface WorkspaceNavItem {
  key: WorkspaceNavKey
  label: string
  path: string
  perms: string[]
}

const ITEMS: WorkspaceNavItem[] = [
  { key: 'overview', label: '概览', path: '/event/event-v4-overview', perms: ['basket:v4Stage:list'] },
  { key: 'rule', label: '赛事规则', path: '/event/event-v4-publish', perms: ['basket:v4Season:list', 'basket:v4FormatRule:view'] },
  { key: 'structure', label: '赛程结构', path: '/event/event-v4-structure', perms: ['basket:v4Stage:generate'] },
  { key: 'schedule', label: '日期与时间', path: '/event/event-v4-schedule', perms: ['basket:v4ScheduleBatch:list'] },
  { key: 'publish', label: '赛程发布', path: '/event/event-v4-publish-preview', perms: ['basket:v4Stage:publish'] }
]

const route = useRoute()
const router = useRouter()

const divisionId = computed(() => {
  const raw = route.query.divisionId
  const value = Array.isArray(raw) ? raw[0] : raw
  const n = Number(value)
  return Number.isFinite(n) && n > 0 ? n : undefined
})

const visibleItems = computed(() => ITEMS.filter(item => checkPermi(item.perms)))

const activeKey = computed<WorkspaceNavKey>(() => {
  const path = route.path
  const hit = ITEMS.find(item => path === item.path || path.endsWith(item.path))
  return hit?.key || 'overview'
})

function onChange(key: string | number | boolean) {
  const item = ITEMS.find(row => row.key === key)
  if (!item || !divisionId.value) return
  if (route.path === item.path) return
  router.push({ path: item.path, query: { divisionId: String(divisionId.value) } })
}
</script>

<style scoped>
.workspace-nav {
  margin-bottom: 12px;
}
</style>
