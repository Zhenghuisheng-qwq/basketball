<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { communityLinks } from '@/constants/communityLinks'

type CommunityCard = {
  id: string
  title: string
  description: string
  action: string
  url: string
}

const cards: CommunityCard[] = [
  { id: 'kook', title: '加入 KOOK', description: '官方赛事社区', action: '加入社区', url: '#' },
  { id: 'qq', title: '加入 QQ 群', description: '球员与观众交流群', action: '加入群聊', url: '#' },
  { id: 'bilibili', title: '关注 B 站', description: '赛事直播与精彩内容', action: '访问主页', url: '#' },
  { id: 'douyin', title: '关注抖音', description: '赛事短视频与即时动态', action: '立即关注', url: '#' },
].map((card) => ({
  ...card,
  url: communityLinks.find((link) => link.id === card.id)?.url || card.url,
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
  <section class="site-section home-community-section" aria-labelledby="home-community-title">
    <div class="site-shell home-community-shell">
      <div class="home-community-paper">
      <div class="home-community-inner">
        <header class="home-community-header">
          <h2 id="home-community-title" class="home-community-title">加入 CNPA</h2>
          <span class="home-community-sub">JOIN THE COMMUNITY</span>
          <p>关注赛事动态，与球员、战队和观众一起加入 CNPA 社区。</p>
        </header>

        <div class="home-community-grid">
          <button
            v-for="card in cards"
            :key="card.id"
            type="button"
            class="home-community-item"
            @click="openLink(card)"
          >
            <span class="home-community-item__icon" aria-hidden="true">
              <svg v-if="card.id === 'kook'" viewBox="0 0 32 32" fill="none">
                <path d="M7 8.5h12.5a6.5 6.5 0 0 1 0 13H16l-5.2 4.4V21.5H7A4.5 4.5 0 0 1 7 8.5Z" fill="currentColor"/>
              </svg>
              <svg v-else-if="card.id === 'qq'" viewBox="0 0 32 32" fill="none">
                <path d="M16 5.5c-3.4 0-6.2 3.2-6.2 7.1 0 1.3.3 2.5.8 3.6-2.3 1.3-3.8 3.6-3.8 6.2 0 .8.6 1.1 1.2.7 1-.7 1.9-1.2 2.8-1.5.7 2.1 2.7 3.6 5.2 3.6s4.5-1.5 5.2-3.6c.9.3 1.8.8 2.8 1.5.6.4 1.2.1 1.2-.7 0-2.6-1.5-4.9-3.8-6.2.5-1.1.8-2.3.8-3.6 0-3.9-2.8-7.1-6.2-7.1Z" fill="currentColor"/>
              </svg>
              <svg v-else-if="card.id === 'bilibili'" viewBox="0 0 32 32" fill="none">
                <path d="M8.2 7.2 11 10h10l2.8-2.8 1.5 1.5L22.8 12H25a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V14a2 2 0 0 1 2-2h2.2L6.7 8.7 8.2 7.2ZM9 15.5v7h14v-7H9Z" fill="currentColor"/>
              </svg>
              <svg v-else viewBox="0 0 32 32" fill="none">
                <path d="M18.2 5.5v13.2a4.7 4.7 0 1 1-3.4-4.5V8.8c2.6 1.3 4.5 3.3 5.6 5.8 1.5-2.1 2.6-4.7 3.1-7.6h-2.4c-.4 1.6-1.1 3.1-2 4.4V5.5h-.9Z" fill="currentColor"/>
              </svg>
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
