<script setup lang="ts">
import { computed, inject, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import PaperSection from '@/components/common/PaperSection.vue'
import { getV4PublicStageList } from '@/api/v4Public'
import type { V4PublicStageItem } from '@/api/types/v4Public'
import type { usePublicSeasonContext } from '../composables/usePublicSeasonContext'
import RegularStandings from './RegularStandings.vue'
import PlayoffBracket from './PlayoffBracket.vue'
import CompetitionFormat from './CompetitionFormat.vue'
import LeagueAbout from './LeagueAbout.vue'
import StageProgressHeader from './StageProgressHeader.vue'
import PaperTear from '@/components/common/PaperTear.vue'
import {
  isBracketStage,
  isGroupedStage,
  pickDefaultStage,
  stageEmptyText,
  stageHeading,
  stageSelectorLabel,
} from '../utils/stageRenderer'

type SeasonContext = ReturnType<typeof usePublicSeasonContext>
const ctx = inject<SeasonContext>('publicSeasonContext')!
const { t } = useI18n()

const stages = ref<V4PublicStageItem[]>([])
const selectedStageId = ref('')
const stagesLoading = ref(false)
const stagesFailed = ref(false)

const selectedStage = computed(() =>
  stages.value.find((item) => item.stageId === selectedStageId.value) || null,
)

const showBracket = computed(() => isBracketStage(selectedStage.value))

const stageOptions = computed(() =>
  stages.value.map((item) => ({
    value: item.stageId,
    label: stageSelectorLabel(item),
  })),
)

const heading = computed(() => stageHeading(selectedStage.value))
const emptyText = computed(() => stageEmptyText(selectedStage.value))
const grouped = computed(() => isGroupedStage(selectedStage.value))
const seriesLabel = computed(() => {
  const name = ctx.overview.value?.tournamentName || ctx.seasonDetail.value?.seriesName || ''
  return String(name).trim()
})

const stageLive = computed(() => {
  const status = String(selectedStage.value?.executionStatus || '').toUpperCase()
  return selectedStage.value?.currentStage === true || status === 'RUNNING' || status === 'RESULT_PENDING_CONFIRM'
})

async function loadStages(divisionId: string) {
  if (!divisionId) {
    stages.value = []
    selectedStageId.value = ''
    stagesFailed.value = false
    return
  }
  stagesLoading.value = true
  stagesFailed.value = false
  try {
    const res = await getV4PublicStageList(divisionId)
    const list = [...(res.data ?? [])].sort(
      (a, b) => (a.stageOrder ?? 0) - (b.stageOrder ?? 0) || String(a.stageId).localeCompare(String(b.stageId)),
    )
    stages.value = list
    const current = pickDefaultStage(list)
    selectedStageId.value = current?.stageId || ''
  } catch {
    stages.value = []
    selectedStageId.value = ''
    stagesFailed.value = true
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
        <PaperSection edge="none" tone="gray" class="stage-main">
          <div class="stage-main__inner">
            <StageProgressHeader
              v-if="stageOptions.length"
              v-model="selectedStageId"
              :options="stageOptions"
              variant="paper"
            />
            <p v-if="stagesLoading" class="stage-state">{{ t('eventsPage.stage.loading') }}</p>
            <p v-else-if="stagesFailed" class="stage-state">{{ t('eventsPage.stage.loadFailed') }}</p>
            <p v-else-if="!selectedStageId" class="stage-state">{{ t('eventsPage.stage.empty') }}</p>
            <RegularStandings
              v-else
              :stage-id="selectedStageId"
              :grouped="grouped"
              :stage-name="selectedStage?.stageName"
              :advance-count="selectedStage?.advancementCount"
              :empty-text="emptyText"
              :zone-tone="grouped ? 'qualify' : 'playoff'"
            />
          </div>
        </PaperSection>
      </div>

      <CompetitionFormat paper-edge="none" :current-stage="selectedStageId" />
    </template>

    <template v-else>
      <div class="site-shell stage-playoffs-shell">
        <PaperSection edge="none" tone="gray" class="stage-progress-section">
          <div class="stage-progress-section__inner">
            <StageProgressHeader
              v-if="stageOptions.length"
              v-model="selectedStageId"
              :options="stageOptions"
              variant="paper"
              :context-label="seriesLabel"
              :stage-title="heading.title"
              :stage-title-en="heading.titleEn"
              :stage-live="stageLive"
            />
            <p v-if="stagesLoading" class="stage-state">{{ t('eventsPage.stage.loading') }}</p>
            <p v-else-if="stagesFailed" class="stage-state">{{ t('eventsPage.stage.loadFailed') }}</p>
            <PlayoffBracket
              v-else
              hide-heading
              :focus-stage-id="selectedStageId"
              :title="heading.title"
              :title-en="heading.titleEn"
              :empty-text="emptyText"
            />
          </div>
        </PaperSection>

        <PaperSection edge="none" tone="gray" class="competition-format-section">
          <div class="stage-section-tear" aria-hidden="true">
            <PaperTear position="top" tone="dark" />
          </div>
          <CompetitionFormat embedded :current-stage="selectedStageId" />
        </PaperSection>
      </div>
    </template>

    <LeagueAbout />
  </div>
</template>
<style scoped lang="scss">
.stage-main {
  margin-top: 0;
  color: #111;
}

.stage-main__inner {
  padding:
    clamp(24px, 2.2vw, 36px)
    var(--cnpa-public-content-gutter)
    clamp(24px, 2.2vw, 36px);
}

.stage-main :deep(.stage-progress-header) {
  margin-bottom: clamp(12px, 1.2vw, 16px);
}

.stage-state {
  margin: 0;
  padding: 24px 0;
  color: rgba(17, 17, 17, 0.55);
}

.stage-playoffs-shell {
  --stage-title-inline: var(--cnpa-public-content-gutter);
  display: flex;
  flex-direction: column;
}

.stage-progress-section,
.competition-format-section {
  margin-top: 0;
}

.stage-progress-section__inner {
  padding:
    clamp(22px, 2vw, 34px)
    var(--stage-title-inline)
    clamp(12px, 1.2vw, 20px);
}

.stage-playoffs-shell :deep(.stage-progress-header) {
  margin-bottom: 10px;
}

.stage-playoffs-shell :deep(.stage-progress-header__select) {
  width: clamp(140px, 12vw, 180px);
}

.competition-format-section :deep(.competition-format--embedded .competition-format__inner) {
  padding-inline: var(--stage-title-inline);
}

.stage-section-tear {
  --paper-tear-color: #161616;
  height: 18px;
  overflow: hidden;
}

@media (max-width: 768px) {
  .stage-progress-section__inner {
    padding-left: var(--cnpa-public-content-gutter);
    padding-right: var(--cnpa-public-content-gutter);
  }
}
</style>
