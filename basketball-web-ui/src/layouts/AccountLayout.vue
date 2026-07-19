<script setup lang="ts">
import WebHeader from '@/components/layout/WebHeader.vue'

const featureTags = ['赛事报名', '球员档案', '数据记录']
</script>

<template>
  <div class="account-layout">
    <WebHeader />

    <div class="account-layout__body">
      <aside class="account-layout__promo" aria-label="CNPA 会员系统">
        <div class="account-layout__promo-inner">
          <p class="account-layout__eyebrow">CNPA MEMBER SYSTEM · 2026</p>
          <h1 class="account-layout__hero-title">加入 CNPA</h1>
          <p class="account-layout__hero-sub">连接赛事 · 战队 · 球员</p>

          <ul class="account-layout__tags">
            <li v-for="tag in featureTags" :key="tag">{{ tag }}</li>
          </ul>
        </div>
      </aside>

      <main class="account-layout__main">
        <div class="account-layout__edge" aria-hidden="true" />
        <div class="account-layout__form-wrap">
          <router-view />
        </div>
      </main>
    </div>

    <footer class="account-layout__footer">
      <div class="account-layout__footer-inner site-shell">
        <span>CNPA 中国职业电竞篮球联赛</span>
        <span>© 2026 CNPA. All rights reserved.</span>
      </div>
    </footer>
  </div>
</template>

<style scoped lang="scss">
.account-layout {
  min-height: 100vh;
  background: #050505;
  color: #fff;
}

.account-layout__body {
  display: grid;
  grid-template-columns: minmax(320px, 42%) 1fr;
  min-height: calc(100vh - var(--header-height) - 56px);
}

.account-layout__promo {
  position: relative;
  overflow: hidden;
  background:
    linear-gradient(115deg, rgba(225, 6, 19, 0.24) 0%, transparent 45%),
    linear-gradient(180deg, #0a0a0a 0%, #050505 100%);
}

.account-layout__promo-inner {
  display: flex;
  flex-direction: column;
  justify-content: center;
  min-height: 100%;
  padding: clamp(32px, 5vw, 72px);
}

.account-layout__eyebrow {
  margin: 0 0 18px;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.18em;
  color: rgba(255, 255, 255, 0.55);
}

.account-layout__hero-title {
  margin: 0;
  font-size: clamp(48px, 6vw, 72px);
  font-weight: 900;
  line-height: 1;
  letter-spacing: 0.04em;
  color: var(--cnpa-red);
}

.account-layout__hero-sub {
  margin: 16px 0 32px;
  font-size: 16px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.72);
}

.account-layout__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin: 0;
  padding: 0;
  list-style: none;

  li {
    padding: 8px 14px;
    border: 1px solid rgba(255, 255, 255, 0.16);
    background: rgba(255, 255, 255, 0.04);
    font-size: 13px;
    font-weight: 700;
  }
}

.account-layout__main {
  position: relative;
  background: var(--cnpa-paper-light);
  color: var(--cnpa-paper-text);
}

.account-layout__edge {
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 14px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.08) 0%, var(--cnpa-paper-light) 100%);
  clip-path: polygon(0 0, 100% 2%, 100% 98%, 0 100%);
  pointer-events: none;
}

.account-layout__form-wrap {
  min-height: 100%;
  padding: clamp(32px, 4vw, 56px) clamp(24px, 4vw, 64px);
}

.account-layout__footer {
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  background: #080808;
}

.account-layout__footer-inner {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-height: 56px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.45);
}

@media (max-width: 992px) {
  .account-layout__body {
    grid-template-columns: 1fr;
  }

  .account-layout__promo-inner {
    min-height: auto;
    padding-block: 40px;
  }

  .account-layout__edge {
    display: none;
  }
}
</style>
