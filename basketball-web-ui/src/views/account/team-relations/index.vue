<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

onMounted(() => {
  router.replace({ name: 'AccountMyTeam' })
})
</script>

<template>
  <p>正在跳转到我的球队…</p>
</template>
