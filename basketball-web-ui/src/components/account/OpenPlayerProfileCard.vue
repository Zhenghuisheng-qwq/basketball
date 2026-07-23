<script setup lang="ts">
import { computed } from 'vue'
import AccountStatusBadge from '@/components/account/AccountStatusBadge.vue'
import type { CertifiedPlayerForOpen } from '@/types/account'

const props = defineProps<{
  player: CertifiedPlayerForOpen
  readonly?: boolean
}>()

const showAvatar = computed(() => Boolean(props.player.avatarUrl?.trim()))
</script>

<template>
  <div class="open-player-profile">
    <div class="open-player-profile__avatar">
      <img v-if="showAvatar" :src="player.avatarUrl" :alt="player.nickname" />
      <svg v-else viewBox="0 0 24 24" aria-hidden="true">
        <path fill="currentColor" d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z" />
      </svg>
    </div>
    <div class="open-player-profile__body">
      <strong>{{ player.nickname }}</strong>
      <AccountStatusBadge type="success" :text="player.identityStatusText" />
      <div class="account-info-list">
        <div class="account-info-row">
          <span class="account-info-row__label">永久球员 ID</span>
          <span>{{ player.playerNo }}</span>
        </div>
        <div class="account-info-row">
          <span class="account-info-row__label">游戏平台</span>
          <span>{{ player.platform }}</span>
        </div>
        <div class="account-info-row">
          <span class="account-info-row__label">游戏 ID</span>
          <span>{{ player.gameId }}</span>
        </div>
      </div>
      <p v-if="!readonly" class="open-player-profile__hint">
        报名资料来自已认证球员身份；如需修改平台或游戏 ID，请前往
        <RouterLink to="/account/player-identity/platforms">平台与游戏 ID 管理</RouterLink>。
      </p>
    </div>
  </div>
</template>

<style scoped lang="scss">
@import '@/views/account/styles/account-center.scss';
</style>
