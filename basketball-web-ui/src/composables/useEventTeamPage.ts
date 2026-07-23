import { computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useEventTeamStore } from '@/stores/eventTeam'
import { useAccountStore } from '@/stores/account'
import { isSupportedEventTeamTournament } from '@/mock/eventTeamMock'

export function useEventTeamPage() {
  const route = useRoute()
  const router = useRouter()
  const eventTeamStore = useEventTeamStore()
  const accountStore = useAccountStore()

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
      label: eventTeamStore.tournament.tournamentName,
      to: teamPath(),
    },
  ])

  function ensureAccess() {
    if (!accountStore.isVerifiedPlayer) {
      ElMessage.warning('请先完成球员身份认证')
      router.replace('/account/player-identity')
      return false
    }
    if (!isSupportedEventTeamTournament(tournamentId.value)) {
      ElMessage.warning('当前赛事暂无球队管理入口')
      router.replace('/account/my-events?tab=joined')
      return false
    }
    eventTeamStore.ensureLoaded(tournamentId.value)
    if (!eventTeamStore.isCaptain) {
      ElMessage.warning('仅本届赛事队长可访问球队管理')
      router.replace('/account/my-events?tab=joined')
      return false
    }
    return true
  }

  onMounted(() => {
    ensureAccess()
  })

  watch(tournamentId, (next) => {
    if (next) eventTeamStore.ensureLoaded(next)
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
