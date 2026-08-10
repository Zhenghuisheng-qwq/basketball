<template>
  <div class="app-container partner-manage-page">
    <el-tabs v-model="activeTab">
      <el-tab-pane label="合作伙伴" name="partner" lazy>
        <PartnerPanel ref="partnerPanelRef" @support-changed="onSupportChanged" />
      </el-tab-pane>
      <el-tab-pane label="支持记录" name="support" lazy>
        <SupportRecordPanel ref="supportPanelRef" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts" name="BasketPartner">
import { ref } from 'vue'
import PartnerPanel from './PartnerPanel.vue'
import SupportRecordPanel from './SupportRecordPanel.vue'

const activeTab = ref('partner')
const partnerPanelRef = ref<{ getList: () => void }>()
const supportPanelRef = ref<{ getList: () => void }>()

function onSupportChanged() {
  supportPanelRef.value?.getList?.()
}
</script>
