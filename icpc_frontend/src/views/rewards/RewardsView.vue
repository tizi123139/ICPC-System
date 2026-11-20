<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import EmptyState from '@/components/EmptyState.vue'
import { getMembers } from '@/api/members'
import { createPunishment, createReward, getPunishments, getRewards } from '@/api/rewards'
import { formatDate } from '@/utils/dict'
import type {
  MemberInfo,
  PunishmentRecord,
  PunishmentType,
  RewardCategory,
  RewardLevel,
  RewardRecord,
} from '@/types'

const activeTab = ref<'reward' | 'punishment'>('reward')
const loading = ref(false)
const rewards = ref<RewardRecord[]>([])
const punishments = ref<PunishmentRecord[]>([])
const memberOptions = ref<MemberInfo[]>([])

const rewardCategoryOptions: Array<{ label: string; value: RewardCategory }> = [
  { label: '竞赛获奖', value: 'COMPETITION' },
  { label: '训练表现', value: 'TRAINING' },
  { label: '学业成绩', value: 'ACADEMIC' },
  { label: '协会服务', value: 'SERVICE' },
  { label: '其他', value: 'OTHER' },
]
const rewardLevelOptions: Array<{ label: string; value: RewardLevel }> = [
  { label: '会级', value: 'ASSOCIATION' },
  { label: '校级', value: 'SCHOOL' },
  { label: '省级', value: 'PROVINCIAL' },
  { label: '国家级', value: 'NATIONAL' },
  { label: '国际级', value: 'INTERNATIONAL' },
]
const rewardCategoryLabels: Record<RewardCategory, string> = {
  COMPETITION: '竞赛获奖',
  TRAINING: '训练表现',
  ACADEMIC: '学业成绩',
  SERVICE: '协会服务',
  OTHER: '其他',
}
const rewardLevelLabels: Record<RewardLevel, string> = {
  ASSOCIATION: '会级',
  SCHOOL: '校级',
  PROVINCIAL: '省级',
  NATIONAL: '国家级',
  INTERNATIONAL: '国际级',
}
const punishmentTypeOptions: Array<{ label: string; value: PunishmentType }> = [
  { label: '警告', value: 'WARNING' },
  { label: '严重警告', value: 'SERIOUS_WARNING' },
  { label: '降级 / 撤销职务', value: 'REVOCATION' },
  { label: '除名', value: 'EXPULSION' },
]
const punishmentTypeLabels: Record<PunishmentType, string> = {
  WARNING: '警告',
  SERIOUS_WARNING: '严重警告',
  REVOCATION: '降级 / 撤销职务',
  EXPULSION: '除名',
}

/* ============ 录入奖励（POST /rewards，接口手册 12.1） ============ */
const rewardDialogVisible = ref(false)
const rewardSubmitting = ref(false)
const rewardFormRef = ref<FormInstance>()
const rewardForm = reactive<{
  memberId: number | null
  title: string
  category: RewardCategory
  level: RewardLevel
  amount: number
  description: string
  grantDate: string
  issuerName: string
}>({
  memberId: null,
  title: '',
  category: 'COMPETITION',
  level: 'SCHOOL',
  amount: 0,
  description: '',
  grantDate: '',
  issuerName: 'ICPC 协会',
})
const rewardRules: FormRules = {
  memberId: [{ required: true, message: '请选择会员', trigger: 'change' }],
  title: [{ required: true, message: '请填写奖励名称', trigger: 'blur' }],
  grantDate: [{ required: true, message: '请选择颁发日期', trigger: 'change' }],
  issuerName: [{ required: true, message: '请填写颁发单位', trigger: 'blur' }],
}

/* ============ 录入处分（POST /punishments，接口手册 12.2） ============ */
const punishmentDialogVisible = ref(false)
const punishmentSubmitting = ref(false)
const punishmentFormRef = ref<FormInstance>()
const punishmentForm = reactive<{
  memberId: number | null
  type: PunishmentType
  reason: string
  description: string
  effectiveDate: string
  expireDate: string
  issuerName: string
}>({
  memberId: null,
  type: 'WARNING',
  reason: '',
  description: '',
  effectiveDate: '',
  expireDate: '',
  issuerName: 'ICPC 协会组织部',
})
const punishmentRules: FormRules = {
  memberId: [{ required: true, message: '请选择会员', trigger: 'change' }],
  reason: [{ required: true, message: '请填写处分事由', trigger: 'blur' }],
  effectiveDate: [{ required: true, message: '请选择生效日期', trigger: 'change' }],
  expireDate: [{ required: true, message: '请选择解除日期', trigger: 'change' }],
  issuerName: [{ required: true, message: '请填写处分单位', trigger: 'blur' }],
}

