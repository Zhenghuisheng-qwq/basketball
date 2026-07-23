<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import PaperTear from '@/components/common/PaperTear.vue'
import {
  buildAnnouncementDetailFromQuery,
  getTournamentRoute,
  goToAnnouncementDetail,
  goToAnnouncementList,
} from '@/utils/announcementNavigate'
import AnnouncementHero from './components/AnnouncementHero.vue'
import AnnouncementStatusBadge from './components/AnnouncementStatusBadge.vue'
import AnnouncementContentRenderer from './components/AnnouncementContentRenderer.vue'
import {
  getAnnouncementById,
  getAnnouncementScopeLabel,
} from './mock/announcements'

const route = useRoute()
const router = useRouter()

const announcementId = computed(() => Number(route.params.announcementId))
const announcement = computed(() => {
  if (!Number.isFinite(announcementId.value)) return undefined
  return getAnnouncementById(announcementId.value)
})

const listState = computed(() => buildAnnouncementDetailFromQuery(route.query))

const scopeLabel = computed(() =>
  announcement.value ? getAnnouncementScopeLabel(announcement.value) : '',
)

const tournamentRoute = computed(() =>
  announcement.value ? getTournamentRoute(announcement.value.tournamentId) : null,
)

const replacementAnnouncement = computed(() => {
  if (!announcement.value?.replacementAnnouncementId) return undefined
  return getAnnouncementById(announcement.value.replacementAnnouncementId)
})

function backToList() {
  goToAnnouncementList(router, {
    category: listState.value.fromCategory,
    keyword: listState.value.fromKeyword,
    page: listState.value.fromPage,
  })
}

function openReplacement() {
  const id = announcement.value?.replacementAnnouncementId
  if (!id) return
  goToAnnouncementDetail(router, id, {
    category: listState.value.fromCategory,
    keyword: listState.value.fromKeyword,
    page: listState.value.fromPage,
  })
}
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
            <template v-if="announcement">
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
                <AnnouncementStatusBadge :status="announcement.status" />
              </div>

              <header class="announcement-detail-page__header">
                <h1 class="announcement-detail-page__title">
                  {{ announcement.title }}
                  <span v-if="announcement.subtitle" class="announcement-detail-page__subtitle">
                    {{ announcement.subtitle }}
                  </span>
                </h1>
              </header>

              <div
                v-if="announcement.status === 'REPLACED'"
                class="announcement-detail-page__alert announcement-detail-page__alert--replaced"
              >
                <p>本公告已被新公告替代。</p>
                <button
                  v-if="replacementAnnouncement || announcement.replacementAnnouncementId"
                  type="button"
                  class="announcement-detail-page__alert-link"
                  @click="openReplacement"
                >
                  查看最新公告 →
                </button>
              </div>

              <div
                v-else-if="announcement.status === 'EXPIRED'"
                class="announcement-detail-page__alert announcement-detail-page__alert--expired"
              >
                <p>本公告当前已失效，仅供历史查阅。</p>
                <p v-if="announcement.invalidReason" class="announcement-detail-page__alert-reason">
                  失效原因：{{ announcement.invalidReason }}
                </p>
              </div>

              <div class="announcement-detail-page__meta">
                <div class="announcement-detail-page__meta-item">
                  <span class="announcement-detail-page__meta-label">关联赛事</span>
                  <router-link
                    v-if="tournamentRoute"
                    :to="tournamentRoute"
                    class="announcement-detail-page__meta-link"
                  >
                    {{ scopeLabel }}
                  </router-link>
                  <span v-else class="announcement-detail-page__meta-value">{{ scopeLabel }}</span>
                </div>
                <div class="announcement-detail-page__meta-item">
                  <span class="announcement-detail-page__meta-label">发布时间</span>
                  <time class="announcement-detail-page__meta-value">{{ announcement.publishDateText }}</time>
                </div>
              </div>

              <section class="announcement-detail-page__body">
                <AnnouncementContentRenderer
                  v-if="announcement.contentBlocks.length > 0"
                  :blocks="announcement.contentBlocks"
                />
                <p v-else class="announcement-detail-page__empty-content">暂无正文内容</p>
              </section>

              <footer class="announcement-detail-page__footer">
                <button type="button" class="announcement-detail-page__back" @click="backToList">
                  ← 返回公告列表
                </button>
                <div class="announcement-detail-page__signature">
                  <p class="announcement-detail-page__publisher">{{ announcement.publisherName }}</p>
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

  &--penalty {
    color: #9a7420;
    border-color: rgba(154, 116, 32, 0.35);
    background: rgba(154, 116, 32, 0.08);
  }

  &--platform {
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

.announcement-detail-page__subtitle {
  display: block;
  margin-top: 8px;
}

.announcement-detail-page__alert {
  margin-top: 18px;
  padding: 14px 16px;
  border: 1px solid rgba(0, 0, 0, 0.1);
  font-size: 14px;

  p {
    margin: 0;
  }

  &--replaced {
    background: rgba(255, 255, 255, 0.28);
  }

  &--expired {
    background: rgba(0, 0, 0, 0.04);
    color: #555;
  }
}

.announcement-detail-page__alert-link {
  margin-top: 8px;
  padding: 0;
  border: 0;
  background: transparent;
  color: #111;
  font: inherit;
  font-weight: 800;
  cursor: pointer;
  text-decoration: underline;
  text-underline-offset: 3px;

  &:hover {
    color: var(--cnpa-red, #e10613);
  }
}

.announcement-detail-page__alert-reason {
  margin-top: 6px;
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

.announcement-detail-page__meta-value,
.announcement-detail-page__meta-link {
  color: #111;
  font-size: 14px;
  font-weight: 700;
  text-decoration: none;
}

.announcement-detail-page__meta-link:hover {
  color: var(--cnpa-red, #e10613);
}

.announcement-detail-page__body {
  padding-top: 46px;
}

.announcement-detail-page__empty-content {
  margin: 0;
  color: var(--cnpa-paper-text-secondary, #666);
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

  .announcement-detail-page__meta {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
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
