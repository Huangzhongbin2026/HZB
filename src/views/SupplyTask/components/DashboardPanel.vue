<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from 'vue'
import * as echarts from 'echarts'
import { taskApi } from '@/api/modules/task'

const chartRef = ref<HTMLDivElement>()
const stat = ref({
  total: 0,
  pending: 0,
  overdue: 0,
  onTimeRate: 0,
})

const cards = computed(() => [
  { label: '任务总量', value: stat.value.total },
  { label: '待处理', value: stat.value.pending },
  { label: '超时任务', value: stat.value.overdue },
  { label: '准时率(%)', value: stat.value.onTimeRate },
])

const renderChart = () => {
  if (!chartRef.value) return
  const chart = echarts.init(chartRef.value)
  chart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 24, right: 24, top: 24, bottom: 24, containLabel: true },
    xAxis: {
      type: 'category',
      data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日'],
    },
    yAxis: { type: 'value' },
    series: [
      {
        name: '新建任务',
        type: 'line',
        smooth: true,
        areaStyle: {},
        data: [16, 20, 18, 24, 30, 12, 10],
      },
      {
        name: '完成任务',
        type: 'line',
        smooth: true,
        areaStyle: {},
        data: [8, 12, 16, 18, 22, 10, 9],
      },
    ],
  })
}

onMounted(async () => {
  const res = await taskApi.dashboard()
  stat.value = res.data
  await nextTick()
  renderChart()
})
</script>

<template>
  <div class="board-layout">
    <el-row :gutter="12">
      <el-col v-for="item in cards" :key="item.label" :span="6">
        <el-card shadow="never" class="stat-card">
          <div class="stat-label">{{ item.label }}</div>
          <div class="stat-value">{{ item.value }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" class="chart-card">
      <template #header>
        <div class="panel-title">任务趋势</div>
      </template>
      <div ref="chartRef" class="chart-container" />
    </el-card>
  </div>
</template>

<style scoped>
.board-layout {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.stat-card {
  border-radius: 12px;
}

.stat-label {
  color: #64748b;
  font-size: 13px;
}

.stat-value {
  margin-top: 8px;
  color: #0f172a;
  font-size: 26px;
  font-weight: 700;
}

.chart-card {
  border-radius: 12px;
}

.panel-title {
  font-size: 16px;
  font-weight: 700;
}

.chart-container {
  height: 320px;
}
</style>
