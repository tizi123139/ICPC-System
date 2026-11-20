<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import type { EChartsOption } from 'echarts'
import PageHeader from '@/components/PageHeader.vue'
import StatCard from '@/components/StatCard.vue'
import StatusTag from '@/components/StatusTag.vue'
import BaseChart from '@/components/BaseChart.vue'
import EmptyState from '@/components/EmptyState.vue'
import { getMemberOverview, getTrainingSummary } from '@/api/statistics'
import { applyJoin, applyResign, getApprovals } from '@/api/members'
import { getTrainings } from '@/api/trainings'
import { useAuthStore } from '@/stores/auth'
import {
  applyStatusMap,
  APPROVAL_MANAGER_ROLES,
  CONTENT_MANAGER_ROLES,
  formatDateTime,
  MEMBER_ROLES,
  memberTypeMap,
  roleLabels,
  STATISTICS_ROLES,
  trainingStatusMap,
  trainingTypeMap,
} from '@/utils/dict'
import type { ApplyRecord, MemberOverview, TrainingInfo, TrainingSummary, TrainingStatus, TrainingType } from '@/types'

const router = useRouter()
const auth = useAuthStore()

const overview = ref<MemberOverview | null>(null)
const summary = ref<TrainingSummary | null>(null)
const trainings = ref<TrainingInfo[]>([])
const approvals = ref<ApplyRecord[]>([])
const loading = ref(true)

const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 6) return '夜深了'
  if (hour < 12) return '上午好'
  if (hour < 14) return '中午好'
  if (hour < 18) return '下午好'
  return '晚上好'
})

const today = computed(() =>
  new Intl.DateTimeFormat('zh-CN', { dateStyle: 'full' }).format(new Date()),
)

const pendingCount = computed(() => approvals.value.filter((item) => item.status === 'PENDING').length)

/**
 * 会员管理角色（干事及以上）才可查看会员结构与待审批申请。
 * 对应接口 /statistics/members/overview、/members/approvals 的后端权限均为 OFFICER+，
 * 观察会员 / 队长发起请求会被 403，故前端直接不请求、不渲染。
 */
const canManageMembers = computed(() => auth.hasRole(APPROVAL_MANAGER_ROLES))

/**
 * 训练汇总、统计卡和训练活跃度图仅干事及以上可见（权限表「数据统计分析」）。
 * 会员结构仍仅干事及以上可见。
 */
const canViewTrainingSummary = computed(() => auth.hasRole(STATISTICS_ROLES))

/* ============ 会员服务：入会 / 退会申请 ============ */
const currentRole = computed(() => auth.roles[0])
const currentRoleLabel = computed(() =>
  currentRole.value ? roleLabels[currentRole.value] : '未分配角色',
)
/** 观察会员可提交入会申请 */
const canApplyJoin = computed(() => currentRole.value === 'OBSERVE_MEMBER')
/** 观察会员 / 正式会员 / 队长可提交退会申请（权限表「提交退会申请」一行） */
const canApplyResign = computed(() => auth.hasRole(MEMBER_ROLES))
const appliedType = ref<'JOIN' | 'RESIGN' | null>(null)

const applyVisible = ref(false)
const applyType = ref<'JOIN' | 'RESIGN'>('JOIN')
const applySubmitting = ref(false)
const applyFormRef = ref<FormInstance>()
const applyForm = reactive({ name: '', studentId: '', reason: '' })

const applyRules: FormRules = {
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  studentId: [{ required: true, message: '缺少学号信息', trigger: 'blur' }],
  reason: [
    { required: true, message: '请填写申请理由', trigger: 'blur' },
    { min: 10, message: '请至少填写 10 个字，便于管理员了解情况', trigger: 'blur' },
  ],
}

function openApply(type: 'JOIN' | 'RESIGN') {
  applyType.value = type
  applyForm.name = auth.user?.name ?? ''
  applyForm.studentId = auth.user?.studentId ?? ''
  applyForm.reason = ''
  applyVisible.value = true
}

async function submitApply() {
  if (!applyFormRef.value) return
  const valid = await applyFormRef.value.validate().catch(() => false)
  if (!valid) return

  applySubmitting.value = true
  try {
    if (applyType.value === 'JOIN') {
      // 接口：POST /members/apply/join —— 提交入会申请
      await applyJoin({
        name: applyForm.name,
        studentId: applyForm.studentId,
        reason: applyForm.reason,
      })
    } else {
      // 接口：POST /members/apply/resign —— 提交退会申请
      await applyResign({ reason: applyForm.reason })
    }
    appliedType.value = applyType.value
    applyVisible.value = false
    ElMessage.success(
      applyType.value === 'JOIN' ? '入会申请已提交，等待管理员审批' : '退会申请已提交，等待管理员审批',
    )
    // 审批列表同步刷新，便于切换管理账号后查看（普通会员无审批权限，不请求）
    if (canManageMembers.value) {
      approvals.value = await getApprovals()
    }
  } finally {
    applySubmitting.value = false
  }
}

