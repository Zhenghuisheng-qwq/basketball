<script setup lang="ts">
import WebHeader from '@/components/layout/WebHeader.vue'
import loginBackground from '@/assets/icon/login/login_background.png'
</script>

<template>
  <div class="account-layout">
    <WebHeader />

    <div class="account-layout__body site-main">
      <div class="site-shell account-layout__shell">
        <div
          class="account-layout__panel"
          :style="{ backgroundImage: `url(${loginBackground})` }"
        >
          <aside class="account-layout__visual" aria-hidden="true" />

          <main class="account-layout__main">
            <div class="account-layout__form-wrap">
              <router-view />
            </div>
          </main>
        </div>
      </div>
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
  width: 100%;
  min-height: calc(100vh - var(--site-header-height) - 56px);
}

.account-layout__shell {
  min-height: inherit;
}

.account-layout__panel {
  display: grid;
  grid-template-columns: 58% 42%;
  min-height: calc(100vh - var(--site-header-height) - 56px);
  overflow: hidden;
  background-color: #050505;
  background-repeat: no-repeat;
  background-size: cover;
  background-position: center;
}

.account-layout__visual {
  min-width: 0;
}

.account-layout__main {
  position: relative;
  min-width: 0;
  background: transparent;
  color: var(--cnpa-paper-text);
}

.account-layout__form-wrap {
  display: flex;
  align-items: center;
  min-height: 100%;
  padding: clamp(28px, 3.5vw, 48px) clamp(20px, 3vw, 40px);
  box-sizing: border-box;
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
  .account-layout__panel {
    grid-template-columns: 1fr;
    min-height: auto;
    background-position: center top;
  }

  .account-layout__body {
    min-height: auto;
  }

  .account-layout__visual {
    min-height: clamp(180px, 34vw, 260px);
  }

  .account-layout__main {
    background-color: var(--cnpa-paper-bg);
    background-image: linear-gradient(
      180deg,
      #ebe9e9 0%,
      var(--cnpa-paper-bg) 45%,
      #e7e5e5 100%
    );
  }

  .account-layout__form-wrap {
    align-items: flex-start;
    padding: 28px 20px 36px;
  }
}

@media (max-width: 767px) {
  .account-layout__visual {
    min-height: clamp(160px, 42vw, 220px);
  }

  .account-layout__form-wrap {
    padding-inline: 18px;
  }
}
</style>
