<script setup lang="ts">
import { computed, inject, ref, watch } from 'vue'
import { getV4PublicRank } from '@/api/v4Public'
import PaperSection from '@/components/common/PaperSection.vue'
import { formatDecimal } from '@/utils/publicDisplay'
import type { PlayerLeader } from '../types/events'
import type { usePublicSeasonContext } from '../composables/usePublicSeasonContext'
import { V4_LEADER_RANK_TYPE } from '../utils/v4PublicAdapters'
import { standingToTeam } from '../utils/mapPublic'
import { buildTeamLookup } from '../utils/teamLookup'
import TeamLogo from './TeamLogo.vue'

type SeasonContext = ReturnType<typeof usePublicSeasonContext>

const LEADER_CONFIG = [
  { key: 'AVG_POINTS', title: '场均得分', unit: 'PPG' as const },
  { key: 'AVG_ASSISTS', title: '场均助攻', unit: 'APG' as const },
  { key: 'AVG_REBOUNDS', title: '场均篮板', unit: 'RPG' as const },
]

const ctx = inject<SeasonContext>('publicSeasonContext')!

const seasonLeaders = ref<PlayerLeader[]>([])
const state = ref<'loading' | 'success' | 'empty' | 'error'>('loading')
const errorMessage = ref('')
const imageErrors = ref<Record<number, boolean>>({})
let reqId = 0

function handlePortraitError(id: number) {
  imageErrors.value[id] = true
}

async function load(divisionId: string) {
  const current = ++reqId
  if (!divisionId) {
    seasonLeaders.value = []
    state.value = 'empty'
    return
  }
  state.value = 'loading'
  errorMessage.value = ''
  try {
    const results = await Promise.all(
      LEADER_CONFIG.map(async (config, index) => {
        const rankType = V4_LEADER_RANK_TYPE[config.key]
        const res = await getV4PublicRank(divisionId, rankType)
        const leader = (res.data ?? []).find((item) => item.rankNo === 1) ?? (res.data ?? [])[0]
        if (!leader) return null
        return {
          id: index + 1,
          title: config.title,
          value: Number(formatDecimal(leader.rankValue, 1)),
          unit: config.unit,
          playerName: leader.playerName || '--',
          teamId: String(leader.eventTeamId || ''),
          teamName: leader.teamName || '',
          teamLogo: '',
          portrait: '',
        } satisfies PlayerLeader
      }),
    )
    if (current !== reqId) return
    seasonLeaders.value = results.filter((item) => item !== null) as PlayerLeader[]
    state.value = seasonLeaders.value.length ? 'success' : 'empty'
  } catch (err) {
    if (current !== reqId) return
    seasonLeaders.value = []
    state.value = 'error'
    errorMessage.value = err instanceof Error ? err.message : '加载高光选手失败'
  }
}

const teamLookup = computed(() => {
  const teams = seasonLeaders.value.map((leader) =>
    standingToTeam({
      teamId: leader.teamId,
      teamName: leader.teamName,
      teamLogoUrl: leader.teamLogo,
    }),
  )
  return buildTeamLookup(teams)
})

function getTeamById(id: number | string) {
  return teamLookup.value(id)
}

watch(
  () => ctx.divisionId.value,
  (divisionId) => {
    void load(divisionId)
  },
  { immediate: true },
)
</script>
<template>
  <div class="site-shell season-leaders-shell">
    <PaperSection edge="both" tone="gray" class="season-leaders">
      <div class="season-leaders__inner">
        <div class="season-leaders__header">
          <div class="section-head-group">
            <h2 class="cnpa-paper-section__title">高光选手</h2>
            <span class="cnpa-paper-section__subtitle">Season Leaders</span>
          </div>
        </div>

        <div class="season-leaders__grid">
          <article v-for="leader in seasonLeaders" :key="leader.id" class="leader-card">
            <div class="leader-info">
              <span class="leader-title">{{ leader.title }}</span>
              <div class="leader-value sport-num">
                {{ leader.value }}
                <small>{{ leader.unit }}</small>
              </div>
              <strong class="leader-name">{{ leader.playerName }}</strong>
              <TeamEntryLink
                v-if="getTeamById(leader.teamId)"
                :team="getTeamById(leader.teamId)!"
                class="leader-team"
              >
                <TeamLogo :team="getTeamById(leader.teamId)!" size="sm" />
                <span>{{ getTeamById(leader.teamId)!.name }}</span>
              </TeamEntryLink>
            </div>

            <div class="leader-portrait">
              <img
                v-if="!imageErrors[leader.id]"
                :src="leader.portrait"
                :alt="leader.playerName"
                @error="handlePortraitError(leader.id)"
              />
              <div v-else class="leader-silhouette" aria-hidden="true" />
            </div>
          </article>
        </div>
      </div>
    </PaperSection>
  </div>
</template>
<style scoped lang="scss">
.season-leaders-shell {
  margin-top: 0;
}

.season-leaders {
  margin-top: 0;
}

.season-leaders__inner {
  padding:
    clamp(28px, 2.4vw, 42px)
    clamp(28px, 3vw, 52px)
    clamp(32px, 2.8vw, 48px);
}

.season-leaders__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 22px;
}

.season-leaders__grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
}

.leader-card {
  display: grid;
  grid-template-columns: 1fr 120px;
  min-height: 180px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 6px;
  background: linear-gradient(135deg, #171717 0%, #080808 100%);
  overflow: hidden;
  transition: transform 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease;

  &:hover {
    transform: translateY(-4px);
    border-color: var(--cnpa-red);
    box-shadow: 0 12px 32px rgba(225, 6, 19, 0.12);
  }
}

.leader-info {
  padding: 24px 20px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.leader-title {
  font-size: 13px;
  color: var(--cnpa-text-secondary);
}

.leader-value {
  font-size: clamp(36px, 3.6vw, 48px);
  line-height: 1;
  color: var(--cnpa-red);

  small {
    margin-left: 6px;
    font-size: 14px;
    color: rgba(255, 255, 255, 0.72);
  }
}

.leader-name {
  font-size: 18px;
  font-weight: 900;
  color: #fff;
}

.leader-team {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: auto;
  font-size: 13px;
  color: var(--cnpa-text-secondary);
  text-decoration: none;
}

.leader-portrait {
  position: relative;
  background: linear-gradient(180deg, rgba(225, 6, 19, 0.08) 0%, rgba(0, 0, 0, 0.4) 100%);

  img,
  .leader-silhouette {
    width: 100%;
    height: 100%;
    object-fit: cover;
    object-position: top center;
  }
}

.leader-silhouette {
  background:
    radial-gradient(circle at 50% 28%, rgba(255, 255, 255, 0.18) 0%, transparent 34%),
    linear-gradient(180deg, #2a2a2a 0%, #111 100%);
  clip-path: polygon(28% 100%, 72% 100%, 68% 38%, 50% 24%, 32% 38%);
}

@media (max-width: 768px) {
  .season-leaders__inner {
    padding-left: 16px;
    padding-right: 16px;
  }

  .season-leaders__header {
    align-items: flex-start;
    flex-direction: column;
  }

  .season-leaders__grid {
    grid-template-columns: 1fr;
  }

  .leader-card {
    grid-template-columns: 1fr 96px;
  }
}
</style>