/** 训练活跃度折线图 */
const trendOption = computed<EChartsOption>(() => ({
  tooltip: { trigger: 'axis' },
  legend: {
    data: ['提交数', 'AC 数'],
    top: 0,
    right: 0,
    itemWidth: 10,
    itemHeight: 10,
    textStyle: { color: '#6d7f8b', fontSize: 12 },
  },
  grid: { left: 8, right: 12, top: 36, bottom: 4, containLabel: true },
  xAxis: {
    type: 'category',
    boundaryGap: false,
    data: summary.value?.trend.map((item) => item.date) ?? [],
    axisLine: { lineStyle: { color: '#e3eef7' } },
    axisTick: { show: false },
    axisLabel: { color: '#9aa9b3', fontSize: 12 },
  },
  yAxis: {
    type: 'value',
    splitLine: { lineStyle: { color: '#f1f7fc' } },
    axisLabel: { color: '#9aa9b3', fontSize: 12 },
  },
  series: [
    {
      name: '提交数',
      type: 'line',
      smooth: true,
      symbol: 'circle',
      symbolSize: 6,
      lineStyle: { width: 2, color: '#5b9bd5' },
      itemStyle: { color: '#5b9bd5' },
      areaStyle: {
        color: {
          type: 'linear',
          x: 0,
          y: 0,
          x2: 0,
          y2: 1,
          colorStops: [
            { offset: 0, color: 'rgba(91,155,213,0.18)' },
            { offset: 1, color: 'rgba(91,155,213,0.02)' },
          ],
        },
      },
      data: summary.value?.trend.map((item) => item.submissions) ?? [],
    },
    {
      name: 'AC 数',
      type: 'line',
      smooth: true,
      symbol: 'circle',
      symbolSize: 6,
      lineStyle: { width: 2, color: '#d9b25f' },
      itemStyle: { color: '#d9b25f' },
      data: summary.value?.trend.map((item) => item.ac) ?? [],
    },
  ],
}))

/** 会员结构环形图 */
const pieOption = computed<EChartsOption>(() => ({
  tooltip: { trigger: 'item', formatter: '{b}：{c} 人（{d}%）' },
  legend: {
    bottom: 0,
    left: 'center',
    itemWidth: 10,
    itemHeight: 10,
    textStyle: { color: '#6d7f8b', fontSize: 12 },
  },
  color: ['#5b9bd5', '#a9cbe8', '#e2c98e'],
  series: [
    {
      type: 'pie',
      radius: ['52%', '74%'],
      center: ['50%', '45%'],
      avoidLabelOverlap: true,
      itemStyle: { borderColor: '#ffffff', borderWidth: 3, borderRadius: 6 },
      label: { show: false },
      data:
        overview.value?.byType.map((item) => ({
          name: memberTypeMap[item.name]?.label ?? item.name,
          value: item.value,
        })) ?? [],
    },
  ],
}))

