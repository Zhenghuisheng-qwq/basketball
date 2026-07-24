<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import TeamLogo from '@/components/team/TeamLogo.vue'
import HallPanelHead from './HallPanelHead.vue'
import HallSelect from './HallSelect.vue'
import HallPagination from './HallPagination.vue'
import { getPublicHallTournamentHonorPage } from '@/api/publicHallOfFame'
import type { PublicHonorItemResp } from '@/api/types/public'
import { displayText, formatDateTime } from '@/utils/publicDisplay'

const props = defineProps<{
  year: string
  page: number
}>()

const emit = defineEmits<{
  'update:year': [value: string]
  'update:page': [value: number]
}>()

const router = useRouter()
const panelRef = ref<HTMLElement | null>(null)
const pageSize = 8

const allChampions = ref<PublicHonorItemResp[]>([])
const state = ref<'loading' | 'success' | 'empty' | 'error'>('loading')
const errorMessage = ref('')
let reqId = 0

const yearOptions = computed(() => {
  const years = new Set<string>()
  allChampions.value.forEach((item) => {
    const year = String(item.awardDate || '').slice(0, 4)
    if (/^\d{4}$/.test(year)) years.add(year)
  })
  const sorted = Array.from(years).sort((a, b) => Number(b) - Number(a))
  return [
    { value: 'ALL', label: '全部年份' },
    ...sorted.map((year) => ({ value: year, label: `${year} 年` })),
  ]
})

const filtered = computed(() => {
  if (props.year === 'ALL') return allChampions.value
  return allChampions.value.filter((item) => String(item.awardDate || '').startsWith(props.year))
})

const totalPages = computed(() => Math.max(1, Math.ceil(filtered.value.length / pageSize)))

const pageItems = computed(() => {
  const start = (props.page - 1) * pageSize
  return filtered.value.slice(start, start + pageSize)
})

const groupedByYear = computed(() => {
  const map = new Map<number, PublicHonorItemResp[]>()
  pageItems.value.forEach((item) => {
    const year = Number(String(item.awardDate || '').slice(0, 4)) || 0
    const list = map.get(year) || []
    list.push(item)
    map.set(year, list)
  })
  return Array.from(map.entries())
    .sort((a, b) => b[0] - a[0])
    .map(([year, items]) => ({ year, items }))
})

async function loadAll() {
  const current = ++reqId
  state.value = 'loading'
  errorMessage.value = ''
  try {
    const collected: PublicHonorItemResp[] = []
    let page = 1
    let pages = 1
    do {
      const res = await getPublicHallTournamentHonorPage({
        currentPage: page,
        pageSize: 100,
        sortField: 'awardDate',
        sortOrder: 'desc',
      })
      if (current !== reqId) return
      const data = res.data
      const records = data?.records ?? []
      collected.push(...records)
      pages = Math.max(1, Number(data?.pages || 1))
      page += 1
    } while (page <= pages && page <= 10)

    allChampions.value = collected.filter(
      (item) => (item.honorCode || '').toUpperCase() === 'CHAMPION',
    )
    state.value = allChampions.value.length ? 'success' : 'empty'
  } catch (err) {
    if (current !== reqId) return
    allChampions.value = []
    state.value = 'error'
    errorMessage.value = err instanceof Error ? err.message : '加载历届冠军失败'
  }
}

watch(
  () => true,
  () => {
    void loadAll()
  },
  { immediate: true },
)

watch(
  () => [props.year, props.page, filtered.value.length, totalPages.value] as const,
  () => {
    if (props.page > totalPages.value) {
      emit('update:page', totalPages.value)
    }
  },
)

function updateYear(value: string | number) {
  emit('update:year', String(value))
  emit('update:page', 1)
}

