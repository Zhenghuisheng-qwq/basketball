<script setup lang="ts">
import { computed, onBeforeUnmount, ref } from 'vue'
import PaperTear from '@/components/common/PaperTear.vue'
import { getPublicPartners } from '@/api/public/partner'
import type { PublicPartnerSupportRecordResp } from '@/api/types/public'
import { displayText, toNumber } from '@/utils/publicDisplay'
import partnersHeroImg from '@/assets/icon/hzhb/hzhb.png'

const PAGE_SIZE = 8

const loading = ref(false)
const error = ref('')
const pageNum = ref(1)
const total = ref(0)
const supportRecordCount = ref(0)
const totalCashAmount = ref(0)
const records = ref<PublicPartnerSupportRecordResp[]>([])

const wallRef = ref<HTMLElement | null>(null)
let requestSeq = 0

const totalPages = computed(() => {
  if (total.value <= 0) return 0
  return Math.ceil(total.value / PAGE_SIZE)
})

const pageState = computed(() => {
  if (loading.value) return 'loading'
  if (error.value) return 'error'
  if (total.value <= 0 || records.value.length === 0) return 'empty'
  return 'success'
})

const showPagination = computed(() => pageState.value === 'success' && total.value > 0)

const formattedTotalCash = computed(() => formatMoney(totalCashAmount.value))

