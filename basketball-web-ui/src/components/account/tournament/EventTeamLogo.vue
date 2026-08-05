<script setup lang="ts">
import { ref, computed } from 'vue'

const props = defineProps<{
  logoUrl?: string
  teamCode: string
  size?: 'sm' | 'md' | 'lg' | 'xl'
  variant?: 'default' | 'versus'
}>()

const imageError = ref(false)

const showImage = computed(
  () => Boolean(props.logoUrl?.trim()) && !imageError.value,
)

const displayCode = computed(() => {
  const code = (props.teamCode || '').trim()
  if (!code) return '--'
  if (code.length <= 4) return code.toUpperCase()
  return code.slice(0, 2).toUpperCase()
})

function onError() {
  imageError.value = true
}
</script>

<template>
  <div
    class="event-team-logo"
    :class="[
      `event-team-logo--${size || 'md'}`,
      variant === 'versus' ? 'event-team-logo--versus' : '',
    ]"
    :aria-label="`${teamCode} 球队 Logo`"
  >
    <img
      v-if="showImage"
      :src="logoUrl"
      :alt="`${teamCode} Logo`"
      @error="onError"
    />
    <span v-else class="event-team-logo__fallback">{{ displayCode }}</span>
  </div>
</template>

<style scoped lang="scss">
.event-team-logo {
  display: grid;
  place-items: center;
  border: 1px solid rgba(0, 0, 0, 0.1);
  background: linear-gradient(145deg, #1a1a1a, #3a1010);
  color: #fff;
  font-weight: 900;
  letter-spacing: 0.04em;
  overflow: hidden;
  flex-shrink: 0;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }

  &--sm {
    width: 48px;
    height: 48px;
    font-size: 14px;
  }

  &--md {
    width: 72px;
    height: 72px;
    font-size: 18px;
  }

  &--lg {
    width: 96px;
    height: 96px;
    font-size: 24px;
  }

  &--xl {
    width: 96px;
    height: 96px;
    font-size: 28px;
  }

  &--versus {
    border-radius: 16px;
    border: 1px solid rgba(220, 0, 0, 0.35);
    background: linear-gradient(135deg, #1b0808 0%, #050505 100%);
    box-shadow:
      0 12px 30px rgba(0, 0, 0, 0.35),
      inset 0 1px 0 rgba(255, 255, 255, 0.06);

    .event-team-logo__fallback {
      position: relative;
      padding-bottom: 6px;

      &::after {
        content: '';
        position: absolute;
        left: 50%;
        bottom: 0;
        width: 18px;
        height: 2px;
        transform: translateX(-50%);
        background: rgba(225, 6, 19, 0.75);
        border-radius: 1px;
      }
    }
  }
}
</style>
