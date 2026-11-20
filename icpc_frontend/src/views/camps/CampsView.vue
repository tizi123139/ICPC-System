<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import EmptyState from '@/components/EmptyState.vue'
import { createCamp, getCamps } from '@/api/camps'
import { CONTENT_MANAGER_ROLES, campTypeMap, formatDate } from '@/utils/dict'
import { useAuthStore } from '@/stores/auth'
import type { CampInfo, CampPayload, CampQuery, CampType } from '@/types'

const router = useRouter()
const auth = useAuthStore()

const loading = ref(false)
const list = ref<CampInfo[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(9)

const query = reactive<CampQuery>({
  keyword: '',
  type: '',
  page: 1,
  size: 9,
})

const typeTabs: Array<{ key: CampType | ''; label: string }> = [
  { key: '', label: '全部' },
  { key: 'WINTER', label: '寒假集训' },
  { key: 'SUMMER', label: '暑假集训' },
]

const activeType = computed({
  get: () => query.type ?? '',
  set: (value: CampType | '') => {
    query.type = value
    page.value = 1
    load()
  },
})

/** 发布集训通知：POST /camps（权限：ASSISTANT_COACH+，3.2 矩阵「创建 / 发布训练」一行） */
const canPublish = computed(() => auth.hasRole(CONTENT_MANAGER_ROLES))

async function load() {
  loading.value = true
  try {
    // 接口：GET /camps（type=WINTER / SUMMER 筛选）
    const result = await getCamps({ ...query, page: page.value, size: size.value })
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

/* ============ 发布集训通知 ============ */
const dialogVisible = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance>()
const dateRange = ref<[string, string] | null>(null)

const emptyForm = (): CampPayload => ({
  name: '',
  type: 'WINTER',
  startDate: '',
  endDate: '',
  registrationDeadline: '',
  location: '',
  description: '',
})

const form = reactive<CampPayload>(emptyForm())

const rules: FormRules<CampPayload> = {
  name: [{ required: true, message: '请输入集训名称', trigger: 'blur' }],
  location: [{ required: true, message: '请输入集训地点', trigger: 'blur' }],
}

function openCreate() {
  Object.assign(form, emptyForm())
  dateRange.value = null
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  if (!dateRange.value || dateRange.value.length !== 2) {
    ElMessage.warning('请选择集训起止日期')
    return
  }
  if (!form.registrationDeadline) {
    ElMessage.warning('请选择报名截止时间')
    return
  }

  form.startDate = dateRange.value[0]
  form.endDate = dateRange.value[1]

  submitting.value = true
  try {
    // 接口：POST /camps（发布集训通知，权限：ASSISTANT_COACH+）
    const camp = await createCamp({ ...form })
    ElMessage.success('集训通知已发布')
    dialogVisible.value = false
    router.push(`/camps/${camp.campId}`)
  } finally {
    submitting.value = false
  }
}

onMounted(load)
</script>

<template>
<PageHeader title="集训管理" subtitle="发布寒暑假集训通知、审核报名、发布每日任务并管理考勤">
    <template #actions>
      <el-button v-if="canPublish" type="primary" :icon="'Plus'" @click="openCreate">
        发布集训通知
      </el-button>
    </template>
</PageHeader>

<div class="page">
  <div class="ic-card toolbar-card">
    <div class="tabs">
      <button
        v-for="tab in typeTabs"
        :key="tab.key || 'all'"
        type="button"
        class="tab"
        :class="{ active: activeType === tab.key }"
        @click="activeType = tab.key"
      >
        {{ tab.label }}
      </button>
    </div>
    <div class="toolbar-right">
      <el-input
        v-model="query.keyword"
        placeholder="搜索集训名称"
        clearable
        style="width: 220px"
        :prefix-icon="'Search'"
        @keyup.enter="handleSearch"
        @clear="handleSearch"
      />
    </div>
  </div>

  <div v-loading="loading" class="camp-grid">
    <div v-for="camp in list" :key="camp.campId" class="ic-card ic-card--hover camp-card">
      <div class="card-top">
        <span class="type-badge">{{ campTypeMap[camp.type] }}</span>
        <span class="date-badge">{{ formatDate(camp.startDate) }} 起</span>
      </div>

      <h3 class="camp-name" @click="router.push(`/camps/${camp.campId}`)">{{ camp.name }}</h3>
      <p class="camp-desc">{{ camp.description }}</p>

      <div class="info-row">
        <el-icon><Calendar /></el-icon>
        <span>{{ formatDate(camp.startDate) }} ~ {{ formatDate(camp.endDate) }}</span>
      </div>
      <div class="info-row">
        <el-icon><Location /></el-icon>
        <span>{{ camp.location }}</span>
      </div>
      <div class="info-row">
        <el-icon><AlarmClock /></el-icon>
        <span>报名截止：{{ formatDate(camp.registrationDeadline) }}</span>
      </div>

      <div class="card-actions">
        <span class="muted">发布人：{{ camp.creator.name }}</span>
        <el-button link type="primary" @click="router.push(`/camps/${camp.campId}`)">
          查看详情
        </el-button>
      </div>
    </div>

    <EmptyState
      v-if="!loading && list.length === 0"
      class="grid-empty"
      title="暂无集训"
      description="助理教练及以上角色可发布寒假 / 暑假集训通知"
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

<!-- 发布集训通知 -->
<el-dialog v-model="dialogVisible" title="发布集训通知" width="660px" top="8vh" destroy-on-close>
  <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
    <el-form-item label="集训名称" prop="name">
      <el-input v-model="form.name" placeholder="如 2027 年寒假集训" />
    </el-form-item>
    <div class="form-grid">
      <el-form-item label="集训类型">
        <el-select v-model="form.type" style="width: 100%">
          <el-option label="寒假集训" value="WINTER" />
          <el-option label="暑假集训" value="SUMMER" />
        </el-select>
      </el-form-item>
      <el-form-item label="集训地点" prop="location">
        <el-input v-model="form.location" placeholder="如 计算机学院实验楼 A301" />
      </el-form-item>
    </div>
    <el-form-item label="集训时间">
      <el-date-picker
        v-model="dateRange"
        type="daterange"
        range-separator="至"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        value-format="YYYY-MM-DD"
        style="width: 100%"
      />
    </el-form-item>
    <el-form-item label="报名截止">
      <el-date-picker
        v-model="form.registrationDeadline"
        type="datetime"
        placeholder="选择报名截止时间"
        value-format="YYYY-MM-DDTHH:mm:ss+08:00"
        style="width: 100%"
      />
    </el-form-item>
    <el-form-item label="集训说明">
      <el-input
        v-model="form.description"
        type="textarea"
        :rows="3"
        placeholder="集训安排、训练强度、注意事项等"
      />
    </el-form-item>
  </el-form>
  <div class="create-tip">
    <el-icon><InfoFilled /></el-icon>
    发布后会员可提交集训报名，报名需经干事及以上角色审核。
  </div>
  <template #footer>
    <el-button @click="dialogVisible = false">取消</el-button>
    <el-button type="primary" :loading="submitting" @click="handleSubmit">发布通知</el-button>
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

.camp-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
  min-height: 200px;
}

.camp-card {
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
  color: var(--primary-deep);
  padding: 2px 10px;
  border-radius: var(--radius-pill);
  background: var(--primary-lighter);
}

.date-badge {
  font-size: 11px;
  color: var(--text-muted);
  padding: 2px 8px;
  border-radius: var(--radius-pill);
  background: var(--bg-blue-soft);
}

.camp-name {
  margin: 0 0 8px;
  font-size: 15px;
  font-weight: 600;
  color: var(--text-main);
  cursor: pointer;
  line-height: 1.5;
}

.camp-name:hover {
  color: var(--primary-deep);
}

.camp-desc {
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

.create-tip {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--primary-deep);
}

@media (max-width: 1280px) {
  .camp-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 860px) {
  .camp-grid {
    grid-template-columns: 1fr;
  }
  .toolbar-card {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }
}
</style>
