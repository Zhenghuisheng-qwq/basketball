<script setup lang="ts">
import { computed } from 'vue'
import TornEdge from '@/components/home/TornEdge.vue'

type PaperEdge = 'none' | 'top' | 'bottom' | 'both'
type PaperTone = 'gray' | 'light'

interface Props {
  edge?: PaperEdge
  tone?: PaperTone
  tag?: string
}

const props = withDefaults(defineProps<Props>(), {
  edge: 'both',
  tone: 'gray',
  tag: 'section',
})

const sectionClasses = computed(() => [
  'cnpa-paper-section',
  `cnpa-paper-section--${props.tone}`,
])

const tornTone = computed(() => (props.tone === 'light' ? 'light' : 'gray'))

const showTopEdge = computed(() => props.edge === 'top' || props.edge === 'both')
const showBottomEdge = computed(() => props.edge === 'bottom' || props.edge === 'both')
</script>

<template>
  <component :is="tag" :class="sectionClasses">
    <TornEdge v-if="showTopEdge" position="top" :tone="tornTone" />
    <div class="cnpa-paper-section__content">
      <slot />
    </div>
    <TornEdge v-if="showBottomEdge" position="bottom" :tone="tornTone" />
  </component>
</template>
