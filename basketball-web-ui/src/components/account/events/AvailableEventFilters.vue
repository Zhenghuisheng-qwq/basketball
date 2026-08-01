<script setup lang="ts">
export interface MyEventsSeasonFilterValues {
  eventName: string
  seasonId: string
  platformCode: string
  status: string
  signupTimeRange: [string, string] | null
}

const props = defineProps<{
  modelValue: MyEventsSeasonFilterValues
  seasonOptions: Array<{ seasonId: string | number; seasonName: string }>
  loading?: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: MyEventsSeasonFilterValues]
  search: []
  reset: []
}>()

const platformOptions = [
  { value: 'ALL', label: '全部' },
  { value: 'PC', label: 'PC' },
  { value: 'PS', label: 'PS' },
  { value: 'XBOX', label: 'Xbox' },
]

const statusOptions = [
  { value: '', label: '全部' },
  { value: 'NOT_STARTED', label: '报名未开始' },
  { value: 'OPEN', label: '报名中' },
  { value: 'CLOSED', label: '报名已结束' },
  { value: 'IN_PROGRESS', label: '进行中' },
  { value: 'ENDED', label: '已结束' },
]

function updateField<K extends keyof MyEventsSeasonFilterValues>(
  key: K,
  value: MyEventsSeasonFilterValues[K],
) {
  emit('update:modelValue', { ...props.modelValue, [key]: value })
}

function dateInputValue(value?: string) {
  if (!value) return ''
  return value.slice(0, 10)
}

function updateSignupTimeRange(part: 'start' | 'end', rawDate: string) {
  const current = props.modelValue.signupTimeRange || ['', '']
  const [start, end] = current
  if (part === 'start') {
    updateField('signupTimeRange', [rawDate ? `${rawDate} 00:00:00` : '', end])
    return
  }
  updateField('signupTimeRange', [start, rawDate ? `${rawDate} 23:59:59` : ''])
}
</script>

<template>
  <form class="my-events-filter" @submit.prevent="emit('search')">
    <div class="my-events-filter__body">
      <div class="my-events-filter__grid">
        <label class="my-events-filter__field">
          <span class="my-events-filter__label">赛事名称</span>
          <input
            :value="modelValue.eventName"
            type="search"
            class="my-events-filter__input"
            placeholder="请输入赛事名称"
            @input="updateField('eventName', ($event.target as HTMLInputElement).value)"
          >
        </label>

        <label class="my-events-filter__field">
          <span class="my-events-filter__label">赛季</span>
          <select
            :value="modelValue.seasonId"
            class="my-events-filter__input"
            @change="updateField('seasonId', ($event.target as HTMLSelectElement).value)"
          >
            <option value="">全部</option>
            <option
              v-for="item in seasonOptions"
              :key="String(item.seasonId)"
              :value="String(item.seasonId)"
            >
              {{ item.seasonName }}
            </option>
          </select>
        </label>

        <label class="my-events-filter__field">
          <span class="my-events-filter__label">游戏端</span>
          <select
            :value="modelValue.platformCode"
            class="my-events-filter__input"
            @change="updateField('platformCode', ($event.target as HTMLSelectElement).value)"
          >
            <option v-for="item in platformOptions" :key="item.value" :value="item.value">
              {{ item.label }}
            </option>
          </select>
        </label>

        <label class="my-events-filter__field">
          <span class="my-events-filter__label">赛事状态</span>
          <select
            :value="modelValue.status"
            class="my-events-filter__input"
            @change="updateField('status', ($event.target as HTMLSelectElement).value)"
          >
            <option v-for="item in statusOptions" :key="item.value || 'all'" :value="item.value">
              {{ item.label }}
            </option>
          </select>
        </label>

        <label class="my-events-filter__field my-events-filter__field--range">
          <span class="my-events-filter__label">报名时间</span>
          <div class="my-events-filter__range">
            <el-date-picker
              :model-value="dateInputValue(modelValue.signupTimeRange?.[0]) || null"
              type="date"
              value-format="YYYY-MM-DD"
              format="YYYY-MM-DD"
              placeholder="开始日期"
              clearable
              class="my-events-filter__date-picker"
              @update:model-value="updateSignupTimeRange('start', $event || '')"
            />
            <span class="my-events-filter__range-sep">至</span>
            <el-date-picker
              :model-value="dateInputValue(modelValue.signupTimeRange?.[1]) || null"
              type="date"
              value-format="YYYY-MM-DD"
              format="YYYY-MM-DD"
              placeholder="结束日期"
              clearable
              class="my-events-filter__date-picker"
              @update:model-value="updateSignupTimeRange('end', $event || '')"
            />
          </div>
        </label>
      </div>

      <div class="my-events-filter__actions">
        <button type="submit" class="account-btn-primary" :disabled="loading">搜索</button>
        <button type="button" class="account-btn-secondary" :disabled="loading" @click="emit('reset')">重置</button>
      </div>
    </div>
  </form>
</template>

<style scoped lang="scss">
.my-events-filter {
  margin-bottom: 16px;
  padding: 16px;
  border: 1px solid rgba(0, 0, 0, 0.14);
  border-radius: 4px;
  background: rgba(255, 255, 255, 0.35);
}

.my-events-filter__body {
  display: flex;
  align-items: flex-end;
  gap: 16px;
}

.my-events-filter__grid {
  flex: 1;
  min-width: 0;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px 16px;
}

.my-events-filter__field {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}

.my-events-filter__field--range {
  grid-column: span 2;
}

.my-events-filter__label {
  font-size: 13px;
  font-weight: 700;
  color: rgba(0, 0, 0, 0.62);
}

.my-events-filter__input {
  width: 100%;
  min-height: 38px;
  padding: 0 10px;
  border: 1px solid rgba(0, 0, 0, 0.18);
  border-radius: 3px;
  background: #fff;
  font-size: 14px;
  box-sizing: border-box;
}

.my-events-filter__range {
  display: flex;
  align-items: center;
  gap: 8px;
}

.my-events-filter__date-picker {
  flex: 1;
  min-width: 0;

  :deep(.el-input__wrapper) {
    min-height: 38px;
    padding: 0 10px;
    border-radius: 3px;
    box-shadow: 0 0 0 1px rgba(0, 0, 0, 0.18) inset;
    background: #fff;
  }

  :deep(.el-input__inner) {
    font-size: 14px;
    color: #111;
  }
}

.my-events-filter__range-sep {
  flex: 0 0 auto;
  font-size: 13px;
  color: #666;
}

.my-events-filter__actions {
  flex: 0 0 auto;
  display: flex;
  gap: 10px;
}

@media (max-width: 960px) {
  .my-events-filter__body {
    flex-direction: column;
    align-items: stretch;
  }

  .my-events-filter__grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .my-events-filter__field--range {
    grid-column: span 2;
  }

  .my-events-filter__actions {
    justify-content: flex-end;
  }
}

@media (max-width: 640px) {
  .my-events-filter__grid {
    grid-template-columns: 1fr;
  }

  .my-events-filter__field--range {
    grid-column: span 1;
  }

  .my-events-filter__range {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
