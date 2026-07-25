import { computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useEventTeamStore } from '@/stores/eventTeam'
import { useAccountStore } from '@/stores/account'

export function useEventTeamPage(options?: { requireCaptain?: boolean }) {
  const route = useRoute()
  const router = useRouter()
  const eventTeamStore = useEventTeamStore()
  const accountStore = useAccountStore()
  const requireCaptain = options?.requireCaptain ?? true

  const tournamentId = computed(() => String(route.params.tournamentId || ''))

  const teamBasePath = computed(() => `/account/my-tournaments/${tournamentId.value}/team`)

  function teamPath(suffix = '') {
    return `${teamBasePath.value}${suffix}`
  }

  const breadcrumbRoot = computed(() => [
    { label: '首页', to: '/home' },
    { label: '个人中心', to: '/account/overview' },
    { label: '我的赛事', to: '/account/my-events?tab=joined' },
    {
      label: eventTeamStore.tournament.tournamentName || '本届球队',
      to: teamPath(),
    },
  ])

  async function ensureAccess() {
    if (!accountStore.isVerifiedPlayer) {
      ElMessage.warning('请先完成球员身份认证')
      router.replace('/account/player-identity')
      return false
    }
    const loaded = await eventTeamStore.ensureLoaded(tournamentId.value)
    if (!loaded) {
      ElMessage.warning(eventTeamStore.lastError || '当前赛事暂无球队管理入口')
      router.replace('/account/my-events?tab=joined')
      return false
    }
    if (requireCaptain && !eventTeamStore.isCaptain) {
      ElMessage.warning('仅本届赛事队长可访问球队管理')
      router.replace(`/account/my-tournaments/${tournamentId.value}/team/roster`)
      return false
    }
    return true
  }

  onMounted(() => {
    void ensureAccess()
  })

  watch(tournamentId, (next) => {
    if (next) void eventTeamStore.ensureLoaded(next)
  })

  return {
    tournamentId,
    teamBasePath,
    teamPath,
    breadcrumbRoot,
    eventTeamStore,
    accountStore,
    ensureAccess,
  }
}
