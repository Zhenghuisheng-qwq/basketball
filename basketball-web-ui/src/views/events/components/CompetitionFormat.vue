<script setup lang="ts">
import { computed } from 'vue'
import { competitionStages } from '../mock'

const props = defineProps<{
  currentStage?: string
}>()

const activeKey = computed(() => props.currentStage || 'regular')
</script>

<template>
  <section class="competition-format events-section events-section-dark">
    <div class="events-inner">
      <div class="section-head">
        <div>
          <h2 class="section-head-title">赛事详情</h2>
          <span class="section-head-sub">Competition Format</span>
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
          <span v-if="index < competitionStages.length - 1" class="step-line" aria-hidden="true" />
        </div>
      </div>

      <div class="stage-cards">
        <article
          v-for="stage in competitionStages"
          :key="stage.key"
          class="stage-card"
          :class="{ active: stage.key === activeKey }"
        >
          <header>
            <h3>{{ stage.name }}</h3>
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
  </section>
</template>

<style scoped lang="scss">
.competition-format {
  padding: 56px 0;
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
  color: rgba(255, 255, 255, 0.55);

  &.active {
    color: #fff;

    .step-index {
      border-color: var(--cnpa-red);
      color: var(--cnpa-red);
    }
  }
}

.step-index {
  width: 36px;
  height: 36px;
  display: grid;
  place-items: center;
  border: 1px solid rgba(255, 255, 255, 0.18);
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
  background: rgba(255, 255, 255, 0.16);
  transform: translate(100%, -50%);
}

.stage-cards {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.stage-card {
  padding: 24px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  background: #111;
  transition: border-color 0.2s ease, transform 0.2s ease;

  &.active {
    border-color: var(--cnpa-red);
    box-shadow: inset 0 0 0 1px rgba(225, 6, 19, 0.18);
  }

  header {
    margin-bottom: 18px;

    h3 {
      margin: 0 0 6px;
      font-size: 20px;
    }

    span {
      font-size: 13px;
      color: var(--cnpa-text-secondary);
    }
  }
}

.field-row {
  display: grid;
  grid-template-columns: 92px 1fr;
  gap: 12px;
  padding: 8px 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);

  dt {
    margin: 0;
    font-size: 13px;
    color: var(--cnpa-text-secondary);
  }

  dd {
    margin: 0;
    font-size: 14px;
    font-weight: 700;
  }
}

@media (max-width: 992px) {
  .stage-cards,
  .stage-steps {
    grid-template-columns: 1fr;
  }

  .step-line {
    display: none;
  }
}
</style>
