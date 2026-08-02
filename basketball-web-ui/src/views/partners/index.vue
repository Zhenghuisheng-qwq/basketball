<script setup lang="ts">
import { computed, onBeforeUnmount, ref } from 'vue'
import PaperTear from '@/components/common/PaperTear.vue'
import { getPublicPartners } from '@/api/public/partner'
import type { PublicPartnerSupportRecordResp } from '@/api/types/public'
import { displayText, toNumber } from '@/utils/publicDisplay'
import partnersHeroImg from '@/assets/partners/partners-hero.jpg'

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

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / PAGE_SIZE)))
const formattedTotalCash = computed(() => formatMoney(totalCashAmount.value))
const isEmpty = computed(() => !loading.value && !error.value && records.value.length === 0)

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
  if (record.supportType === 'CASH') {
    return displayText(record.supportDisplayText, `¥${formatMoney(toNumber(record.supportAmount, 0))}`)
  }
  return displayText(record.supportTypeText, displayText(record.supportDisplayText, '其他支持'))
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
    error.value = err instanceof Error ? err.message : '合作伙伴数据加载失败，请稍后重试'
  } finally {
    if (current === requestSeq) {
      loading.value = false
    }
  }
}

function goPage(next: number) {
  if (loading.value || next < 1 || next > totalPages.value || next === pageNum.value) return
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
    <section class="partners-hero" aria-label="合作伙伴主题横幅">
      <div class="site-shell">
        <div class="partners-hero__frame">
          <img
            class="partners-hero__image"
            :src="partnersHeroImg"
            alt="CNPA 合作伙伴 CNPA PARTNERS"
            draggable="false"
          />
        </div>
      </div>
    </section>

    <section class="partners-page__content">
      <div class="site-shell">
        <div class="cnpa-paper-section cnpa-paper-section--light partners-page__paper">
          <PaperTear position="top" tone="light" />
          <div class="cnpa-paper-section__content partners-page__inner">
            <section class="partners-summary">
              <h2 class="partners-section-title">
                <span class="cn">感谢支持</span>
                <span class="en">SUPPORT SUMMARY</span>
              </h2>
              <div class="partners-summary__grid">
                <article class="partners-summary__card">
                  <div class="partners-summary__label">支持伙伴总数</div>
                  <div class="partners-summary__number">{{ formatCount(supportRecordCount) }}</div>
                </article>
                <article class="partners-summary__card">
                  <div class="partners-summary__label">累计赞助金额</div>
                  <div class="partners-summary__number partners-summary__number--money">
                    <span class="currency">¥</span>{{ formattedTotalCash }}
                  </div>
                </article>
              </div>
            </section>

            <div class="partners-paper-rule" aria-hidden="true" />

            <section ref="wallRef" class="partners-wall">
              <h2 class="partners-section-title">
                <span class="cn">赞助感谢墙</span>
                <span class="en">SPONSOR WALL</span>
              </h2>

              <div v-if="loading" class="partners-state">加载中...</div>
              <div v-else-if="error" class="partners-state partners-state--error">
                <p>{{ error }}</p>
                <button type="button" class="partners-retry" @click="loadPartners(pageNum)">重新加载</button>
              </div>
              <div v-else-if="isEmpty" class="partners-state">暂无公开的合作伙伴支持记录</div>
              <div v-else class="partners-table" role="table" aria-label="赞助感谢墙">
                <div class="partners-table__row partners-table__row--header" role="row">
                  <div role="columnheader">支持伙伴</div>
                  <div role="columnheader">支持方式</div>
                  <div role="columnheader">支持日期</div>
                </div>
                <div
                  v-for="record in records"
                  :key="String(record.recordId)"
                  class="partners-table__row"
                  role="row"
                >
                  <div class="partners-table__name" role="cell" :title="displayText(record.partnerName)">
                    {{ displayText(record.partnerName) }}
                  </div>
                  <div
                    class="partners-table__support"
                    :class="{ 'is-cash': record.supportType === 'CASH' }"
                    role="cell"
                    :title="record.publicDescription || undefined"
                  >
                    {{ supportText(record) }}
                  </div>
                  <div class="partners-table__date" role="cell">
                    {{ displayText(record.supportDate, '-') }}
                  </div>
                </div>
              </div>

              <nav
                v-if="!loading && !error && totalPages >= 1"
                class="partners-pagination"
                aria-label="分页"
              >
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
}

