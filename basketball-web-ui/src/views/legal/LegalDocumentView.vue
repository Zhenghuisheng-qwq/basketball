<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import request from '@/api/request'

const props = defineProps<{
  kind: 'userAgreement' | 'privacy'
}>()

const { t, tm, locale } = useI18n()
const version = ref('v1.0')
const englishReviewStatus = ref('PENDING_REVIEW')

const isEnglish = computed(() => String(locale.value).toLowerCase().startsWith('en'))
const points = computed(() => {
  const value = tm(`legal.${props.kind}.points`)
  return Array.isArray(value) ? value.map((item) => String(item)) : []
})

onMounted(async () => {
  try {
    const res = await request.get('/basket/member/auth/agreements') as {
      data?: Array<{ agreementVersion?: string, englishReviewStatus?: string }>
    }
    const first = res.data?.[0]
    if (first?.agreementVersion) version.value = first.agreementVersion
    if (first?.englishReviewStatus) englishReviewStatus.value = first.englishReviewStatus
  } catch {
    version.value = 'v1.0'
  }
})
</script>

<template>
  <main class="legal-page">
    <article class="legal-card">
      <p class="legal-kicker">CNPA</p>
      <h1>{{ t(`legal.${kind}.title`) }}</h1>
      <p class="legal-version">{{ t('legal.versionLabel', { version }) }}</p>
      <p v-if="isEnglish && englishReviewStatus === 'PENDING_REVIEW'" class="legal-pending">
        {{ t('legal.pendingReview') }}
      </p>
      <p class="legal-summary">{{ t(`legal.${kind}.summary`) }}</p>
      <ul>
        <li v-for="(point, index) in points" :key="index">{{ point }}</li>
      </ul>
      <p v-if="isEnglish" class="legal-note">{{ t('legal.pendingReviewDetail') }}</p>
      <p class="legal-note">{{ t('legal.noRepublish') }}</p>
      <p class="legal-note">{{ t('legal.noPublishDate') }}</p>
    </article>
  </main>
</template>

<style scoped lang="scss">
.legal-page {
  min-height: 60vh;
  padding: 48px 20px 72px;
  background: #f6f6f6;
}

.legal-card {
  max-width: 760px;
  margin: 0 auto;
  padding: 32px 28px;
  background: #fff;
  border: 1px solid rgba(15, 15, 15, 0.08);
}

.legal-kicker {
  margin: 0 0 8px;
  letter-spacing: 0.16em;
  font-size: 12px;
}

h1 {
  margin: 0 0 8px;
  font-size: 32px;
  line-height: 1.2;
}

.legal-version,
.legal-summary,
.legal-note {
  color: #333;
  line-height: 1.6;
}

.legal-pending {
  display: inline-block;
  margin: 0 0 12px;
  padding: 4px 8px;
  background: #fff4e5;
  color: #8a4b08;
  font-size: 13px;
}

ul {
  padding-left: 18px;
  line-height: 1.7;
}

@media (max-width: 420px) {
  h1 {
    font-size: 26px;
  }

  .legal-card {
    padding: 24px 16px;
  }
}
</style>
