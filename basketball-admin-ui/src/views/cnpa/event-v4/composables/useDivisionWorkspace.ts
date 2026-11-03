import { computed, onActivated, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getV4DivisionWorkflowOverview, type DivisionWorkspaceEntry, type DivisionWorkflowOverviewVo } from '@/api/cnpa/event-v4/overview'

const WORKSPACE_PATHS: Record<DivisionWorkspaceEntry, string> = {
  overview: '/event/event-v4-overview',
  publish: '/event/event-v4-publish',
  structure: '/event/event-v4-structure',
  schedule: '/event/event-v4-schedule',
  'publish-preview': '/event/event-v4-publish-preview',
  roster: '/event/event-v4-registration-audit',
  leaderboard: '/event/event-v4-leaderboard'
}

export function useDivisionWorkspace(entry: DivisionWorkspaceEntry) {
  const route = useRoute()
  const router = useRouter()
  const loading = ref(false)
  const overview = ref<DivisionWorkflowOverviewVo>()
  const boundDivisionId = ref<number | undefined>()

  const routeDivisionId = computed(() => {
    const raw = route.query.divisionId
    const value = Array.isArray(raw) ? raw[0] : raw
    const n = Number(value)
    return Number.isFinite(n) && n > 0 ? n : undefined
  })

  function isActivePage() {
    const expected = WORKSPACE_PATHS[entry]
    const path = route.path || ''
    return path === expected || path.endsWith(expected)
  }

  const ruleLocked = computed(() => Boolean(overview.value?.rule?.locked))
  const requireRule = computed(() => Boolean(overview.value?.capabilities?.requireRulePublished))
  const blocked = computed(() => {
    if (entry === 'publish' || entry === 'structure') return false
    if (!boundDivisionId.value || !overview.value) return false
    if (requireRule.value && !ruleLocked.value) {
      return overview.value.capabilities?.blockReason || '请先完成赛事规则发布'
    }
    return ''
  })

  function selectDivision(id: number) {
    router.replace({ query: { ...route.query, divisionId: String(id) } })
  }

  function clearDivision() {
    const next = { ...route.query }
    delete next.divisionId
    router.replace({ query: next })
  }

  async function loadOverview() {
    if (!boundDivisionId.value) {
      overview.value = undefined
      return
    }
    loading.value = true
    try {
      const res = await getV4DivisionWorkflowOverview(boundDivisionId.value)
      overview.value = res.data
    } finally {
      loading.value = false
    }
  }

  function syncActiveDivision() {
    if (!isActivePage()) {
      return
    }
    boundDivisionId.value = routeDivisionId.value
    void loadOverview()
  }

  watch([routeDivisionId, () => route.path], () => {
    syncActiveDivision()
  }, { immediate: true })

  onActivated(() => {
    syncActiveDivision()
  })

  return {
    entry,
    divisionId: boundDivisionId,
    overview,
    loading,
    blocked,
    ruleLocked,
    selectDivision,
    clearDivision,
    loadOverview
  }
}
