import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getV4DivisionWorkflowOverview, type DivisionWorkspaceEntry, type DivisionWorkflowOverviewVo } from '@/api/cnpa/event-v4/overview'

export function useDivisionWorkspace(entry: DivisionWorkspaceEntry) {
  const route = useRoute()
  const router = useRouter()
  const loading = ref(false)
  const overview = ref<DivisionWorkflowOverviewVo>()

  const divisionId = computed(() => {
    const raw = route.query.divisionId
    const value = Array.isArray(raw) ? raw[0] : raw
    const n = Number(value)
    return Number.isFinite(n) && n > 0 ? n : undefined
  })

  const ruleLocked = computed(() => Boolean(overview.value?.rule?.locked))
  const requireRule = computed(() => Boolean(overview.value?.capabilities?.requireRulePublished))
  const blocked = computed(() => {
    if (entry === 'publish') return false
    if (!divisionId.value || !overview.value) return false
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
    if (!divisionId.value) {
      overview.value = undefined
      return
    }
    loading.value = true
    try {
      const res = await getV4DivisionWorkflowOverview(divisionId.value)
      overview.value = res.data
    } finally {
      loading.value = false
    }
  }

  watch(divisionId, () => {
    void loadOverview()
  }, { immediate: true })

  return {
    entry,
    divisionId,
    overview,
    loading,
    blocked,
    ruleLocked,
    selectDivision,
    clearDivision,
    loadOverview
  }
}
