<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import logoImg from '@/assets/icon/logo.png'
import CompetitionDropdown from './CompetitionDropdown.vue'
import { useCompetitionSelector } from '@/views/events/composables/useCompetitionSelector'

const route = useRoute()
const router = useRouter()

const {
  competitionOptions,
  currentCompetitionId,
  selectCompetition,
} = useCompetitionSelector()

const hasHonorRoute = router.getRoutes().some((r) => r.path === '/honor')

const navItems = [
  { label: '数据', to: '/data' },
  {
    label: '名人堂',
    to: hasHonorRoute ? '/honor' : { path: '/home', hash: '#teams' },
    hashMatch: !hasHonorRoute,
  },
  { label: '公告', to: '/notice' },
]

const isCompetitionMenuOpen = ref(false)
const isMobileMenuOpen = ref(false)
const isMobileCompetitionOpen = ref(false)
const competitionNavRef = ref<HTMLElement | null>(null)
const competitionTriggerRef = ref<HTMLButtonElement | null>(null)
const competitionDropdownRef = ref<InstanceType<typeof CompetitionDropdown> | null>(null)

let closeTimer: ReturnType<typeof setTimeout> | null = null

const isTournamentRoute = computed(
  () => route.path === '/tournament' || route.path === '/events',
)

function isActive(item: (typeof navItems)[number]) {
  if (item.hashMatch) return route.path === '/home' && route.hash === '#teams'
  if (item.to === '/honor') return route.path === '/honor'
  if (typeof item.to === 'string') return route.path === item.to
  return false
}

function clearCloseTimer() {
  if (closeTimer) {
    clearTimeout(closeTimer)
    closeTimer = null
  }
}

function openCompetitionMenu() {
  clearCloseTimer()
  isCompetitionMenuOpen.value = true
}

function closeCompetitionMenu(focusTrigger = false) {
  clearCloseTimer()
  isCompetitionMenuOpen.value = false
  if (focusTrigger) {
    competitionTriggerRef.value?.focus()
  }
}

function toggleCompetitionMenu() {
  if (isCompetitionMenuOpen.value) {
    closeCompetitionMenu()
  } else {
    openCompetitionMenu()
  }
}

function handleCompetitionMouseEnter() {
  clearCloseTimer()
  isCompetitionMenuOpen.value = true
}

function handleCompetitionMouseLeave() {
  clearCloseTimer()
  closeTimer = setTimeout(() => {
    isCompetitionMenuOpen.value = false
  }, 150)
}

function handleSelectCompetition(competition: (typeof competitionOptions)[number]) {
  selectCompetition(competition)
  closeCompetitionMenu()
  isMobileCompetitionOpen.value = false
  isMobileMenuOpen.value = false
}

function handleDocumentPointerDown(event: PointerEvent) {
  if (!isCompetitionMenuOpen.value) return

  const target = event.target as Node | null
  if (!target) return

  const root = competitionNavRef.value
  if (root?.contains(target)) return

  closeCompetitionMenu()
}

function handleDocumentKeyDown(event: KeyboardEvent) {
  if (event.key === 'Escape') {
    if (isCompetitionMenuOpen.value) {
      event.preventDefault()
      closeCompetitionMenu(true)
      return
    }

    if (isMobileMenuOpen.value) {
      isMobileMenuOpen.value = false
      isMobileCompetitionOpen.value = false
    }
  }
}

function handleCompetitionTriggerKeyDown(event: KeyboardEvent) {
  if (event.key === 'Enter' || event.key === ' ') {
    event.preventDefault()
    toggleCompetitionMenu()
    if (isCompetitionMenuOpen.value) {
      competitionDropdownRef.value?.focusFirstItem()
    }
    return
  }

  if (event.key === 'ArrowDown') {
    event.preventDefault()
    if (!isCompetitionMenuOpen.value) {
      openCompetitionMenu()
    }
    competitionDropdownRef.value?.focusFirstItem()
  }
}

function toggleMobileMenu() {
  isMobileMenuOpen.value = !isMobileMenuOpen.value
  if (!isMobileMenuOpen.value) {
    isMobileCompetitionOpen.value = false
  }
}

