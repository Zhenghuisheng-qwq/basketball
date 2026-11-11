<script setup lang="ts">
import { computed, onBeforeUnmount, ref } from 'vue'
import { usePublicPageHero, visibleHeroImage } from '@/composables/usePublicPageHero'
import { useI18n } from 'vue-i18n'
import { getPublicPartners } from '@/api/public/partner'
import type { PublicPartnerSupportRecordResp } from '@/api/types/public'
import { displayText, toNumber } from '@/utils/publicDisplay'
import { iconAssets } from '@/constants/iconAssets'

const partnersHeroImg = iconAssets['hzhb/hzhb.png']

const PAGE_SIZE = 8
const { t, locale } = useI18n()
const pageHero = usePublicPageHero('PARTNER')
const heroSrc = computed(() => visibleHeroImage(
  pageHero.settled.value,
  pageHero.configured.value,
  pageHero.imageUrl.value,
  partnersHeroImg,
))
const heroStyle = computed(() => (
  pageHero.configured.value ? { objectPosition: pageHero.objectPosition.value } : undefined
))

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
  return new Intl.NumberFormat(locale.value, {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(Number.isFinite(value) ? value : 0)
}

function formatCount(value: number) {
  return Number.isFinite(value) ? String(value) : '0'
}

function isCashSupport(record: PublicPartnerSupportRecordResp) {
  return (record.supportType || 'CASH').toUpperCase() === 'CASH'
}

function supportMethodText(record: PublicPartnerSupportRecordResp) {
  if (isCashSupport(record)) {
    return `¥ ${formatMoney(toNumber(record.supportAmount, 0))}`
  }
  return displayText(record.supportDisplayText || record.supportTypeText, '-')
}

function supportDateText(record: PublicPartnerSupportRecordResp) {
  const date = displayText(record.supportDate, '')
  return date ? date.slice(0, 10) : '-'
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
    error.value = err instanceof Error ? err.message : t('publicPages.partners.loadFailed')
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
    <section class="partners-hero" :aria-label="t('publicPages.partners.heroAria')">
      <div class="site-shell partners-hero__shell">
        <div class="partners-hero__visual cnpa-public-hero-frame">
          <img
            v-if="heroSrc"
            class="partners-hero__image"
            :src="heroSrc"
            :alt="pageHero.altText.value || t('publicPages.partners.heroAria')"
            :style="heroStyle"
            draggable="false"
          />
          <div
            v-if="pageHero.configured.value && !pageHero.fullImage.value"
            class="partners-hero__mask"
            :style="{ background: `rgba(0, 0, 0, ${pageHero.maskOpacity.value / 100})` }"
            aria-hidden="true"
          />
        </div>
      </div>
    </section>

    <section class="partners-content">
      <div class="partners-panel">
        <div class="site-shell partners-panel__inner">
          <section class="partners-summary">
            <h2 class="section-heading">
              <span class="section-heading__cn">{{ t('publicPages.partners.thanks') }}</span>
              <span class="section-heading__en">SUPPORT SUMMARY</span>
            </h2>
            <div class="partners-summary__grid">
              <article class="partners-summary__card">
                <div class="partners-summary__label">{{ t('publicPages.partners.partnerCount') }}</div>
                <div class="partners-summary__number">
                  <template v-if="loading">—</template>
                  <template v-else>{{ formatCount(supportRecordCount) }}</template>
                </div>
              </article>
              <article class="partners-summary__card">
                <div class="partners-summary__label">{{ t('publicPages.partners.totalAmount') }}</div>
                <div class="partners-summary__number partners-summary__number--money">
                  <span class="currency">¥</span>
                  <template v-if="loading">—</template>
                  <template v-else>{{ formattedTotalCash }}</template>
                </div>
              </article>
            </div>
          </section>

          <section ref="wallRef" class="partners-wall">
            <h2 class="section-heading">
              <span class="section-heading__cn">{{ t('publicPages.partners.wall') }}</span>
              <span class="section-heading__en">SPONSOR WALL</span>
            </h2>

            <div v-if="pageState === 'loading'" class="partners-state">{{ t('publicPages.partners.loading') }}</div>
            <div v-else-if="pageState === 'error'" class="partners-state partners-state--error">
              <p>{{ error }}</p>
              <button type="button" class="partners-retry" @click="loadPartners(pageNum)">{{ t('publicPages.partners.reload') }}</button>
            </div>
            <div v-else-if="pageState === 'empty'" class="partners-empty">
              {{ t('publicPages.partners.empty') }}
            </div>
            <div v-else class="partners-table-wrap">
              <table class="partners-table">
                <thead>
                  <tr>
                    <th scope="col">{{ t('publicPages.partners.partner') }}</th>
                    <th scope="col">{{ t('publicPages.partners.method') }}</th>
                    <th scope="col">{{ t('publicPages.partners.date') }}</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="record in records" :key="String(record.recordId || record.partnerId)">
                    <td>
                      <a
                        v-if="record.websiteUrl"
                        class="partners-table__name"
                        :href="record.websiteUrl"
                        target="_blank"
                        rel="noopener noreferrer"
                      >{{ displayText(record.partnerName) }}</a>
                      <span v-else class="partners-table__name">{{ displayText(record.partnerName) }}</span>
                    </td>
                    <td>
                      <span class="partners-table__method" :class="{ 'is-cash': isCashSupport(record) }">
                        {{ supportMethodText(record) }}
                      </span>
                    </td>
                    <td class="partners-table__date">{{ supportDateText(record) }}</td>
                  </tr>
                </tbody>
              </table>
            </div>

            <nav v-if="showPagination" class="partners-pagination" :aria-label="t('publicPages.partners.pagination')">
              <button
                type="button"
                class="partners-pagination__btn"
                :disabled="pageNum <= 1 || loading"
                :aria-label="t('publicPages.partners.prevPage')"
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
                :aria-label="t('publicPages.partners.nextPage')"
                @click="goPage(pageNum + 1)"
              >
                ›
              </button>
            </nav>
          </section>
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

.partners-hero__mask {
  position: absolute;
  inset: 0;
  z-index: 1;
  pointer-events: none;
}

.partners-content {
  width: 100%;
  background: #080808;
}

.partners-panel {
  background: #f6f4f1;
  color: #161616;
}

.partners-panel__inner {
  padding-block: clamp(36px, 4vw, 56px) clamp(40px, 4vw, 64px);
}

.section-heading {
  display: flex;
  align-items: baseline;
  flex-wrap: wrap;
  gap: 10px 14px;
  margin: 0;
}

.section-heading__cn {
  font-size: clamp(26px, 2.2vw, 34px);
  line-height: 1.1;
  font-weight: 900;
  letter-spacing: 0.5px;
  color: #141414;
}

.section-heading__en {
  font-family: Impact, 'Arial Narrow', sans-serif;
  font-style: italic;
  font-weight: 800;
  font-size: clamp(13px, 1vw, 16px);
  letter-spacing: 0.8px;
  color: #e10613;
}

.partners-summary__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: clamp(16px, 2vw, 28px);
  margin-top: clamp(22px, 2vw, 28px);
}

.partners-summary__card {
  min-height: clamp(148px, 16vw, 188px);
  padding: 22px 16px;
  border: 1px solid rgba(28, 28, 28, 0.72);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.55);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  box-sizing: border-box;
}

