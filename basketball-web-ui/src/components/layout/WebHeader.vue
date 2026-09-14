<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import logoImg from '@/assets/icon/logo.png'
import PublicCompetitionMegaMenu from './PublicCompetitionMegaMenu.vue'
import PublicCompetitionMobileMenu from './PublicCompetitionMobileMenu.vue'
import { usePublicCompetitionMegaMenu } from '@/composables/usePublicCompetitionMegaMenu'
import type { MegaMenuType } from '@/constants/publicCompetitionMenu'
import { useAuth } from '@/composables/useAuth'
import { goToPlayerDetail } from '@/utils/playerNavigate'
import { getAvatarInitials } from '@/utils/accountAvatar'

const route = useRoute()
const router = useRouter()
const { isAuthenticated, currentUser, unreadNotificationCount } = useAuth()

const {
  isOpen: isMegaMenuOpen,
  openMenu,
  currentGroups,
  isEventsNavActive,
  isDataNavActive,
  closeMenu,
  clearCloseTimer,
  handleMouseEnter,
  handleMouseLeave,
  toggleMenu,
  navigate: navigateMegaMenuEntry,
  getEntryHighlight,
} = usePublicCompetitionMegaMenu()

const navItems = [
  {
    label: '名人堂',
    to: '/hall-of-fame',
  },
  { label: '公告', to: '/announcements' },
  { label: '合作伙伴', to: '/partners' },
]

const isMobileMenuOpen = ref(false)
const isSearchOpen = ref(false)
const searchQuery = ref('')
const searchPanelRef = ref<HTMLElement | null>(null)
const searchInputRef = ref<HTMLInputElement | null>(null)
const megaMenuNavRef = ref<HTMLElement | null>(null)
const eventsTriggerRef = ref<HTMLButtonElement | null>(null)
const megaMenuRef = ref<InstanceType<typeof PublicCompetitionMegaMenu> | null>(null)

const megaMenuAnchorLeft = ref(0)

function updateMegaMenuAnchorPosition() {
  if (!isMegaMenuOpen.value || !megaMenuNavRef.value || !eventsTriggerRef.value) {
    return
  }

  const navRect = megaMenuNavRef.value.getBoundingClientRect()
  const triggerRect = eventsTriggerRef.value.getBoundingClientRect()
  megaMenuAnchorLeft.value = Math.max(0, triggerRect.left - navRect.left)
}

watch([isMegaMenuOpen, openMenu], () => {
  window.requestAnimationFrame(updateMegaMenuAnchorPosition)
}, { flush: 'post' })

const searchResults = computed(() => [] as Array<{
  id: string
  displayName: string
  currentTeamAbbr: string
  position: string
}>)
const searchUnavailableMessage = '搜索 API 尚未开放'

const accountLabel = computed(() => currentUser.value?.nickname || '个人中心')
const accountInitials = computed(() => getAvatarInitials(currentUser.value?.nickname))
const showAccountInitials = computed(() => Boolean(accountInitials.value))

function isActive(item: (typeof navItems)[number]) {
  if (item.to === '/announcements') {
    return route.path.startsWith('/announcements')
  }
  if (item.to === '/partners') {
    return route.path.startsWith('/partners')
  }
  if (typeof item.to === 'string') return route.path === item.to
  return false
}

function handleMegaMenuMouseEnter(type: MegaMenuType) {
  handleMouseEnter(type)
}

function handleMegaMenuMouseLeave() {
  handleMouseLeave()
}

function handleOtherNavMouseEnter() {
  closeMenu()
}

function handleMegaMenuToggle(type: MegaMenuType) {
  toggleMenu(type)
  if (isMegaMenuOpen.value) {
    megaMenuRef.value?.focusFirstItem()
  }
}

function handleMegaMenuTriggerKeyDown(event: KeyboardEvent, type: MegaMenuType) {
  if (event.key === 'Enter' || event.key === ' ') {
    event.preventDefault()
    handleMegaMenuToggle(type)
    return
  }

  if (event.key === 'ArrowDown') {
    event.preventDefault()
    if (!isMegaMenuOpen.value) {
      handleMouseEnter(type)
    }
    megaMenuRef.value?.focusFirstItem()
  }
}

function closeMegaMenu(focusTrigger = false) {
  closeMenu()
  if (focusTrigger) {
    eventsTriggerRef.value?.focus()
  }
}

function handleDocumentPointerDown(event: PointerEvent) {
  const target = event.target as Node | null
  if (!target) return

  if (isMegaMenuOpen.value) {
    const root = megaMenuNavRef.value
    if (!root?.contains(target)) {
      closeMegaMenu()
    }
  }

  if (isSearchOpen.value) {
    const searchRoot = searchPanelRef.value
    if (!searchRoot?.contains(target)) {
      closeSearch()
    }
  }
}

