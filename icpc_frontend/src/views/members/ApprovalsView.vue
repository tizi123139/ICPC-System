<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import StatusTag from '@/components/StatusTag.vue'
import EmptyState from '@/components/EmptyState.vue'
import { auditApply, getApprovals } from '@/api/members'
import { RESIGN_APPROVAL_ROLES, applyStatusMap, formatDateTime } from '@/utils/dict'
import { useAuthStore } from '@/stores/auth'
import type { ApplyRecord, ApplyStatus } from '@/types'

const auth = useAuthStore()

const loading = ref(false)
const records = ref<ApplyRecord[]>([])
type ApplyTypeTab = 'ALL' | 'JOIN' | 'RESIGN'

const activeType = ref<ApplyTypeTab>('ALL')
const statusFilter = ref<ApplyStatus | ''>('PENDING')

const typeTabs: Array<{ key: ApplyTypeTab; label: string }> = [
  { key: 'ALL', label: '全部申请' },
  { key: 'JOIN', label: '入会申请' },
  { key: 'RESIGN', label: '退会申请' },
]

function switchType(key: ApplyTypeTab) {
  activeType.value = key
}

const dialogVisible = ref(false)
const submitting = ref(false)
const current = ref<ApplyRecord | null>(null)
const auditForm = reactive<{ action: 'APPROVE' | 'REJECT'; remark: string }>({
  action: 'APPROVE',
  remark: '',
})

const filtered = computed(() =>
  records.value.filter((item) => {
    const typeMatched = activeType.value === 'ALL' || item.type === activeType.value
    const statusMatched = !statusFilter.value || item.status === statusFilter.value
    return typeMatched && statusMatched
  }),
)

/** 权限矩阵 v2：入会审批干事即可，退会/除名审批仅会长/副会长、指导老师 */
function canAudit(row: ApplyRecord) {
  return row.type === 'JOIN' || auth.hasRole(RESIGN_APPROVAL_ROLES)
}

async function load() {
  loading.value = true
  try {
    // 接口：GET /members/approvals
    records.value = await getApprovals()
  } finally {
    loading.value = false
  }
}

function openAudit(row: ApplyRecord, action: 'APPROVE' | 'REJECT') {
  current.value = row
  auditForm.action = action
  auditForm.remark = ''
  dialogVisible.value = true
}

async function submitAudit() {
  if (!current.value) return
  if (!canAudit(current.value)) {
    ElMessage.warning('退会 / 除名审批仅会长/副会长与指导老师可操作')
    return
  }
  submitting.value = true
  try {
    // 接口：POST /members/approvals/{applyId}/audit
    await auditApply(current.value.applyId, auditForm.action, auditForm.remark)
    ElMessage.success(auditForm.action === 'APPROVE' ? '已通过申请' : '已驳回申请')
    dialogVisible.value = false
    load()
  } finally {
    submitting.value = false
  }
}

onMounted(load)
</script>

<template>
<PageHeader title="入会审批" subtitle="审核入会与退会申请，审批结果将同步至会员状态">
    <template #actions>
      <el-radio-group v-model="statusFilter" size="small">
        <el-radio-button value="PENDING">待审核</el-radio-button>
        <el-radio-button value="APPROVED">已通过</el-radio-button>
        <el-radio-button value="REJECTED">已驳回</el-radio-button>
        <el-radio-button value="">全部</el-radio-button>
      </el-radio-group>
    </template>
</PageHeader>

