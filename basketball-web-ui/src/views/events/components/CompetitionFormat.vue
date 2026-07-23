<script setup lang="ts">
import { computed } from 'vue'
import PaperSection from '@/components/common/PaperSection.vue'
import { competitionStages } from '../mock'

type PaperEdge = 'none' | 'top' | 'bottom' | 'both'

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

      <div class="stage-steps">
        <div
          v-for="(stage, index) in competitionStages"
          :key="stage.key"
          class="stage-step"
          :class="{ active: stage.key === activeKey }"
        >
          <span class="step-index">{{ stage.index }}</span>
          <span class="step-name">{{ stage.name }}</span>
          <span v-if="index < competitionStages.length - 1" class="stage-line" aria-hidden="true" />
        </div>
      </div>

      <div class="stage-cards competition-format__grid">
        <article
          v-for="stage in competitionStages"
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
            <div v-for="field in stage.fields" :key="field.label" class="field-row">
              <dt>{{ field.label }}</dt>
              <dd>{{ field.value }}</dd>
            </div>
          </dl>
        </article>
      </div>
    </div>
  </div>

  <div v-else class="site-shell competition-format-shell">
    <PaperSection :edge="paperEdge" tone="gray" class="competition-format">
      <div class="competition-format__inner">
        <div class="competition-format__header">
          <div class="section-head-group">
            <h2 class="cnpa-paper-section__title">赛事详情</h2>
            <span class="cnpa-paper-section__subtitle">Competition Format</span>
          </div>
        </div>

        <div class="stage-steps">
          <div
            v-for="(stage, index) in competitionStages"
            :key="stage.key"
            class="stage-step"
            :class="{ active: stage.key === activeKey }"
          >
            <span class="step-index">{{ stage.index }}</span>
            <span class="step-name">{{ stage.name }}</span>
            <span v-if="index < competitionStages.length - 1" class="stage-line" aria-hidden="true" />
          </div>
        </div>

        <div class="stage-cards competition-format__grid">
          <article
            v-for="stage in competitionStages"
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
              <div v-for="field in stage.fields" :key="field.label" class="field-row">
                <dt>{{ field.label }}</dt>
                <dd>{{ field.value }}</dd>
              </div>
            </dl>
          </article>
        </div>
      </div>
    </PaperSection>
  </div>
</template>

<style scoped lang="scss">
.competition-format-shell {
  margin-top: 0;
}

.competition-format {
  margin-top: 0;
}

.competition-format__inner {
  padding:
    clamp(28px, 2.4vw, 42px)
    clamp(28px, 3vw, 52px)
    clamp(32px, 2.8vw, 48px);
}

.competition-format--embedded .competition-format__inner {
  padding-top: clamp(20px, 2vw, 28px);
}

.competition-format__header {
  margin-bottom: 24px;
}

.stage-steps {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 28px;
}

.stage-step {
  position: relative;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 0;
  color: var(--cnpa-paper-text-muted);

  &.active {
    color: var(--cnpa-paper-text);

    .step-index {
      border-color: var(--cnpa-red);
      color: var(--cnpa-red);
    }

    .step-line {
      background: rgba(225, 6, 19, 0.28);

      &::after {
        border-left-color: var(--cnpa-red);
      }
    }
  }
}

.step-index {
  width: 36px;
  height: 36px;
  display: grid;
  place-items: center;
  border: 1px solid rgba(17, 17, 17, 0.18);
  border-radius: 50%;
  font-size: 13px;
  font-weight: 900;
}

.step-name {
  font-size: 15px;
  font-weight: 800;
}

.step-line {
  position: absolute;
  right: -6px;
  top: 50%;
  width: calc(100% - 120px);
  max-width: 120px;
  height: 1px;
  background: rgba(17, 17, 17, 0.14);
  transform: translate(100%, -50%);

  &::after {
    content: '';
    position: absolute;
    right: 0;
    top: 50%;
    transform: translateY(-50%);
    border-top: 4px solid transparent;
    border-bottom: 4px solid transparent;
    border-left: 6px solid rgba(17, 17, 17, 0.28);
  }
}

.competition-format__grid,
.stage-cards {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.stage-card {
  padding: 24px;
  border: 1px solid rgba(17, 17, 17, 0.12);
  border-radius: 6px;
  background: var(--cnpa-paper-surface);
  transition: border-color 0.2s ease, transform 0.2s ease, box-shadow 0.2s ease;

  &.active {
    border-color: rgba(225, 6, 19, 0.42);
    box-shadow: 0 10px 28px rgba(225, 6, 19, 0.08);
  }

  header {
    margin-bottom: 18px;

    h3 {
      display: flex;
      align-items: center;
      gap: 8px;
      margin: 0 0 6px;
      font-size: 20px;
      color: var(--cnpa-paper-text);
    }

    span {
      font-size: 13px;
      color: var(--cnpa-paper-text-muted);
    }
  }
}

.stage-card__index {
  font-size: 14px;
  font-weight: 900;
  color: var(--cnpa-red);
}

.field-row {
  display: grid;
  grid-template-columns: 92px 1fr;
  gap: 12px;
  padding: 8px 0;
  border-bottom: 1px solid rgba(17, 17, 17, 0.08);

  dt {
    margin: 0;
    font-size: 13px;
    color: var(--cnpa-paper-text-muted);
  }

  dd {
    margin: 0;
    font-size: 14px;
    font-weight: 700;
    color: var(--cnpa-paper-text);
  }
}

@media (max-width: 992px) {
  .competition-format__grid,
  .stage-cards,
  .stage-steps {
    grid-template-columns: 1fr;
  }

  .step-line {
    display: none;
  }
}

@media (max-width: 768px) {
  .competition-format__inner {
    padding-left: 16px;
    padding-right: 16px;
  }
}
</style>
