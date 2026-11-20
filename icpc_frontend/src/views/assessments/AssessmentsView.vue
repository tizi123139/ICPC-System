<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import StatusTag from '@/components/StatusTag.vue'
import EmptyState from '@/components/EmptyState.vue'
import { getMembers } from '@/api/members'
import {
  auditAssessmentResult,
  createAssessment,
  getAssessmentResults,
  getAssessments,
  publishAssessment,
  updateAssessmentResults,
} from '@/api/assessments'
import { RESIGN_APPROVAL_ROLES, applyStatusMap, formatDateTime } from '@/utils/dict'
import { useAuthStore } from '@/stores/auth'
import type {
  ApplyStatus,
  AssessmentInfo,
  AssessmentResultRecord,
  AssessmentType,
  MemberInfo,
} from '@/types'

const auth = useAuthStore()

const assessmentTypeLabels: Record<AssessmentType, string> = {
  OBSERVATION: '观察期考核',
  REGULAR: '常规考核',
  PROMOTION: '晋级考核',
}

const loading = ref(false)
const assessments = ref<AssessmentInfo[]>([])

/** 创建 / 发布 / 录入成绩：助理教练、会长/副会长、指导老师 */
const canManage = computed(() => auth.hasRole(['ASSISTANT_COACH', 'PRESIDENT', 'ADVISOR']))
/** 审核考核结果：仅会长/副会长、指导老师（接口手册 13） */
const canAudit = computed(() => auth.hasRole(RESIGN_APPROVAL_ROLES))

/* ============ 创建考核（POST /assessments，接口手册 13） ============ */
const createVisible = ref(false)
const submitting = ref(false)
const createFormRef = ref<FormInstance>()
const createForm = reactive<{
  name: string
  type: AssessmentType
  description: string
  startTime: string
  endTime: string
  passScore: number
}>({
  name: '',
  type: 'OBSERVATION',
  description: '',
  startTime: '',
  endTime: '',
  passScore: 60,
})
const createRules: FormRules = {
  name: [{ required: true, message: '请填写考核名称', trigger: 'blur' }],
  startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  endTime: [{ required: true, message: '请选择结束时间', trigger: 'change' }],
}

/* ============ 成绩管理 ============ */
const resultsVisible = ref(false)
const resultsLoading = ref(false)
const currentAssessment = ref<AssessmentInfo | null>(null)
const results = ref<AssessmentResultRecord[]>([])
const memberOptions = ref<MemberInfo[]>([])

/** 批量录入成绩行（PUT /assessments/{id}/results，接口手册 13） */
const entryRows = reactive<Array<{ memberId: number | null; score: number; remark: string }>>([])
const entrySubmitting = ref(false)

/** 审核成绩（POST /assessments/{id}/results/{resultId}/audit） */
const auditVisible = ref(false)
const auditSubmitting = ref(false)
const auditTarget = ref<AssessmentResultRecord | null>(null)
const auditForm = reactive<{ status: 'APPROVED' | 'REJECTED'; auditRemark: string }>({
  status: 'APPROVED',
  auditRemark: '',
})

async function load() {
  loading.value = true
  try {
    // 接口：GET /assessments（已登录；普通会员仅见已发布，由后端过滤）
    assessments.value = await getAssessments()
  } finally {
    loading.value = false
  }
}

async function loadMemberOptions() {
  // 接口：GET /members（用于录入成绩时的会员下拉）
  const result = await getMembers({ page: 1, size: 500 })
  memberOptions.value = result.records
}

function openCreate() {
  Object.assign(createForm, {
    name: '',
    type: 'OBSERVATION',
    description: '',
    startTime: '',
    endTime: '',
    passScore: 60,
  })
  createVisible.value = true
}

async function submitCreate() {
  if (!createFormRef.value) return
  await createFormRef.value.validate()
  submitting.value = true
  try {
    // 接口：POST /assessments
    await createAssessment({ ...createForm })
    ElMessage.success('考核已创建（草稿状态）')
    createVisible.value = false
    load()
  } finally {
    submitting.value = false
  }
}

async function handlePublish(row: AssessmentInfo) {
  // 接口：POST /assessments/{assessmentId}/publish
  await publishAssessment(row.assessmentId)
  ElMessage.success('考核已发布')
  load()
}

async function openResults(row: AssessmentInfo) {
  currentAssessment.value = row
  entryRows.length = 0
  resultsVisible.value = true
  await loadResults()
}

