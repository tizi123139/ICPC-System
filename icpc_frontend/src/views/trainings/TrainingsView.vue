<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import StatusTag from '@/components/StatusTag.vue'
import EmptyState from '@/components/EmptyState.vue'
import {
  createTraining,
  deleteTraining,
  getTrainings,
  publishTraining,
  updateTraining,
} from '@/api/trainings'
import { getProblems } from '@/api/problems'
import {
  CONTENT_MANAGER_ROLES,
  formatDateTime,
  trainingStatusMap,
  trainingTypeMap,
} from '@/utils/dict'
import { useAuthStore } from '@/stores/auth'
import type {
  ProblemInfo,
  TrainingInfo,
  TrainingPayload,
  TrainingQuery,
  TrainingStatus,
  TrainingType,
} from '@/types'

const router = useRouter()
const auth = useAuthStore()

const loading = ref(false)
const list = ref<TrainingInfo[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(9)
const problems = ref<ProblemInfo[]>([])

const query = reactive<TrainingQuery>({
  keyword: '',
  status: '',
  type: '',
  page: 1,
  size: 9,
})

const statusTabs: Array<{ key: TrainingStatus | ''; label: string }> = [
  { key: '', label: '全部' },
  { key: 'DRAFT', label: '草稿' },
  { key: 'PUBLISHED', label: '已发布' },
  { key: 'ONGOING', label: '进行中' },
  { key: 'ENDED', label: '已结束' },
]

const activeStatus = computed({
  get: () => query.status ?? '',
  set: (value: TrainingStatus | '') => {
    query.status = value
    page.value = 1
    load()
  },
})

const problemTitleMap = computed(() => {
  const map = new Map<number, ProblemInfo>()
  problems.value.forEach((item) => map.set(item.problemId, item))
  return map
})

async function load() {
  loading.value = true
  try {
    // 接口：GET /trainings（分页 + 状态 / 类型筛选）
    const result = await getTrainings({ ...query, page: page.value, size: size.value })
    list.value = result.records
    total.value = result.total
  } finally {
    loading.value = false
  }
}

async function loadProblems() {
  // 接口：GET /problems（用于创建训练时选择题目）
  const result = await getProblems({ page: 1, size: 100 })
  problems.value = result.records
}

function handleSearch() {
  page.value = 1
  load()
}

watch(page, load)

async function handlePublish(training: TrainingInfo) {
  // 接口：POST /trainings/{trainingId}/publish
  await publishTraining(training.trainingId)
  ElMessage.success(`「${training.name}」已发布`)
  load()
}

/** 训练创建者或会长/副会长、指导老师可删除 */
function canDelete(training: TrainingInfo) {
  return (
    auth.hasRole(['PRESIDENT', 'ADVISOR']) ||
    training.creator.userId === auth.user?.userId
  )
}

async function handleDelete(training: TrainingInfo) {
  const confirmed = await ElMessageBox.confirm(
    `确认删除训练「${training.name}」？删除后题目、提交与榜单数据将不可恢复。`,
    '删除训练',
    { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' },
  ).catch(() => false)
  if (!confirmed) return

  // 接口：DELETE /trainings/{trainingId}
  await deleteTraining(training.trainingId)
  ElMessage.success('训练已删除')
  load()
}

/* ============ 创建 / 编辑训练 ============ */
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const submitting = ref(false)
const formRef = ref<FormInstance>()
const timeRange = ref<[string, string] | null>(null)

const emptyForm = (): TrainingPayload => ({
  name: '',
  type: 'SOLO',
  startTime: '',
  endTime: '',
  problemIds: [],
  leaderboardVisible: true,
  freezeTime: 30,
})

const form = reactive<TrainingPayload>(emptyForm())

const rules: FormRules<TrainingPayload> = {
  name: [{ required: true, message: '请输入训练名称', trigger: 'blur' }],
  problemIds: [{ required: true, type: 'array', min: 1, message: '请至少选择一道题目', trigger: 'change' }],
}

function openCreate() {
  editingId.value = null
  Object.assign(form, emptyForm())
  timeRange.value = null
  dialogVisible.value = true
}

/** 编辑训练：仅草稿状态可修改 */
function openEdit(training: TrainingInfo) {
  editingId.value = training.trainingId
  Object.assign(form, {
    name: training.name,
    type: training.type,
    startTime: training.startTime,
    endTime: training.endTime,
    problemIds: [...training.problemIds],
    leaderboardVisible: training.leaderboardVisible,
    freezeTime: training.freezeTime,
  })
  // 去掉时区后缀，回填日期选择器
  timeRange.value = [
    training.startTime.slice(0, 19),
    training.endTime.slice(0, 19),
  ]
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  if (!timeRange.value || timeRange.value.length !== 2) {
    ElMessage.warning('请选择训练起止时间')
    return
  }

  form.startTime = `${timeRange.value[0]}+08:00`
  form.endTime = `${timeRange.value[1]}+08:00`

  submitting.value = true
  try {
    if (editingId.value) {
      // 接口：PUT /trainings/{trainingId}（仅草稿状态可修改）
      await updateTraining(editingId.value, { ...form })
      ElMessage.success('训练已更新，仍保持草稿状态')
      dialogVisible.value = false
      load()
    } else {
      // 接口：POST /trainings（创建训练，权限：ASSISTANT_COACH+）
      const training = await createTraining({ ...form })
      ElMessage.success('训练已创建，处于草稿状态')
      dialogVisible.value = false
      router.push(`/trainings/${training.trainingId}`)
    }
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  load()
  loadProblems()
})
</script>

<template>
<PageHeader title="训练列表" subtitle="创建单人 / 组队训练，管理题目、提交与榜单">
    <template #actions>
      <el-button v-permission="CONTENT_MANAGER_ROLES" type="primary" :icon="'Plus'" @click="openCreate">
        创建训练
      </el-button>
    </template>
</PageHeader>

<div class="page">
  <div class="ic-card toolbar-card">
    <div class="tabs">
      <button
        v-for="tab in statusTabs"
        :key="tab.key || 'all'"
        type="button"
        class="tab"
        :class="{ active: activeStatus === tab.key }"
        @click="activeStatus = tab.key"
      >
        {{ tab.label }}
      </button>
    </div>
    <div class="toolbar-right">
      <el-select v-model="query.type" placeholder="训练类型" clearable style="width: 130px" @change="handleSearch">
        <el-option label="单人训练" value="SOLO" />
        <el-option label="组队训练" value="TEAM" />
      </el-select>
      <el-input
        v-model="query.keyword"
        placeholder="搜索训练名称"
        clearable
        style="width: 220px"
        :prefix-icon="'Search'"
        @keyup.enter="handleSearch"
        @clear="handleSearch"
      />
    </div>
  </div>

  <div v-loading="loading" class="training-grid">
    <div
      v-for="training in list"
      :key="training.trainingId"
      class="ic-card ic-card--hover training-card"
    >
      <div class="card-top">
        <StatusTag
          :label="trainingStatusMap[training.status as TrainingStatus].label"
          :tone="trainingStatusMap[training.status as TrainingStatus].tone"
        />
        <span class="type-badge">{{ trainingTypeMap[training.type] }}</span>
      </div>

      <h3 class="training-name" @click="router.push(`/trainings/${training.trainingId}`)">
        {{ training.name }}
      </h3>

      <div class="info-row">
        <el-icon><Clock /></el-icon>
        <span>{{ formatDateTime(training.startTime) }}</span>
      </div>
      <div class="info-row">
        <el-icon><Tickets /></el-icon>
        <span>{{ training.problemIds.length }} 道题目 · {{ training.leaderboardVisible ? '公开榜单' : '不公开榜单' }}</span>
      </div>
      <div class="info-row">
        <el-icon><User /></el-icon>
        <span>创建人：{{ training.creator.name }}</span>
      </div>

      <div class="problem-preview">
        <el-tag
          v-for="problemId in training.problemIds.slice(0, 3)"
          :key="problemId"
          size="small"
          effect="plain"
          round
        >
          {{ problemTitleMap.get(problemId)?.title ?? `题目 ${problemId}` }}
        </el-tag>
        <span v-if="training.problemIds.length > 3" class="more">
          +{{ training.problemIds.length - 3 }}
        </span>
      </div>

      <div class="card-actions">
        <el-button link type="primary" @click="router.push(`/trainings/${training.trainingId}`)">
          查看详情
        </el-button>
        <el-button
          v-if="training.status === 'DRAFT' && auth.hasRole(CONTENT_MANAGER_ROLES)"
          link
          type="primary"
          @click="openEdit(training)"
        >
          编辑
        </el-button>
        <el-button
          v-if="training.status === 'DRAFT' && auth.hasRole(CONTENT_MANAGER_ROLES)"
          link
          type="warning"
          @click="handlePublish(training)"
        >
          发布训练
        </el-button>
        <el-button v-if="canDelete(training)" link type="danger" @click="handleDelete(training)">
          删除
        </el-button>
      </div>
    </div>

    <EmptyState
      v-if="!loading && list.length === 0"
      class="grid-empty"
      title="暂无训练"
      description="创建一个新的训练，开始本周的算法训练计划"
    />
  </div>

  <el-pagination
    v-if="total > 0"
    v-model:current-page="page"
    v-model:page-size="size"
    :total="total"
    :page-sizes="[9, 18, 36]"
    layout="total, sizes, prev, pager, next"
    background
    class="page-pagination"
  />
</div>

<!-- 创建 / 编辑训练 -->
<el-dialog
  v-model="dialogVisible"
  :title="editingId ? '编辑训练' : '创建训练'"
  width="680px"
  top="8vh"
  destroy-on-close
>
  <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
    <el-form-item label="训练名称" prop="name">
      <el-input v-model="form.name" placeholder="如 第十周队内训练赛" />
    </el-form-item>
    <div class="form-grid">
      <el-form-item label="训练类型">
        <el-select v-model="form.type" style="width: 100%">
          <el-option label="单人训练" value="SOLO" />
          <el-option label="组队训练" value="TEAM" />
        </el-select>
      </el-form-item>
      <el-form-item label="封榜时间">
        <el-input-number
          v-model="form.freezeTime"
          :min="0"
          :max="180"
          :step="10"
          style="width: 100%"
        />
      </el-form-item>
    </div>
    <el-form-item label="训练时间">
      <el-date-picker
        v-model="timeRange"
        type="datetimerange"
        range-separator="至"
        start-placeholder="开始时间"
        end-placeholder="结束时间"
        value-format="YYYY-MM-DDTHH:mm:ss"
        style="width: 100%"
      />
    </el-form-item>
    <el-form-item label="选择题目" prop="problemIds">
      <el-select
        v-model="form.problemIds"
        multiple
        filterable
        collapse-tags
        collapse-tags-tooltip
        placeholder="从题库中选择训练题目"
        style="width: 100%"
      >
        <el-option
          v-for="problem in problems"
          :key="problem.problemId"
          :label="`${problem.ojProblemId} · ${problem.title}`"
          :value="problem.problemId"
        />
      </el-select>
    </el-form-item>
    <el-form-item label="榜单设置">
      <div class="switch-row">
        <el-switch v-model="form.leaderboardVisible" />
        <span class="muted">训练期间对参赛成员公开实时榜单</span>
      </div>
    </el-form-item>
  </el-form>
  <div class="create-tip">
    <el-icon><InfoFilled /></el-icon>
    {{
      editingId
        ? '仅草稿状态可修改，保存后仍为草稿，确认无误后再发布。'
        : '创建后训练为草稿状态，确认题目与时间后再发布。'
    }}
  </div>
  <template #footer>
    <el-button @click="dialogVisible = false">取消</el-button>
    <el-button type="primary" :loading="submitting" @click="handleSubmit">
      {{ editingId ? '保存修改' : '创建训练' }}
    </el-button>
  </template>
</el-dialog>
</template>

<style scoped>
.toolbar-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 18px;
  margin-bottom: 18px;
}

.tabs {
  display: flex;
  gap: 4px;
}

.tab {
  border: none;
  background: transparent;
  padding: 7px 14px;
  border-radius: var(--radius-pill);
  font-size: 13px;
  color: var(--text-sub);
  cursor: pointer;
}

.tab:hover {
  color: var(--primary-deep);
}

.tab.active {
  color: var(--primary-deep);
  font-weight: 600;
  background: var(--primary-lighter);
}

.toolbar-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.training-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
  min-height: 200px;
}

