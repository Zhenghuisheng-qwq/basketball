<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { getPublicSocialLinks } from '@/api/publicSite'
import type { PublicSocialLinkResp } from '@/api/types/public'
import { isSafeHttpUrl, SOCIAL_PLATFORM_ICONS } from '@/constants/communityLinks'
import { iconAssets } from '@/constants/iconAssets'
import { localizedText } from '@/utils/publicDisplay'

const { t } = useI18n()

type CommunityCard = {
  id: string
  platformCode: string
  title: string
  description: string
  action: string
  url: string
  icon: string
  clickable: boolean
}

const rawLinks = ref<PublicSocialLinkResp[]>([])
const cards = computed<CommunityCard[]>(() => rawLinks.value
  .filter((item) => item.platformCode && SOCIAL_PLATFORM_ICONS[item.platformCode])
  .map((item) => {
    const url = item.jumpUrl?.trim() || ''
    const clickable = isSafeHttpUrl(url)
    return {
      id: String(item.id),
      platformCode: item.platformCode,
      title: localizedText(item.title, item.titleEn, ''),
      description: localizedText(item.subtitle, item.subtitleEn, ''),
      action: localizedText(item.buttonText, item.buttonTextEn, ''),
      url: clickable ? url : '',
      icon: SOCIAL_PLATFORM_ICONS[item.platformCode],
      clickable,
    }
  }))

onMounted(async () => {
  try {
    const res = await getPublicSocialLinks()
    rawLinks.value = res.data || []
  } catch {
    rawLinks.value = []
  }
})

function openLink(card: CommunityCard) {
  if (!card.clickable) {
    return
  }
  window.open(card.url, '_blank', 'noopener,noreferrer')
}
</script>

<template>
  <section class="site-section home-community-section" :aria-label="t('home.join')">
    <div class="site-shell home-community-shell">
      <div class="home-community-paper">
      <img
        class="home-community-bg"
        :src="iconAssets['home/join_cnpa.png']"
        alt=""
        aria-hidden="true"
        draggable="false"
      />
      <img
        class="home-community-tear"
        :src="iconAssets['home/sizhi_cnpa_top.png']"
        alt=""
        aria-hidden="true"
        draggable="false"
      />
      <div class="home-community-tear-bottom" aria-hidden="true">
        <img :src="iconAssets['home/sizhi_cnpa_under.png']" alt="" draggable="false" />
      </div>
      <div class="home-community-inner">
        <div class="home-community-grid">
          <button
            v-for="card in cards"
            :key="card.id"
            type="button"
            class="home-community-item"
            :class="{ 'is-static': !card.clickable }"
            :aria-disabled="!card.clickable"
            @click="openLink(card)"
          >
            <span class="home-community-item__icon" aria-hidden="true">
              <img :src="card.icon" alt="" draggable="false" />
            </span>
            <strong>{{ card.title }}</strong>
            <span class="home-community-item__desc">{{ card.description }}</span>
            <small>{{ card.action }}</small>
          </button>
        </div>
      </div>
      </div>
    </div>
  </section>
</template>
