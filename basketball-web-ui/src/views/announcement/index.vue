<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import PaperTear from '@/components/common/PaperTear.vue'
import {
  buildAnnouncementListQuery,
  goToAnnouncementDetail,
  normalizeAnnouncementCategory,
  parseAnnouncementKeyword,
  parseAnnouncementPage,
} from '@/utils/announcementNavigate'
import AnnouncementHero from './components/AnnouncementHero.vue'
import AnnouncementCategoryTabs from './components/AnnouncementCategoryTabs.vue'
import AnnouncementSearch from './components/AnnouncementSearch.vue'
import AnnouncementList from './components/AnnouncementList.vue'
import AnnouncementPagination from './components/AnnouncementPagination.vue'
import { queryAnnouncements } from './mock/announcements'
import type { AnnouncementFilterCategory } from './types'

const route = useRoute()
const router = useRouter()
const listHeadRef = ref<HTMLElement | null>(null)

const category = computed(() => normalizeAnnouncementCategory(route.query.category) as AnnouncementFilterCategory)
const keyword = computed(() => parseAnnouncementKeyword(route.query.keyword))
const page = computed(() => parseAnnouncementPage(route.query.page, 1))

const listResult = computed(() =>
  queryAnnouncements({
    category: category.value,
    keyword: keyword.value,
    page: page.value,
    pageSize: 10,
  }),
)

const emptyText = computed(() => {
  if (keyword.value.trim()) {
    return `未找到与“${keyword.value.trim()}”相关的公告`
  }
  if (category.value !== 'ALL') {
    return '当前分类暂无公告'
  }
  return '暂无公告'
})

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

function handleCategoryChange(value: AnnouncementFilterCategory) {
  replaceQuery({ category: value, page: 1 })
}

function handleSearch(value: string) {
  replaceQuery({ keyword: value, page: 1 })
}

function handlePageChange(nextPage: number) {
  replaceQuery({ page: nextPage })
  listHeadRef.value?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

function openDetail(id: number) {
  goToAnnouncementDetail(router, id, {
    category: category.value,
    keyword: keyword.value,
    page: page.value,
  })
}

watch(
  () => [route.query.category, route.query.keyword, route.query.page] as const,
  () => {
    const result = listResult.value
    if (page.value > result.totalPages) {
      replaceQuery({ page: result.totalPages })
    }
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
              <p class="announcement-list-page__count">共 {{ listResult.total }} 条公告</p>
            </header>

            <div class="announcement-list-page__toolbar">
              <AnnouncementCategoryTabs
                :model-value="category"
                @update:model-value="handleCategoryChange"
              />
              <AnnouncementSearch
                :model-value="keyword"
                @search="handleSearch"
              />
            </div>

            <AnnouncementList
              :items="listResult.items"
              :empty-text="emptyText"
              @select="openDetail"
            />

            <AnnouncementPagination
              :page="listResult.page"
              :total-pages="listResult.totalPages"
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

.announcement-list-page__paper :deep(.paper-tear) {
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