async function loadResults() {
  if (!currentAssessment.value) return
  resultsLoading.value = true
  try {
    // 接口：GET /assessments/{assessmentId}/results
    results.value = await getAssessmentResults(currentAssessment.value.assessmentId)
  } finally {
    resultsLoading.value = false
  }
}

function addEntryRow() {
  entryRows.push({ memberId: null, score: 80, remark: '' })
}

function removeEntryRow(index: number) {
  entryRows.splice(index, 1)
}

async function submitEntries() {
  if (!currentAssessment.value) return
  const valid = entryRows.filter((row) => row.memberId != null)
  if (valid.length === 0) {
    ElMessage.warning('请至少添加一条成绩记录')
    return
  }
  entrySubmitting.value = true
  try {
    // 接口：PUT /assessments/{assessmentId}/results —— 批量录入
    await updateAssessmentResults(currentAssessment.value.assessmentId, {
      results: valid.map((row) => ({
        memberId: row.memberId!,
        score: row.score,
        remark: row.remark || null,
      })),
    })
    ElMessage.success('成绩已录入')
    entryRows.length = 0
    loadResults()
  } finally {
    entrySubmitting.value = false
  }
}

function openAudit(row: AssessmentResultRecord) {
  auditTarget.value = row
  auditForm.status = 'APPROVED'
  auditForm.auditRemark = ''
  auditVisible.value = true
}

async function submitAudit() {
  if (!currentAssessment.value || !auditTarget.value) return
  auditSubmitting.value = true
  try {
    // 接口：POST /assessments/{assessmentId}/results/{resultId}/audit
    await auditAssessmentResult(currentAssessment.value.assessmentId, auditTarget.value.resultId, {
      ...auditForm,
    })
    ElMessage.success('审核完成')
    auditVisible.value = false
    loadResults()
  } finally {
    auditSubmitting.value = false
  }
}

onMounted(() => {
  load()
  loadMemberOptions()
})
</script>

