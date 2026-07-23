<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import TeamLogo from '@/components/team/TeamLogo.vue'
import HallPanelHead from './HallPanelHead.vue'
import HallSelect from './HallSelect.vue'
import HallPagination from './HallPagination.vue'
import { getChampionFilterOptions, queryChampionRecords } from '../mock/hallOfFame'

const props = defineProps<{
  year: string
  platform: string
  series: string
  page: number
}>()

const emit = defineEmits<{
  'update:year': [value: string]
  'update:platform': [value: string]
  'update:series': [value: string]
  'update:page': [value: number]
}>()

const router = useRouter()
const panelRef = ref<HTMLElement | null>(null)

const filterOptions = computed(() => getChampionFilterOptions())
const pageResult = computed(() =>
  queryChampionRecords(props.year, props.platform, props.series, props.page, 8),
)

function updateYear(value: string | number) {
  emit('update:year', String(value))
  emit('update:page', 1)
}

function updatePlatform(value: string | number) {
  emit('update:platform', String(value))
  emit('update:page', 1)
}

function updateSeries(value: string | number) {
  emit('update:series', String(value))
  emit('update:page', 1)
}

function updatePage(page: number) {
  emit('update:page', page)
  panelRef.value?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

function openTeam(teamId: string) {
  router.push(`/tournament/team/${teamId}/overview`)
}

function openEventHonor(tournamentId: string, showRoster = false) {
  router.replace({
    path: '/hall-of-fame',
    query: {
      tab: 'event',
      tournamentId,
      ...(showRoster ? { showRoster: '1' } : {}),
    },
  })
}
</script>

<template>
  <section ref="panelRef" class="past-champions-panel">
    <HallPanelHead title="历届冠军" subtitle="Past Champions">
      <template #filters>
        <HallSelect
          width="filter"
          :model-value="year"
          :options="filterOptions.years"
          @update:model-value="updateYear"
        />
        <HallSelect
          width="filter"
          :model-value="platform"
          :options="filterOptions.platforms"
          @update:model-value="updatePlatform"
        />
        <HallSelect
          width="filter"
          :model-value="series"
          :options="filterOptions.series"
          @update:model-value="updateSeries"
        />
      </template>
    </HallPanelHead>

    <div v-if="pageResult.total === 0" class="hall-empty">
      当前筛选条件下暂无冠军记录
    </div>

    <template v-else>
      <div v-for="group in pageResult.groupedByYear" :key="group.year" class="past-champions-group">
        <h3 class="past-champions-group__year">{{ group.year }}</h3>
        <div class="past-champions-grid">
          <article v-for="record in group.items" :key="record.id" class="past-champion-card hall-card-surface">
            <div class="past-champion-card__main">
              <div class="past-champion-card__left">
                <div class="past-champion-card__meta">
                  <span class="past-champion-card__series">{{ record.seriesName }}</span>
                  <span class="past-champion-card__platform">{{ record.platformName }}</span>
                </div>
                <p class="past-champion-card__name">{{ record.tournamentName }}</p>
                <p class="past-champion-card__date">{{ record.championDate }}</p>
              </div>

              <button type="button" class="past-champion-card__team" @click="openTeam(record.teamId)">
                <TeamLogo
                  :team="{
                    id: record.teamId,
                    name: record.teamName,
                    shortName: record.teamAbbr,
                    logoUrl: record.teamLogoUrl,
                  }"
                  size="xl"
                />
                <strong>{{ record.teamName }}</strong>
              </button>
            </div>

            <div class="past-champion-card__actions">
              <button type="button" @click="openEventHonor(record.tournamentId, true)">
                查看冠军阵容
                <span aria-hidden="true">→</span>
              </button>
              <span class="past-champion-card__divider" aria-hidden="true" />
              <button type="button" @click="openEventHonor(record.tournamentId, false)">
                进入赛事荣誉
                <span aria-hidden="true">→</span>
              </button>
            </div>
          </article>
        </div>
      </div>

      <HallPagination
        :page="pageResult.page"
        :total-pages="pageResult.totalPages"
        @update:page="updatePage"
      />
    </template>
  </section>
</template>

<style scoped lang="scss">
@use '../styles/hall-shared.scss';

.past-champions-panel {
  width: 100%;
  scroll-margin-top: calc(var(--site-header-height, 72px) + 16px);
}

.past-champions-group + .past-champions-group {
  margin-top: 32px;
}

.past-champions-group__year {
  margin: 0 0 14px;
  padding-bottom: 8px;
  border-bottom: 2px solid var(--hall-red, #e71922);
  color: var(--hall-red, #e71922);
  font-size: clamp(38px, 3.2vw, 44px);
  font-weight: 900;
  line-height: 1;
}

.past-champions-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.past-champion-card {
  display: grid;
  grid-template-rows: 1fr 48px;
  min-height: 245px;
  transition: border-color 0.18s ease;

  &:hover {
    border-color: rgba(231, 25, 34, 0.55);
  }
}

.past-champion-card__main {
  display: grid;
  grid-template-columns: 46% 54%;
  gap: 16px;
  min-height: 0;
  padding: 22px 22px 16px;
}

.past-champion-card__left {
  display: flex;
  flex-direction: column;
  justify-content: center;
  min-width: 0;
}

.past-champion-card__meta {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.past-champion-card__series {
  color: rgba(255, 255, 255, 0.78);
  font-size: 13px;
  font-weight: 700;
}

.past-champion-card__platform {
  padding: 2px 8px;
  border: 1px solid rgba(231, 25, 34, 0.65);
  color: var(--hall-red, #e71922);
  font-size: 11px;
  font-weight: 700;
}

.past-champion-card__name {
  margin: 12px 0 0;
  color: rgba(255, 255, 255, 0.72);
  font-size: 14px;
  line-height: 1.5;
}

.past-champion-card__date {
  margin: 12px 0 0;
  color: var(--hall-gold, #d2a648);
  font-size: 13px;
  font-weight: 700;
}

.past-champion-card__team {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin: 0;
  padding: 0;
  border: 0;
  background: transparent;
  color: #fff;
  cursor: pointer;
  text-align: center;

  :deep(.team-logo--xl) {
    width: 110px;
    height: 110px;
  }

  strong {
    font-size: 30px;
    font-weight: 900;
    line-height: 1.15;
  }
}

.past-champion-card__actions {
  display: flex;
  align-items: stretch;
  border-top: 1px solid rgba(255, 255, 255, 0.1);

  button {
    flex: 1;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    gap: 6px;
    min-height: 48px;
    padding: 0 12px;
    border: 0;
    background: rgba(255, 255, 255, 0.03);
    color: rgba(255, 255, 255, 0.88);
    font: inherit;
    font-size: 13px;
    font-weight: 700;
    cursor: pointer;
    transition: background 0.18s ease, color 0.18s ease;

    span {
      transition: transform 0.18s ease;
    }

    &:hover {
      background: rgba(74, 9, 13, 0.88);
      color: #fff;

      span {
        transform: translateX(3px);
      }
    }
  }
}

.past-champion-card__divider {
  width: 1px;
  background: rgba(255, 255, 255, 0.12);
  flex-shrink: 0;
}

@media (max-width: 1199px) {
  .past-champions-grid {
    grid-template-columns: 1fr;
  }

  .past-champion-card__main {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 767px) {
  .past-champion-card__main {
    grid-template-columns: 1fr;
  }

  .past-champion-card__team {
    padding-top: 8px;
  }

  .past-champion-card__actions {
    flex-direction: column;
  }

  .past-champion-card__divider {
    width: 100%;
    height: 1px;
  }
}
</style>
