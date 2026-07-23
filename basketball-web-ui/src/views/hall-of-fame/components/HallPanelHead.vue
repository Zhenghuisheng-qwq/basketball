<script setup lang="ts">
withDefaults(
  defineProps<{
    title: string
    subtitle: string
    layout?: 'stack' | 'inline'
  }>(),
  {
    layout: 'stack',
  },
)
</script>

<template>
  <header class="hall-panel-head" :class="`hall-panel-head--${layout}`">
    <div class="hall-panel-head__titles">
      <h2 class="hall-panel-head__title">{{ title }}</h2>
      <span class="hall-panel-head__sub">{{ subtitle }}</span>
    </div>
    <div v-if="$slots.filters" class="hall-panel-head__filters">
      <slot name="filters" />
    </div>
  </header>
</template>

<style scoped lang="scss">
.hall-panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 24px;
}

.hall-panel-head__titles {
  min-width: 0;
}

.hall-panel-head--inline .hall-panel-head__titles {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: 10px 14px;
}

.hall-panel-head__title {
  margin: 0;
  color: var(--hall-text, #171717);
  font-size: 30px;
  font-weight: 800;
  line-height: 1.1;
}

.hall-panel-head__sub {
  display: block;
  margin-top: 5px;
  color: var(--hall-red, #e71922);
  font-size: 14px;
  font-weight: 700;
  font-style: italic;
  letter-spacing: 0.06em;
}

.hall-panel-head--inline .hall-panel-head__sub {
  margin-top: 0;
}

.hall-panel-head__filters {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}

@media (max-width: 767px) {
  .hall-panel-head {
    flex-direction: column;
    align-items: stretch;
  }

  .hall-panel-head__filters {
    width: 100%;
  }
}
</style>