function updatePage(page: number) {
  emit('update:page', page)
  panelRef.value?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

function openTeam(teamId?: string | null) {
  if (!teamId) return
  router.push(`/tournament/team/${teamId}/overview`)
}

function openEventHonor(tournamentId?: string | null, showRoster = false) {
  if (!tournamentId) return
  router.replace({
    path: '/hall-of-fame',
    query: {
      tab: 'event',
      tournamentId,
      ...(showRoster ? { showRoster: '1' } : {}),
    },
  })
}

function awardDateText(value?: string | null) {
  const dt = formatDateTime(value)
  return dt.date === '--' ? '--' : dt.date
}
</script>

<template>
  <section ref="panelRef" class="past-champions-panel">
    <HallPanelHead title="历届冠军" subtitle="Past Champions">
      <template #filters>
        <HallSelect
          width="filter"
          :model-value="year"
          :options="yearOptions"
          @update:model-value="updateYear"
        />
      </template>
    </HallPanelHead>

    <div v-if="state === 'loading'" class="hall-empty">加载中...</div>
    <div v-else-if="state === 'error'" class="hall-empty">{{ errorMessage }}</div>
    <div v-else-if="state === 'empty' || filtered.length === 0" class="hall-empty">
      当前筛选条件下暂无冠军记录
    </div>

    <template v-else>
      <div v-for="group in groupedByYear" :key="group.year" class="past-champions-group">
        <h3 class="past-champions-group__year">{{ group.year || '未知年份' }}</h3>
        <div class="past-champions-grid">
          <article
            v-for="record in group.items"
            :key="record.honorId"
            class="past-champion-card hall-card-surface"
          >
            <div class="past-champion-card__main">
              <div class="past-champion-card__left">
                <div class="past-champion-card__meta">
                  <span class="past-champion-card__series">
                    {{ displayText(record.honorName, '冠军') }}
                  </span>
                </div>
                <p class="past-champion-card__name">
                  {{ displayText(record.tournamentName, '公开赛事') }}
                </p>
                <p class="past-champion-card__date">{{ awardDateText(record.awardDate) }}</p>
              </div>

              <button
                type="button"
                class="past-champion-card__team"
                :disabled="!record.teamId"
                @click="openTeam(record.teamId)"
              >
                <TeamLogo
                  :team="{
                    id: String(record.teamId || ''),
                    name: displayText(record.teamName),
                    shortName: displayText(record.teamName),
                    logoUrl: record.teamLogoUrl || '',
                  }"
                  size="xl"
                />
                <strong>{{ displayText(record.teamName, '冠军球队') }}</strong>
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
        :page="page"
        :total-pages="totalPages"
        @update:page="updatePage"
      />
    </template>
  </section>
</template>

<style scoped lang="scss">
.past-champions-panel {
  width: 100%;
}

.past-champions-group + .past-champions-group {
  margin-top: 28px;
}

.past-champions-group__year {
  margin: 0 0 14px;
  color: #111;
  font-size: 22px;
  font-weight: 900;
}

.past-champions-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.past-champion-card {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 20px 22px;
}

.past-champion-card__main {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.past-champion-card__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.past-champion-card__series {
  color: var(--cnpa-red, #e10613);
  font-size: 12px;
  font-weight: 800;
}

.past-champion-card__name {
  margin: 10px 0 0;
  color: #111;
  font-size: 18px;
  font-weight: 900;
}

.past-champion-card__date {
  margin: 8px 0 0;
  color: #666;
  font-size: 13px;
}

.past-champion-card__team {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  min-width: 110px;
  padding: 0;
  border: 0;
  background: transparent;
  color: #111;
  font: inherit;
  cursor: pointer;

  &:disabled {
    cursor: default;
  }

  strong {
    font-size: 14px;
    font-weight: 800;
    text-align: center;
  }
}

.past-champion-card__actions {
  display: flex;
  align-items: center;
  gap: 12px;
  border-top: 1px solid rgba(0, 0, 0, 0.08);
  padding-top: 12px;

  button {
    padding: 0;
    border: 0;
    background: transparent;
    color: #111;
    font: inherit;
    font-size: 13px;
    font-weight: 800;
    cursor: pointer;

    &:hover {
      color: var(--cnpa-red, #e10613);
    }
  }
}

.past-champion-card__divider {
  width: 1px;
  height: 14px;
  background: rgba(0, 0, 0, 0.12);
}

@media (max-width: 1023px) {
  .past-champions-grid {
    grid-template-columns: 1fr;
  }
}
</style>