async function load() {
  loading.value = true
  try {
    // 接口：GET /rewards、GET /punishments（权限：本人 / OFFICER+）
    const [rewardData, punishmentData] = await Promise.all([getRewards(), getPunishments()])
    rewards.value = rewardData
    punishments.value = punishmentData
  } finally {
    loading.value = false
  }
}

async function loadMemberOptions() {
  // 接口：GET /members（用于录入时的会员下拉）
  const result = await getMembers({ page: 1, size: 500 })
  memberOptions.value = result.records
}

function openRewardCreate() {
  Object.assign(rewardForm, {
    memberId: null,
    title: '',
    category: 'COMPETITION',
    level: 'SCHOOL',
    amount: 0,
    description: '',
    grantDate: '',
    issuerName: 'ICPC 协会',
  })
  rewardDialogVisible.value = true
}

function openPunishmentCreate() {
  Object.assign(punishmentForm, {
    memberId: null,
    type: 'WARNING',
    reason: '',
    description: '',
    effectiveDate: '',
    expireDate: '',
    issuerName: 'ICPC 协会组织部',
  })
  punishmentDialogVisible.value = true
}

async function submitReward() {
  if (!rewardFormRef.value) return
  await rewardFormRef.value.validate()
  rewardSubmitting.value = true
  try {
    // 接口：POST /rewards
    await createReward({ ...rewardForm, memberId: rewardForm.memberId! })
    ElMessage.success('奖励记录已录入')
    rewardDialogVisible.value = false
    load()
  } finally {
    rewardSubmitting.value = false
  }
}

async function submitPunishment() {
  if (!punishmentFormRef.value) return
  await punishmentFormRef.value.validate()
  punishmentSubmitting.value = true
  try {
    // 接口：POST /punishments
    await createPunishment({ ...punishmentForm, memberId: punishmentForm.memberId! })
    ElMessage.success('处分记录已录入')
    punishmentDialogVisible.value = false
    load()
  } finally {
    punishmentSubmitting.value = false
  }
}

onMounted(() => {
  load()
  loadMemberOptions()
})
</script>

