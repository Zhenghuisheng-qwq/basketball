<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import PaperTear from '@/components/common/PaperTear.vue'
import {
  getPublicAnnouncementDetail,
  getPublicAnnouncementPreviousNext,
} from '@/api/publicAnnouncement'
import type { PublicAnnouncementItemResp } from '@/api/types/public'
import {
  buildAnnouncementDetailFromQuery,
  formatAnnouncementDate,
  goToAnnouncementDetail,
  goToAnnouncementList,
} from '@/utils/announcementNavigate'
import { displayText } from '@/utils/publicDisplay'
import AnnouncementHero from './components/AnnouncementHero.vue'
import AnnouncementContentRenderer from './components/AnnouncementContentRenderer.vue'
import type { AnnouncementDetail } from './types'

const route = useRoute()
const router = useRouter()

const announcement = ref<AnnouncementDetail | null>(null)
const previous = ref<PublicAnnouncementItemResp | null>(null)
const next = ref<PublicAnnouncementItemResp | null>(null)
const state = ref<'loading' | 'success' | 'empty' | 'error'>('loading')
const errorMessage = ref('')
let reqId = 0

const announcementId = computed(() => String(route.params.announcementId || ''))
const listState = computed(() => buildAnnouncementDetailFromQuery(route.query))

async function loadDetail(id: string) {
  const current = ++reqId
  if (!id) {
    announcement.value = null
    state.value = 'empty'
    return
  }
  state.value = 'loading'
  errorMessage.value = ''
  previous.value = null
  next.value = null
  try {
    const [detailRes, adjacentRes] = await Promise.all([
      getPublicAnnouncementDetail(id),
      getPublicAnnouncementPreviousNext(id).catch(() => null),
    ])
    if (current !== reqId) return
    const data = detailRes.data
    if (!data) {
      announcement.value = null
      state.value = 'empty'
      return
    }
    announcement.value = {
      id: String(data.announcementId),
      title: displayText(data.title, '未命名公告'),
      categoryCode: displayText(data.categoryCode, 'OTHER'),
      categoryName: displayText(data.categoryName, '其他'),
      pinned: Number(data.isTop || 0) === 1,
      summary: displayText(data.summary, ''),
      authorName: displayText(data.authorName, 'CNPA'),
      publishTime: data.publishTime || '',
      publishDateText: formatAnnouncementDate(data.publishTime),
      viewCount: Number(data.viewCount || 0),
      contentText: data.contentText ?? '',
    }
    previous.value = adjacentRes?.data?.previous ?? null
    next.value = adjacentRes?.data?.next ?? null
    state.value = 'success'
    document.title = `${announcement.value.title} - CNPA 篮球赛事平台`
  } catch (err) {
    if (current !== reqId) return
    announcement.value = null
    state.value = 'error'
    errorMessage.value = err instanceof Error ? err.message : '加载公告详情失败'
  }
}

function backToList() {
  goToAnnouncementList(router, {
    category: listState.value.fromCategory,
    keyword: listState.value.fromKeyword,
    page: listState.value.fromPage,
  })
}

function openAdjacent(item?: PublicAnnouncementItemResp | null) {
  if (!item?.announcementId) return
  goToAnnouncementDetail(router, item.announcementId, {
    category: listState.value.fromCategory,
    keyword: listState.value.fromKeyword,
    page: listState.value.fromPage,
  })
}

watch(
  announcementId,
  (id) => {
    void loadDetail(id)
  },
  { immediate: true },
)
</script>

