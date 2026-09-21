<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { getPublicSocialLinks } from '@/api/publicSite'
import { isSafeHttpUrl } from '@/constants/communityLinks'

type CommunityCard = {
  id: string
  platformCode: string
  title: string
  description: string
  url: string
  clickable: boolean
}

const iconMap: Record<string, string> = {
  KOOK: 'K',
  WECHAT: 'W',
  BILIBILI: 'B',
  DOUYIN: 'D',
}

const links = ref<CommunityCard[]>([])

onMounted(async () => {
  try {
    const res = await getPublicSocialLinks()
    links.value = (res.data || []).map((item) => {
      const url = item.jumpUrl?.trim() || ''
      const clickable = isSafeHttpUrl(url)
      return {
        id: String(item.id),
        platformCode: item.platformCode,
        title: item.title || '',
        description: item.subtitle || '',
        url: clickable ? url : '',
        clickable,
      }
    })
  } catch {
    links.value = []
  }
})

function handleClick(link: CommunityCard, event?: Event) {
  event?.preventDefault()
  if (!link.clickable) {
    return
  }
  window.open(link.url, '_blank', 'noopener,noreferrer')
}
</script>
<template>
  <div class="site-shell">
    <section class="community-section events-section-red">
      <div class="site-shell-inner">
      <div class="section-head">
        <div>
          <h2 class="section-head-title">加入 CNPA</h2>
          <span class="section-head-sub">Join The Community</span>
        </div>
      </div>

      <p class="community-desc">关注赛事动态、参与互动，和观众一起加入 CNPA 社区。</p>

      <div class="community-grid">
        <button
          v-for="link in links"
          :key="link.id"
          type="button"
          class="community-card"
          :class="{ 'is-static': !link.clickable }"
          :aria-disabled="!link.clickable"
          @click="handleClick(link, $event)"
        >
          <span class="community-icon">{{ iconMap[link.platformCode] || link.platformCode.slice(0, 1) }}</span>
          <span class="community-copy">
            <strong>{{ link.title }}</strong>
            <small>{{ link.description }}</small>
          </span>
          <span class="community-arrow">→</span>
        </button>
      </div>
      </div>
    </section>
  </div>
</template>
<style scoped lang="scss">
.community-section {
  padding: 56px 0;
}

.community-desc {
  margin: -8px 0 24px;
  color: rgba(255, 255, 255, 0.82);
}

.community-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.community-card {
  display: grid;
  width: 100%;
  grid-template-columns: 44px 1fr auto;
  gap: 12px;
  align-items: center;
  padding: 18px 16px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  background: rgba(8, 8, 8, 0.82);
  color: #fff;
  text-align: left;
  font: inherit;
  cursor: pointer;
  transition: transform 0.2s ease, border-color 0.2s ease;

  &:hover {
    transform: translateY(-2px);
    border-color: rgba(255, 255, 255, 0.24);
  }

  &.is-static {
    cursor: default;

    &:hover {
      transform: none;
      border-color: rgba(255, 255, 255, 0.08);
    }
  }
}

.community-icon {
  width: 44px;
  height: 44px;
  display: grid;
  place-items: center;
  border-radius: 2px;
  background: rgba(255, 255, 255, 0.08);
  font-size: 18px;
  font-weight: 900;
}

.community-copy {
  display: flex;
  flex-direction: column;
  gap: 4px;

  strong {
    font-size: 15px;
  }

  small {
    font-size: 12px;
    line-height: 1.5;
    color: rgba(255, 255, 255, 0.62);
  }
}

.community-arrow {
  font-size: 18px;
  opacity: 0.72;
}

@media (max-width: 992px) {
  .community-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .community-grid {
    grid-template-columns: 1fr;
  }
}
</style>
