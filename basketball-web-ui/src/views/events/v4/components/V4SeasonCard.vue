<script setup lang="ts">
import { computed, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import type { V4PublicSeasonItem } from '@/api/types/v4Public'
import { displayText, formatDateTime, localizedText } from '@/utils/publicDisplay'
import {
  isSeasonRegistrationOpen,
  v4GameModeLabel,
  v4JoinLabels,
  v4PlatformLabel,
  v4RegistrationModeLabel,
  v4SeasonStatusLabel,
  v4SeriesAbbrev,
  v4SeriesLabel,
} from '@/utils/v4Display'

const props = defineProps<{
  season: V4PublicSeasonItem
}>()

const emit = defineEmits<{
  select: [season: V4PublicSeasonItem]
}>()

const { t } = useI18n()
const imageFailed = ref(false)

const coverUrl = computed(() => props.season.bannerUrl || props.season.coverUrl || '')
const showCover = computed(() => Boolean(coverUrl.value?.trim()) && !imageFailed.value)

const displayTitle = computed(() =>
  localizedText(
    props.season.displayTitle,
    props.season.displayTitleEn,
    displayText(props.season.seasonName, t('eventsPage.list.unnamedSeason')),
  ),
)

const secondaryTitle = computed(() => {
  const parts = [props.season.seasonName, props.season.seasonYear ? t('eventsPage.count.seasonYear', { year: props.season.seasonYear }) : '']
    .filter(Boolean)
    .filter((item) => item !== displayTitle.value)
  return parts.join(' · ')
})

const platformSummary = computed(() =>
  v4JoinLabels(
    props.season.platformCodes?.length
      ? props.season.platformCodes
      : props.season.divisions?.map((item) => item.platformCode),
    v4PlatformLabel,
  ),
)

const gameModeSummary = computed(() =>
  v4JoinLabels(props.season.gameModes, v4GameModeLabel),
)

const registrationSummary = computed(() =>
  v4JoinLabels(props.season.registrationModes, v4RegistrationModeLabel),
)

const divisionCountText = computed(() => {
  const count = props.season.divisionCount ?? props.season.divisions?.length ?? 0
  return t('eventsPage.count.divisions', { count })
})

const registrationEndText = computed(() => {
  const endTime = props.season.nearestRegistrationEndTime || props.season.registrationEndTime
  return endTime ? formatDateTime(endTime).full : '--'
})

const matchTimeText = computed(() => {
  const start = formatDateTime(props.season.matchStartTime).full
  const end = formatDateTime(props.season.matchEndTime).full
  if (start === '--' && end === '--') return '--'
  if (start === '--') return end
  if (end === '--') return start
  return `${start} — ${end}`
})

const actionLabel = computed(() =>
  isSeasonRegistrationOpen(props.season.seasonStatus) ? t('eventsPage.list.viewDivisions') : t('eventsPage.list.viewEvent'),
)

const statusClass = computed(() => {
  const status = (props.season.seasonStatus || '').toUpperCase()
  if (status === 'REGISTRATION_OPEN') return 'is-open'
  if (status === 'PLAYING') return 'is-playing'
  if (status === 'FINISHED' || status === 'ARCHIVED') return 'is-finished'
  return ''
})

function onImageError() {
  imageFailed.value = true
}
</script>

<template>
  <article class="v4-season-card">
    <div class="v4-season-card__cover">
      <img
        v-if="showCover"
        :src="coverUrl"
        :alt="displayTitle"
        class="v4-season-card__cover-img"
        loading="lazy"
        @error="onImageError"
      />
      <div
        v-else
        class="v4-season-card__cover-placeholder"
        :class="`v4-season-card__cover-placeholder--${(season.seriesCode || 'default').toLowerCase()}`"
      >
        <span class="v4-season-card__cover-abbr">{{ v4SeriesAbbrev(season.seriesCode) }}</span>
        <span class="v4-season-card__cover-year">{{ season.seasonYear || 'CNPA' }}</span>
      </div>
    </div>

    <div class="v4-season-card__body">
      <div class="v4-season-card__tags">
        <span class="v4-season-card__tag v4-season-card__tag--series">
          {{ v4SeriesLabel(season.seriesCode) }}
        </span>
        <span class="v4-season-card__tag" :class="statusClass">
          {{ v4SeasonStatusLabel(season.seasonStatus) }}
        </span>
      </div>

      <h2 class="v4-season-card__title">{{ displayTitle }}</h2>
      <p v-if="secondaryTitle" class="v4-season-card__subtitle">{{ secondaryTitle }}</p>

      <dl class="v4-season-card__summary">
        <div>
          <dt>{{ t('eventsPage.list.platform') }}</dt>
          <dd>{{ platformSummary }}</dd>
        </div>
        <div>
          <dt>{{ t('eventsPage.list.mode') }}</dt>
          <dd>{{ gameModeSummary }}</dd>
        </div>
        <div>
          <dt>{{ t('eventsPage.list.registration') }}</dt>
          <dd>{{ registrationSummary }}</dd>
        </div>
        <div>
          <dt>{{ t('eventsPage.list.division') }}</dt>
          <dd>{{ divisionCountText }}</dd>
        </div>
      </dl>

      <dl class="v4-season-card__times">
        <div>
          <dt>{{ t('eventsPage.list.registrationDeadline') }}</dt>
          <dd>{{ registrationEndText }}</dd>
        </div>
        <div>
          <dt>{{ t('eventsPage.list.matchTime') }}</dt>
          <dd>{{ matchTimeText }}</dd>
        </div>
      </dl>

      <button
        type="button"
        class="v4-season-card__action"
        @click="emit('select', season)"
      >
        {{ actionLabel }}
      </button>
    </div>
  </article>
</template>

<style scoped lang="scss">
.v4-season-card {
  display: flex;
  flex-direction: column;
  height: 100%;
  border: 1px solid var(--cnpa-paper-border, rgba(25, 25, 25, 0.11));
  background:
    linear-gradient(180deg, #f5f3f3 0%, #f1efef 100%);
  box-shadow: 0 14px 36px rgba(0, 0, 0, 0.08);
  overflow: hidden;
}

.v4-season-card__cover {
  position: relative;
  aspect-ratio: 16 / 7;
  background: #111;
  overflow: hidden;
}

.v4-season-card__cover-img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.v4-season-card__cover-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  color: #fff;
  background:
    linear-gradient(135deg, rgba(8, 8, 8, 0.96) 0%, rgba(142, 0, 8, 0.88) 48%, rgba(225, 6, 19, 0.82) 100%),
    repeating-linear-gradient(
      -28deg,
      rgba(255, 255, 255, 0.05) 0,
      rgba(255, 255, 255, 0.05) 1px,
      transparent 1px,
      transparent 12px
    );
}

.v4-season-card__cover-abbr {
  font-size: clamp(34px, 4vw, 48px);
  font-weight: 900;
  letter-spacing: 0.12em;
  line-height: 1;
}

.v4-season-card__cover-year {
  margin-top: 8px;
  font-size: 14px;
  font-weight: 800;
  letter-spacing: 0.16em;
  color: rgba(255, 255, 255, 0.72);
}

.v4-season-card__body {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 12px;
  padding: clamp(16px, 2vw, 22px);
}

.v4-season-card__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.v4-season-card__tag {
  display: inline-flex;
  align-items: center;
  min-height: 24px;
  padding: 0 10px;
  border: 1px solid rgba(25, 25, 25, 0.12);
  background: rgba(255, 255, 255, 0.72);
  color: #333;
  font-size: 12px;
  font-weight: 800;

  &--series {
    border-color: rgba(225, 6, 19, 0.24);
    background: rgba(225, 6, 19, 0.08);
    color: var(--cnpa-red, #e10613);
  }

  &.is-open {
    border-color: rgba(24, 180, 107, 0.28);
    background: rgba(24, 180, 107, 0.12);
    color: #128a52;
  }

  &.is-playing {
    border-color: rgba(225, 6, 19, 0.28);
    background: rgba(225, 6, 19, 0.12);
    color: var(--cnpa-red, #e10613);
  }

  &.is-finished {
    color: #666;
  }
}

.v4-season-card__title {
  margin: 0;
  color: var(--cnpa-paper-text, #171717);
  font-size: clamp(18px, 1.8vw, 22px);
  font-weight: 900;
  line-height: 1.25;
}

.v4-season-card__subtitle {
  margin: -4px 0 0;
  color: var(--cnpa-paper-text-secondary, #666);
  font-size: 13px;
}

.v4-season-card__summary,
.v4-season-card__times {
  display: grid;
  gap: 8px;
  margin: 0;
  font-size: 13px;
}

.v4-season-card__summary {
  grid-template-columns: repeat(2, minmax(0, 1fr));
  padding: 12px;
  border: 1px solid rgba(25, 25, 25, 0.08);
  background: rgba(255, 255, 255, 0.56);
}

.v4-season-card__summary dt,
.v4-season-card__times dt {
  margin-bottom: 2px;
  color: #888;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.06em;
}

.v4-season-card__summary dd,
.v4-season-card__times dd {
  margin: 0;
  color: var(--cnpa-paper-text, #171717);
  font-weight: 700;
  line-height: 1.45;
}

.v4-season-card__times {
  margin-top: auto;
}

.v4-season-card__action {
  align-self: flex-start;
  min-height: 42px;
  padding: 0 20px;
  border: 0;
  background: var(--cnpa-red, #e10613);
  color: #fff;
  font: inherit;
  font-size: 14px;
  font-weight: 800;
  cursor: pointer;
  clip-path: polygon(0 0, calc(100% - 10px) 0, 100% 10px, 100% 100%, 10px 100%, 0 calc(100% - 10px));
  transition: filter 0.18s ease;

  &:hover {
    filter: brightness(1.08);
  }
}

@media (max-width: 767px) {
  .v4-season-card__summary {
    grid-template-columns: 1fr;
  }
}
</style>