function handleDocumentKeyDown(event: KeyboardEvent) {
  if (event.key === 'Escape') {
    if (isSearchOpen.value) {
      event.preventDefault()
      closeSearch(true)
      return
    }

    if (isMegaMenuOpen.value) {
      event.preventDefault()
      closeMegaMenu(true)
      return
    }

    if (isMobileMenuOpen.value) {
      isMobileMenuOpen.value = false
    }
  }
}

function handleEventsTriggerKeyDown(event: KeyboardEvent) {
  handleMegaMenuTriggerKeyDown(event, 'event')
}

function toggleMobileMenu() {
  isMobileMenuOpen.value = !isMobileMenuOpen.value
}

function goHome() {
  router.push('/home')
}

function goLogin() {
  router.push('/login')
}

function goRegister() {
  router.push('/register')
}

function goAccount() {
  router.push('/account')
}

function closeMobileMenu() {
  isMobileMenuOpen.value = false
}

function handleMobileNavigate(entry: Parameters<typeof navigateMegaMenuEntry>[0]) {
  navigateMegaMenuEntry(entry)
  closeMobileMenu()
}

function openSearch() {
  isSearchOpen.value = true
  window.setTimeout(() => searchInputRef.value?.focus(), 0)
}

function closeSearch(focusTrigger = false) {
  isSearchOpen.value = false
  searchQuery.value = ''
  if (focusTrigger) {
    document.querySelector<HTMLButtonElement>('.search-btn')?.focus()
  }
}

function toggleSearch() {
  if (isSearchOpen.value) {
    closeSearch()
  } else {
    openSearch()
  }
}

const isScrolled = ref(false)

function handleWindowScroll() {
  isScrolled.value = window.scrollY > 20
}

function handleSearchSelect(playerId: string) {
  goToPlayerDetail(router, playerId, 'overview')
  closeSearch()
  closeMobileMenu()
}

onMounted(() => {
  handleWindowScroll()
  window.addEventListener('scroll', handleWindowScroll, { passive: true })
  window.addEventListener('resize', updateMegaMenuAnchorPosition)
  document.addEventListener('pointerdown', handleDocumentPointerDown)
  document.addEventListener('keydown', handleDocumentKeyDown)
})

onBeforeUnmount(() => {
  clearCloseTimer()
  window.removeEventListener('scroll', handleWindowScroll)
  window.removeEventListener('resize', updateMegaMenuAnchorPosition)
  document.removeEventListener('pointerdown', handleDocumentPointerDown)
  document.removeEventListener('keydown', handleDocumentKeyDown)
})
</script>

