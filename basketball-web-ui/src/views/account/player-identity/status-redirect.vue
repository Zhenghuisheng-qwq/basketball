<script setup lang="ts">
import { onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAccountStore } from '@/stores/account'
import {
  resolvePlayerIdentityRouteFromQuery,
  resolvePlayerIdentityStatusRedirect,
} from '@/utils/playerIdentityNavigate'

const route = useRoute()
const router = useRouter()
const accountStore = useAccountStore()

onMounted(async () => {
  try {
    await accountStore.loadPlayerCertification()
  } catch {
    // continue redirect
  }
  const queryStatus = route.query.status
  if (typeof queryStatus === 'string') {
    router.replace(resolvePlayerIdentityRouteFromQuery(queryStatus, accountStore))
    return
  }
  router.replace(resolvePlayerIdentityStatusRedirect(accountStore))
})
</script>

<template>
  <div class="account-view" aria-busy="true" aria-label="正在跳转球员身份状态页" />
</template>
