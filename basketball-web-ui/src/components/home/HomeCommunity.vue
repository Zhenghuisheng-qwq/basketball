<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { communityLinks } from '@/constants/communityLinks'
import iconKook from '@/assets/icon/home/social_kook.png'
import iconQq from '@/assets/icon/home/social_qq.png'
import iconBili from '@/assets/icon/home/social_bili.png'
import iconDouyin from '@/assets/icon/home/social_douyin.png'

type CommunityCard = {
  id: string
  title: string
  description: string
  action: string
  url: string
  icon: string
}

const cardIcons: Record<string, string> = {
  kook: iconKook,
  qq: iconQq,
  bilibili: iconBili,
  douyin: iconDouyin,
}

const cards: CommunityCard[] = [
  { id: 'kook', title: '加入 KOOK', description: '官方赛事社区', action: '加入社区', url: '#' },
  { id: 'qq', title: '加入 QQ 群', description: '球员与观众交流群', action: '加入群聊', url: '#' },
  { id: 'bilibili', title: '关注 B 站', description: '赛事直播与精彩内容', action: '访问主页', url: '#' },
  { id: 'douyin', title: '关注抖音', description: '赛事短视频与即时动态', action: '立即关注', url: '#' },
].map((card) => ({
  ...card,
  url: communityLinks.find((link) => link.id === card.id)?.url || card.url,
  icon: cardIcons[card.id],
}))

function openLink(card: CommunityCard) {
  if (!card.url || card.url === '#') {
    ElMessage.info('社区入口暂未开放')
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
            @click="openLink(card)"
          >
            <span class="home-community-item__icon" aria-hidden="true">
              <img :src="card.icon" alt="" draggable="false" />
            </span>
            <strong>{{ card.title }}</strong>
            <span class="home-community-item__desc">{{ card.description }}</span>
            <small>{{ card.action }} →</small>
          </button>
        </div>
      </div>
      </div>
    </div>
  </section>
</template>