<template>
  <PageHeader title="奖惩管理" subtitle="记录会员奖励与处分情况，干事及以上角色可录入">
    <template #actions>
      <el-button v-if="activeTab === 'reward'" type="primary" @click="openRewardCreate">
        录入奖励
      </el-button>
      <el-button v-else type="danger" plain @click="openPunishmentCreate">录入处分</el-button>
    </template>
  </PageHeader>

  <div class="page">
    <el-tabs v-model="activeTab" class="ic-card reward-tabs">
      <el-tab-pane label="奖励记录" name="reward">
        <el-table v-loading="loading" :data="rewards" row-key="rewardId">
          <el-table-column label="会员" min-width="150">
            <template #default="{ row }">
              <div class="member-cell">
                <div class="avatar">{{ row.memberName.slice(0, 1) }}</div>
                <div class="meta">
                  <strong>{{ row.memberName }}</strong>
                  <span class="mono">{{ row.studentId }}</span>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="title" label="奖励名称" min-width="180" show-overflow-tooltip />
          <el-table-column label="类别" width="110">
            <template #default="{ row }">{{ rewardCategoryLabels[row.category as RewardCategory] }}</template>
          </el-table-column>
          <el-table-column label="级别" width="100">
            <template #default="{ row }">
              <el-tag size="small" effect="plain">{{ rewardLevelLabels[row.level as RewardLevel] }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="金额" width="100">
            <template #default="{ row }">{{ row.amount > 0 ? `¥${row.amount}` : '—' }}</template>
          </el-table-column>
          <el-table-column prop="issuerName" label="颁发单位" width="140" show-overflow-tooltip />
          <el-table-column label="颁发日期" width="120">
            <template #default="{ row }">{{ formatDate(row.grantDate) }}</template>
          </el-table-column>
          <template #empty>
            <EmptyState title="暂无奖励记录" description="点击右上角「录入奖励」添加第一条记录" />
          </template>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="处分记录" name="punishment">
        <el-table v-loading="loading" :data="punishments" row-key="punishmentId">
          <el-table-column label="会员" min-width="150">
            <template #default="{ row }">
              <div class="member-cell">
                <div class="avatar">{{ row.memberName.slice(0, 1) }}</div>
                <div class="meta">
                  <strong>{{ row.memberName }}</strong>
                  <span class="mono">{{ row.studentId }}</span>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="处分类型" width="140">
            <template #default="{ row }">
              <el-tag size="small" type="danger" effect="plain">
                {{ punishmentTypeLabels[row.type as PunishmentType] }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="reason" label="事由" min-width="200" show-overflow-tooltip />
          <el-table-column prop="description" label="说明" min-width="200" show-overflow-tooltip />
          <el-table-column prop="issuerName" label="处分单位" width="160" show-overflow-tooltip />
          <el-table-column label="生效 / 解除" width="200">
            <template #default="{ row }">
              {{ formatDate(row.effectiveDate) }} ~ {{ formatDate(row.expireDate) }}
            </template>
          </el-table-column>
          <template #empty>
            <EmptyState title="暂无处分记录" description="点击右上角「录入处分」添加第一条记录" />
          </template>
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </div>

  <el-dialog v-model="rewardDialogVisible" title="录入奖励" width="560px">
    <el-form ref="rewardFormRef" :model="rewardForm" :rules="rewardRules" label-width="90px">
      <el-form-item label="会员" prop="memberId">
        <el-select v-model="rewardForm.memberId" filterable placeholder="选择会员" style="width: 100%">
          <el-option
            v-for="member in memberOptions"
            :key="member.memberId"
            :label="`${member.name}（${member.studentId}）`"
            :value="member.memberId"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="奖励名称" prop="title">
        <el-input v-model="rewardForm.title" placeholder="如：2026 校赛一等奖" />
      </el-form-item>
      <el-form-item label="类别">
        <el-select v-model="rewardForm.category" style="width: 100%">
          <el-option v-for="item in rewardCategoryOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="级别">
        <el-select v-model="rewardForm.level" style="width: 100%">
          <el-option v-for="item in rewardLevelOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="金额（元）">
        <el-input-number v-model="rewardForm.amount" :min="0" style="width: 100%" />
      </el-form-item>
      <el-form-item label="说明">
        <el-input v-model="rewardForm.description" type="textarea" :rows="2" placeholder="奖励说明（可选）" />
      </el-form-item>
      <el-form-item label="颁发日期" prop="grantDate">
        <el-date-picker v-model="rewardForm.grantDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
      </el-form-item>
      <el-form-item label="颁发单位" prop="issuerName">
        <el-input v-model="rewardForm.issuerName" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="rewardDialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="rewardSubmitting" @click="submitReward">保存</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="punishmentDialogVisible" title="录入处分" width="560px">
    <el-form ref="punishmentFormRef" :model="punishmentForm" :rules="punishmentRules" label-width="90px">
      <el-form-item label="会员" prop="memberId">
        <el-select v-model="punishmentForm.memberId" filterable placeholder="选择会员" style="width: 100%">
          <el-option
            v-for="member in memberOptions"
            :key="member.memberId"
            :label="`${member.name}（${member.studentId}）`"
            :value="member.memberId"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="处分类型">
        <el-select v-model="punishmentForm.type" style="width: 100%">
          <el-option v-for="item in punishmentTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="事由" prop="reason">
        <el-input v-model="punishmentForm.reason" type="textarea" :rows="2" placeholder="处分事由" />
      </el-form-item>
      <el-form-item label="说明">
        <el-input v-model="punishmentForm.description" type="textarea" :rows="2" placeholder="补充说明（可选）" />
      </el-form-item>
      <el-form-item label="生效日期" prop="effectiveDate">
        <el-date-picker v-model="punishmentForm.effectiveDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
      </el-form-item>
      <el-form-item label="解除日期" prop="expireDate">
        <el-date-picker v-model="punishmentForm.expireDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
      </el-form-item>
      <el-form-item label="处分单位" prop="issuerName">
        <el-input v-model="punishmentForm.issuerName" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="punishmentDialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="punishmentSubmitting" @click="submitPunishment">保存</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.reward-tabs {
  padding: 4px 16px 16px;
}

.member-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.avatar {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: var(--ic-primary-soft, #e8f1ff);
  color: var(--ic-primary, #2f6bff);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
}

.meta {
  display: flex;
  flex-direction: column;
  line-height: 1.4;
}

.mono {
  font-family: var(--ic-mono, 'JetBrains Mono', Consolas, monospace);
  font-size: 12px;
  color: var(--ic-text-muted, #8a94a6);
}
</style>
