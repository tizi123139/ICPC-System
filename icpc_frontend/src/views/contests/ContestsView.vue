<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import StatusTag from '@/components/StatusTag.vue'
import EmptyState from '@/components/EmptyState.vue'
import { createContest, getContests } from '@/api/contests'
import { getProblems } from '@/api/problems'
import { CONTENT_MANAGER_ROLES, contestCategoryMap, contestStatusMap, formatDateTime } from '@/utils/dict'
import { useAuthStore } from '@/stores/auth'
import type {
  ContestCategory,
  ContestInfo,
  ContestPayload,
  ContestQuery,
  ContestStatus,
  ProblemInfo,
} from '@/types'

const router = useRouter()
const auth = useAuthStore()

const loading = ref(false)
const list = ref<ContestInfo[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(9)

const query = reactive<ContestQuery>({
  keyword: '',
  category: '',
  page: 1,
  size: 9,
})

const categoryTabs: Array<{ key: ContestCategory | ''; label: string }> = [
  { key: '', label: '全部' },
  { key: 'SCHOLASTIC', label: '校赛' },
  { key: 'XCPC', label: 'XCPC 系列' },
  { key: 'NON_XCPC', label: '非 XCPC 校外赛' },
]

const activeCategory = computed({
  get: () => query.category ?? '',
  set: (value: ContestCategory | '') => {
    query.category = value
    page.value = 1
    load()
  },
})

/** 创建 / 发布赛事：POST /contests（权限：ASSISTANT_COACH+） */
const canPublish = computed(() => auth.hasRole(CONTENT_MANAGER_ROLES))

async function load() {
  loading.value = true
  try {
    // 接口：GET /contests（支持 category 筛选）
    const result = await getContests({ ...query, page: page.value, size: size.value })
    list.value = result.records
    total.value = result.total
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  page.value = 1
  load()
}

watch(page, load)

/* ============ 创建 / 发布赛事 ============ */
const dialogVisible = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance>()
const problemOptions = ref<ProblemInfo[]>([])

const emptyForm = (): ContestPayload => ({
  name: '',
  category: 'SCHOLASTIC',
  startTime: '',
  endTime: '',
  registrationDeadline: '',
  description: '',
  maxTeamSize: 3,
  problemIds: [],
  freezeMinutes: 60,
})

const form = reactive<ContestPayload>(emptyForm())

const rules: FormRules<ContestPayload> = {
  name: [{ required: true, message: '请输入赛事名称', trigger: 'blur' }],
}

async function openCreate() {
  Object.assign(form, emptyForm())
  dialogVisible.value = true
  if (problemOptions.value.length === 0) {
    // 接口：GET /problems（选择赛事题目）
    const result = await getProblems({ page: 1, size: 100 })
    problemOptions.value = result.records
  }
}

async function handleSubmit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  if (!form.startTime || !form.endTime) {
    ElMessage.warning('请选择赛事开始与结束时间')
    return
  }
  if (!form.registrationDeadline) {
    ElMessage.warning('请选择报名截止时间')
    return
  }

  submitting.value = true
  try {
    // 接口：POST /contests（创建 / 发布赛事，权限：ASSISTANT_COACH+）
    const contest = await createContest({ ...form })
    ElMessage.success('赛事已发布')
    dialogVisible.value = false
    router.push(`/contests/${contest.contestId}`)
  } finally {
    submitting.value = false
  }
}

onMounted(load)
</script>

<template>
<PageHeader title="赛事管理" subtitle="创建与发布校赛 / XCPC / 非 XCPC 赛事，管理报名、榜单与获奖证书">
    <template #actions>
      <el-button v-if="canPublish" type="primary" :icon="'Plus'" @click="openCreate">
        创建赛事
      </el-button>
    </template>
  </PageHeader>

<div class="page">
  <div class="ic-card toolbar-card">
    <div class="tabs">
      <button
        v-for="tab in categoryTabs"
        :key="tab.key || 'all'"
        type="button"
        class="tab"
        :class="{ active: activeCategory === tab.key }"
        @click="activeCategory = tab.key"
      >
        {{ tab.label }}
      </button>
    </div>
    <div class="toolbar-right">
      <el-input
        v-model="query.keyword"
        placeholder="搜索赛事名称"
        clearable
        style="width: 220px"
        :prefix-icon="'Search'"
        @keyup.enter="handleSearch"
        @clear="handleSearch"
      />
    </div>
  </div>

  <div v-loading="loading" class="contest-grid">
    <div
      v-for="contest in list"
      :key="contest.contestId"
      class="ic-card ic-card--hover contest-card"
    >
      <div class="card-top">
        <StatusTag
          :label="contestCategoryMap[contest.category as ContestCategory]?.label ?? contest.category"
          :tone="contestCategoryMap[contest.category as ContestCategory]?.tone ?? 'info'"
        />
        <StatusTag
          :label="contestStatusMap[contest.status as ContestStatus]?.label ?? contest.status"
          :tone="contestStatusMap[contest.status as ContestStatus]?.tone ?? 'info'"
        />
      </div>

      <h3 class="contest-name" @click="router.push(`/contests/${contest.contestId}`)">
        {{ contest.name }}
      </h3>
      <p class="contest-desc">{{ contest.description }}</p>

      <div class="info-row">
        <el-icon><Calendar /></el-icon>
        <span>{{ formatDateTime(contest.startTime) }} ~ {{ formatDateTime(contest.endTime) }}</span>
      </div>
      <div class="info-row">
        <el-icon><AlarmClock /></el-icon>
        <span>报名截止：{{ formatDateTime(contest.registrationDeadline) }}</span>
      </div>
      <div class="info-row">
        <el-icon><Trophy /></el-icon>
        <span>{{ contest.problemIds.length }} 题 · 队伍上限 {{ contest.maxTeamSize }} 人</span>
      </div>

      <div class="card-actions">
        <span class="muted">发布人：{{ contest.creator.name }}</span>
        <el-button link type="primary" @click="router.push(`/contests/${contest.contestId}`)">
          查看详情
        </el-button>
      </div>
    </div>

    <EmptyState
      v-if="!loading && list.length === 0"
      class="grid-empty"
      title="暂无赛事"
      description="助理教练及以上角色可创建并发布赛事"
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

<!-- 创建 / 发布赛事 -->
<el-dialog v-model="dialogVisible" title="创建赛事" width="680px" top="6vh" destroy-on-close>
  <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
    <el-form-item label="赛事名称" prop="name">
      <el-input v-model="form.name" placeholder="如 中南民族大学第十届校内程序设计竞赛" />
    </el-form-item>
    <div class="form-grid">
      <el-form-item label="赛事分类">
        <el-select v-model="form.category" style="width: 100%">
          <el-option label="校赛" value="SCHOLASTIC" />
          <el-option label="XCPC 系列" value="XCPC" />
          <el-option label="非 XCPC 校外赛" value="NON_XCPC" />
        </el-select>
      </el-form-item>
      <el-form-item label="队伍上限">
        <el-input-number v-model="form.maxTeamSize" :min="1" :max="3" style="width: 100%" />
      </el-form-item>
    </div>
    <div class="form-grid">
      <el-form-item label="开始时间">
        <el-date-picker
          v-model="form.startTime"
          type="datetime"
          placeholder="选择开始时间"
          value-format="YYYY-MM-DDTHH:mm:ss+08:00"
          style="width: 100%"
        />
      </el-form-item>
      <el-form-item label="结束时间">
        <el-date-picker
          v-model="form.endTime"
          type="datetime"
          placeholder="选择结束时间"
          value-format="YYYY-MM-DDTHH:mm:ss+08:00"
          style="width: 100%"
        />
      </el-form-item>
    </div>
    <div class="form-grid">
      <el-form-item label="报名截止">
        <el-date-picker
          v-model="form.registrationDeadline"
          type="datetime"
          placeholder="选择报名截止时间"
          value-format="YYYY-MM-DDTHH:mm:ss+08:00"
          style="width: 100%"
        />
      </el-form-item>
      <el-form-item label="封榜时长">
        <el-input-number
          v-model="form.freezeMinutes"
          :min="0"
          :step="10"
          style="width: 100%"
        />
        <span class="freeze-hint">分钟（0 表示不封榜）</span>
      </el-form-item>
    </div>
    <el-form-item label="赛事题目">
      <el-select
        v-model="form.problemIds"
        multiple
        filterable
        placeholder="选择赛事题目"
        style="width: 100%"
      >
        <el-option
          v-for="problem in problemOptions"
          :key="problem.problemId"
          :label="`${problem.ojProblemId} · ${problem.title}`"
          :value="problem.problemId"
        />
      </el-select>
    </el-form-item>
    <el-form-item label="赛事说明">
      <el-input
        v-model="form.description"
        type="textarea"
        :rows="3"
        placeholder="赛事规则、参赛要求、奖项设置等"
      />
    </el-form-item>
  </el-form>
  <div class="create-tip">
    <el-icon><InfoFilled /></el-icon>
    发布后会员可提交队伍 / 个人报名，报名需经干事及以上角色审核。
  </div>
  <template #footer>
    <el-button @click="dialogVisible = false">取消</el-button>
    <el-button type="primary" :loading="submitting" @click="handleSubmit">发布赛事</el-button>
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

.contest-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
  min-height: 200px;
}

.contest-card {
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

.contest-name {
  margin: 0 0 8px;
  font-size: 15px;
  font-weight: 600;
  color: var(--text-main);
  cursor: pointer;
  line-height: 1.5;
}

.contest-name:hover {
  color: var(--primary-deep);
}

.contest-desc {
  margin: 0 0 12px;
  font-size: 12.5px;
  line-height: 1.7;
  color: var(--text-sub);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 42px;
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

.card-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
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

.freeze-hint {
  margin-left: 8px;
  font-size: 12px;
  color: var(--text-muted);
}

.create-tip {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--primary-deep);
}

@media (max-width: 1280px) {
  .contest-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 860px) {
  .contest-grid {
    grid-template-columns: 1fr;
  }
  .toolbar-card {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }
}
</style>