.training-card {
  display: flex;
  flex-direction: column;
  padding: 18px 20px 12px;
}

.card-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.type-badge {
  font-size: 11px;
  color: var(--text-muted);
  padding: 2px 8px;
  border-radius: var(--radius-pill);
  background: var(--bg-blue-soft);
}

.training-name {
  margin: 0 0 14px;
  font-size: 15px;
  font-weight: 600;
  color: var(--text-main);
  cursor: pointer;
  line-height: 1.5;
}

.training-name:hover {
  color: var(--primary-deep);
}

.info-row {
  display: flex;
  align-items: center;
  gap: 7px;
  font-size: 12px;
  color: var(--text-sub);
  margin-bottom: 7px;
}

.info-row .el-icon {
  color: var(--primary);
}

.problem-preview {
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
  margin: 12px 0 6px;
  min-height: 24px;
}

.more {
  font-size: 11px;
  color: var(--text-muted);
  align-self: center;
}

.card-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 4px;
  margin-top: auto;
  padding-top: 10px;
  border-top: 1px dashed var(--border);
}

.grid-empty {
  grid-column: 1 / -1;
}

.page-pagination {
  margin-top: 18px;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  column-gap: 18px;
}

.switch-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.create-tip {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--primary-deep);
}

@media (max-width: 1280px) {
  .training-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 860px) {
  .training-grid {
    grid-template-columns: 1fr;
  }
  .toolbar-card {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }
}
</style>