<template>
  <header class="site-header" :class="{ 'is-scrolled': isScrolled }">
    <div class="site-shell site-header__inner">
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
            @mouseenter="handleOtherNavMouseEnter"
          >
            首页
          </router-link>

          <div
            ref="megaMenuNavRef"
            class="site-nav__mega"
            @mouseenter="clearCloseTimer()"
            @mouseleave="handleMegaMenuMouseLeave()"
          >
            <button
              ref="eventsTriggerRef"
              type="button"
              class="nav-link nav-link--button"
              :class="{ 'nav-link-active': isEventsNavActive || (isMegaMenuOpen && openMenu === 'event') }"
              aria-haspopup="dialog"
              :aria-expanded="isMegaMenuOpen && openMenu === 'event'"
              @mouseenter="handleMegaMenuMouseEnter('event')"
              @click="handleMegaMenuToggle('event')"
              @keydown="handleEventsTriggerKeyDown"
            >
              赛事
            </button>

            <Transition name="mega-menu">
              <div
                v-if="isMegaMenuOpen"
                class="competition-mega-menu-anchor"
                :style="{ left: `${megaMenuAnchorLeft}px` }"
                @mouseenter="clearCloseTimer()"
                @mouseleave="handleMegaMenuMouseLeave()"
              >
                <PublicCompetitionMegaMenu
                  ref="megaMenuRef"
                  type="event"
                  ariaLabel="赛事导航"
                  :groups="currentGroups"
                  :get-entry-highlight="getEntryHighlight"
                  @navigate="navigateMegaMenuEntry"
                />
              </div>
            </Transition>
          </div>

          <router-link
            to="/data"
            class="nav-link"
            active-class=""
            exact-active-class=""
            :class="{ 'nav-link-active': isDataNavActive }"
            @mouseenter="handleOtherNavMouseEnter"
          >
            数据
          </router-link>

          <router-link
            v-for="item in navItems"
            :key="item.label"
            :to="item.to"
            class="nav-link"
            active-class=""
            exact-active-class=""
            :class="{ 'nav-link-active': isActive(item) }"
            @mouseenter="handleOtherNavMouseEnter"
          >
            {{ item.label }}
          </router-link>
        </nav>
      </div>

      <div class="header-actions">
        <div ref="searchPanelRef" class="header-search">
          <button
            type="button"
            class="search-btn"
            aria-label="搜索"
            :aria-expanded="isSearchOpen"
            @click="toggleSearch"
          >
            <svg viewBox="0 0 24 24" width="20" height="20" aria-hidden="true">
              <circle cx="11" cy="11" r="7" fill="none" stroke="currentColor" stroke-width="2" />
              <path d="M20 20l-4-4" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" />
            </svg>
          </button>

          <div v-if="isSearchOpen" class="header-search__panel">
            <label class="header-search__field">
              <span class="header-search__icon" aria-hidden="true">⌕</span>
              <input
                ref="searchInputRef"
                v-model="searchQuery"
                type="search"
                placeholder="搜索球员"
                autocomplete="off"
              />
            </label>

            <ul v-if="searchQuery.trim()" class="header-search__results">
              <li v-if="searchResults.length === 0" class="header-search__empty">
                {{ searchUnavailableMessage }}
              </li>
              <li v-for="player in searchResults" :key="player.id">
                <button type="button" class="header-search__item" @click="handleSearchSelect(player.id)">
                  <span class="header-search__type">PLAYER</span>
                  <span class="header-search__name">{{ player.displayName }}</span>
                  <span class="header-search__meta">{{ player.currentTeamAbbr }} · {{ player.position }}</span>
                </button>
              </li>
            </ul>
          </div>
        </div>
        <button
          v-if="isAuthenticated"
          type="button"
          class="account-btn"
          @click="goAccount"
        >
          <span class="account-btn__avatar" aria-hidden="true">
            <template v-if="showAccountInitials">{{ accountInitials }}</template>
            <svg v-else class="account-btn__avatar-icon" viewBox="0 0 24 24">
              <path
                fill="currentColor"
                d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"
              />
            </svg>
          </span>
          <span class="account-btn__label">个人中心</span>
          <span v-if="unreadNotificationCount > 0" class="account-btn__badge">
            {{ unreadNotificationCount > 99 ? '99+' : unreadNotificationCount }}
          </span>
        </button>
        <template v-else>
          <button type="button" class="login-btn" @click="goLogin">登录</button>
          <button type="button" class="register-btn" @click="goRegister">注册</button>
        </template>
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

        <PublicCompetitionMobileMenu
          :is-events-nav-active="isEventsNavActive"
          :get-entry-highlight="getEntryHighlight"
          @navigate="handleMobileNavigate"
          @close="closeMobileMenu"
        />

        <router-link
          to="/data"
          class="mobile-nav__link"
          :class="{ 'mobile-nav__link--active': isDataNavActive }"
          @click="closeMobileMenu"
        >
          数据
        </router-link>

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

        <button
          v-if="isAuthenticated"
          type="button"
          class="mobile-nav__link mobile-nav__link--account"
          @click="goAccount(); closeMobileMenu()"
        >
          个人中心 · {{ accountLabel }}
        </button>
        <template v-else>
          <button
            type="button"
            class="mobile-nav__link mobile-nav__link--account"
            @click="goLogin(); closeMobileMenu()"
          >
            登录
          </button>
          <button
            type="button"
            class="mobile-nav__link mobile-nav__link--account"
            @click="goRegister(); closeMobileMenu()"
          >
            注册
          </button>
        </template>
      </nav>
    </Transition>
  </header>
</template>

<style scoped lang="scss">
.site-header {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 2000;
  width: 100%;
  height: var(--site-header-height);
  min-height: var(--site-header-height);
  overflow: visible;
  background: rgba(7, 8, 10, 0.96);
  border-top: 2px solid var(--cnpa-red, #e10613);
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(10px);
  box-shadow: none;
  transition:
    background-color 0.24s ease,
    box-shadow 0.24s ease;

  &.is-scrolled {
    background: rgba(7, 8, 10, 0.98);
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.42);
  }
}

.site-header__inner {
  height: var(--site-header-height);
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
  height: var(--site-header-height);
  min-width: 0;
}

.site-nav__mega {
  position: relative;
  display: flex;
  align-items: center;
  height: var(--site-header-height);
  gap: clamp(34px, 3.4vw, 72px);
}

