<template>
  <div class="app-container">
    <el-alert
      title="这里只配置数据中心、名人堂、公告和合作伙伴的头图。赛事头图从赛季管理进入，按赛季分别发布。没有发布时，页面继续用现在的静态头图。"
      type="info"
      :closable="false"
      show-icon
      class="mb8"
    />
    <el-tabs v-model="activeKey">
      <el-tab-pane v-for="item in pages" :key="item.key" :label="item.label" :name="item.key" lazy>
        <page-hero-form :page-key="item.key" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts" name="BasketPageHero">
import PageHeroForm from './PageHeroForm.vue'

const pages = [
  { key: 'DATA', label: '数据中心' },
  { key: 'HALL', label: '名人堂' },
  { key: 'ANNOUNCEMENT', label: '公告' },
  { key: 'PARTNER', label: '合作伙伴' }
]
const activeKey = ref('DATA')
</script>

<style scoped>
.mb8 {
  margin-bottom: 8px;
}
</style>
