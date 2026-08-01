<template>
  <el-card shadow="never" class="trend-panel">
    <div class="trend-header">
      <div>
        <div class="trend-title">运营趋势</div>
        <div class="trend-subtitle">按时间查看认证、报名、比赛数据等业务变化</div>
      </div>
    </div>

    <div class="trend-toolbar">
      <div class="toolbar-group">
        <el-radio-group v-model="rangeType" size="small" @change="handleRangeTypeChange">
          <el-radio-button label="7d">近7天</el-radio-button>
          <el-radio-button label="30d">近30天</el-radio-button>
          <el-radio-button label="90d">近90天</el-radio-button>
          <el-radio-button label="custom">自定义日期</el-radio-button>
        </el-radio-group>
        <el-date-picker
          v-if="rangeType === 'custom'"
          v-model="customRange"
          type="daterange"
          unlink-panels
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
          size="small"
          class="custom-range-picker"
          @change="handleCustomRangeChange"
        />
      </div>
      <div class="toolbar-group">
        <span class="toolbar-label">指标类型</span>
        <el-radio-group v-model="metricGroup" size="small" @change="loadTrend">
          <el-radio-button label="ALL">全部</el-radio-button>
          <el-radio-button label="AUTH">账号认证</el-radio-button>
          <el-radio-button label="REGISTRATION">报名与阵容</el-radio-button>
          <el-radio-button label="MATCH_DATA">比赛数据</el-radio-button>
        </el-radio-group>
      </div>
    </div>

    <div v-loading="loading" class="chart-wrapper">
      <div v-if="errorMessage" class="chart-state error">{{ errorMessage }}</div>
      <div v-else-if="isEmpty" class="chart-state empty">
        <div class="empty-title">暂无趋势数据</div>
        <div class="empty-desc">有认证、报名或比赛数据提交后将在此展示</div>
      </div>
      <div v-show="!errorMessage && !isEmpty" ref="chartRef" class="chart-container" />
    </div>

    <div class="summary-row">
      <div class="summary-item">
        <div class="summary-label">今日新增</div>
        <div class="summary-value">{{ summary.todayNew }}</div>
      </div>
      <div class="summary-item">
        <div class="summary-label">今日已处理</div>
        <div class="summary-value">{{ summary.todayHandled }}</div>
      </div>
      <div class="summary-item">
        <div class="summary-label">当前待处理</div>
        <div class="summary-value">{{ pendingTotalDisplay }}</div>
      </div>
      <div class="summary-item">
        <div class="summary-label">本月业务量</div>
        <div class="summary-value">{{ summary.monthTotal }}</div>
      </div>
    </div>
  </el-card>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import * as echarts from 'echarts'
import { getDashboardTrend, type DashboardTrendData, type DashboardTrendSummary } from '@/api/basket/dashboard'

const props = defineProps<{
  pendingTotal?: number
}>()

const rangeType = ref('30d')
const metricGroup = ref('ALL')
const customRange = ref<[string, string] | null>(null)
const loading = ref(false)
const errorMessage = ref('')
const trendData = ref<DashboardTrendData | null>(null)
const chartRef = ref<HTMLDivElement | null>(null)

let chartInstance: echarts.ECharts | null = null

const summary = computed<DashboardTrendSummary>(() => trendData.value?.summary || {
  todayNew: 0,
  todayHandled: 0,
  pendingTotal: 0,
  monthTotal: 0
})

const pendingTotalDisplay = computed(() => {
  if (props.pendingTotal != null) {
    return props.pendingTotal
  }
  return summary.value.pendingTotal
})

const chartTitle = computed(() => {
  switch (rangeType.value) {
    case '7d':
      return '近7天业务趋势'
    case '90d':
      return '近90天业务趋势'
    case 'custom':
      return '自定义日期业务趋势'
    default:
      return '近30天业务趋势'
  }
})

const defaultSelectedLegend = ['球员认证', '球队报名', '比赛数据', '数据争议']

