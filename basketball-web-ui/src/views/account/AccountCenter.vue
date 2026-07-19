<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import WebHeader from '@/components/layout/WebHeader.vue'
import { getMaskedRegisterEmail } from '@/composables/useRegisterFlow'

const router = useRouter()
const activeMenu = ref('overview')
const maskedEmail = getMaskedRegisterEmail()

const menuItems = [
  { key: 'overview', label: '概览' },
  { key: 'profile', label: '个人资料' },
  { key: 'player', label: '球员认证' },
  { key: 'notice', label: '消息通知' },
  { key: 'security', label: '账号安全' },
  { key: 'logout', label: '退出登录' },
]

function handleMenuClick(key: string) {
  if (key === 'logout') {
    router.push('/home')
    return
  }
  activeMenu.value = key
}
</script>

<template>
  <div class="account-center-page">
    <WebHeader />

    <div class="site-shell account-center-page__body">
      <aside class="account-sidebar">
        <div class="account-sidebar__profile">
          <div class="account-sidebar__avatar">CN</div>
          <h2 class="account-sidebar__name">CNPA_2026</h2>
          <p class="account-sidebar__role">基础用户</p>
          <p class="account-sidebar__email">{{ maskedEmail }}</p>
        </div>

        <nav class="account-sidebar__nav">
          <button
            v-for="item in menuItems"
            :key="item.key"
            type="button"
            class="account-sidebar__nav-item"
            :class="{ active: activeMenu === item.key }"
            @click="handleMenuClick(item.key)"
          >
            {{ item.label }}
          </button>
        </nav>
      </aside>

      <main class="account-center-main">
        <nav class="account-center-main__breadcrumb" aria-label="面包屑">
          <router-link to="/home">首页</router-link>
          <span>/</span>
          <span>个人中心</span>
        </nav>

        <div class="account-center-main__head">
          <h1 class="account-center-main__title">个人中心</h1>
          <span class="account-center-main__sub">Account Center</span>
        </div>

        <section class="welcome-banner">
          <div>
            <h2>欢迎回来，CNPA_2026</h2>
            <p>邮箱已验证</p>
          </div>
          <span class="welcome-banner__badge">已验证</span>
        </section>

        <div class="account-cards">
          <article class="account-card">
            <h3>账号信息</h3>
            <p>昵称：CNPA_2026</p>
            <p>邮箱：{{ maskedEmail }}</p>
            <p>身份：基础用户</p>
          </article>

          <article class="account-card">
            <h3>球员认证</h3>
            <p>当前尚未完成球员认证。</p>
            <button type="button" class="account-card__action">申请球员认证</button>
          </article>

          <article class="account-card">
            <h3>消息通知</h3>
            <p>暂无新消息。</p>
            <button type="button" class="account-card__action">查看全部</button>
          </article>

          <article class="account-card">
            <h3>快捷操作</h3>
            <div class="account-card__actions">
              <button type="button" class="account-card__action">编辑资料</button>
              <button type="button" class="account-card__action">修改密码</button>
              <router-link to="/tournament" class="account-card__action">查看赛事</router-link>
            </div>
          </article>
        </div>
      </main>
    </div>

    <footer class="account-center-page__footer">
      <div class="account-center-page__footer-inner site-shell">
        <span>CNPA 中国职业电竞篮球联赛</span>
        <span>© 2026 CNPA. All rights reserved.</span>
      </div>
    </footer>
  </div>
</template>

<style scoped lang="scss">
.account-center-page {
  min-height: 100vh;
  background: #050505;
  color: #fff;
}

.account-center-page__body {
  display: grid;
  grid-template-columns: 280px 1fr;
  min-height: calc(100vh - var(--header-height) - 56px);
}

.account-sidebar {
  background:
    linear-gradient(180deg, rgba(225, 6, 19, 0.12) 0%, transparent 40%),
    #080808;
  border-right: 1px solid rgba(255, 255, 255, 0.08);
  padding: 32px 20px;
}

.account-sidebar__profile {
  padding-bottom: 24px;
  margin-bottom: 24px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.account-sidebar__avatar {
  width: 72px;
  height: 72px;
  display: grid;
  place-items: center;
  margin-bottom: 14px;
  border-radius: 50%;
  border: 1px solid rgba(255, 255, 255, 0.16);
  background: linear-gradient(135deg, #1a1a1a 0%, #3a3a3a 100%);
  font-size: 20px;
  font-weight: 900;
}

.account-sidebar__name {
  margin: 0 0 6px;
  font-size: 18px;
  font-weight: 900;
}

.account-sidebar__role,
.account-sidebar__email {
  margin: 0;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.62);
}

.account-sidebar__role {
  margin-bottom: 4px;
}

.account-sidebar__nav {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.account-sidebar__nav-item {
  width: 100%;
  padding: 12px 14px;
  border: none;
  border-radius: 2px;
  background: transparent;
  color: rgba(255, 255, 255, 0.72);
  text-align: left;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  font-family: inherit;
  transition: all 0.2s ease;

  &:hover {
    background: rgba(255, 255, 255, 0.06);
    color: #fff;
  }

  &.active {
    background: rgba(225, 6, 19, 0.18);
    color: #fff;
  }
}

.account-center-main {
  background: var(--cnpa-paper-light);
  color: var(--cnpa-paper-text);
  padding: clamp(28px, 3vw, 40px) clamp(24px, 3vw, 48px);
}

.account-center-main__breadcrumb {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 20px;
  font-size: 13px;
  color: var(--cnpa-paper-text-muted);

  a {
    color: inherit;
    text-decoration: none;

    &:hover {
      color: var(--cnpa-red);
    }
  }
}

.account-center-main__head {
  display: flex;
  align-items: baseline;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 24px;
}

.account-center-main__title {
  margin: 0;
  font-size: clamp(24px, 2.4vw, 30px);
  font-weight: 900;
}

.account-center-main__sub {
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: var(--cnpa-paper-text-muted);
}

.welcome-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 24px;
  padding: 20px 24px;
  border: 1px solid rgba(17, 17, 17, 0.1);
  background: rgba(255, 255, 255, 0.72);

  h2 {
    margin: 0 0 6px;
    font-size: 20px;
    font-weight: 900;
  }

  p {
    margin: 0;
    font-size: 13px;
    color: var(--cnpa-paper-text-muted);
  }
}

.welcome-banner__badge {
  padding: 6px 10px;
  border: 1px solid rgba(24, 180, 107, 0.35);
  background: rgba(24, 180, 107, 0.1);
  color: #18b46b;
  font-size: 12px;
  font-weight: 800;
}

.account-cards {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.account-card {
  padding: 20px;
  border: 1px solid rgba(17, 17, 17, 0.1);
  background: rgba(255, 255, 255, 0.72);

  h3 {
    margin: 0 0 12px;
    font-size: 16px;
    font-weight: 900;
  }

  p {
    margin: 0 0 8px;
    font-size: 13px;
    color: var(--cnpa-paper-text-muted);
  }
}

.account-card__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.account-card__action {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 34px;
  padding: 0 12px;
  border: 1px solid rgba(17, 17, 17, 0.16);
  background: #fff;
  color: var(--cnpa-paper-text);
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
  text-decoration: none;
  font-family: inherit;
  transition: all 0.2s ease;

  &:hover {
    border-color: var(--cnpa-red);
    color: var(--cnpa-red);
  }
}

.account-center-page__footer {
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  background: #080808;
}

.account-center-page__footer-inner {
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
  .account-center-page__body {
    grid-template-columns: 1fr;
  }

  .account-cards {
    grid-template-columns: 1fr;
  }
}
</style>