.partners-hero__frame {
  width: 100%;
  height: clamp(300px, 28vw, 395px);
  overflow: hidden;
  background: #000;
}

.partners-hero__image {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center;
  user-select: none;
}

.partners-page__content {
  width: 100%;
}

.partners-page__paper {
  --cnpa-paper-bg: #f4f3ef;
  margin-top: 0;
}

.partners-page__inner {
  padding: 36px 0 42px;
}

.partners-section-title {
  display: flex;
  align-items: baseline;
  gap: 14px;
  margin: 0;
  font-weight: 900;
  letter-spacing: 0.5px;

  .cn {
    font-size: clamp(24px, 2.2vw, 27px);
    line-height: 1.15;
    color: #121212;
  }

  .en {
    font-family: Impact, 'Arial Narrow', sans-serif;
    font-style: italic;
    color: #ca1118;
    font-size: clamp(15px, 1.4vw, 17px);
    letter-spacing: 0.8px;
  }
}

.partners-summary__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 19px;
  margin-top: 25px;
}

.partners-summary__card {
  min-height: 196px;
  border: 1px solid rgba(30, 30, 30, 0.38);
  border-radius: 7px;
  background: rgba(255, 255, 255, 0.34);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  box-shadow: inset 0 0 35px rgba(255, 255, 255, 0.25);
}

.partners-summary__label {
  font-weight: 900;
  font-size: clamp(16px, 1.5vw, 19px);
  letter-spacing: 1px;
  margin-bottom: 18px;
  color: #121212;
}

.partners-summary__number {
  font-family: Impact, 'Arial Black', sans-serif;
  font-stretch: condensed;
  color: #c90911;
  font-size: clamp(52px, 6vw, 77px);
  line-height: 0.9;
  letter-spacing: 1px;
  text-shadow: 0 1px 0 rgba(0, 0, 0, 0.08);
}

.partners-summary__number--money {
  font-size: clamp(42px, 4.8vw, 61px);
  white-space: nowrap;

  .currency {
    font-family: Arial, sans-serif;
    font-size: clamp(20px, 2vw, 27px);
    vertical-align: 14px;
    margin-right: 5px;
  }
}

.partners-paper-rule {
  height: 30px;
  margin: 36px 0 31px;
  position: relative;
  overflow: hidden;

  &::before {
    content: '';
    position: absolute;
    left: 0;
    right: 0;
    top: 15px;
    border-top: 1px solid rgba(50, 50, 50, 0.17);
    filter: blur(0.15px);
  }

  &::after {
    content: '';
    position: absolute;
    left: 0;
    right: 0;
    top: 10px;
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

.partners-table {
  margin-top: 30px;
  border: 1px solid rgba(30, 30, 30, 0.37);
  border-radius: 8px;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.26);
}

.partners-table__row {
  display: grid;
  grid-template-columns: 1.2fr 1fr 0.8fr;
  min-height: 56px;
  align-items: center;
  padding: 0 22px;
  font-size: 16px;
  color: #1a1a1a;
  border-top: 1px solid rgba(30, 30, 30, 0.12);

  &:first-child {
    border-top: 0;
  }

  &--header {
    min-height: 60px;
    font-size: 18px;
    font-weight: 900;
    background: rgba(255, 255, 255, 0.23);
  }
}

.partners-table__name {
  font-weight: 700;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.partners-table__support {
  font-weight: 700;

  &.is-cash {
    color: #c90911;
  }
}

.partners-table__date {
  text-align: right;
  font-weight: 600;
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
  margin-top: 30px;
  padding: 48px 16px;
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

@media (max-width: 768px) {
  .partners-summary__grid {
    grid-template-columns: 1fr;
  }

  .partners-table__row {
    grid-template-columns: 1fr;
    gap: 6px;
    padding: 14px 16px;
  }

  .partners-table__date {
    text-align: left;
  }
}
</style>