<div class="page">
  <div class="ic-card">
    <div class="toolbar">
      <div class="tabs">
        <button
          v-for="tab in typeTabs"
          :key="tab.key"
          type="button"
          class="tab"
          :class="{ active: activeType === tab.key }"
          @click="switchType(tab.key)"
        >
          {{ tab.label }}
        </button>
      </div>
      <span class="muted">共 {{ filtered.length }} 条记录</span>
    </div>

    <el-table v-loading="loading" :data="filtered" row-key="applyId">
      <el-table-column label="申请人" min-width="150">
        <template #default="{ row }">
          <div class="applicant">
            <div class="avatar">{{ row.memberName.slice(0, 1) }}</div>
            <div class="meta">
              <strong>{{ row.memberName }}</strong>
              <span class="mono">{{ row.studentId }}</span>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="申请类型" width="110">
        <template #default="{ row }">
          <StatusTag
            :label="row.type === 'JOIN' ? '入会申请' : '退会申请'"
            :tone="row.type === 'JOIN' ? 'primary' : 'warning'"
          />
        </template>
      </el-table-column>
      <el-table-column prop="reason" label="申请说明" min-width="260" show-overflow-tooltip />
      <el-table-column label="申请时间" width="160">
        <template #default="{ row }">{{ formatDateTime(row.applyTime) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <StatusTag
            :label="applyStatusMap[row.status as ApplyStatus].label"
            :tone="applyStatusMap[row.status as ApplyStatus].tone"
          />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <template v-if="row.status === 'PENDING' && canAudit(row)">
            <el-button link type="primary" @click="openAudit(row, 'APPROVE')">通过</el-button>
            <el-button link type="danger" @click="openAudit(row, 'REJECT')">驳回</el-button>
          </template>
          <span v-else-if="row.status === 'PENDING'" class="muted">仅会长/副会长可审批</span>
          <span v-else class="muted">已处理</span>
        </template>
      </el-table-column>
      <template #empty>
        <EmptyState title="暂无申请" description="当前筛选条件下没有待处理记录" />
      </template>
    </el-table>
  </div>
</div>

<el-dialog v-model="dialogVisible" title="审批申请" width="520px">
  <div v-if="current" class="audit-summary">
    <div class="row"><span>申请人</span><strong>{{ current.memberName }}（{{ current.studentId }}）</strong></div>
    <div class="row"><span>申请类型</span><strong>{{ current.type === 'JOIN' ? '入会申请' : '退会申请' }}</strong></div>
    <div class="row"><span>申请说明</span><strong class="reason">{{ current.reason }}</strong></div>
  </div>
  <el-form label-position="top">
    <el-form-item label="审批意见（可选）">
      <el-input
        v-model="auditForm.remark"
        type="textarea"
        :rows="3"
        maxlength="120"
        show-word-limit
        placeholder="填写审批备注，将同步给申请人"
      />
    </el-form-item>
  </el-form>
  <div class="audit-tip">
    <el-icon><InfoFilled /></el-icon>
    {{ auditForm.action === 'APPROVE' ? '通过后申请人将收到通知，会员状态自动更新。' : '驳回后申请人可在补充材料后重新提交。' }}
  </div>
  <template #footer>
    <el-button @click="dialogVisible = false">取消</el-button>
    <el-button
      :type="auditForm.action === 'APPROVE' ? 'primary' : 'danger'"
      :loading="submitting"
      @click="submitAudit"
    >
      {{ auditForm.action === 'APPROVE' ? '确认通过' : '确认驳回' }}
    </el-button>
  </template>
</el-dialog>
</template>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid var(--border);
}

.tabs {
  display: flex;
  gap: 6px;
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

.el-table {
  padding: 0 8px 8px;
}

.applicant {
  display: flex;
  align-items: center;
  gap: 10px;
}

.avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  color: var(--primary-deep);
  background: var(--primary-light);
}

.meta {
  display: flex;
  flex-direction: column;
  line-height: 1.35;
}

.meta span {
  font-size: 11px;
  color: var(--text-muted);
}

.audit-summary {
  padding: 14px 16px;
  border-radius: var(--radius-md);
  background: var(--bg-blue-softer);
  margin-bottom: 16px;
}

.audit-summary .row {
  display: flex;
  gap: 14px;
  padding: 4px 0;
  font-size: 13px;
}

.audit-summary .row span {
  width: 64px;
  flex-shrink: 0;
  color: var(--text-sub);
}

.audit-summary .reason {
  font-weight: 400;
  line-height: 1.7;
}

.audit-tip {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--primary-deep);
}
</style>
