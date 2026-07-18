<script setup lang="ts">
import { useRoute, useRouter } from 'vue-router'
import logoImg from '@/assets/icon/logo.png'

const route = useRoute()
const router = useRouter()

const hasHonorRoute = router.getRoutes().some((r) => r.path === '/honor')

const navItems = [
  { label: '首页', to: '/home', exact: true },
  { label: '赛事', to: '/tournament' },
  { label: '数据', to: '/data' },
  {
    label: '名人堂',
    to: hasHonorRoute ? '/honor' : { path: '/home', hash: '#teams' },
    hashMatch: !hasHonorRoute,
  },
  { label: '公告', to: '/notice' },
]

function isActive(item: (typeof navItems)[number]) {
  if (item.exact) return route.path === '/home' && !route.hash
  if (item.hashMatch) return route.path === '/home' && route.hash === '#teams'
  if (item.to === '/honor') return route.path === '/honor'
  if (typeof item.to === 'string') return route.path === item.to
  return false
}

function goHome() {
  router.push('/home')
}

function goLogin() {
  router.push('/login')
}

function goRegister() {
  const hasRegister = router.getRoutes().some((r) => r.path === '/register')
  router.push(hasRegister ? '/register' : '/login')
}
</script>

<template>
  <header class="web-header">
    <div class="web-header-inner">
      <div class="header-left">
        <div
          class="header-logo"
          role="button"
          tabindex="0"
          @click="goHome"
          @keyup.enter="goHome"
        >
          <img class="logo-image" :src="logoImg" alt="CNPA" />
        </div>

        <nav class="header-nav" aria-label="主导航">
          <router-link
            v-for="item in navItems"
            :key="item.label"
            :to="item.to"
            class="nav-link"
            active-class=""
            exact-active-class=""
            :class="{ 'nav-link-active': isActive(item) }"
          >
            {{ item.label }}
          </router-link>
        </nav>
      </div>

      <div class="header-actions">
        <button type="button" class="search-btn" aria-label="搜索">
          <svg viewBox="0 0 24 24" width="20" height="20" aria-hidden="true">
            <circle cx="11" cy="11" r="7" fill="none" stroke="currentColor" stroke-width="2" />
            <path d="M20 20l-4-4" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" />
          </svg>
        </button>
        <button type="button" class="login-btn" @click="goLogin">登录</button>
        <button type="button" class="register-btn" @click="goRegister">注册</button>
      </div>
    </div>
  </header>
</template>

<style scoped lang="scss">
.web-header {
  position: sticky;
  top: 0;
  z-index: 100;
  width: 100%;
  height: 72px;
  background:
    linear-gradient(
      90deg,
      rgba(14, 22, 25, 0.94) 0%,
      rgba(8, 8, 10, 0.96) 42%,
      rgba(36, 18, 34, 0.92) 100%
    );
  border-top: 2px solid #ff2d19;
  border-bottom: 1px solid rgba(255, 255, 255, 0.12);
  backdrop-filter: blur(14px);
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.36);
}

.web-header-inner {
  width: min(calc(100% - var(--site-gutter) * 2), var(--site-width));
  height: 100%;
  margin: 0 auto;
  padding: 0;
  box-sizing: border-box;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.header-left {
  display: flex;
  align-items: center;
  min-width: 0;
}

.header-logo {
  flex: 0 0 auto;
  display: flex;
  align-items: center;
  cursor: pointer;
  user-select: none;
  margin-right: clamp(42px, 4.5vw, 84px);
}

.logo-image {
  display: block;
  width: clamp(116px, 8vw, 152px);
  height: 42px;
  object-fit: contain;
  object-position: left center;
}

.header-nav {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  gap: clamp(34px, 3.4vw, 72px);
  height: 72px;
  min-width: 0;
}

.nav-link {
  position: relative;
  height: 72px;
  display: flex;
  align-items: center;
  color: rgba(255, 255, 255, 0.88);
  font-size: 16px;
  font-weight: 900;
  text-decoration: none;
  white-space: nowrap;
  transition: color 0.2s ease, text-shadow 0.2s ease;

  &:hover {
    color: #fff;
    text-shadow: 0 0 12px rgba(255, 255, 255, 0.55);
  }

  &.nav-link-active {
    color: var(--cnpa-red);

    &::after {
      content: '';
      position: absolute;
      left: 50%;
      bottom: 12px;
      width: 28px;
      height: 3px;
      border-radius: 999px;
      background: var(--cnpa-red);
      transform: translateX(-50%);
    }
  }
}

.header-actions {
  flex: 0 0 auto;
  display: flex;
  align-items: center;
  gap: 16px;
  margin-left: 32px;
}

.search-btn {
  width: 40px;
  height: 40px;
  display: grid;
  place-items: center;
  border: none;
  border-radius: 3px;
  background: transparent;
  color: rgba(255, 255, 255, 0.82);
  cursor: pointer;
  transition: color 0.2s ease, background 0.2s ease;

  &:hover {
    color: #fff;
    background: rgba(255, 255, 255, 0.06);
  }
}

.login-btn,
.register-btn {
  height: 44px;
  min-width: 92px;
  padding: 0 26px;
  border-radius: 3px;
  font-size: 15px;
  font-weight: 900;
  cursor: pointer;
  font-family: inherit;
  transition: all 0.2s ease;
}

.login-btn {
  border: 1px solid rgba(255, 255, 255, 0.28);
  background: rgba(0, 0, 0, 0.28);
  color: #fff;
}

.register-btn {
  border: 1px solid #ff321f;
  background: linear-gradient(180deg, #ff3b25 0%, #e5160c 100%);
  color: #fff;
  box-shadow: 0 0 18px rgba(255, 45, 25, 0.28);
}

.login-btn:hover,
.register-btn:hover {
  transform: translateY(-1px);
  filter: brightness(1.08);
}

@media (max-width: 1200px) {
  .header-logo {
    margin-right: 40px;
  }

  .header-nav {
    gap: 34px;
  }

  .logo-image {
    width: 118px;
  }

  .login-btn,
  .register-btn {
    min-width: 78px;
    padding: 0 18px;
  }
}

@media (max-width: 900px) {
  .web-header {
    height: 64px;
  }

  .web-header-inner {
    padding: 0 16px;
  }

  .logo-image {
    width: 108px;
    height: 36px;
  }

  .header-nav {
    display: none;
  }

  .header-actions {
    margin-left: auto;
    gap: 10px;
  }

  .login-btn,
  .register-btn {
    height: 38px;
    min-width: 66px;
    padding: 0 14px;
    font-size: 13px;
  }
}
</style>
