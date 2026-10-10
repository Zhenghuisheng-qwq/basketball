<script setup lang="ts">
import { onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'

const { t } = useI18n()
const router = useRouter()

onMounted(() => {
  router.replace({ name: 'AccountMyTeam' })
})
</script>

<template>
  <p>{{ t('accountTeam.relations.redirecting') }}</p>
</template>
