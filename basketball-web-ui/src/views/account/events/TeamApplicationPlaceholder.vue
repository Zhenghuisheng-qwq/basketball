<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

const router = useRouter()

onMounted(() => {
  ElMessage.info('球队报名组队页将在后续版本开放')
  router.replace('/account/my-events?tab=applications')
})
</script>

<template>
  <div class="account-view" aria-busy="true" />
</template>