<template>
  <div class="announcement-detail-page">
    <AnnouncementHero variant="detail" />

    <section class="announcement-detail-page__content">
      <div class="site-shell">
        <div class="cnpa-paper-section cnpa-paper-section--light announcement-detail-page__paper">
          <PaperTear position="top" tone="light" />
          <div class="cnpa-paper-section__content">
            <article class="announcement-detail-page__article">
              <div v-if="state === 'loading'" class="announcement-detail-page__not-found">
                <p>加载中...</p>
              </div>

              <div v-else-if="state === 'error'" class="announcement-detail-page__not-found">
                <h1>加载失败</h1>
                <p>{{ errorMessage }}</p>
                <button type="button" class="announcement-detail-page__back" @click="backToList">
                  返回公告中心
                </button>
              </div>

              <template v-else-if="announcement">
                <nav class="announcement-detail-page__breadcrumb" aria-label="面包屑">
                  <router-link to="/home">首页</router-link>
                  <span>/</span>
                  <button type="button" class="announcement-detail-page__crumb-btn" @click="backToList">
                    公告中心
                  </button>
                  <span>/</span>
                  <span>{{ announcement.categoryName }}</span>
                </nav>

                <div class="announcement-detail-page__tags">
                  <span v-if="announcement.pinned" class="announcement-detail-page__pin">置顶</span>
                  <span
                    class="announcement-detail-page__category"
                    :class="`announcement-detail-page__category--${announcement.categoryCode.toLowerCase()}`"
                  >
                    {{ announcement.categoryName }}
                  </span>
                </div>

                <header class="announcement-detail-page__header">
                  <h1 class="announcement-detail-page__title">{{ announcement.title }}</h1>
                </header>

                <div class="announcement-detail-page__meta">
                  <div class="announcement-detail-page__meta-item">
                    <span class="announcement-detail-page__meta-label">发布人</span>
                    <span class="announcement-detail-page__meta-value">{{ announcement.authorName }}</span>
                  </div>
                  <div class="announcement-detail-page__meta-item">
                    <span class="announcement-detail-page__meta-label">发布时间</span>
                    <time class="announcement-detail-page__meta-value">{{ announcement.publishDateText }}</time>
                  </div>
                </div>

                <section class="announcement-detail-page__body">
                  <AnnouncementContentRenderer :text="announcement.contentText" />
                </section>

                <nav class="announcement-detail-page__adjacent" aria-label="上一篇下一篇">
                  <button
                    type="button"
                    class="announcement-detail-page__adjacent-btn"
                    :disabled="!previous"
                    @click="openAdjacent(previous)"
                  >
                    <span>上一篇</span>
                    <strong>{{ previous ? displayText(previous.title) : '没有更早的公告' }}</strong>
                  </button>
                  <button
                    type="button"
                    class="announcement-detail-page__adjacent-btn announcement-detail-page__adjacent-btn--next"
                    :disabled="!next"
                    @click="openAdjacent(next)"
                  >
                    <span>下一篇</span>
                    <strong>{{ next ? displayText(next.title) : '没有更新的公告' }}</strong>
                  </button>
                </nav>

                <footer class="announcement-detail-page__footer">
                  <button type="button" class="announcement-detail-page__back" @click="backToList">
                    ← 返回公告列表
                  </button>
                  <div class="announcement-detail-page__signature">
                    <p class="announcement-detail-page__publisher">{{ announcement.authorName }}</p>
                    <p class="announcement-detail-page__publish-date">{{ announcement.publishDateText }}</p>
                  </div>
                </footer>
              </template>

              <section v-else class="announcement-detail-page__not-found">
                <h1>公告不存在或已被删除</h1>
                <p>请返回公告中心浏览其他官方公告。</p>
                <button type="button" class="announcement-detail-page__back" @click="backToList">
                  返回公告中心
                </button>
              </section>
            </article>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped lang="scss">
.announcement-detail-page {
  --announcement-article-width: 1080px;
  width: 100%;
  background: #000;
  overflow-x: clip;
}

.announcement-detail-page__content {
  width: 100%;
}

.announcement-detail-page__paper {
  position: relative;
  width: 100%;
}

.announcement-detail-page__article {
  width: min(100%, var(--announcement-article-width));
  margin-inline: auto;
  padding: 34px 32px 80px;
  box-sizing: border-box;
}

.announcement-detail-page__breadcrumb {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  padding-bottom: 18px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.1);
  color: var(--cnpa-paper-text-secondary, #666);
  font-size: 13px;

  a,
  button {
    color: inherit;
    text-decoration: none;
    background: transparent;
    border: 0;
    padding: 0;
    font: inherit;
    cursor: pointer;
  }

  a:hover,
  button:hover {
    color: #111;
  }
}

.announcement-detail-page__tags {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-top: 20px;
}