.partners-summary__label {
  margin-bottom: clamp(12px, 1.4vw, 18px);
  font-size: clamp(16px, 1.4vw, 20px);
  font-weight: 900;
  letter-spacing: 0.5px;
  color: #171717;
}

.partners-summary__number {
  font-family: Impact, 'Arial Black', sans-serif;
  font-size: clamp(52px, 5.2vw, 76px);
  line-height: 0.9;
  letter-spacing: 1px;
  color: #d10012;
}

.partners-summary__number--money {
  display: flex;
  align-items: baseline;
  justify-content: center;
  gap: 6px;
  max-width: 100%;
  font-size: clamp(36px, 4.2vw, 64px);
  white-space: nowrap;

  .currency {
    font-family: Arial, sans-serif;
    font-size: 0.42em;
    line-height: 1;
  }
}

.partners-wall {
  margin-top: clamp(40px, 4vw, 56px);
  scroll-margin-top: calc(var(--site-header-height) + 16px);
}

.partners-table-wrap {
  margin-top: clamp(18px, 2vw, 26px);
  overflow-x: auto;
}

.partners-table {
  width: 100%;
  border-collapse: collapse;
  table-layout: fixed;
}

.partners-table th,
.partners-table td {
  padding: 16px 8px;
  text-align: left;
  vertical-align: middle;
}

.partners-table th {
  padding-top: 4px;
  padding-bottom: 14px;
  border-bottom: 1px solid rgba(22, 22, 22, 0.14);
  font-size: clamp(14px, 1.2vw, 16px);
  font-weight: 900;
  letter-spacing: 0.4px;
  color: #171717;
}

.partners-table td {
  border-bottom: 1px solid rgba(22, 22, 22, 0.08);
  font-size: clamp(14px, 1.15vw, 16px);
  font-weight: 700;
  color: #1a1a1a;
}

.partners-table th:nth-child(2),
.partners-table td:nth-child(2) {
  text-align: center;
}

.partners-table th:nth-child(3),
.partners-table td:nth-child(3) {
  text-align: right;
}

.partners-table__name {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: inherit;
  font-weight: 800;
  text-decoration: none;
}

a.partners-table__name:hover {
  color: #d10012;
}

.partners-table__method {
  font-weight: 800;
}

.partners-table__method.is-cash {
  color: #e10613;
}

.partners-table__date {
  font-variant-numeric: tabular-nums;
  color: #2a2a2a;
}

.partners-pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 10px;
  margin-top: 28px;
}

.partners-pagination__btn {
  min-width: 36px;
  height: 36px;
  padding: 0 10px;
  border: 1px solid rgba(22, 22, 22, 0.16);
  border-radius: 6px;
  background: #fff;
  color: #222;
  font: inherit;
  font-size: 18px;
  font-weight: 700;
  line-height: 1;
  cursor: pointer;

  &:disabled {
    opacity: 0.4;
    cursor: not-allowed;
  }

  &.is-current {
    background: #e10613;
    border-color: #e10613;
    color: #fff;
    cursor: default;
  }
}

.partners-state,
.partners-empty {
  margin-top: 24px;
  min-height: 160px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  color: #666;
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
  border: 1px solid rgba(22, 22, 22, 0.18);
  border-radius: 6px;
  background: #fff;
  color: #161616;
  font: inherit;
  font-weight: 700;
  cursor: pointer;
}

@media (max-width: 640px) {
  .partners-summary__grid {
    grid-template-columns: 1fr;
  }

  .partners-summary__number--money {
    white-space: normal;
  }

  .partners-table th,
  .partners-table td {
    padding-inline: 4px;
  }
}
</style>
