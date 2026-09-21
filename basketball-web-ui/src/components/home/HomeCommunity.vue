<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { getPublicSocialLinks } from '@/api/publicSite'
import { isSafeHttpUrl, SOCIAL_PLATFORM_ICONS } from '@/constants/communityLinks'

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

const cards = ref<CommunityCard[]>([])

onMounted(async () => {
  try {
    const res = await getPublicSocialLinks()
    const list = res.data || []
    cards.value = list
      .filter((item) => item.platformCode && SOCIAL_PLATFORM_ICONS[item.platformCode])
      .map((item) => {
        const url = item.jumpUrl?.trim() || ''
        const clickable = isSafeHttpUrl(url)
        return {
          id: String(item.id),
          platformCode: item.platformCode,
          title: item.title || '',
          description: item.subtitle || '',
          action: item.buttonText || '',
          url: clickable ? url : '',
          icon: SOCIAL_PLATFORM_ICONS[item.platformCode],
          clickable,
        }
      })
  } catch {
    cards.value = []
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
  <section class="site-section home-community-section" aria-label="加入 CNPA">
    <div class="site-shell home-community-shell">
      <div class="home-community-paper">
      <img
        class="home-community-bg"
        src="@/assets/icon/home/join_cnpa.png"
        alt=""
        aria-hidden="true"
        draggable="false"
      />
      <img
        class="home-community-tear"
        src="@/assets/icon/home/sizhi_cnpa_top.png"
        alt=""
        aria-hidden="true"
        draggable="false"
      />
      <div class="home-community-tear-bottom" aria-hidden="true">
        <img src="@/assets/icon/home/sizhi_cnpa_under.png" alt="" draggable="false" />
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