function formatMoney(value: number) {
  return new Intl.NumberFormat('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(Number.isFinite(value) ? value : 0)
}

function formatCount(value: number) {
  return Number.isFinite(value) ? String(value) : '0'
}

function supportText(record: PublicPartnerSupportRecordResp) {
  return displayText(record.supportDisplayText, `¥${formatMoney(toNumber(record.supportAmount, 0))}`)
}

function partnerInitial(name?: string | null) {
  const text = displayText(name, '合').trim()
  return text.charAt(0).toUpperCase()
}

async function loadPartners(nextPage = pageNum.value) {
  const current = ++requestSeq
  loading.value = true
  error.value = ''
  try {
    const res = await getPublicPartners({ currentPage: nextPage, pageSize: PAGE_SIZE })
    if (current !== requestSeq) return
    const data = res.data
    supportRecordCount.value = toNumber(data?.summary?.supportRecordCount, 0)
    totalCashAmount.value = toNumber(data?.summary?.totalCashAmount, 0)
    total.value = toNumber(data?.page?.total, 0)
    records.value = data?.page?.rows ?? []
    pageNum.value = nextPage
  } catch (err) {
    if (current !== requestSeq) return
    records.value = []
    total.value = 0
    supportRecordCount.value = 0
    totalCashAmount.value = 0
    error.value = err instanceof Error ? err.message : '合作伙伴数据加载失败，请稍后重试'
  } finally {
    if (current === requestSeq) {
      loading.value = false
    }
  }
}

function goPage(next: number) {
  if (loading.value || !showPagination.value) return
  if (next < 1 || next > totalPages.value || next === pageNum.value) return
  void loadPartners(next).then(() => {
    wallRef.value?.scrollIntoView({ behavior: 'smooth', block: 'start' })
  })
}

onBeforeUnmount(() => {
  requestSeq += 1
})

void loadPartners(1)
</script>

<template>
  <div class="partners-page">
    <section class="partners-hero" aria-label="合作伙伴">
      <div class="site-shell partners-hero__shell">
        <div class="partners-hero__visual cnpa-public-hero-frame">
          <img
            class="partners-hero__image"
            :src="partnersHeroImg"
            alt="CNPA 合作伙伴"
            draggable="false"
          />
        </div>
      </div>
    </section>

    <section class="partners-content">
      <div class="site-shell">
        <div class="cnpa-paper-section cnpa-paper-section--light partners-content__paper">
          <PaperTear position="top" tone="light" />
          <div class="cnpa-paper-section__content partners-content__inner">
            <section class="partners-summary">
              <h2 class="section-heading">
                <span class="section-heading__cn">感谢支持</span>
                <span class="section-heading__en">SUPPORT SUMMARY</span>
              </h2>
              <div class="partners-summary__grid">
                <article class="partners-summary__card">
                  <div class="partners-summary__label">支持伙伴总数</div>
                  <div class="partners-summary__number">
                    <template v-if="loading">—</template>
                    <template v-else>{{ formatCount(supportRecordCount) }}</template>
                  </div>
                </article>
                <article class="partners-summary__card">
                  <div class="partners-summary__label">累计赞助金额</div>
                  <div class="partners-summary__number partners-summary__number--money">
                    <span class="currency">¥</span>
                    <template v-if="loading">—</template>
                    <template v-else>{{ formattedTotalCash }}</template>
                  </div>
                </article>
              </div>
            </section>

            <div class="partners-paper-rule" aria-hidden="true" />

            <section ref="wallRef" class="partners-wall">
              <h2 class="section-heading">
                <span class="section-heading__cn">赞助感谢墙</span>
                <span class="section-heading__en">SPONSOR WALL</span>
              </h2>

              <div v-if="pageState === 'loading'" class="partners-state">加载中...</div>
              <div v-else-if="pageState === 'error'" class="partners-state partners-state--error">
                <p>{{ error }}</p>
                <button type="button" class="partners-retry" @click="loadPartners(pageNum)">重新加载</button>
              </div>
              <div v-else-if="pageState === 'empty'" class="partners-empty">
                暂无公开的合作伙伴
              </div>
              <div v-else class="partners-wall__grid" aria-label="赞助感谢墙">
                <component
                  :is="record.websiteUrl ? 'a' : 'article'"
                  v-for="record in records"
                  :key="String(record.partnerId || record.recordId)"
                  class="partners-card"
                  :href="record.websiteUrl || undefined"
                  :target="record.websiteUrl ? '_blank' : undefined"
                  :rel="record.websiteUrl ? 'noopener noreferrer' : undefined"
                >
                  <div class="partners-card__logo">
                    <img
                      v-if="record.partnerLogoUrl"
                      :src="record.partnerLogoUrl"
                      :alt="displayText(record.partnerName)"
                      loading="lazy"
                    />
                    <span v-else class="partners-card__fallback">{{ partnerInitial(record.partnerName) }}</span>
                  </div>
                  <div class="partners-card__name" :title="displayText(record.partnerName)">
                    {{ displayText(record.partnerName) }}
                  </div>
                  <div class="partners-card__label">累计赞助</div>
                  <div class="partners-card__amount">{{ supportText(record) }}</div>
                </component>
              </div>

              <nav v-if="showPagination" class="partners-pagination" aria-label="分页">
                <button
                  type="button"
                  class="partners-pagination__btn"
                  :disabled="pageNum <= 1 || loading"
                  aria-label="上一页"
                  @click="goPage(pageNum - 1)"
                >
                  ‹
                </button>
                <button
                  type="button"
                  class="partners-pagination__btn is-current"
                  aria-current="page"
                >
                  {{ pageNum }}
                </button>
                <button
                  type="button"
                  class="partners-pagination__btn"
                  :disabled="pageNum >= totalPages || loading"
                  aria-label="下一页"
                  @click="goPage(pageNum + 1)"
                >
                  ›
                </button>
              </nav>
            </section>
          </div>
          <PaperTear position="bottom" tone="light" />
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped lang="scss">
.partners-page {
  width: 100%;
  background: #080808;
  overflow-x: clip;
}

.partners-hero {
  width: 100%;
  background: #000;
  border: none;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.partners-hero__shell {
  overflow: hidden;
}

.partners-hero__visual {
  line-height: 0;
  border: none;
  box-shadow: none;
}

.partners-hero__image {
  border: none;
  outline: none;
}

.partners-content {
  width: 100%;
  background: #080808;
}

.partners-content__paper {
  --cnpa-paper-bg: #f4f3ef;
  margin-top: 0;

  :deep(.paper-tear) {
    display: none; /* 临时隐藏撕纸，勿删 */
  }
}

.partners-content__inner {
  padding: clamp(36px, 3.4vw, 44px) clamp(20px, 3.2vw, 32px) clamp(36px, 3vw, 48px);
  box-sizing: border-box;
}

.section-heading {
  display: flex;
  align-items: baseline;
  flex-wrap: wrap;
  gap: 12px 16px;
  margin: 0;
  font-weight: 900;
  letter-spacing: 0.5px;
}

.section-heading__cn {
  font-size: clamp(24px, 2vw, 32px);
  line-height: 1.1;
  color: #121212;
}

.section-heading__en {
  font-family: Impact, 'Arial Narrow', sans-serif;
  font-style: italic;
  color: var(--cnpa-red, #e10613);
  font-size: clamp(13px, 1vw, 16px);
  font-weight: 800;
  letter-spacing: 0.8px;
}

.partners-summary {
  padding-top: 8px;
  padding-bottom: clamp(36px, 3vw, 52px);
}

.partners-summary__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: clamp(18px, 2vw, 28px);
  margin-top: clamp(24px, 2.2vw, 30px);
}

.partners-summary__card {
  min-height: clamp(160px, 14vw, 190px);
  max-height: 220px;
  padding: 24px;
  border: 1px solid rgba(30, 30, 30, 0.38);
  border-radius: 7px;
  background: rgba(255, 255, 255, 0.34);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  box-shadow: inset 0 0 35px rgba(255, 255, 255, 0.25);
  box-sizing: border-box;
}

.partners-summary__label {
  font-weight: 900;
  font-size: clamp(16px, 1.5vw, 19px);
  letter-spacing: 1px;
  margin-bottom: clamp(16px, 1.6vw, 22px);
  color: #121212;
}

.partners-summary__number {
  font-family: Impact, 'Arial Black', sans-serif;
  font-stretch: condensed;
  color: #c90911;
  font-size: clamp(56px, 5.5vw, 72px);
  line-height: 0.9;
  letter-spacing: 1px;
  text-shadow: 0 1px 0 rgba(0, 0, 0, 0.08);
}

.partners-summary__number--money {
  display: flex;
  align-items: baseline;
  justify-content: center;
  gap: 4px;
  font-size: clamp(48px, 4.6vw, 68px);
  white-space: nowrap;

  .currency {
    font-family: Arial, sans-serif;
    font-size: clamp(20px, 2vw, 27px);
    line-height: 1;
    transform: translateY(-0.08em);
  }
}

.partners-paper-rule {
  height: clamp(24px, 2.5vw, 36px);
  margin: 0 0 clamp(24px, 2.2vw, 30px);
  position: relative;
  overflow: hidden;

  &::before {
    content: '';
    position: absolute;
    left: 0;
    right: 0;
    top: 50%;
    border-top: 1px solid rgba(50, 50, 50, 0.17);
    filter: blur(0.15px);
  }

  &::after {
    content: '';
    position: absolute;
    left: 0;
    right: 0;
    top: calc(50% - 5px);
    height: 10px;
    opacity: 0.15;
    background: repeating-linear-gradient(
      175deg,
      transparent 0 26px,
      rgba(30, 30, 30, 0.35) 27px 28px,
      transparent 29px 47px
    );
  }
}

.partners-wall {
  scroll-margin-top: calc(var(--site-header-height) + 16px);
}

.partners-wall__grid {
  margin-top: clamp(24px, 2.2vw, 30px);
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: clamp(14px, 1.6vw, 20px);
}

.partners-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  min-height: 190px;
  padding: 22px 16px 20px;
  border: 1px solid rgba(30, 30, 30, 0.28);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.34);
  color: inherit;
  text-decoration: none;
  box-sizing: border-box;
  transition: transform 0.18s ease, box-shadow 0.18s ease;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 8px 18px rgba(0, 0, 0, 0.08);
  }
}

