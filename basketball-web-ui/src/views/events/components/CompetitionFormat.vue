<script setup lang="ts">
import { computed, inject } from 'vue'
import PaperSection from '@/components/common/PaperSection.vue'
import { displayText } from '@/utils/publicDisplay'
import type { usePublicSeasonContext } from '../composables/usePublicSeasonContext'

type PaperEdge = 'none' | 'top' | 'bottom' | 'both'
type SeasonContext = ReturnType<typeof usePublicSeasonContext>

const props = withDefaults(
  defineProps<{
    currentStage?: string
    paperEdge?: PaperEdge
    embedded?: boolean
  }>(),
  {
    currentStage: 'regular',
    paperEdge: 'both',
    embedded: false,
  },
)

const ctx = inject<SeasonContext | null>('publicSeasonContext', null)

const stages = computed(() => {
  const season = ctx?.currentSeason.value
  const overview = ctx?.overview.value
  const period =
    season?.seasonStartTime && season?.seasonEndTime
      ? `${String(season.seasonStartTime).slice(0, 10)} ~ ${String(season.seasonEndTime).slice(0, 10)}`
      : displayText(season?.seasonYear, '--')

  return [
    {
      key: 'regular',
      index: '01',
      name: '赛季概览',
      period,
      fields: [
        { label: '参赛战队', value: `${displayText(overview?.teamCount ?? 0, '0')} 支` },
        { label: '公开赛程', value: `${displayText(overview?.publicMatchCount ?? 0, '0')} 场` },
        { label: '已结束', value: `${displayText(overview?.finishedMatchCount ?? 0, '0')} 场` },
        { label: '积分记录', value: `${displayText(overview?.standingCount ?? 0, '0')} 条` },
      ],
    },
  ]
})

const activeKey = computed(() => props.currentStage || 'regular')
</script>

<template>
  <div v-if="embedded" class="competition-format competition-format--embedded">
    <div class="competition-format__inner">
      <div class="competition-format__header">
        <div class="section-head-group">
          <h2 class="cnpa-paper-section__title">赛事详情</h2>
          <span class="cnpa-paper-section__subtitle">Competition Format</span>
        </div>
      </div>

      <div v-if="!ctx?.overview.value" class="competition-format__empty">暂无赛事阶段详情</div>
      <template v-else>
        <div class="stage-cards competition-format__grid">
          <article
            v-for="stage in stages"
            :key="stage.key"
            class="stage-card"
            :class="{ active: stage.key === activeKey }"
          >
            <header>
              <h3>
                <span class="stage-card__index">{{ stage.index }}</span>
                {{ stage.name }}
              </h3>
              <span>{{ stage.period }}</span>
            </header>
            <dl>
              <div v-for="field in stage.fields" :key="field.label">
                <dt>{{ field.label }}</dt>
                <dd>{{ field.value }}</dd>
              </div>
            </dl>
          </article>
        </div>
      </template>
    </div>
  </div>

  <div v-else class="site-shell">
    <PaperSection :edge="paperEdge" tone="gray" class="competition-format">
      <div class="competition-format__inner">
        <div class="competition-format__header">
          <div class="section-head-group">
            <h2 class="cnpa-paper-section__title">赛事详情</h2>
            <span class="cnpa-paper-section__subtitle">Competition Format</span>
          </div>
        </div>

        <div v-if="!ctx?.overview.value" class="competition-format__empty">暂无赛事阶段详情</div>
        <template v-else>
          <div class="stage-cards competition-format__grid">
            <article
              v-for="stage in stages"
              :key="stage.key"
              class="stage-card"
              :class="{ active: stage.key === activeKey }"
            >
              <header>
                <h3>
                  <span class="stage-card__index">{{ stage.index }}</span>
                  {{ stage.name }}
                </h3>
                <span>{{ stage.period }}</span>
              </header>
              <dl>
                <div v-for="field in stage.fields" :key="field.label">
                  <dt>{{ field.label }}</dt>
                  <dd>{{ field.value }}</dd>
                </div>
              </dl>
            </article>
          </div>
        </template>
      </div>
    </PaperSection>
  </div>
</template>

<style scoped lang="scss">
.competition-format__inner {
  padding:
    clamp(28px, 2.4vw, 42px)
    clamp(28px, 3vw, 52px)
    clamp(32px, 2.8vw, 48px);
}

.competition-format__header {
  margin-bottom: 20px;
}

.competition-format__empty {
  min-height: 120px;
  display: grid;
  place-items: center;
  color: rgba(17, 17, 17, 0.5);
  font-weight: 700;
}

.competition-format__grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 16px;
}

.stage-card {
  border: 1px solid rgba(17, 17, 17, 0.12);
  background: #fff;
  padding: 18px;

  header {
    display: flex;
    justify-content: space-between;
    gap: 12px;
    margin-bottom: 14px;

    h3 {
      margin: 0;
      font-size: 16px;
    }

    span {
      color: rgba(17, 17, 17, 0.5);
      font-size: 12px;
      font-weight: 700;
    }
  }

  &.active {
    border-color: #e10613;
  }
}

.stage-card__index {
  margin-right: 8px;
  color: #e10613;
}

dl {
  margin: 0;
  display: grid;
  gap: 10px;

  div {
    display: flex;
    justify-content: space-between;
    gap: 12px;
    font-size: 13px;
  }

  dt {
    color: rgba(17, 17, 17, 0.55);
  }

  dd {
    margin: 0;
    font-weight: 700;
  }
}

.competition-format--embedded {
  .competition-format__inner {
    padding-top: 8px;
  }
}
</style>
