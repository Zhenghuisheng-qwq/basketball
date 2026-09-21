<script setup lang="ts">
import logoImg from '@/assets/icon/logo.png'

const hallLinks = [
  { label: '周最佳', query: { tab: 'weekly' } },
  { label: '赛事荣誉', query: { tab: 'event' } },
  { label: '历届冠军', query: { tab: 'champions' } },
  { label: '最佳阵容', query: { tab: 'event' } },
]

const seriesLinks = [
  { label: 'CNPA League', to: { path: '/tournament', query: { series: 'LEAGUE' } } },
  { label: 'CNPA Arena', to: { path: '/tournament', query: { series: 'ARENA' } } },
  { label: 'CNPA Cup', to: { path: '/tournament', query: { series: 'CUP' } } },
  { label: 'CNPA Open', to: { path: '/tournament', query: { series: 'OPEN' } } },
]
</script>

<template>
  <footer class="web-footer">
    <div class="footer-shell site-shell">
      <div class="footer-grid">
        <div class="footer-brand">
          <img :src="logoImg" alt="CNPA" class="footer-logo" />
          <p>
            CNPA 以赛事为核心，<br />
            连接球员、战队、内容，<br />
            构建属于中国玩家的篮球电竞赛事平台。
          </p>
        </div>

        <div class="footer-col">
          <h4>赛事</h4>
          <router-link
            v-for="item in seriesLinks"
            :key="item.label"
            :to="item.to"
          >
            {{ item.label }}
          </router-link>
        </div>

        <div class="footer-col">
          <h4>数据</h4>
          <router-link to="/data">球员数据</router-link>
          <router-link to="/data">战队数据</router-link>
          <router-link to="/tournament?tab=schedule">赛事赛程</router-link>
          <router-link to="/data">数据记录</router-link>
        </div>

        <div class="footer-col">
          <h4>名人堂</h4>
          <router-link
            v-for="item in hallLinks"
            :key="item.label"
            :to="{ path: '/hall-of-fame', query: item.query }"
          >
            {{ item.label }}
          </router-link>
        </div>

        <div class="footer-col">
          <h4>关于 CNPA</h4>
          <span class="footer-plain">关于我们</span>
          <router-link to="/announcements">公告中心</router-link>
          <router-link to="/partners">合作伙伴</router-link>
          <span class="footer-plain">联系我们</span>
        </div>
      </div>

      <div class="footer-bottom">
        <span>© 2026 CNPA 中国职业电竞篮球联赛，保留所有权利。</span>
        <div class="footer-links">
          <span class="footer-plain">隐私政策</span>
          <span class="footer-plain">服务条款</span>
        </div>
      </div>
    </div>
  </footer>
</template>

<style scoped lang="scss">
.web-footer {
  position: relative;
  z-index: 1;
  width: 100%;
  background: var(--cnpa-black);
  border-top: 1px solid var(--cnpa-border);
}

.footer-shell {
  padding: clamp(40px, 5vw, 72px) 0 clamp(24px, 3vw, 36px);
}

.footer-grid {
  display: grid;
  grid-template-columns: 1.4fr repeat(4, 1fr);
  gap: var(--cnpa-public-card-spacing);
}

.footer-brand {
  p {
    margin: 18px 0 0;
    font-size: 13px;
    line-height: 1.8;
    color: rgba(255, 255, 255, 0.68);
  }
}

.footer-logo {
  width: 120px;
  height: auto;
}

.footer-col {
  display: flex;
  flex-direction: column;
  gap: 10px;

  h4 {
    margin: 0 0 8px;
    font-size: 14px;
    color: #fff;
  }

  a,
  :deep(a) {
    font-size: 13px;
    color: var(--cnpa-text-secondary);
    transition: color 0.2s ease;
    text-decoration: none;

    &:hover {
      color: var(--cnpa-red);
    }
  }
}

.footer-plain {
  font-size: 13px;
  color: var(--cnpa-text-secondary);
  cursor: default;
}

.footer-bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-top: clamp(28px, 3vw, 44px);
  padding-top: 20px;
  border-top: 1px solid rgba(255, 255, 255, 0.12);
  font-size: 12px;
  color: var(--cnpa-text-secondary);
}

.footer-links {
  display: flex;
  gap: 18px;
}

@media (max-width: 1200px) {
  .footer-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .footer-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .footer-bottom {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