.partners-card__logo {
  width: 72px;
  height: 72px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.7);
  border: 1px solid rgba(30, 30, 30, 0.12);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;

  img {
    width: 100%;
    height: 100%;
    object-fit: contain;
  }
}

.partners-card__fallback {
  font-size: 28px;
  font-weight: 800;
  color: #333;
}

.partners-card__name {
  margin-top: 14px;
  font-size: 16px;
  font-weight: 800;
  color: #121212;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.partners-card__label {
  margin-top: 10px;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.5px;
  color: #666;
}

.partners-card__amount {
  margin-top: 6px;
  font-size: 18px;
  font-weight: 800;
  color: #c90911;
}

.partners-pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 8px;
  margin-top: 23px;
}

.partners-pagination__btn {
  min-width: 40px;
  height: 40px;
  padding: 0 12px;
  border: 1px solid rgba(30, 30, 30, 0.24);
  border-radius: 4px;
  background: rgba(255, 255, 255, 0.42);
  color: #111;
  font: inherit;
  font-size: 18px;
  font-weight: 700;
  cursor: pointer;

  &:disabled {
    opacity: 0.45;
    cursor: not-allowed;
  }

  &.is-current {
    background: #d60712;
    border-color: #d60712;
    color: #fff;
  }
}

.partners-state {
  margin-top: clamp(24px, 2.2vw, 30px);
  padding: 32px 16px;
  text-align: center;
  color: #555;
  font-size: 16px;
  font-weight: 700;
}

.partners-empty {
  margin-top: clamp(24px, 2.2vw, 30px);
  min-height: 160px;
  max-height: 220px;
  padding: 48px 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center;
  color: #555;
  font-size: 16px;
  font-weight: 700;
}

.partners-state--error p {
  margin: 0 0 14px;
  color: #8b1a1a;
}

.partners-retry {
  min-width: 120px;
  height: 40px;
  border: 1px solid rgba(30, 30, 30, 0.24);
  border-radius: 4px;
  background: #fff;
  color: #111;
  font: inherit;
  font-weight: 700;
  cursor: pointer;
}

@media (max-width: 1100px) {
  .partners-wall__grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .partners-summary__grid {
    grid-template-columns: 1fr;
  }

  .partners-wall__grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
