import { computed, ref, watch } from 'vue'
import { getPublicTeamSeasonHistory } from '@/api/publicTeam'
import { getV4PublicStageList } from '@/api/v4Public'
import type { V4PublicStageItem } from '@/api/types/v4Public'
import type { PublicTeamPageContext } from './usePublicTeamPage'

export interface TeamEventOption {
  eventTeamId: string
  divisionId: string
  seasonId: string
  seasonName: string
  divisionName: string
  label: string
}

function eventLabel(seasonName: string, divisionName: string): string {
  if (seasonName && divisionName && seasonName !== divisionName) {
    return `${seasonName} · ${divisionName}`
  }
  return seasonName || divisionName || '赛事'
}

export function useTeamEventFilters(ctx: PublicTeamPageContext) {
  const events = ref<TeamEventOption[]>([])
  const stages = ref<V4PublicStageItem[]>([])
  const eventId = ref('all')
  const stageId = ref('all')
  const ready = ref(false)
  let initialized = false
  let boundTeamId = ''
  let eventReq = 0
  let stageReq = 0

  const stageOptions = computed(() => {
    if (eventId.value === 'all') return []
    return [...stages.value]
      .sort((a, b) => (Number(a.stageOrder) || 0) - (Number(b.stageOrder) || 0))
      .map((stage) => ({
        value: String(stage.stageId),
        label: stage.stageName || stage.stageTypeName || stage.competitionFormatName || String(stage.stageId),
      }))
  })

  const targets = computed(() => {
    if (eventId.value === 'all') return events.value
    return events.value.filter((item) => item.eventTeamId === eventId.value)
  })

  function pageEvent(): TeamEventOption | null {
    const eventTeamId = String(ctx.teamId.value || '').trim()
    const divisionId = String(ctx.divisionId.value || '').trim()
    if (!eventTeamId || !divisionId) return null
    const seasonName = ctx.detail.value?.seasonName || ctx.teamView.value?.seasonName || ''
    const divisionName = ctx.detail.value?.divisionName || ctx.teamView.value?.divisionName || ''
    return {
      eventTeamId,
      divisionId,
      seasonId: String(ctx.seasonId.value || ctx.detail.value?.seasonId || ''),
      seasonName,
      divisionName,
      label: eventLabel(seasonName, divisionName),
    }
  }

  async function loadEvents() {
    const current = ++eventReq
    const routeTeamId = String(ctx.teamId.value || '').trim()
    const detail = ctx.detail.value
    const detailMatchesRoute = Boolean(detail && String(detail.teamId || '') === routeTeamId)
    const fallback = pageEvent()
    const sourceId = detailMatchesRoute ? String(detail?.sourceTeamId || '').trim() : ''
    if (boundTeamId && boundTeamId !== routeTeamId) {
      initialized = false
      eventId.value = 'all'
      stageId.value = 'all'
      events.value = []
      stages.value = []
      ready.value = false
    }
    if (routeTeamId) boundTeamId = routeTeamId
    if (!fallback && !sourceId) {
      if (ctx.state.value === 'loading') return
      events.value = []
      stages.value = []
      ready.value = true
      return
    }
    let list: TeamEventOption[] = []
    if (sourceId) {
      try {
        const res = await getPublicTeamSeasonHistory(sourceId)
        if (current !== eventReq) return
        list = (res.data ?? [])
          .filter((row) => row.eventTeamId && row.divisionId)
          .map((row) => {
            const seasonName = row.seasonName || row.tournamentName || ''
            const divisionName = row.divisionName || ''
            return {
              eventTeamId: String(row.eventTeamId),
              divisionId: String(row.divisionId),
              seasonId: String(row.seasonId || ''),
              seasonName,
              divisionName,
              label: eventLabel(seasonName, divisionName),
            }
          })
      } catch {
        list = []
      }
    }
    if (current !== eventReq) return
    if (fallback && !list.some((item) => item.eventTeamId === fallback.eventTeamId)) {
      list.unshift(fallback)
    }
    const seen = new Set<string>()
    events.value = list.filter((item) => {
      if (seen.has(item.eventTeamId)) return false
      seen.add(item.eventTeamId)
      return true
    })
    ready.value = true
    const routeEventId = String(ctx.teamId.value || '')
    if (!initialized && routeEventId && ctx.divisionId.value) {
      eventId.value = events.value.some((item) => item.eventTeamId === routeEventId)
        ? routeEventId
        : 'all'
      initialized = true
    } else if (eventId.value !== 'all' && !events.value.some((item) => item.eventTeamId === eventId.value)) {
      eventId.value = events.value.some((item) => item.eventTeamId === routeEventId)
        ? routeEventId
        : (events.value[0]?.eventTeamId || 'all')
    }
  }

  async function loadStages(id: string, resetStage: boolean) {
    const current = ++stageReq
    if (resetStage) stageId.value = 'all'
    if (id === 'all') {
      stages.value = []
      return
    }
    const event = events.value.find((item) => item.eventTeamId === id)
    if (!event) {
      stages.value = []
      return
    }
    try {
      const res = await getV4PublicStageList(event.divisionId)
      if (current !== stageReq || eventId.value !== id) return
      stages.value = res.data ?? []
    } catch {
      if (current !== stageReq) return
      stages.value = []
    }
  }

  watch(eventId, (id) => {
    void loadStages(id, true)
  })

  watch(events, () => {
    if (eventId.value !== 'all') {
      void loadStages(eventId.value, false)
    }
  })

  watch(
    () => [
      ctx.detail.value?.sourceTeamId,
      ctx.detail.value?.teamId,
      ctx.teamId.value,
      ctx.divisionId.value,
      ctx.detail.value?.seasonName,
      ctx.detail.value?.divisionName,
      ctx.state.value,
    ],
    () => {
      void loadEvents()
    },
    { immediate: true },
  )

  return {
    events,
    eventId,
    stageId,
    stageOptions,
    targets,
    ready,
  }
}
