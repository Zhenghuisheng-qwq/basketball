<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import PaperTear from '@/components/common/PaperTear.vue'
import DataHero from './components/DataHero.vue'
import DataTypeTabs from './components/DataTypeTabs.vue'
import DataStatisticsTable from './components/DataStatisticsTable.vue'
import DataPagination from './components/DataPagination.vue'
import {
  DEFAULT_TOURNAMENT_ID,
  playerStatisticsList,
  teamStatisticsList,
  type DataType,
  type StatisticType,
} from './mock/dataCenter'

const activeDataType = ref<DataType>('team')
const activeStatisticType = ref<StatisticType>('average')
const selectedTournamentId = ref(DEFAULT_TOURNAMENT_ID)
const currentPage = ref(1)
const pageSize = ref(10)

const sourceRows = computed(() =>
  activeDataType.value === 'team' ? teamStatisticsList : playerStatisticsList,
)

const total = computed(() => sourceRows.value.length)

const pagedRows = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return sourceRows.value.slice(start, start + pageSize.value)
})

function resetPage() {
  currentPage.value = 1
}

watch([activeDataType, activeStatisticType, selectedTournamentId], resetPage)
</script>

<template>
  <div class="data-center-page">
    <DataHero v-model:tournament-id="selectedTournamentId" />

    <section class="data-center-page__main">
      <div class="site-shell">
        <DataTypeTabs v-model="activeDataType" />

        <div class="data-center-panel">
          <div class="data-center-panel__tear-top" aria-hidden="true">
            <PaperTear position="top" tone="light" />
          </div>

          <div class="data-center-panel__shell">
            <div class="data-center-panel__body">
              <div class="data-center-panel__toolbar">
                <div class="data-stat-switch" role="group" aria-label="统计类型">
                  <button
                    type="button"
                    class="data-stat-switch__btn"
                    :class="{ 'is-active': activeStatisticType === 'average' }"
                    @click="activeStatisticType = 'average'"
                  >
                    场均
                  </button>
                  <button
                    type="button"
                    class="data-stat-switch__btn"
                    :class="{ 'is-active': activeStatisticType === 'total' }"
                    @click="activeStatisticType = 'total'"
                  >
                    总计
                  </button>
                </div>
              </div>

              <DataStatisticsTable
                :data-type="activeDataType"
                :statistic-type="activeStatisticType"
                :rows="pagedRows"
              />

              <DataPagination
                v-model:current-page="currentPage"
                :page-size="pageSize"
                :total="total"
              />
            </div>

            <div class="data-center-panel__edge-bottom" aria-hidden="true" />
          </div>
        </div>
      </div>
    </section>

    <div class="data-center-page__red-tear" aria-hidden="true">
      <svg
        class="data-center-page__red-tear-svg"
        viewBox="0 0 1440 14"
        preserveAspectRatio="none"
        focusable="false"
      >
        <path
          d="M0 8.2
             C18 3.1 34 11.4 52 6.8
             C71 1.8 88 10.6 109 5.4
             C128 0.8 146 9.9 167 4.6
             C189 -0.4 208 10.2 231 6.1
             C252 2.4 271 11.8 295 7.2
             C318 2.7 338 12.1 362 6.4
             C384 1.2 404 10.8 428 5.9
             C451 1.4 471 11.6 496 7.0
             C519 2.8 540 12.4 565 6.6
             C588 1.1 609 10.9 634 5.2
             C656 0.4 677 11.2 702 6.8
             C725 2.9 746 12.6 771 7.4
             C794 2.5 815 11.0 840 5.7
             C862 1.0 883 10.4 908 6.2
             C931 2.3 952 12.0 977 7.1
             C999 2.6 1020 11.5 1045 5.8
             C1067 0.9 1088 10.7 1113 6.5
             C1135 2.7 1156 12.2 1181 7.3
             C1203 2.9 1224 11.1 1249 5.6
             C1271 0.7 1292 10.5 1317 6.3
             C1338 2.4 1358 11.8 1382 7.0
             C1401 3.2 1418 10.2 1440 6.1
             L1440 14 L0 14 Z"
        />
      </svg>
    </div>
  </div>
</template>