.announcement-detail-page__pin {
  padding: 2px 8px;
  background: var(--cnpa-red, #e10613);
  color: #fff;
  font-size: 12px;
  font-weight: 800;
}

.announcement-detail-page__category {
  padding: 2px 8px;
  font-size: 12px;
  font-weight: 800;
  border: 1px solid currentColor;

  &--event {
    color: #8e0008;
    border-color: rgba(142, 0, 8, 0.35);
    background: rgba(142, 0, 8, 0.06);
  }

  &--rule {
    color: #222;
    border-color: rgba(0, 0, 0, 0.18);
    background: rgba(0, 0, 0, 0.04);
  }

  &--notice {
    color: #9a7420;
    border-color: rgba(154, 116, 32, 0.35);
    background: rgba(154, 116, 32, 0.08);
  }

  &--other {
    color: #555;
    border-color: rgba(0, 0, 0, 0.14);
    background: rgba(0, 0, 0, 0.03);
  }
}

.announcement-detail-page__header {
  margin-top: 18px;
  padding-bottom: 18px;
  border-bottom: 3px solid #111;
}

.announcement-detail-page__title {
  margin: 0;
  max-width: 850px;
  color: #111;
  font-size: clamp(30px, 3vw, 46px);
  font-weight: 900;
  line-height: 1.35;
}

.announcement-detail-page__meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 32px;
  padding-block: 14px;
  border-top: 1px solid rgba(0, 0, 0, 0.08);
  border-bottom: 1px solid rgba(0, 0, 0, 0.08);
}

.announcement-detail-page__meta-item {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.announcement-detail-page__meta-label {
  color: var(--cnpa-paper-text-secondary, #666);
  font-size: 13px;
  white-space: nowrap;
}

.announcement-detail-page__meta-value {
  color: #111;
  font-size: 14px;
  font-weight: 700;
}

.announcement-detail-page__body {
  padding-top: 46px;
}

.announcement-detail-page__adjacent {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-top: 36px;
}

.announcement-detail-page__adjacent-btn {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 6px;
  min-height: 78px;
  padding: 14px 16px;
  border: 1px solid rgba(0, 0, 0, 0.12);
  background: rgba(255, 255, 255, 0.22);
  color: #111;
  font: inherit;
  text-align: left;
  cursor: pointer;

  span {
    color: #777;
    font-size: 12px;
    font-weight: 700;
  }

  strong {
    font-size: 14px;
    font-weight: 800;
    line-height: 1.4;
  }

  &:hover:not(:disabled) {
    border-color: rgba(0, 0, 0, 0.28);
  }

  &:disabled {
    opacity: 0.55;
    cursor: default;
  }

  &--next {
    align-items: flex-end;
    text-align: right;
  }
}

.announcement-detail-page__footer {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
  margin-top: 42px;
  padding-top: 24px;
  border-top: 1px solid rgba(0, 0, 0, 0.08);
}

.announcement-detail-page__signature {
  text-align: right;
}

.announcement-detail-page__publisher {
  margin: 0;
  color: #111;
  font-size: 15px;
  font-weight: 800;
}

.announcement-detail-page__publish-date {
  margin: 6px 0 0;
  color: var(--cnpa-paper-text-secondary, #666);
  font-size: 14px;
}

.announcement-detail-page__back {
  min-width: 160px;
  height: 44px;
  padding: 0 18px;
  border: 1px solid #111;
  background: var(--cnpa-paper-bg, #e9e7e7);
  color: #111;
  font: inherit;
  font-size: 14px;
  font-weight: 800;
  cursor: pointer;
  transition: background 0.18s ease, color 0.18s ease;

  &:hover {
    background: #111;
    color: #fff;
  }
}

.announcement-detail-page__not-found {
  padding: 48px 0;
  text-align: center;

  h1 {
    margin: 0 0 12px;
    color: #111;
    font-size: 28px;
  }

  p {
    margin: 0 0 24px;
    color: var(--cnpa-paper-text-secondary, #666);
  }
}

.announcement-detail-page__paper :deep(.paper-tear) {
  display: none; /* 临时隐藏撕纸，勿删 */
  height: 0;
  overflow: visible;
  z-index: 6;
}

.announcement-detail-page__paper :deep(.torn-edge-top) {
  transform: translateY(-48%);
}

.announcement-detail-page__paper :deep(.torn-edge-main) {
  fill: var(--cnpa-paper-bg) !important;
}

.announcement-detail-page__paper :deep(.torn-edge-shadow) {
  display: none;
}

@media (max-width: 767px) {
  .announcement-detail-page__article {
    padding: 24px 16px 56px;
  }

  .announcement-detail-page__meta,
  .announcement-detail-page__adjacent {
    grid-template-columns: 1fr;
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .announcement-detail-page__adjacent {
    display: flex;
  }

  .announcement-detail-page__adjacent-btn--next {
    align-items: flex-start;
    text-align: left;
  }

  .announcement-detail-page__footer {
    flex-direction: column-reverse;
    align-items: stretch;
    margin-top: 34px;
  }

  .announcement-detail-page__signature {
    text-align: left;
  }
}
</style>
