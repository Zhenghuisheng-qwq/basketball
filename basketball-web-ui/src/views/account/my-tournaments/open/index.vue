<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAccountStore } from '@/stores/account'

const router = useRouter()
const accountStore = useAccountStore()

onMounted(() => {
  const status = accountStore.openApplication?.status
  if (status === 'PENDING') router.replace({ name: 'AccountOpenApplicationPending' })
  else if (status === 'REJECTED') router.replace({ name: 'AccountOpenApplicationRejected' })
  else if (status === 'APPROVED') router.replace({ name: 'AccountOpenApplicationApproved' })
  else router.replace({ name: 'AccountOpenApplicationApply' })
})
</script>

<template>
  <div class="account-view" aria-busy="true" aria-label="正在跳转报名状态页" />
</template>
