<script setup lang="ts">
import { inject, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import PaperSection from '@/components/common/PaperSection.vue'
import TeamLogo from '@/components/team/TeamLogo.vue'
import type { PublicSeasonPlacementResp } from '@/api/types/public'
import { displayText } from '@/utils/publicDisplay'
import type { usePublicSeasonContext } from '../composables/usePublicSeasonContext'

type SeasonContext = ReturnType<typeof usePublicSeasonContext>

const ctx = inject<SeasonContext | null>('publicSeasonContext', null)
const router = useRouter()

const placement = ref<PublicSeasonPlacementResp | null>(null)
const state = ref<'loading' | 'success' | 'empty' | 'error' | 'unpublished'>('loading')
const errorMessage = ref('')
let reqId = 0

async function load(divisionId: string) {
  const current = ++reqId
  if (!divisionId) {
    placement.value = null
    state.value = 'empty'
    return
  }
  state.value = 'loading'
  errorMessage.value = ''
  try {
    // V4 暂无独立冠亚军发布接口；保持 master 未发布态 UI
    if (current !== reqId) return
    placement.value = {
      seasonId: ctx?.seasonId.value || '',
      published: false,
    }
    state.value = 'unpublished'
  } catch (err) {
    if (current !== reqId) return
    placement.value = null
    state.value = 'error'
    errorMessage.value = err instanceof Error ? err.message : '加载赛季名次失败'
  }
}

watch(
  () => ctx?.divisionId.value ?? '',
  (divisionId) => {
    void load(divisionId)
  },
  { immediate: true },
)

function openTeam(teamId?: string | null) {
  if (!teamId) return
  router.push({
    path: `/tournament/team/${teamId}/overview`,
    query: ctx?.divisionId.value ? { divisionId: ctx.divisionId.value } : {},
  })
}
</script>

<template>
  <div class="site-shell">
    <PaperSection edge="both" tone="gray" class="season-placement">
      <div class="season-placement__inner">
        <div class="season-placement__header">
          <div class="section-head-group">
            <h2 class="cnpa-paper-section__title">赛季名次</h2>
            <span class="cnpa-paper-section__subtitle">Season Placement</span>
          </div>
        </div>

        <div v-if="state === 'loading'" class="season-placement__empty">加载中...</div>
        <div v-else-if="state === 'error'" class="season-placement__empty">{{ errorMessage }}</div>
        <div v-else-if="state === 'unpublished' || state === 'empty'" class="season-placement__empty">
          冠亚军尚未发布
        </div>
        <div v-else class="season-placement__grid">
          <button
            type="button"
            class="season-placement__card season-placement__card--champion"
            :disabled="!placement?.championTeamId"
            @click="openTeam(placement?.championTeamId)"
          >
            <span class="season-placement__label">冠军</span>
            <TeamLogo
              :team="{
                id: String(placement?.championTeamId || ''),
                name: displayText(placement?.championTeamName),
                shortName: displayText(placement?.championTeamName),
                logoUrl: placement?.championTeamLogoUrl || '',
              }"
              size="lg"
            />
            <strong>{{ displayText(placement?.championTeamName, '待公布') }}</strong>
          </button>
          <button
            type="button"
            class="season-placement__card"
            :disabled="!placement?.runnerUpTeamId"
            @click="openTeam(placement?.runnerUpTeamId)"
          >
            <span class="season-placement__label">亚军</span>
            <TeamLogo
              :team="{
                id: String(placement?.runnerUpTeamId || ''),
                name: displayText(placement?.runnerUpTeamName),
                shortName: displayText(placement?.runnerUpTeamName),
                logoUrl: placement?.runnerUpTeamLogoUrl || '',
              }"
              size="lg"
            />
            <strong>{{ displayText(placement?.runnerUpTeamName, '待公布') }}</strong>
          </button>
        </div>
      </div>
    </PaperSection>
  </div>
</template>

<style scoped lang="scss">
.season-placement__inner {
  padding:
    clamp(28px, 2.4vw, 42px)
    clamp(28px, 3vw, 52px)
    clamp(32px, 2.8vw, 48px);
}

.season-placement__header {
  margin-bottom: 20px;
}

.season-placement__empty {
  min-height: 120px;
  display: grid;
  place-items: center;
  color: rgba(17, 17, 17, 0.5);
  font-weight: 700;
}

.season-placement__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.season-placement__card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  min-height: 180px;
  padding: 24px 18px;
  border: 1px solid rgba(17, 17, 17, 0.12);
  background: #fff;
  color: #111;
  font: inherit;
  cursor: pointer;

  &:disabled {
    cursor: default;
  }

  strong {
    font-size: 18px;
    font-weight: 900;
  }

  &--champion {
    border-color: rgba(225, 6, 19, 0.35);
  }
}

.season-placement__label {
  color: var(--cnpa-red, #e10613);
  font-size: 13px;
  font-weight: 800;
  letter-spacing: 0.08em;
}

@media (max-width: 767px) {
  .season-placement__grid {
    grid-template-columns: 1fr;
  }
}
</style>
