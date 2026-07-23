<script setup lang="ts">
import AccountPanel from '@/components/account/AccountPanel.vue'
import AccountStatusBadge from '@/components/account/AccountStatusBadge.vue'

defineProps<{
  title: string
  statusText: string
  statusType?: 'success' | 'warning' | 'danger' | 'default'
  description?: string
}>()
</script>

<template>
  <AccountPanel :title="title">
    <AccountStatusBadge :text="statusText" :type="statusType || 'default'" />
    <p v-if="description" class="status-card__desc">{{ description }}</p>
    <slot />
  </AccountPanel>
</template>

<style scoped lang="scss">
.status-card__desc {
  margin: 12px 0 0;
  font-size: 13px;
  color: #696969;
  line-height: 1.6;
}
</style>