.competition-mega-menu-anchor {
  position: absolute;
  top: calc(100% + 1px);
  left: 0;
  z-index: 2100;
  transition: left 160ms ease;
}

.site-nav__item--competitions {
  position: relative;
  height: var(--site-header-height);
  display: flex;
  align-items: center;
}

.competition-dropdown-anchor {
  position: absolute;
  top: calc(100% + 1px);
  left: 0;
  z-index: 2100;
}

.nav-link {
  position: relative;
  height: var(--site-header-height);
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

.header-search {
  position: relative;
}

.header-search__panel {
  position: absolute;
  top: calc(100% + 10px);
  right: 0;
  width: min(360px, calc(100vw - 32px));
  padding: 12px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 4px;
  background: #111;
  box-shadow: 0 18px 40px rgba(0, 0, 0, 0.45);
  z-index: 2100;
}

.header-search__field {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 12px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  background: rgba(255, 255, 255, 0.04);
}

.header-search__field input {
  width: 100%;
  height: 40px;
  border: 0;
  background: transparent;
  color: #fff;
  font: inherit;
  outline: none;
}

.header-search__icon {
  color: rgba(255, 255, 255, 0.56);
}

.header-search__results {
  margin: 10px 0 0;
  padding: 0;
  list-style: none;
  max-height: 280px;
  overflow-y: auto;
}

.header-search__empty {
  padding: 12px;
  color: rgba(255, 255, 255, 0.56);
  font-size: 13px;
}

.header-search__item {
  width: 100%;
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 4px 10px;
  padding: 10px 12px;
  border: 0;
  background: transparent;
  color: #fff;
  text-align: left;
  cursor: pointer;
  font: inherit;

  &:hover {
    background: rgba(225, 6, 19, 0.12);
  }
}

.header-search__type {
  grid-row: span 2;
  align-self: center;
  padding: 2px 6px;
  border: 1px solid rgba(225, 6, 19, 0.65);
  color: #ff4d57;
  font-size: 10px;
  letter-spacing: 0.08em;
}

.header-search__name {
  font-weight: 700;
}

.header-search__meta {
  color: rgba(255, 255, 255, 0.56);
  font-size: 12px;
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
.register-btn:hover,
.account-btn:hover {
  transform: translateY(-1px);
  filter: brightness(1.08);
}

.account-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  height: 44px;
  min-width: 92px;
  padding: 0 20px;
  border-radius: 3px;
  border: 1px solid rgba(255, 255, 255, 0.16);
  background: rgba(255, 255, 255, 0.06);
  color: #fff;
  font-size: 15px;
  font-weight: 900;
  cursor: pointer;
  font-family: inherit;
  transition: all 0.2s ease;
}

.account-btn__avatar {
  display: grid;
  place-items: center;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: rgba(225, 6, 19, 0.28);
  font-size: 10px;
  font-weight: 900;
}

.account-btn__avatar-icon {
  width: 14px;
  height: 14px;
}

.account-btn__label {
  white-space: nowrap;
}

.account-btn__badge {
  min-width: 18px;
  padding: 2px 6px;
  border-radius: 10px;
  background: var(--cnpa-red, #e10613);
  color: #fff;
  font-size: 11px;
  font-weight: 900;
  line-height: 1.2;
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
  position: relative;
  z-index: 2100;
  width: 100%;
  padding: 8px 0 16px;
  background: rgba(8, 8, 10, 0.98);
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.mobile-nav__link {
  display: block;
  width: 100%;
  padding: 14px var(--site-margin-inline);
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

.mega-menu-enter-active,
.mega-menu-leave-active {
  transition:
    opacity 160ms ease,
    transform 160ms ease;
}

.mega-menu-enter-from,
.mega-menu-leave-to {
  opacity: 0;
  transform: translateY(-6px);
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
  .register-btn,
  .account-btn {
    min-width: 78px;
    padding: 0 18px;
  }
}

@media (max-width: 900px) {
  .site-header__inner {
    padding: 0;
  }

  .logo-image {
    width: 108px;
    height: 36px;
  }

  .header-nav {
    display: none;
  }

  .competition-mega-menu-anchor {
    display: none;
  }

  .header-actions {
    margin-left: auto;
    gap: 10px;
  }

  .login-btn,
  .register-btn,
  .account-btn {
    height: 38px;
    min-width: 66px;
    padding: 0 14px;
    font-size: 13px;
  }

  .account-btn__label {
    display: none;
  }

  .mobile-menu-btn {
    display: block;
  }

  .mobile-nav {
    display: block;
  }
}
</style>