<style scoped lang="scss">
.data-center-page {
  width: 100%;
  background: #050505;
  overflow-x: clip;
  color: #fff;
}

.data-center-page__main {
  width: 100%;
  padding: 8px 0 0;
  background: #050505;
}

.data-center-panel {
  --paper-tear-color: var(--cnpa-paper-bg);
  position: relative;
  z-index: 2;
  width: 100%;
  color: var(--cnpa-paper-text);
  overflow: visible;
}

.data-center-panel__tear-top {
  position: relative;
  z-index: 4;
  height: 0;
  pointer-events: none;

  :deep(.paper-tear) {
    height: 0;
  }

  :deep(.torn-edge-top) {
    top: 0;
    transform: translateY(-48%);
  }

  :deep(.torn-edge-main) {
    fill: var(--cnpa-paper-bg) !important;
  }

  :deep(.torn-edge-shadow) {
    display: none;
  }
}

.data-center-panel__shell {
  position: relative;
  z-index: 2;
  width: 100%;
  background-color: var(--cnpa-paper-bg);
  background-image: linear-gradient(
    180deg,
    #ebe9e9 0%,
    var(--cnpa-paper-bg) 45%,
    #e7e5e5 100%
  );
  border-radius: 2px 2px 0 0;
  box-shadow: 0 10px 28px rgba(0, 0, 0, 0.28);
  overflow: hidden;
}

.data-center-panel__body {
  position: relative;
  z-index: 1;
  padding: 20px clamp(14px, 1.8vw, 24px) 0;
  box-sizing: border-box;
  background: transparent;
}

.data-center-panel__toolbar {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  margin-bottom: 18px;
}

.data-stat-switch {
  display: inline-flex;
  align-items: center;
}

.data-stat-switch__btn {
  min-width: 86px;
  height: 45px;
  margin: 0;
  padding: 0 18px;
  border: 0;
  border-radius: 4px;
  background: var(--cnpa-paper-surface);
  color: var(--cnpa-paper-text-secondary);
  font-size: 15px;
  font-weight: 700;
  line-height: 1;
  cursor: pointer;
  transition:
    background-color 0.14s ease,
    color 0.14s ease,
    box-shadow 0.14s ease;

  & + & {
    margin-left: 8px;
  }

  &.is-active {
    background: #ff1717;
    color: #fff;
    box-shadow: 0 2px 6px rgba(255, 23, 23, 0.18);
  }
}

.data-center-panel__edge-bottom {
  position: relative;
  z-index: 3;
  width: 100%;
  height: 11px;
  margin-top: -1px;
  background: var(--cnpa-paper-bg);
  pointer-events: none;
  clip-path: polygon(
    0 0,
    100% 0,
    100% 42%,
    98.6% 78%,
    96.8% 24%,
    94.1% 86%,
    91.7% 31%,
    88.2% 69%,
    85.9% 18%,
    82.4% 81%,
    78.6% 36%,
    75.1% 72%,
    71.3% 22%,
    67.8% 88%,
    63.4% 29%,
    59.7% 76%,
    55.2% 15%,
    51.6% 83%,
    47.1% 33%,
    43.4% 70%,
    38.9% 21%,
    34.6% 86%,
    30.2% 27%,
    26.5% 74%,
    21.8% 19%,
    17.9% 81%,
    13.4% 35%,
    9.6% 68%,
    5.8% 24%,
    2.7% 79%,
    0 46%
  );
}

.data-center-page__red-tear {
  position: relative;
  z-index: 3;
  width: 100%;
  height: 12px;
  margin-top: 36px;
  line-height: 0;
  overflow: hidden;
  background: transparent;
}

.data-center-page__red-tear-svg {
  display: block;
  width: 100%;
  height: 12px;

  path {
    fill: #e10613;
  }
}

@media (max-width: 1199px) {
  .data-center-panel__body {
    padding-inline: 12px;
  }
}

@media (max-width: 767px) {
  .data-center-page__main {
    padding-top: 4px;
  }

  .data-stat-switch__btn {
    min-width: 76px;
    height: 40px;
    font-size: 14px;
  }

  .data-center-page__red-tear {
    height: 10px;
    margin-top: 28px;
  }

  .data-center-page__red-tear-svg {
    height: 10px;
  }
}
</style>
