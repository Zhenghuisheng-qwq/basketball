<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import PaperTear from '@/components/common/PaperTear.vue'
import {
  getPublicAnnouncementCategories,
  getPublicAnnouncementPage,
} from '@/api/publicAnnouncement'
import type { PublicAnnouncementItemResp } from '@/api/types/public'
import {
  buildAnnouncementListQuery,
  formatAnnouncementDate,
  goToAnnouncementDetail,
  normalizeAnnouncementCategory,
  parseAnnouncementKeyword,
  parseAnnouncementPage,
} from '@/utils/announcementNavigate'
import { displayText, toNumber } from '@/utils/publicDisplay'
import AnnouncementHero from './components/AnnouncementHero.vue'
import AnnouncementCategoryTabs from './components/AnnouncementCategoryTabs.vue'
import AnnouncementSearch from './components/AnnouncementSearch.vue'
import AnnouncementList from './components/AnnouncementList.vue'
import AnnouncementPagination from './components/AnnouncementPagination.vue'
import type { AnnouncementCategory, AnnouncementListItem } from './types'

const route = useRoute()
const router = useRouter()
const listHeadRef = ref<HTMLElement | null>(null)

const categories = ref<AnnouncementCategory[]>([])
const items = ref<AnnouncementListItem[]>([])
const total = ref(0)
const totalPages = ref(1)
const listState = ref<'loading' | 'success' | 'empty' | 'error'>('loading')
const listError = ref('')
let listReq = 0
let categoryReq = 0

const allowedCodes = computed(() => categories.value.map((item) => item.code))
const category = computed(() =>
  normalizeAnnouncementCategory(route.query.category, allowedCodes.value),
)
const keyword = computed(() => parseAnnouncementKeyword(route.query.keyword))
const page = computed(() => parseAnnouncementPage(route.query.page, 1))

const emptyText = computed(() => {
  if (listState.value === 'error') return listError.value || '加载公告失败'
  if (keyword.value.trim()) {
    return `未找到与“${keyword.value.trim()}”相关的公告`
  }
  if (category.value !== 'ALL') {
    return '当前分类暂无公告'
  }
  return '暂无公告'
})

function mapItem(item: PublicAnnouncementItemResp): AnnouncementListItem {
  return {
    id: String(item.announcementId),
    title: displayText(item.title, '未命名公告'),
    categoryCode: displayText(item.categoryCode, 'OTHER'),
    categoryName: displayText(item.categoryName, '其他'),
    pinned: toNumber(item.isTop, 0) === 1,
    summary: displayText(item.summary, ''),
    authorName: displayText(item.authorName, 'CNPA'),
    publishTime: item.publishTime || '',
    publishDateText: formatAnnouncementDate(item.publishTime),
    viewCount: toNumber(item.viewCount, 0),
  }
}

async function loadCategories() {
  const current = ++categoryReq
  try {
    const res = await getPublicAnnouncementCategories()
    if (current !== categoryReq) return
    categories.value = (res.data ?? []).map((item) => ({
      code: String(item.categoryCode || '').toUpperCase(),
      name: displayText(item.categoryName, item.categoryCode),
    }))
  } catch {
    if (current !== categoryReq) return
    categories.value = []
  }
}

async function loadList() {
  const current = ++listReq
  listState.value = 'loading'
  listError.value = ''
  try {
    const res = await getPublicAnnouncementPage({
      currentPage: page.value,
      pageSize: 10,
      categoryCode: category.value === 'ALL' ? undefined : category.value,
      keyword: keyword.value || undefined,
      sortField: 'publishTime',
      sortOrder: 'desc',
    })
    if (current !== listReq) return
    const data = res.data
    items.value = (data?.records ?? []).map(mapItem)
    total.value = Number(data?.total || 0)
    totalPages.value = Math.max(1, Number(data?.pages || 1))
    listState.value = items.value.length ? 'success' : 'empty'
    if (page.value > totalPages.value) {
      replaceQuery({ page: totalPages.value })
    }
  } catch (err) {
    if (current !== listReq) return
    items.value = []
    total.value = 0
    totalPages.value = 1
    listState.value = 'error'
    listError.value = err instanceof Error ? err.message : '加载公告失败'
  }
}

function replaceQuery(next: Partial<{ category: string; keyword: string; page: number }>) {
  router.replace({
    path: '/announcements',
    query: buildAnnouncementListQuery({
      category: next.category ?? category.value,
      keyword: next.keyword ?? keyword.value,
      page: next.page ?? page.value,
    }),
  })
}

function handleCategoryChange(value: string) {
  replaceQuery({ category: value, page: 1 })
}