async function load() {
  loading.value = true
  try {
    const [overviewData, summaryData, trainingData, approvalData] = await Promise.all([
      // 会员结构统计接口权限为 OFFICER+，普通会员不请求
      canManageMembers.value ? getMemberOverview() : Promise.resolve(null),
      // 训练汇总接口权限为 ASSISTANT_COACH+，普通会员不请求
      canViewTrainingSummary.value ? getTrainingSummary() : Promise.resolve(null),
      getTrainings({ page: 1, size: 4 }),
      // 待审批申请接口权限为 OFFICER+，普通会员不请求
      canManageMembers.value ? getApprovals() : Promise.resolve([]),
    ])
    overview.value = overviewData
    summary.value = summaryData
    trainings.value = trainingData.records
    approvals.value = approvalData
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
<PageHeader
    :title="`${greeting}，${auth.user?.name ?? '同学'}`"
    :subtitle="`${today} · 当前身份：${currentRoleLabel}`"
  >
    <template #actions>
      <!-- TODO(backend): 快捷入口跳转到对应模块，权限由路由守卫与按钮权限共同控制 -->
      <el-button v-permission="CONTENT_MANAGER_ROLES" @click="router.push('/trainings')">
        <el-icon><Plus /></el-icon>创建训练
      </el-button>
      <el-button type="primary" @click="router.push('/problems')">
        <el-icon><Document /></el-icon>浏览题库
      </el-button>
    </template>
</PageHeader>

<div class="page dashboard">
  <!-- 会员服务：观察会员入会 / 正式会员退会 -->
  <div v-if="!appliedType && (canApplyJoin || canApplyResign)" class="ic-card membership-card">
    <div class="membership-icon">
      <el-icon><InfoFilled /></el-icon>
    </div>
    <div class="membership-text">
      <strong>{{ canApplyJoin ? '你当前是观察会员' : '你当前是协会正式会员' }}</strong>
      <p>
        {{
          canApplyJoin
            ? '完成观察期考核后，可提交入会申请，审批通过后转为正式会员。'
            : '如因学业或个人原因需要退出协会，可提交退会申请，由管理员审批。'
        }}
      </p>
    </div>
    <el-button
      type="primary"
      :plain="!canApplyJoin"
      @click="openApply(canApplyJoin ? 'JOIN' : 'RESIGN')"
    >
      {{ canApplyJoin ? '提交入会申请' : '提交退会申请' }}
    </el-button>
  </div>

  <div v-if="appliedType" class="ic-card membership-card membership-card--done">
    <div class="membership-icon">
      <el-icon><CircleCheck /></el-icon>
    </div>
    <div class="membership-text">
      <strong>{{ appliedType === 'JOIN' ? '入会申请已提交' : '退会申请已提交' }}</strong>
      <p>管理员审批后会同步更新你的会员状态，请耐心等待。</p>
    </div>
  </div>

  <!-- 入会 / 退会申请 -->
  <el-dialog
    v-model="applyVisible"
    :title="applyType === 'JOIN' ? '提交入会申请' : '提交退会申请'"
    width="560px"
    top="10vh"
  >
    <el-form
      ref="applyFormRef"
      :model="applyForm"
      :rules="applyRules"
      label-position="top"
    >
      <div class="apply-grid">
        <el-form-item label="姓名" prop="name">
          <el-input v-model="applyForm.name" disabled />
        </el-form-item>
        <el-form-item label="学号" prop="studentId">
          <el-input v-model="applyForm.studentId" disabled />
        </el-form-item>
      </div>
      <el-form-item :label="applyType === 'JOIN' ? '申请理由' : '退会原因'" prop="reason">
        <el-input
          v-model="applyForm.reason"
          type="textarea"
          :rows="6"
          :placeholder="
            applyType === 'JOIN'
              ? '请说明你的学习情况、竞赛经历以及希望加入协会的原因'
              : '请说明退会原因，便于管理员跟进'
          "
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="applyVisible = false">取消</el-button>
      <el-button type="primary" :loading="applySubmitting" @click="submitApply">
        提交申请
      </el-button>
    </template>
  </el-dialog>

  <div v-if="canViewTrainingSummary" v-loading="loading" class="stats">
    <StatCard
      v-if="canManageMembers"
      label="协会会员总数"
      :value="overview?.total ?? '—'"
      icon="User"
      hint="含观察会员"
      tone="blue"
    />
    <StatCard
      label="累计训练场次"
      :value="summary?.totalTrainings ?? '—'"
      icon="DataAnalysis"
      hint="单人 + 组队训练"
      tone="cream"
    />
    <StatCard
      label="累计代码提交"
      :value="summary?.totalSubmissions ?? '—'"
      icon="Upload"
      hint="来自自建 OJ 判题系统"
      tone="green"
    />
    <StatCard
      label="整体 AC 率"
      :value="summary ? `${summary.acRate}%` : '—'"
      icon="TrendCharts"
      hint="近 6 周训练数据"
      tone="amber"
    />
  </div>

  <div v-if="canViewTrainingSummary" class="charts" :class="{ 'charts--single': !canManageMembers }">
    <div class="ic-card chart-card">
      <h2 class="section-title">训练活跃度</h2>
      <BaseChart :option="trendOption" height="300px" />
    </div>
    <!-- 会员结构：仅干事及以上可见 -->
    <div v-if="canManageMembers" class="ic-card chart-card">
      <h2 class="section-title">会员结构</h2>
      <BaseChart :option="pieOption" height="300px" />
    </div>
  </div>

  <div class="bottom" :class="{ 'bottom--single': !canManageMembers }">
    <div class="ic-card list-card">
      <div class="card-head">
        <h2 class="section-title">近期训练</h2>
        <el-button link type="primary" @click="router.push('/trainings')">查看全部</el-button>
      </div>
      <el-table :data="trainings" :show-header="true">
        <el-table-column prop="name" label="训练名称" min-width="180">
          <template #default="{ row }">
            <a class="table-link" @click="router.push(`/trainings/${row.trainingId}`)">
              {{ row.name }}
            </a>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="100">
          <template #default="{ row }">{{ trainingTypeMap[row.type as TrainingType] }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <StatusTag
              :label="trainingStatusMap[row.status as TrainingStatus].label"
              :tone="trainingStatusMap[row.status as TrainingStatus].tone"
            />
          </template>
        </el-table-column>
        <el-table-column label="开始时间" width="150">
          <template #default="{ row }">{{ formatDateTime(row.startTime) }}</template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 待审批申请：仅干事及以上可见 -->
    <div v-if="canManageMembers" class="ic-card list-card">
      <div class="card-head">
        <h2 class="section-title">待审批申请</h2>
        <el-button
          v-permission="APPROVAL_MANAGER_ROLES"
          link
          type="primary"
          @click="router.push('/members/approvals')"
        >
          去处理
        </el-button>
      </div>
      <div class="approval-summary">
        <strong>{{ pendingCount }}</strong>
        <span>条待处理</span>
      </div>
      <ul class="approval-list">
        <li v-for="item in approvals.filter((a) => a.status === 'PENDING').slice(0, 4)" :key="item.applyId">
          <div class="avatar">{{ item.memberName.slice(0, 1) }}</div>
          <div class="approval-main">
            <strong>{{ item.memberName }}</strong>
            <span>{{ item.type === 'JOIN' ? '申请入会' : '申请退会' }} · {{ formatDateTime(item.applyTime) }}</span>
          </div>
          <StatusTag :label="applyStatusMap[item.status].label" :tone="applyStatusMap[item.status].tone" />
        </li>
      </ul>
      <EmptyState
        v-if="pendingCount === 0"
        title="暂无待审批申请"
        description="所有入会 / 退会申请均已处理"
      />
    </div>
  </div>
</div>
</template>

<style scoped>
.dashboard {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.membership-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 18px 22px;
  border: 1px solid var(--primary-light);
  background: linear-gradient(120deg, #f4f9fe, #fdfcf6);
}

.membership-card--done {
  border-color: #cfe6d8;
  background: linear-gradient(120deg, #f2faf5, #fdfcf6);
}

.membership-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  flex-shrink: 0;
  border-radius: 50%;
  background: var(--primary-light);
  color: var(--primary-deep);
  font-size: 19px;
}

.membership-card--done .membership-icon {
  background: #dff2e6;
  color: #2f855a;
}

.membership-text {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 5px;
  min-width: 0;
}

.membership-text strong {
  font-size: 14px;
  color: var(--text-main);
}

.membership-text p {
  margin: 0;
  font-size: 13px;
  line-height: 1.7;
  color: var(--text-sub);
}

.apply-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 16px;
}

.stats {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
}

.charts {
  display: grid;
  grid-template-columns: 1.4fr 1fr;
  gap: 20px;
}

/* 普通会员看不到会员结构，图表区改为单列 */
.charts--single {
  grid-template-columns: 1fr;
}

.chart-card {
  padding: 22px 24px 14px;
}

.bottom {
  display: grid;
  grid-template-columns: 1.6fr 1fr;
  gap: 20px;
}

/* 普通会员看不到待审批申请，列表区改为单列 */
.bottom--single {
  grid-template-columns: 1fr;
}

.list-card {
  padding: 22px 24px;
}

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.card-head .section-title {
  margin-bottom: 12px;
}

.table-link {
  color: var(--text-main);
  cursor: pointer;
  font-weight: 500;
}

.table-link:hover {
  color: var(--primary-deep);
}

.approval-summary {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-bottom: 12px;
  padding: 14px 18px;
  border-radius: var(--radius-md);
  background: linear-gradient(120deg, var(--primary-lighter), #fdfbf3);
}

.approval-summary strong {
  font-size: 28px;
  color: var(--primary-deep);
}

.approval-summary span {
  font-size: 13px;
  color: var(--text-sub);
}

.approval-list {
  list-style: none;
  margin: 0;
  padding: 0;
}

.approval-list li {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 4px;
  border-bottom: 1px dashed var(--border);
}

.approval-list li:last-child {
  border-bottom: none;
}

.avatar {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  color: var(--primary-deep);
  background: var(--primary-light);
}


.approval-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.approval-main strong {
  font-size: 13.5px;
}

.approval-main span {
  font-size: 12px;
  line-height: 1.6;
  color: var(--text-muted);
}

@media (max-width: 1280px) {
  .stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .charts,
  .bottom {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .membership-card {
    flex-wrap: wrap;
    align-items: flex-start;
  }

  .membership-card > .el-button {
    margin-left: 56px;
  }

  .apply-grid {
    grid-template-columns: 1fr;
  }
}
</style>
