<template>
  <div class="app-container">
    <el-alert
      title="首页大图在「首页横幅」里改。这里改 League、Arena、Cup、Open 四个赛事系列头图，保存即上架，不用先发布赛季。没有上架时，赛事页用自带的图。球队页和球员页不使用这些头图。"
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
  { key: 'EVENT_LEAGUE', label: 'League' },
  { key: 'EVENT_ARENA', label: 'Arena' },
  { key: 'EVENT_CUP', label: 'Cup' },
  { key: 'EVENT_OPEN', label: 'Open' },
  { key: 'DATA', label: '数据中心' },
  { key: 'HALL', label: '名人堂' },
  { key: 'ANNOUNCEMENT', label: '公告' },
  { key: 'PARTNER', label: '合作伙伴' }
]
const activeKey = ref('EVENT_LEAGUE')
</script>

<style scoped>
.mb8 {
  margin-bottom: 8px;
}
</style>
