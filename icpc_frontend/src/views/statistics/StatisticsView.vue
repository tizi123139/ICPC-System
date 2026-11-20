<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import BaseChart from '@/components/BaseChart.vue'
import EmptyState from '@/components/EmptyState.vue'
import { exportTrainingReport, getMemberOverview, getTrainingSummary } from '@/api/statistics'
import { getTrainings } from '@/api/trainings'
import { memberTypeMap } from '@/utils/dict'
import type { EChartsOption } from 'echarts'
import type { MemberOverview, TrainingInfo, TrainingSummary } from '@/types'

/* ============ 数据加载 ============ */
const loading = ref(false)
const overview = ref<MemberOverview | null>(null)
const summary = ref<TrainingSummary | null>(null)
/** 可导出报表的训练列表（GET /trainings） */
const trainings = ref<TrainingInfo[]>([])

async function load() {
  loading.value = true
  try {
    // 接口：GET /statistics/members/overview + GET /statistics/training/summary，并行拉取训练列表供导出选择
    const [overviewResult, summaryResult, trainingPage] = await Promise.all([
      getMemberOverview(),
      getTrainingSummary(),
      getTrainings({ page: 1, size: 100 }),
    ])
    overview.value = overviewResult
    summary.value = summaryResult
    trainings.value = trainingPage.records
  } finally {
    loading.value = false
  }
}

/* ============ KPI ============ */
const kpis = computed(() => [
  { label: '会员总数', value: overview.value?.total ?? 0, unit: '人' },
  { label: '训练总数', value: summary.value?.totalTrainings ?? 0, unit: '场' },
  { label: '提交总数', value: summary.value?.totalSubmissions ?? 0, unit: '次' },
  {
    label: '总体 AC 率',
    // acRate 已是百分数（如 63.5 表示 63.5%）
    value: summary.value?.acRate ?? 0,
    unit: '%',
  },
])

/* ============ 提交趋势图 ============ */
const trendOption = computed<EChartsOption>(() => ({
  tooltip: { trigger: 'axis' },
  legend: { data: ['提交数', 'AC 数'], top: 0 },
  grid: { left: 40, right: 24, top: 36, bottom: 28 },
  xAxis: {
    type: 'category',
    // 趋势日期已是「MM-DD」格式
    data: summary.value?.trend.map((item) => item.date) ?? [],
  },
  yAxis: { type: 'value' },
  series: [
    {
      name: '提交数',
      type: 'line',
      smooth: true,
      data: summary.value?.trend.map((item) => item.submissions) ?? [],
      itemStyle: { color: '#5b9bd5' },
      areaStyle: { opacity: 0.12 },
    },
    {
      name: 'AC 数',
      type: 'line',
      smooth: true,
      data: summary.value?.trend.map((item) => item.ac) ?? [],
      itemStyle: { color: '#7cb342' },
    },
  ],
}))

/* ============ 会员结构占比条 ============ */
interface StructRow {
  name: string
  value: number
  percent: number
}

function toRows(list: Array<{ name: string; value: number }>, label?: (name: string) => string): StructRow[] {
  const total = list.reduce((sum, item) => sum + item.value, 0) || 1
  return list.map((item) => ({
    name: label ? label(item.name) : item.name,
    value: item.value,
    percent: Math.round((item.value / total) * 100),
  }))
}

const byTypeRows = computed(() =>
  toRows(overview.value?.byType ?? [], (name) => memberTypeMap[name as keyof typeof memberTypeMap]?.label ?? name),
)
const byGradeRows = computed(() => toRows(overview.value?.byGrade ?? []))
const byDepartmentRows = computed(() => toRows(overview.value?.byDepartment ?? []))

/* ============ 导出训练成绩报表 ============ */
const selectedTrainingId = ref<number | undefined>()
const exporting = ref(false)

async function handleExport() {
  if (!selectedTrainingId.value) {
    ElMessage.warning('请先选择要导出的训练')
    return
  }
  exporting.value = true
  try {
    // 接口：GET /export/training-report/{trainingId}（Excel / CSV）
    await exportTrainingReport(selectedTrainingId.value)
    ElMessage.success('报表已生成并开始下载')
  } finally {
    exporting.value = false
  }
}

onMounted(load)
</script>

