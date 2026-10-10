<script setup lang="ts">
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  competitionMenuGroups,
  type MegaMenuType,
  type PublicMenuEntry,
} from '@/constants/publicCompetitionMenu'

const { t } = useI18n()

const eventGroups = computed(() =>
  competitionMenuGroups.event.map((group) => ({
    titleKey: group.titleKey,
    title: t(group.titleKey),
    entries: group.entries,
  })),
)

defineProps<{
  isEventsNavActive: boolean
  getEntryHighlight: (entry: PublicMenuEntry) => 'primary' | 'secondary' | null
}>()

const emit = defineEmits<{
  navigate: [entry: PublicMenuEntry]
  close: []
}>()

const openSection = ref<MegaMenuType | null>(null)

function toggleSection(section: MegaMenuType) {
  openSection.value = openSection.value === section ? null : section
}

function handleNavigate(entry: PublicMenuEntry) {
  emit('navigate', entry)
  emit('close')
  openSection.value = null
}
</script>

<template>
  <div class="mobile-competition-nav">
    <div class="mobile-competition-nav__section">
      <button
        type="button"
        class="mobile-competition-nav__trigger"
        :class="{ 'mobile-competition-nav__trigger--active': isEventsNavActive }"
        :aria-expanded="openSection === 'event'"
        @click="toggleSection('event')"
      >
        {{ t('nav.events') }}
      </button>

      <div v-if="openSection === 'event'" class="mobile-competition-nav__panel">
        <div
          v-for="group in eventGroups"
          :key="group.titleKey"
          class="mobile-competition-nav__group"
        >
          <div class="mobile-competition-nav__group-title">{{ group.title }}</div>
          <button
            v-for="entry in group.entries"
            :key="entry.id"
            type="button"
            class="mobile-competition-nav__entry"
            :class="{
              'mobile-competition-nav__entry--active': getEntryHighlight(entry) === 'primary',
              'mobile-competition-nav__entry--secondary': getEntryHighlight(entry) === 'secondary',
            }"
            @click="handleNavigate(entry)"
          >
            <span class="mobile-competition-nav__icon">
              <img v-if="entry.icon" :src="entry.icon" alt="" />
              <template v-else>{{ entry.abbrev }}</template>
            </span>
            <span>{{ entry.label }}</span>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.mobile-competition-nav {
  width: 100%;
}

.mobile-competition-nav__section + .mobile-competition-nav__section {
  border-top: 1px solid rgba(255, 255, 255, 0.06);
}

.mobile-competition-nav__trigger {
  display: block;
  width: 100%;
  padding: 14px var(--site-margin-inline);
  border: 0;
  background: transparent;
  color: rgba(255, 255, 255, 0.88);
  font-size: 15px;
  font-weight: 800;
  text-align: left;
  cursor: pointer;
  font-family: inherit;

  &--active {
    color: var(--cnpa-red);
  }
}

.mobile-competition-nav__panel {
  padding: 0 var(--site-margin-inline) 12px;
}

.mobile-competition-nav__group + .mobile-competition-nav__group {
  margin-top: 12px;
}

.mobile-competition-nav__group-title {
  margin-bottom: 6px;
  padding: 7px 10px;
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.07);
  color: rgba(255, 255, 255, 0.82);
  font-size: 13px;
  font-weight: 700;
}

.mobile-competition-nav__entry {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  min-height: 42px;
  padding: 8px 10px;
  border: 0;
  border-radius: 4px;
  background: transparent;
  color: rgba(255, 255, 255, 0.82);
  font-size: 14px;
  font-weight: 600;
  text-align: left;
  cursor: pointer;
  font-family: inherit;

  &--active {
    background: rgba(142, 0, 8, 0.55);
    color: #fff;
  }

  &--secondary {
    background: rgba(255, 255, 255, 0.04);
  }
}

.mobile-competition-nav__icon {
  display: grid;
  place-items: center;
  width: 28px;
  height: 28px;
  border-radius: 4px;
  overflow: hidden;
  background: #fff;
  font-size: 10px;
  font-weight: 800;

  img {
    display: block;
    width: 100%;
    height: 100%;
    object-fit: contain;
  }
}
</style>
