<script setup lang="ts">
import { inject, ref, watch } from 'vue'
import PaperSection from '@/components/common/PaperSection.vue'
import PlayerLink from '@/components/player/PlayerLink.vue'
import TeamEntryLink from '@/components/team/TeamEntryLink.vue'
import { getPublicDataCenterPlayerRanking } from '@/api/publicDataCenter'
import type { PublicPlayerRankItemVo } from '@/api/types/public'
import { displayText, formatDecimal, initials } from '@/utils/publicDisplay'
import type { usePublicSeasonContext } from '../composables/usePublicSeasonContext'
import { toEventTeam } from '../utils/mapPublic'
import TeamLogo from './TeamLogo.vue'

type SeasonContext = ReturnType<typeof usePublicSeasonContext>

interface LeaderCard {
  key: string
  title: string
  unit: string
  rankType: string
  data: PublicPlayerRankItemVo | null
}

const ctx = inject<SeasonContext>('publicSeasonContext')!

const state = ref<'loading' | 'success' | 'empty' | 'error'>('loading')
const errorMessage = ref('')
const leaders = ref<LeaderCard[]>([
  { key: 'ppg', title: '得分王', unit: 'PPG', rankType: 'AVG_POINTS', data: null },
  { key: 'apg', title: '助攻王', unit: 'APG', rankType: 'AVG_ASSISTS', data: null },
  { key: 'rpg', title: '篮板王', unit: 'RPG', rankType: 'AVG_REBOUNDS', data: null },
])
const imageErrors = ref<Record<string, boolean>>({})
let reqId = 0

function handlePortraitError(key: string) {
  imageErrors.value[key] = true
}

async function loadLeaders(seasonId: string) {
  const current = ++reqId
  if (!seasonId) {
    leaders.value = leaders.value.map((item) => ({ ...item, data: null }))
    state.value = 'empty'
    return
  }
  state.value = 'loading'
  errorMessage.value = ''
  try {
    const results = await Promise.all(
      leaders.value.map((item) =>
        getPublicDataCenterPlayerRanking({
          seasonId,
          rankType: item.rankType,
          limit: 1,
        }),
      ),
    )
    if (current !== reqId) return
    leaders.value = leaders.value.map((item, index) => ({
      ...item,
      data: results[index]?.data?.[0] ?? null,
    }))
    state.value = leaders.value.some((item) => item.data) ? 'success' : 'empty'
  } catch (err) {
    if (current !== reqId) return
    leaders.value = leaders.value.map((item) => ({ ...item, data: null }))
    state.value = 'error'
    errorMessage.value = err instanceof Error ? err.message : '加载高光选手失败'
  }
}

watch(
  () => ctx.seasonId.value,
  (sid) => {
    void loadLeaders(sid)
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

        <div v-if="state === 'loading'" class="season-leaders__state">加载中...</div>
        <div v-else-if="state === 'error'" class="season-leaders__state is-error">{{ errorMessage }}</div>
        <div v-else-if="state === 'empty'" class="season-leaders__state">暂无高光选手数据</div>
        <div v-else class="season-leaders__grid">
          <article v-for="leader in leaders" :key="leader.key" class="leader-card">
            <div class="leader-info">
              <span class="leader-title">{{ leader.title }}</span>
              <div class="leader-value sport-num">
                {{ leader.data ? formatDecimal(leader.data.statValue) : '0' }}
                <small>{{ leader.unit }}</small>
              </div>
              <strong class="leader-name">
                <PlayerLink
                  v-if="leader.data"
                  :player-id="leader.data.playerId"
                  :player-name="displayText(leader.data.playerName)"
                  tab="data"
                />
                <template v-else>--</template>
              </strong>
              <TeamEntryLink
                v-if="leader.data?.teamId"
                :team="
                  toEventTeam({
                    teamId: leader.data.teamId,
                    teamName: leader.data.teamName,
                  })
                "
                class="leader-team"
              >
                <TeamLogo
                  :team="
                    toEventTeam({
                      teamId: leader.data.teamId,
                      teamName: leader.data.teamName,
                    })
                  "
                  size="sm"
                />
                <span>{{ displayText(leader.data.teamName) }}</span>
              </TeamEntryLink>
            </div>

            <div class="leader-portrait">
              <img
                v-if="leader.data?.avatarUrl && !imageErrors[leader.key]"
                :src="leader.data.avatarUrl"
                :alt="displayText(leader.data.playerName)"
                @error="handlePortraitError(leader.key)"
              />
              <div v-else class="leader-silhouette" aria-hidden="true">
                {{ initials(leader.data?.playerName) }}
              </div>
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

.season-leaders__state {
  padding: 36px 0;
  text-align: center;
  color: rgba(255, 255, 255, 0.55);
  font-weight: 600;

  &.is-error {
    color: #e10613;
  }
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
  display: grid;
  place-items: center;
  background:
    radial-gradient(circle at 50% 28%, rgba(255, 255, 255, 0.18) 0%, transparent 34%),
    linear-gradient(180deg, #2a2a2a 0%, #111 100%);
  color: rgba(255, 255, 255, 0.55);
  font-size: 18px;
  font-weight: 900;
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
