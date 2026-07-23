<script setup lang="ts">
import { useRouter } from 'vue-router'
import { goToPlayerDetail, type PlayerDetailTab } from '@/utils/playerNavigate'

const props = withDefaults(
  defineProps<{
    playerId: string | number
    playerName: string
    tab?: PlayerDetailTab
    inline?: boolean
  }>(),
  {
    tab: 'overview',
    inline: true,
  },
)

const router = useRouter()

function handleClick(event: MouseEvent) {
  event.preventDefault()
  event.stopPropagation()
  goToPlayerDetail(router, props.playerId, props.tab)
}
</script>

<template>
  <button
    type="button"
    class="player-link"
    :class="{ 'player-link--block': !inline }"
    @click="handleClick"
  >
    {{ playerName }}
  </button>
</template>

<style scoped lang="scss">
.player-link {
  margin: 0;
  padding: 0;
  border: 0;
  background: transparent;
  color: inherit;
  font: inherit;
  cursor: pointer;
  text-decoration: none;
  transition: color 0.16s ease;

  &:hover {
    color: var(--cnpa-red, #e10613);
    text-decoration: underline;
    text-underline-offset: 2px;
  }

  &--block {
    display: inline-block;
  }
}
</style>
