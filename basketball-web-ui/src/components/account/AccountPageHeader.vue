<script setup lang="ts">
defineProps<{
  zhTitle: string
  enTitle: string
  breadcrumbItems: { label: string; to?: string }[]
}>()
</script>

<template>
  <header class="account-page-header account-page-head">
    <nav class="account-breadcrumb page-breadcrumb" aria-label="面包屑">
      <template v-for="(item, index) in breadcrumbItems" :key="`${item.label}-${index}`">
        <router-link v-if="item.to" :to="item.to">{{ item.label }}</router-link>
        <span v-else>{{ item.label }}</span>
        <span v-if="index < breadcrumbItems.length - 1" aria-hidden="true"> / </span>
      </template>
    </nav>

    <div class="account-page-title account-title">
      <h1 class="account-title-cn">{{ zhTitle }}</h1>
      <span class="account-title-en">{{ enTitle }}</span>
    </div>
  </header>
</template>

<style scoped lang="scss">
@import '@/views/account/styles/account-center.scss';
</style>