function handleSearch(value: string) {
  replaceQuery({ keyword: value, page: 1 })
}

function handlePageChange(nextPage: number) {
  replaceQuery({ page: nextPage })
  listHeadRef.value?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

function openDetail(id: string) {
  goToAnnouncementDetail(router, id, {
    category: category.value,
    keyword: keyword.value,
    page: page.value,
  })
}

watch(
  () => true,
  () => {
    void loadCategories()
  },
  { immediate: true },
)

watch(
  () => [route.query.category, route.query.keyword, route.query.page] as const,
  () => {
    void loadList()
  },
  { immediate: true },
)
</script>

<template>
  <div class="announcement-list-page">
    <AnnouncementHero variant="list" />

    <section class="announcement-list-page__content">
      <div class="site-shell">
        <div class="cnpa-paper-section cnpa-paper-section--light announcement-list-page__paper">
          <PaperTear position="top" tone="light" />
          <div class="cnpa-paper-section__content announcement-list-page__inner">
            <header ref="listHeadRef" class="announcement-list-page__heading">
              <div class="announcement-list-page__title-block">
                <h2 class="announcement-list-page__title">官方公告</h2>
                <p class="announcement-list-page__subtitle">OFFICIAL NOTICES</p>
                <p class="announcement-list-page__desc">全站公开公告发布时间持续更新</p>
              </div>
              <p class="announcement-list-page__count">共 {{ total }} 条公告</p>
            </header>

            <div class="announcement-list-page__toolbar">
              <AnnouncementCategoryTabs
                :model-value="category"
                :categories="categories"
                @update:model-value="handleCategoryChange"
              />
              <AnnouncementSearch
                :model-value="keyword"
                @search="handleSearch"
              />
            </div>

            <div v-if="listState === 'loading'" class="announcement-list-page__state">加载中...</div>
            <AnnouncementList
              v-else
              :items="items"
              :empty-text="emptyText"
              @select="openDetail"
            />

            <AnnouncementPagination
              v-if="listState === 'success'"
              :page="page"
              :total-pages="totalPages"
              @update:page="handlePageChange"
            />
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped lang="scss">
.announcement-list-page {
  width: 100%;
  background: #000;
  overflow-x: clip;
}

.announcement-list-page__content {
  width: 100%;
}

.announcement-list-page__paper {
  position: relative;
  width: 100%;
}

.announcement-list-page__inner {
  padding: 38px 32px 42px;
  box-sizing: border-box;
}

.announcement-list-page__heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 32px;
}

.announcement-list-page__title-block {
  min-width: 0;
}

.announcement-list-page__title {
  margin: 0;
  color: #171717;
  font-size: clamp(30px, 3vw, 38px);
  font-weight: 900;
  line-height: 1.15;
}

.announcement-list-page__subtitle {
  margin: 6px 0 0;
  color: var(--cnpa-red, #e10613);
  font-size: 14px;
  font-style: italic;
  font-weight: 700;
  letter-spacing: 0.12em;
}

.announcement-list-page__desc {
  margin: 10px 0 0;
  color: var(--cnpa-paper-text-secondary, #666);
  font-size: 14px;
}

.announcement-list-page__count {
  margin: 8px 0 0;
  color: #171717;
  font-size: 14px;
  font-weight: 700;
  white-space: nowrap;
  flex-shrink: 0;
}

.announcement-list-page__toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 32px;
  margin-top: 26px;
  margin-bottom: 16px;
}

.announcement-list-page__state {
  padding: 48px 0;
  text-align: center;
  color: #666;
  font-size: 14px;
  font-weight: 600;
}

.announcement-list-page__paper :deep(.paper-tear) {
  display: none; /* 临时隐藏撕纸，勿删 */
  height: 0;
  overflow: visible;
  z-index: 6;
}

.announcement-list-page__paper :deep(.torn-edge-top) {
  transform: translateY(-48%);
}

.announcement-list-page__paper :deep(.torn-edge-main) {
  fill: var(--cnpa-paper-bg) !important;
}

.announcement-list-page__paper :deep(.torn-edge-shadow) {
  display: none;
}

@media (max-width: 1023px) {
  .announcement-list-page__toolbar {
    flex-wrap: wrap;
    gap: 16px;
  }
}

@media (max-width: 767px) {
  .announcement-list-page__inner {
    padding: 28px 16px 36px;
  }

  .announcement-list-page__heading {
    flex-direction: column;
    gap: 12px;
  }

  .announcement-list-page__toolbar {
    flex-direction: column;
    align-items: stretch;
    gap: 14px;
  }
}
</style>