const isEmpty = computed(() => {
  if (!trendData.value?.series?.length) {
    return true
  }
  return trendData.value.series.every(item => item.data.every(value => value === 0))
})

function handleResize() {
  chartInstance?.resize()
}

async function loadTrend() {
  if (rangeType.value === 'custom' && (!customRange.value || customRange.value.length !== 2)) {
    return
  }

  loading.value = true
  errorMessage.value = ''
  try {
    const params: Record<string, string> = {
      rangeType: rangeType.value,
      metricGroup: metricGroup.value
    }
    if (rangeType.value === 'custom' && customRange.value) {
      params.startDate = customRange.value[0]
      params.endDate = customRange.value[1]
    }
    const res = await getDashboardTrend(params)
    trendData.value = res.data || null
    await nextTick()
    renderChart()
  } catch (error) {
    trendData.value = null
    errorMessage.value = '趋势数据加载失败，请稍后重试'
    disposeChart()
  } finally {
    loading.value = false
  }
}

function handleRangeTypeChange(value: string | number | boolean | undefined) {
  if (value !== 'custom') {
    customRange.value = null
    loadTrend()
  }
}

function handleCustomRangeChange(value: [string, string] | null) {
  if (value && value.length === 2) {
    loadTrend()
  }
}

function renderChart() {
  if (!chartRef.value || !trendData.value || isEmpty.value || errorMessage.value) {
    disposeChart()
    return
  }

  if (!chartInstance) {
    chartInstance = echarts.init(chartRef.value)
  }

  const selectedLegend = Object.fromEntries(
    (trendData.value.series || []).map(item => [
      item.name,
      defaultSelectedLegend.includes(item.name)
    ])
  )

  chartInstance.setOption({
    title: {
      text: chartTitle.value,
      left: 0,
      textStyle: { fontSize: 14, fontWeight: 500, color: '#303133' }
    },
    tooltip: {
      trigger: 'axis'
    },
    legend: {
      type: 'scroll',
      top: 28,
      selected: selectedLegend
    },
    grid: {
      left: 24,
      right: 24,
      top: 72,
      bottom: 24,
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: trendData.value.dateList || []
    },
    yAxis: {
      type: 'value',
      name: '数量',
      minInterval: 1
    },
    series: (trendData.value.series || []).map(item => ({
      name: item.name,
      type: 'line',
      smooth: true,
      showSymbol: false,
      data: item.data
    }))
  }, true)

  chartInstance.resize()
}

function disposeChart() {
  if (chartInstance) {
    chartInstance.dispose()
    chartInstance = null
  }
}

watch(chartTitle, () => {
  if (!loading.value && !errorMessage.value) {
    renderChart()
  }
})

onMounted(() => {
  window.addEventListener('resize', handleResize)
  loadTrend()
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  disposeChart()
})
</script>

<style scoped lang="scss">
.trend-panel {
  margin-top: 16px;
}

.trend-header {
  margin-bottom: 16px;
}

.trend-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.trend-subtitle {
  margin-top: 6px;
  color: #909399;
  font-size: 13px;
}

.trend-toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.toolbar-group {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
}

.toolbar-label {
  color: #606266;
  font-size: 13px;
}

.custom-range-picker {
  width: 280px;
}

.chart-wrapper {
  min-height: 360px;
}

.chart-container {
  width: 100%;
  height: 360px;
}

.chart-state {
  min-height: 360px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #909399;
}

.chart-state.error {
  color: #f56c6c;
}

.empty-title {
  font-size: 16px;
  color: #606266;
  margin-bottom: 8px;
}

.empty-desc {
  font-size: 13px;
}

.summary-row {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #ebeef5;
}

.summary-item {
  padding: 12px 16px;
  background: #f5f7fa;
  border-radius: 8px;
}

.summary-label {
  color: #909399;
  font-size: 13px;
}

.summary-value {
  margin-top: 8px;
  font-size: 24px;
  font-weight: 700;
  color: #303133;
}

@media (max-width: 992px) {
  .summary-row {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
