<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  percent: number
  items: { key: string; label: string; done: boolean; weight: number }[]
}>()

const displayPercent = computed(() => `${props.percent}%`)
</script>

<template>
  <div class="profile-completeness">
    <div class="profile-completeness__ring" :style="{ '--percent': percent }">
      <span>{{ displayPercent }}</span>
    </div>
    <p class="profile-completeness__title">资料完整度</p>
    <ul class="profile-completeness__list">
      <li v-for="item in items" :key="item.key" :class="{ 'profile-completeness__item--done': item.done }">
        <span class="profile-completeness__mark" aria-hidden="true">{{ item.done ? '✓' : '' }}</span>
        <span>{{ item.label }}（{{ item.weight }}%）</span>
      </li>
    </ul>
  </div>
</template>

<style scoped lang="scss">
.profile-completeness {
  text-align: center;
}

.profile-completeness__ring {
  --percent: 0;
  width: 120px;
  height: 120px;
  margin: 0 auto 12px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  background:
    radial-gradient(circle at center, var(--cnpa-paper-bg) 58%, transparent 59%),
    conic-gradient(var(--account-red, #d5080d) calc(var(--percent) * 1%), rgba(20, 20, 20, 0.08) 0);
  font-size: 24px;
  font-weight: 900;
}

.profile-completeness__title {
  margin: 0 0 14px;
  font-size: 15px;
  font-weight: 900;
}

.profile-completeness__list {
  list-style: none;
  margin: 0;
  padding: 0;
  text-align: left;
}

.profile-completeness__list li {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 0;
  font-size: 13px;
  color: #696969;
}

.profile-completeness__item--done {
  color: var(--account-text);
  font-weight: 700;
}

.profile-completeness__mark {
  width: 16px;
  height: 16px;
  border-radius: 50%;
  border: 1px solid rgba(20, 20, 20, 0.25);
  display: grid;
  place-items: center;
  font-size: 10px;
  flex-shrink: 0;
}

.profile-completeness__item--done .profile-completeness__mark {
  border-color: var(--account-red);
  background: var(--account-red);
  color: #fff;
}
</style>