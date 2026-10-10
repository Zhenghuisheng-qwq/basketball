<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import logoImg from '@/assets/icon/logo.png'

const { t } = useI18n()

const hallLinks = [
  { key: 'weekly', labelKey: 'nav.footer.weekly', query: { tab: 'weekly' } },
  { key: 'event', labelKey: 'nav.footer.eventHonors', query: { tab: 'event' } },
  { key: 'champions', labelKey: 'nav.footer.champions', query: { tab: 'champions' } },
  { key: 'lineup', labelKey: 'nav.footer.bestLineup', query: { tab: 'event' } },
]

const seriesLinks = [
  { key: 'league', label: 'CNPA League', to: { path: '/tournament', query: { series: 'LEAGUE' } } },
  { key: 'arena', label: 'CNPA Arena', to: { path: '/tournament', query: { series: 'ARENA' } } },
  { key: 'cup', label: 'CNPA Cup', to: { path: '/tournament', query: { series: 'CUP' } } },
  { key: 'open', label: 'CNPA Open', to: { path: '/tournament', query: { series: 'OPEN' } } },
]
</script>

<template>
  <footer class="web-footer">
    <div class="footer-shell site-shell">
      <div class="footer-grid">
        <div class="footer-brand">
          <img :src="logoImg" alt="CNPA" class="footer-logo" />
          <p class="footer-brand__text">{{ t('nav.footer.brand') }}</p>
        </div>

        <div class="footer-col">
          <h4>{{ t('nav.footer.events') }}</h4>
          <router-link
            v-for="item in seriesLinks"
            :key="item.key"
            :to="item.to"
          >
            {{ item.label }}
          </router-link>
        </div>

        <div class="footer-col">
          <h4>{{ t('nav.footer.data') }}</h4>
          <router-link to="/data">{{ t('nav.footer.playerData') }}</router-link>
          <router-link to="/data">{{ t('nav.footer.teamData') }}</router-link>
          <router-link to="/tournament?tab=schedule">{{ t('nav.footer.schedule') }}</router-link>
          <router-link to="/data">{{ t('nav.footer.records') }}</router-link>
        </div>

        <div class="footer-col">
          <h4>{{ t('nav.footer.hall') }}</h4>
          <router-link
            v-for="item in hallLinks"
            :key="item.key"
            :to="{ path: '/hall-of-fame', query: item.query }"
          >
            {{ t(item.labelKey) }}
          </router-link>
        </div>

        <div class="footer-col">
          <h4>{{ t('nav.footer.about') }}</h4>
          <span class="footer-plain">{{ t('nav.footer.aboutUs') }}</span>
          <router-link to="/announcements">{{ t('nav.footer.announcementCenter') }}</router-link>
          <router-link to="/partners">{{ t('nav.footer.partners') }}</router-link>
          <span class="footer-plain">{{ t('nav.footer.contact') }}</span>
        </div>
      </div>

      <div class="footer-bottom">
        <span>{{ t('nav.footer.copyright') }}</span>
        <div class="footer-links">
          <span class="footer-plain">{{ t('nav.footer.privacy') }}</span>
          <span class="footer-plain">{{ t('nav.footer.terms') }}</span>
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
    white-space: pre-line;
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
