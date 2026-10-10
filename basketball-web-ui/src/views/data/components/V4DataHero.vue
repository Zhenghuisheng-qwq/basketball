<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import PublicHero from '@/components/layout/PublicHero.vue'
import type { V4PublicDivisionBrief, V4PublicSeasonItem } from '@/api/types/v4Public'
import { localizedText } from '@/utils/publicDisplay'
import { v4PlatformLabel, v4SeriesLabel } from '@/utils/v4Display'

const props = defineProps<{
  loading: boolean
  error: string
  seasons: V4PublicSeasonItem[]
  selectedSeasonId: string
  selectedDivisionId: string
  divisions: V4PublicDivisionBrief[]
}>()

const emit = defineEmits<{
  'update:selectedSeasonId': [value: string]
  'update:selectedDivisionId': [value: string]
}>()

const { t } = useI18n()

type OpenPanel = 'season' | 'division' | null

const openPanel = ref<OpenPanel>(null)
const rootRef = ref<HTMLElement | null>(null)

const selectedSeason = computed(
  () => props.seasons.find((item) => item.seasonId === props.selectedSeasonId) ?? props.seasons[0],
)

const selectedDivision = computed(
  () => props.divisions.find((item) => item.divisionId === props.selectedDivisionId) ?? props.divisions[0],
)

const seasonLabel = computed(() => {
  if (props.loading) return t('publicPages.data.loadingSeasons')
  if (props.error) return props.error
  if (!selectedSeason.value) return t('publicPages.data.selectSeason')
  return `${v4SeriesLabel(selectedSeason.value.seriesCode)} · ${localizedText(selectedSeason.value.displayTitle, selectedSeason.value.displayTitleEn)}`
})

const divisionLabel = computed(() => {
  if (!selectedDivision.value) return t('publicPages.data.selectDivision')
  return `${v4PlatformLabel(selectedDivision.value.platformCode)} · ${localizedText(selectedDivision.value.divisionName, selectedDivision.value.divisionNameEn)}`
})

const seasonGroups = computed(() => {
  const map = new Map<string, V4PublicSeasonItem[]>()
  for (const season of props.seasons) {
    const key = v4SeriesLabel(season.seriesCode) || t('publicPages.data.seriesFallback')
    if (!map.has(key)) map.set(key, [])
    map.get(key)!.push(season)
  }
  return [...map.entries()].map(([title, items]) => ({ title, items }))
})

function closePanel() {
  openPanel.value = null
}

function togglePanel(panel: Exclude<OpenPanel, null>) {
  if (props.loading || props.error) return
  openPanel.value = openPanel.value === panel ? null : panel
}

function selectSeason(seasonId: string) {
  emit('update:selectedSeasonId', seasonId)
  closePanel()
}

function selectDivision(divisionId: string) {
  emit('update:selectedDivisionId', divisionId)
  closePanel()
}

function handleDocumentClick(event: MouseEvent) {
  if (!openPanel.value || !rootRef.value) return
  if (!rootRef.value.contains(event.target as Node)) {
    closePanel()
  }
}

function handleKeydown(event: KeyboardEvent) {
  if (event.key === 'Escape' && openPanel.value) {
    closePanel()
  }
}

watch(openPanel, async (value) => {
  if (value) await nextTick()
})

onMounted(() => {
  document.addEventListener('click', handleDocumentClick)
  document.addEventListener('keydown', handleKeydown)
})

onBeforeUnmount(() => {
  document.removeEventListener('click', handleDocumentClick)
  document.removeEventListener('keydown', handleKeydown)
})
</script>