function toggleMobileCompetitionMenu() {
  isMobileCompetitionOpen.value = !isMobileCompetitionOpen.value
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

function closeMobileMenu() {
  isMobileMenuOpen.value = false
  isMobileCompetitionOpen.value = false
}

onMounted(() => {
  document.addEventListener('pointerdown', handleDocumentPointerDown)
  document.addEventListener('keydown', handleDocumentKeyDown)
})

onBeforeUnmount(() => {
  clearCloseTimer()
  document.removeEventListener('pointerdown', handleDocumentPointerDown)
  document.removeEventListener('keydown', handleDocumentKeyDown)
})
</script>

<template>
  <header class="web-header">
    <div class="site-shell web-header-inner">
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
            to="/home"
            class="nav-link"
            active-class=""
            exact-active-class=""
            :class="{ 'nav-link-active': route.path === '/home' && !route.hash }"
          >
            首页
          </router-link>

          <div
            ref="competitionNavRef"
            class="site-nav__item site-nav__item--competitions"
            @mouseenter="handleCompetitionMouseEnter"
            @mouseleave="handleCompetitionMouseLeave"
          >
            <button
              ref="competitionTriggerRef"
              type="button"
              class="nav-link nav-link--button"
              :class="{ 'nav-link-active': isTournamentRoute }"
              aria-haspopup="menu"
              :aria-expanded="isCompetitionMenuOpen"
              @click="toggleCompetitionMenu"
              @keydown="handleCompetitionTriggerKeyDown"
            >
              赛事
            </button>

            <Transition name="competition-menu">
              <div
                v-if="isCompetitionMenuOpen"
                class="competition-dropdown-anchor"
                @mouseenter="handleCompetitionMouseEnter"
                @mouseleave="handleCompetitionMouseLeave"
              >
                <CompetitionDropdown
                  ref="competitionDropdownRef"
                  :competitions="competitionOptions"
                  :current-competition-id="currentCompetitionId"
                  @select="handleSelectCompetition"
                />
              </div>
            </Transition>
          </div>

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
        <button
          type="button"
          class="mobile-menu-btn"
          aria-label="打开导航菜单"
          :aria-expanded="isMobileMenuOpen"
          @click="toggleMobileMenu"
        >
          <span />
          <span />
          <span />
        </button>
      </div>
    </div>

    <Transition name="mobile-nav">
      <nav
        v-if="isMobileMenuOpen"
        class="mobile-nav"
        aria-label="移动端主导航"
      >
        <router-link
          to="/home"
          class="mobile-nav__link"
          @click="closeMobileMenu"
        >
          首页
        </router-link>

        <div class="mobile-nav__group">
          <button
            type="button"
            class="mobile-nav__link mobile-nav__link--toggle"
            :class="{ 'mobile-nav__link--active': isTournamentRoute }"
            :aria-expanded="isMobileCompetitionOpen"
            @click="toggleMobileCompetitionMenu"
          >
            赛事
          </button>

          <CompetitionDropdown
            v-if="isMobileCompetitionOpen"
            mobile
            :competitions="competitionOptions"
            :current-competition-id="currentCompetitionId"
            @select="handleSelectCompetition"
          />
        </div>

        <router-link
          v-for="item in navItems"
          :key="item.label"
          :to="item.to"
          class="mobile-nav__link"
          :class="{ 'mobile-nav__link--active': isActive(item) }"
          @click="closeMobileMenu"
        >
          {{ item.label }}
        </router-link>
      </nav>
    </Transition>
  </header>
</template>

<style scoped lang="scss">
.web-header {
  position: sticky;
  top: 0;
  z-index: 100;
  width: 100%;
  min-height: 72px;
  overflow: visible;
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
  height: 100%;
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

.site-nav__item--competitions {
  position: relative;
  height: 72px;
  display: flex;
  align-items: center;
}

.competition-dropdown-anchor {
  position: absolute;
  top: calc(100% + 1px);
  left: 0;
  z-index: 1200;
}

.competition-menu-enter-active,
.competition-menu-leave-active {
  transition:
    opacity 160ms ease,
    transform 160ms ease;
}

.competition-menu-enter-from,
.competition-menu-leave-to {
  opacity: 0;
  transform: translateY(-6px);
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

.nav-link--button {
  border: 0;
  background: transparent;
  cursor: pointer;
  font-family: inherit;
  padding: 0;
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

.mobile-menu-btn {
  display: none;
  width: 40px;
  height: 40px;
  padding: 0;
  border: 0;
  border-radius: 3px;
  background: rgba(255, 255, 255, 0.06);
  cursor: pointer;

  span {
    display: block;
    width: 18px;
    height: 2px;
    margin: 4px auto;
    background: #fff;
    border-radius: 999px;
  }
}

.mobile-nav {
  display: none;
  width: 100%;
  padding: 8px 0 16px;
  background: rgba(8, 8, 10, 0.98);
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.mobile-nav__link {
  display: block;
  width: 100%;
  padding: 14px clamp(16px, 3.2vw, 72px);
  color: rgba(255, 255, 255, 0.88);
  font-size: 15px;
  font-weight: 800;
  text-decoration: none;
  text-align: left;
  background: transparent;
  border: 0;
  cursor: pointer;
  font-family: inherit;

  &--active {
    color: var(--cnpa-red);
  }
}

.mobile-nav__group {
  width: 100%;
}

.mobile-nav-enter-active,
.mobile-nav-leave-active {
  transition: opacity 160ms ease;
}

.mobile-nav-enter-from,
.mobile-nav-leave-to {
  opacity: 0;
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
    min-height: 64px;
  }

  .web-header-inner {
    padding: 0;
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

  .mobile-menu-btn {
    display: block;
  }

  .mobile-nav {
    display: block;
  }
}
</style>
