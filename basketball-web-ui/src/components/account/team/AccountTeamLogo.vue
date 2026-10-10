<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { resolveTeamLogoFallback, resolveTeamLogoUrl } from '@/utils/teamLogo'

const { t } = useI18n()

const props = withDefaults(
  defineProps<{
    logoUrl?: string | null
    shortName?: string | null
    teamName?: string | null
    size?: 'sm' | 'md' | 'lg'
    alt?: string
  }>(),
  {
    size: 'md',
  },
)

const imageError = ref(false)

const logoSrc = computed(() => resolveTeamLogoUrl(props.logoUrl))
const fallbackText = computed(() => resolveTeamLogoFallback(props.shortName, props.teamName))
const showImage = computed(() => Boolean(logoSrc.value) && !imageError.value)
const imageAlt = computed(() => props.alt || props.teamName || t('accountTeam.logo.alt'))

watch(
  () => props.logoUrl,
  () => {
    imageError.value = false
  },
)

function handleError() {
  imageError.value = true
}
</script>

<template>
  <div
    class="account-team-logo"
    :class="`account-team-logo--${size}`"
    role="img"
    :aria-label="imageAlt"
  >
    <img
      v-if="showImage"
      :src="logoSrc"
      :alt="imageAlt"
      class="account-team-logo__img"
      @error="handleError"
    />
    <span v-else class="account-team-logo__fallback">{{ fallbackText }}</span>
  </div>
</template>

<style scoped lang="scss">
.account-team-logo {
  display: grid;
  place-items: center;
  flex-shrink: 0;
  overflow: hidden;
  border: 1px solid rgba(217, 0, 0, 0.45);
  background: linear-gradient(145deg, #111 0%, #2a0a0a 100%);
  color: var(--account-red, #d90000);
  font-weight: 900;
  letter-spacing: 0.04em;
  box-sizing: border-box;
}

.account-team-logo__img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.account-team-logo__fallback {
  padding: 0 4px;
  text-align: center;
  line-height: 1.1;
  word-break: break-all;
}

.account-team-logo--sm {
  width: 72px;
  height: 72px;
  border-radius: 4px;
  font-size: 16px;
}

.account-team-logo--md {
  width: 88px;
  height: 88px;
  border-radius: 4px;
  font-size: 18px;
}

.account-team-logo--lg {
  width: 96px;
  height: 96px;
  border-radius: 4px;
  font-size: 22px;
}
</style>