<template>
  <PageHeader title="考核管理" subtitle="创建观察期 / 常规考核，录入并审核考核成绩">
    <template #actions>
      <el-button v-if="canManage" type="primary" :icon="'Plus'" @click="openCreate">创建考核</el-button>
    </template>
  </PageHeader>

  <div class="page">
    <div class="ic-card">
      <el-table v-loading="loading" :data="assessments" row-key="assessmentId">
        <el-table-column prop="name" label="考核名称" min-width="220" show-overflow-tooltip />
        <el-table-column label="类型" width="120">
          <template #default="{ row }">{{ assessmentTypeLabels[row.type as AssessmentType] }}</template>
        </el-table-column>
        <el-table-column label="起止时间" min-width="300">
          <template #default="{ row }">
            {{ formatDateTime(row.startTime) }} ~ {{ formatDateTime(row.endTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="passScore" label="通过分" width="90" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <StatusTag
              :label="row.status === 'PUBLISHED' ? '已发布' : '草稿'"
              :tone="row.status === 'PUBLISHED' ? 'success' : 'info'"
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button v-if="canManage && row.status === 'DRAFT'" link type="success" @click="handlePublish(row)">
              发布
            </el-button>
            <el-button link type="primary" @click="openResults(row)">查看成绩</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <EmptyState title="暂无考核" description="助理教练及以上角色可创建考核" />
        </template>
      </el-table>
    </div>
  </div>

  <el-dialog v-model="createVisible" title="创建考核" width="560px">
    <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="90px">
      <el-form-item label="名称" prop="name">
        <el-input v-model="createForm.name" placeholder="如：2026 春季观察期考核" />
      </el-form-item>
      <el-form-item label="类型">
        <el-select v-model="createForm.type" style="width: 100%">
          <el-option label="观察期考核" value="OBSERVATION" />
          <el-option label="常规考核" value="REGULAR" />
          <el-option label="晋级考核" value="PROMOTION" />
        </el-select>
      </el-form-item>
      <el-form-item label="说明">
        <el-input v-model="createForm.description" type="textarea" :rows="2" placeholder="考核说明（可选）" />
      </el-form-item>
      <el-form-item label="开始时间" prop="startTime">
        <el-date-picker v-model="createForm.startTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%" />
      </el-form-item>
      <el-form-item label="结束时间" prop="endTime">
        <el-date-picker v-model="createForm.endTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%" />
      </el-form-item>
      <el-form-item label="通过分">
        <el-input-number v-model="createForm.passScore" :min="0" :max="100" style="width: 100%" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="createVisible = false">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="submitCreate">保存</el-button>
    </template>
  </el-dialog>

  <el-drawer
    v-model="resultsVisible"
    :title="`考核成绩 - ${currentAssessment?.name ?? ''}`"
    size="760px"
  >
    <el-table v-loading="resultsLoading" :data="results" row-key="resultId">
      <el-table-column label="会员" min-width="140">
        <template #default="{ row }">
          <strong>{{ row.memberName }}</strong>
          <span class="mono">{{ row.studentId }}</span>
        </template>
      </el-table-column>
      <el-table-column label="得分" width="90">
        <template #default="{ row }">
          <span :class="row.score >= (currentAssessment?.passScore ?? 60) ? 'score-pass' : 'score-fail'">
            {{ row.score }}
          </span>
        </template>
      </el-table-column>
      <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip>
        <template #default="{ row }">{{ row.remark ?? '—' }}</template>
      </el-table-column>
      <el-table-column label="审核状态" width="100">
        <template #default="{ row }">
          <StatusTag
            :label="applyStatusMap[row.auditStatus as ApplyStatus].label"
            :tone="applyStatusMap[row.auditStatus as ApplyStatus].tone"
          />
        </template>
      </el-table-column>
      <el-table-column label="审核意见" min-width="130" show-overflow-tooltip>
        <template #default="{ row }">{{ row.auditRemark ?? '—' }}</template>
      </el-table-column>
      <el-table-column v-if="canAudit" label="操作" width="90" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openAudit(row)">审核</el-button>
        </template>
      </el-table-column>
      <template #empty>
        <EmptyState title="暂无成绩" description="尚未录入该考核的成绩" />
      </template>
    </el-table>

    <template v-if="canManage">
      <div class="entry-title">批量录入成绩</div>
      <div v-for="(row, index) in entryRows" :key="index" class="entry-row">
        <el-select v-model="row.memberId" filterable placeholder="选择会员" style="width: 220px">
          <el-option
            v-for="member in memberOptions"
            :key="member.memberId"
            :label="`${member.name}（${member.studentId}）`"
            :value="member.memberId"
          />
        </el-select>
        <el-input-number v-model="row.score" :min="0" :max="100" />
        <el-input v-model="row.remark" placeholder="备注（可选）" style="flex: 1" />
        <el-button link type="danger" @click="removeEntryRow(index)">移除</el-button>
      </div>
      <div class="entry-actions">
        <el-button :icon="'Plus'" @click="addEntryRow">添加一行</el-button>
        <el-button
          type="primary"
          :loading="entrySubmitting"
          :disabled="entryRows.length === 0"
          @click="submitEntries"
        >
          提交录入
        </el-button>
      </div>
    </template>
  </el-drawer>

  <el-dialog v-model="auditVisible" title="审核考核成绩" width="480px">
    <p class="dialog-tip">
      审核 {{ auditTarget?.memberName }} 的成绩（{{ auditTarget?.score }} 分）
    </p>
    <el-radio-group v-model="auditForm.status">
      <el-radio-button value="APPROVED">通过</el-radio-button>
      <el-radio-button value="REJECTED">驳回</el-radio-button>
    </el-radio-group>
    <el-input
      v-model="auditForm.auditRemark"
      type="textarea"
      :rows="2"
      placeholder="审核意见（可选）"
      style="margin-top: 12px"
    />
    <template #footer>
      <el-button @click="auditVisible = false">取消</el-button>
      <el-button type="primary" :loading="auditSubmitting" @click="submitAudit">提交</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.mono {
  font-family: var(--ic-mono, 'JetBrains Mono', Consolas, monospace);
  font-size: 12px;
  color: var(--ic-text-muted, #8a94a6);
  margin-left: 6px;
}

.score-pass {
  color: var(--ic-success, #22a06b);
  font-weight: 600;
}

.score-fail {
  color: var(--ic-danger, #e5484d);
  font-weight: 600;
}

.entry-title {
  margin: 20px 0 10px;
  font-weight: 600;
}

.entry-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.entry-actions {
  display: flex;
  gap: 10px;
  margin-top: 6px;
}

.dialog-tip {
  margin: 0 0 12px;
  color: var(--ic-text-muted, #8a94a6);
  font-size: 13px;
}
</style>