<template>
  <div>
    <PageHeader
      title="数据统计"
      subtitle="会员结构统计 / 训练数据汇总 / 训练成绩报表导出"
    >
      <template #actions>
        <el-button type="primary" plain @click="load">刷新数据</el-button>
      </template>
    </PageHeader>

    <div v-loading="loading" class="page statistics-page">
      <!-- KPI 指标 -->
      <div class="kpi-row">
        <div v-for="item in kpis" :key="item.label" class="ic-card kpi-card">
          <p class="kpi-label">{{ item.label }}</p>
          <p class="kpi-value">
            {{ item.value }}<span class="kpi-unit">{{ item.unit }}</span>
          </p>
        </div>
      </div>

      <!-- 训练数据汇总：提交趋势 -->
      <div class="ic-card chart-card" data-testid="training-trend">
        <h2 class="section-title">训练提交趋势</h2>
        <BaseChart v-if="summary?.trend.length" :option="trendOption" height="300px" />
        <EmptyState v-else description="暂无趋势数据" />
      </div>

      <!-- 会员结构统计 -->
      <div class="structure-row">
        <div class="ic-card structure-card" data-testid="member-structure">
          <h2 class="section-title">会员类型分布</h2>
          <div v-for="row in byTypeRows" :key="row.name" class="struct-item">
            <div class="struct-head">
              <span>{{ row.name }}</span>
              <span class="muted">{{ row.value }} 人 · {{ row.percent }}%</span>
            </div>
            <div class="struct-bar">
              <div class="struct-fill" :style="{ width: `${row.percent}%` }" />
            </div>
          </div>
          <EmptyState v-if="!loading && byTypeRows.length === 0" description="暂无数据" />
        </div>

        <div class="ic-card structure-card">
          <h2 class="section-title">年级分布</h2>
          <div v-for="row in byGradeRows" :key="row.name" class="struct-item">
            <div class="struct-head">
              <span>{{ row.name }}</span>
              <span class="muted">{{ row.value }} 人 · {{ row.percent }}%</span>
            </div>
            <div class="struct-bar">
              <div class="struct-fill" :style="{ width: `${row.percent}%` }" />
            </div>
          </div>
          <EmptyState v-if="!loading && byGradeRows.length === 0" description="暂无数据" />
        </div>

        <div class="ic-card structure-card">
          <h2 class="section-title">部门分布</h2>
          <div v-for="row in byDepartmentRows" :key="row.name" class="struct-item">
            <div class="struct-head">
              <span>{{ row.name }}</span>
              <span class="muted">{{ row.value }} 人 · {{ row.percent }}%</span>
            </div>
            <div class="struct-bar">
              <div class="struct-fill" :style="{ width: `${row.percent}%` }" />
            </div>
          </div>
          <EmptyState v-if="!loading && byDepartmentRows.length === 0" description="暂无数据" />
        </div>
      </div>

      <!-- 导出训练成绩报表 -->
      <div class="ic-card export-card" data-testid="export-card">
        <h2 class="section-title">导出训练成绩报表</h2>
        <p class="export-tip">选择训练后导出成员提交与成绩明细（Excel），权限：助理教练及以上</p>
        <div class="export-controls">
          <el-select
            v-model="selectedTrainingId"
            placeholder="选择训练"
            class="export-select"
            data-testid="training-select"
          >
            <el-option
              v-for="training in trainings"
              :key="training.trainingId"
              :label="training.name"
              :value="training.trainingId"
            />
          </el-select>
          <el-button
            type="primary"
            data-testid="export-btn"
            :loading="exporting"
            @click="handleExport"
          >
            导出报表
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.statistics-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* KPI */
.kpi-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
}

.kpi-card {
  padding: 18px 20px;
}

.kpi-label {
  margin: 0 0 8px;
  font-size: 12.5px;
  color: var(--text-muted);
}

.kpi-value {
  margin: 0;
  font-size: 26px;
  font-weight: 700;
  color: var(--primary-deep);
  font-variant-numeric: tabular-nums;
}

.kpi-unit {
  margin-left: 4px;
  font-size: 12px;
  font-weight: 400;
  color: var(--text-muted);
}

/* 图表 */
.chart-card {
  padding: 20px 22px;
}

/* 会员结构 */
.structure-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14px;
}

.structure-card {
  padding: 20px 22px;
}

.struct-item {
  margin-bottom: 12px;
}

.struct-item:last-of-type {
  margin-bottom: 0;
}

.struct-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 6px;
  font-size: 12.5px;
}

.muted {
  color: var(--text-muted);
}

.struct-bar {
  height: 8px;
  border-radius: var(--radius-pill);
  background: var(--bg-blue-softer);
  overflow: hidden;
}

.struct-fill {
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #8fc1e9, #5b9bd5);
  transition: width 0.4s ease;
}

/* 导出 */
.export-card {
  padding: 20px 22px;
}

.export-tip {
  margin: -6px 0 14px;
  font-size: 12px;
  color: var(--text-muted);
}

.export-controls {
  display: flex;
  align-items: center;
  gap: 12px;
}

.export-select {
  width: 320px;
}
</style>