<template>
  <PublicHero variant="data" :ariaLabel="t('publicPages.data.heroAria')">
    <div ref="rootRef" class="v4-data-hero__controls">
      <div class="v4-data-selector" :class="{ 'is-open': openPanel === 'season' }">
        <button
          type="button"
          class="v4-data-selector__trigger"
          :aria-expanded="openPanel === 'season'"
          aria-haspopup="listbox"
          @click.stop="togglePanel('season')"
        >
          <span class="v4-data-selector__label">{{ seasonLabel }}</span>
          <span class="v4-data-selector__arrow" aria-hidden="true" />
        </button>

        <div
          v-show="openPanel === 'season'"
          class="v4-data-selector__dropdown"
          role="listbox"
          :aria-label="t('publicPages.data.seasonList')"
        >
          <h3 class="v4-data-selector__title">{{ t('publicPages.data.selectSeason') }}</h3>
          <div v-if="!seasonGroups.length" class="v4-data-selector__empty">{{ t('publicPages.data.noSeasons') }}</div>
          <div
            v-for="group in seasonGroups"
            :key="group.title"
            class="v4-data-selector__group"
          >
            <p class="v4-data-selector__group-title">{{ group.title }}</p>
            <button
              v-for="season in group.items"
              :key="season.seasonId"
              type="button"
              class="v4-data-selector__option"
              :class="{ 'is-selected': season.seasonId === selectedSeasonId }"
              role="option"
              :aria-selected="season.seasonId === selectedSeasonId"
              @click="selectSeason(season.seasonId)"
            >
              <span>{{ localizedText(season.displayTitle, season.displayTitleEn) }}</span>
              <span class="v4-data-selector__check" aria-hidden="true" />
            </button>
          </div>
        </div>
      </div>

      <div
        v-if="!loading && !error && divisions.length"
        class="v4-data-selector"
        :class="{ 'is-open': openPanel === 'division' }"
      >
        <button
          type="button"
          class="v4-data-selector__trigger"
          :aria-expanded="openPanel === 'division'"
          aria-haspopup="listbox"
          @click.stop="togglePanel('division')"
        >
          <span class="v4-data-selector__label">{{ divisionLabel }}</span>
          <span class="v4-data-selector__arrow" aria-hidden="true" />
        </button>

        <div
          v-show="openPanel === 'division'"
          class="v4-data-selector__dropdown"
          role="listbox"
          :aria-label="t('publicPages.data.divisionList')"
        >
          <h3 class="v4-data-selector__title">{{ t('publicPages.data.selectDivision') }}</h3>
          <button
            v-for="division in divisions"
            :key="division.divisionId"
            type="button"
            class="v4-data-selector__option"
            :class="{ 'is-selected': division.divisionId === selectedDivisionId }"
            role="option"
            :aria-selected="division.divisionId === selectedDivisionId"
            @click="selectDivision(division.divisionId)"
          >
            <span>{{ v4PlatformLabel(division.platformCode) }} · {{ localizedText(division.divisionName, division.divisionNameEn) }}</span>
            <span class="v4-data-selector__check" aria-hidden="true" />
          </button>
        </div>
      </div>
    </div>
  </PublicHero>
</template>

<style scoped lang="scss">
.v4-data-hero__controls {
  position: absolute;
  top: clamp(18px, 3.2vw, 30px);
  right: clamp(12px, 2.4vw, 28px);
  z-index: 40;
  display: flex;
  flex-direction: column;
  gap: 10px;
  width: min(100%, 254px);
}

.v4-data-selector {
  position: relative;
  z-index: 40;
  width: 100%;
}

.v4-data-selector__trigger {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  width: 100%;
  height: 42px;
  padding: 0 14px;
  border: 1px solid #4a4a4a;
  border-radius: 6px;
  background: rgba(12, 12, 12, 0.92);
  color: #fff;
  cursor: pointer;
  font: inherit;
  backdrop-filter: blur(8px) saturate(120%);
  -webkit-backdrop-filter: blur(8px) saturate(120%);
}

.v4-data-selector__label {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 14px;
  font-weight: 600;
}

.v4-data-selector__arrow {
  flex: 0 0 auto;
  width: 0;
  height: 0;
  border-left: 5px solid transparent;
  border-right: 5px solid transparent;
  border-top: 6px solid rgba(255, 255, 255, 0.72);
  transition: transform 0.16s ease;
}

.v4-data-selector.is-open .v4-data-selector__arrow {
  transform: rotate(180deg);
}

.v4-data-selector__dropdown {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  left: 0;
  z-index: 50;
  max-height: min(420px, 55vh);
  overflow-y: auto;
  padding: 14px 12px 12px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 8px;
  background: rgba(18, 18, 18, 0.98);
  box-shadow: 0 16px 36px rgba(0, 0, 0, 0.42);
}

.v4-data-selector__title {
  margin: 0 0 10px;
  color: #fff;
  font-size: 15px;
  font-weight: 800;
}

.v4-data-selector__group + .v4-data-selector__group {
  margin-top: 12px;
}

.v4-data-selector__group-title {
  margin: 0 0 6px;
  color: rgba(255, 255, 255, 0.48);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.04em;
  text-transform: uppercase;
}

.v4-data-selector__option {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  width: 100%;
  min-height: 40px;
  margin: 0;
  padding: 8px 10px;
  border: 0;
  border-radius: 4px;
  background: transparent;
  color: rgba(255, 255, 255, 0.86);
  text-align: left;
  cursor: pointer;
  font: inherit;
  font-size: 13px;
  font-weight: 600;

  &:hover,
  &.is-selected {
    background: rgba(225, 6, 19, 0.22);
    color: #fff;
  }
}

.v4-data-selector__check {
  flex: 0 0 auto;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: transparent;
}

.v4-data-selector__option.is-selected .v4-data-selector__check {
  background: #ff1717;
}

.v4-data-selector__empty {
  padding: 16px 8px;
  color: rgba(255, 255, 255, 0.55);
  font-size: 13px;
  text-align: center;
}

@media (max-width: 767px) {
  .v4-data-hero__controls {
    top: 12px;
    right: 10px;
    width: min(68%, 220px);
  }
}
</style>
