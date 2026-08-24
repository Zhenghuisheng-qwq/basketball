<script setup lang="ts">
import { computed, inject, ref, watch } from 'vue'
import PaperSection from '@/components/common/PaperSection.vue'
import { getV4PublicStageList } from '@/api/v4Public'
import type { V4PublicStageItem } from '@/api/types/v4Public'
import type { usePublicSeasonContext } from '../composables/usePublicSeasonContext'
import RegularStandings from './RegularStandings.vue'
import PlayoffBracket from './PlayoffBracket.vue'
import CompetitionFormat from './CompetitionFormat.vue'
import LeagueAbout from './LeagueAbout.vue'
import StageProgressHeader from './StageProgressHeader.vue'

type SeasonContext = ReturnType<typeof usePublicSeasonContext>
const ctx = inject<SeasonContext>('publicSeasonContext')!

const stages = ref<V4PublicStageItem[]>([])
const selectedStageId = ref('')
const stagesLoading = ref(false)

function isEliminationStage(stage?: V4PublicStageItem | null): boolean {
  if (!stage) return false
  const type = `${stage.competitionFormat || ''} ${stage.stageType || ''}`.toUpperCase()
  return type.includes('ELIM') || type.includes('KNOCK') || type.includes('PLAYOFF') || type.includes('FINAL')
}

function isGroupedStage(stage?: V4PublicStageItem | null): boolean {
  if (!stage) return false
  const mode = (stage.organizationMode || '').toUpperCase()
  const type = `${stage.competitionFormat || ''} ${stage.stageType || ''}`.toUpperCase()
  return mode === 'GROUPED' || type.includes('GROUP')
}

const selectedStage = computed(() =>
  stages.value.find((item) => item.stageId === selectedStageId.value) || null,
)

const showBracket = computed(() => isEliminationStage(selectedStage.value))

const stageOptions = computed(() =>
  stages.value.map((stage) => ({
    value: stage.stageId,
    label: isEliminationStage(stage) ? '季后赛' : '常规赛',
  })),
)

const formatCurrentStage = computed(() => (showBracket.value ? 'playoffs' : 'regular'))

async function loadStages(divisionId: string) {
  if (!divisionId) {
    stages.value = []
    selectedStageId.value = ''
    return
  }
  stagesLoading.value = true
  try {
    const res = await getV4PublicStageList(divisionId)
    const list = [...(res.data ?? [])].sort(
      (a, b) => (a.stageOrder ?? 0) - (b.stageOrder ?? 0) || String(a.stageId).localeCompare(String(b.stageId)),
    )
    stages.value = list
    const current = list.find((item) => item.currentStage) || list[0]
    selectedStageId.value = current?.stageId || ''
  } catch {
    stages.value = []
    selectedStageId.value = ''
  } finally {
    stagesLoading.value = false
  }
}

watch(
  () => ctx.divisionId.value,
  (divisionId) => {
    void loadStages(divisionId)
  },
  { immediate: true },
)
</script>
<template>
  <div class="league-stage">
    <template v-if="!showBracket">
      <div class="site-shell">
        <section class="events-section-dark stage-main">
          <div class="site-shell-inner">
            <StageProgressHeader
              v-if="stageOptions.length"
              v-model="selectedStageId"
              :options="stageOptions"
              variant="paper"
            />
            <p v-if="stagesLoading" class="stage-state">加载阶段...</p>
            <RegularStandings
              v-else
              :stage-id="selectedStageId"
              :grouped="isGroupedStage(selectedStage)"
              :stage-name="selectedStage?.stageName"
            />
          </div>
        </section>
      </div>

      <CompetitionFormat :current-stage="formatCurrentStage" />
    </template>

    <template v-else>
      <div class="site-shell stage-playoffs-shell">
        <PaperSection edge="top" tone="gray" class="stage-progress-section">
          <div class="stage-progress-section__inner">
            <StageProgressHeader
              v-if="stageOptions.length"
              v-model="selectedStageId"
              :options="stageOptions"
              variant="paper"
            />
            <PlayoffBracket :focus-stage-id="selectedStageId" />
          </div>
        </PaperSection>

        <PaperSection edge="bottom" tone="gray" class="competition-format-section">
          <div class="stage-section-divider" aria-hidden="true" />
          <CompetitionFormat embedded :current-stage="formatCurrentStage" />
        </PaperSection>
      </div>
    </template>

    <LeagueAbout />
  </div>
</template>
<style scoped lang="scss">
.stage-main {
  padding: 48px 0 24px;
  background: var(--cnpa-public-paper-bg, #f4f1ea);
  color: #111;
}

.stage-state {
  margin: 0;
  padding: 24px 0;
  color: rgba(17, 17, 17, 0.55);
}

.stage-playoffs-shell {
  display: flex;
  flex-direction: column;
}

.stage-progress-section,
.competition-format-section {
  margin-top: 0;
}

.stage-progress-section__inner {
  padding:
    clamp(28px, 2.4vw, 42px)
    clamp(28px, 3vw, 52px)
    clamp(18px, 1.6vw, 28px);
}

.stage-section-divider {
  height: 1px;
  margin:
    0
    clamp(28px, 3vw, 52px)
    clamp(4px, 0.5vw, 8px);
  background:
    linear-gradient(
      90deg,
      transparent 0%,
      rgba(17, 17, 17, 0.28) 8%,
      rgba(17, 17, 17, 0.28) 92%,
      transparent 100%
    );
}

@media (max-width: 768px) {
  .stage-progress-section__inner {
    padding-left: 16px;
    padding-right: 16px;
  }

  .stage-section-divider {
    margin-inline: 16px;
  }
}
</style>
