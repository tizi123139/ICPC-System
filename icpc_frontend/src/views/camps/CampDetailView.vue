<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import StatusTag from '@/components/StatusTag.vue'
import EmptyState from '@/components/EmptyState.vue'
import {
  auditCampRegistration,
  checkinCamp,
  createCampTask,
  getCamp,
  getCampAttendance,
  getCampRegistrations,
  getMyCampRegistration,
  registerCamp,
  updateCampAttendance,
} from '@/api/camps'
import {
  CAMP_MANAGER_ROLES,
  CONTENT_MANAGER_ROLES,
  MEMBER_ROLES,
  applyStatusMap,
  attendanceStatusMap,
  campTypeMap,
  formatDate,
  formatDateTime,
} from '@/utils/dict'
import { useAuthStore } from '@/stores/auth'
import type {
  ApplyStatus,
  AttendanceRecord,
  AttendanceStatus,
  CampDetail,
  CampRegistration,
  CampTaskPayload,
} from '@/types'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const campId = Number(route.params.id)

const activeTab = ref('tasks')
const loading = ref(false)
const camp = ref<CampDetail | null>(null)

/** 报名审核 / 考勤管理：3.2 权限矩阵「审核集训报名」「补签/请假/标记缺勤」行（助理教练、会长/副会长、指导老师） */
const canManage = computed(() => auth.hasRole(CAMP_MANAGER_ROLES))
/** 发布集训每日任务：接口要求 ASSISTANT_COACH+ */
const canPublishTask = computed(() => auth.hasRole(CONTENT_MANAGER_ROLES))
/** 提交集训报名 / 签到打卡：3.2「提交寒暑假集训报名」「集训考勤打卡」一行（会员类角色） */
const canMemberAct = computed(() => auth.hasRole(MEMBER_ROLES))

const myRegistration = ref<CampRegistration | null>(null)
const registrations = ref<CampRegistration[]>([])
const attendance = ref<AttendanceRecord[]>([])

/** 报名截止时间是否已过 */
const registrationOpen = computed(() =>
  camp.value ? Date.now() <= new Date(camp.value.registrationDeadline).getTime() : false,
)
/** 已通过报名、进入参训名单后可签到打卡 */
const canCheckin = computed(() => canMemberAct.value && myRegistration.value?.status === 'APPROVED')
/** 未提交报名且在报名期内可提交报名 */
const canRegister = computed(
  () => canMemberAct.value && !myRegistration.value && registrationOpen.value,
)

const tasks = computed(() =>
  [...(camp.value?.tasks ?? [])].sort((a, b) => a.date.localeCompare(b.date)),
)
const pendingCount = computed(
  () => registrations.value.filter((item) => item.status === 'PENDING').length,
)

async function load() {
  loading.value = true
  try {
    // 接口：GET /camps/{campId}（集训详情，含每日任务列表）
    camp.value = await getCamp(campId)
    // 接口：GET /camps/{campId}/registrations/mine（判断本人报名状态）
    myRegistration.value = await getMyCampRegistration(campId)
  } finally {
    loading.value = false
  }
}

async function loadRegistrations() {
  // 接口：GET /camps/{campId}/registrations（报名列表，权限：助理教练、会长/副会长、指导老师）
  if (!canManage.value) return
  registrations.value = await getCampRegistrations(campId)
}

async function loadAttendance() {
  // 接口：GET /camps/{campId}/attendance（本人记录 / 干事及以上查看全体）
  attendance.value = await getCampAttendance(campId, canManage.value)
}

/* ============ 发布集训每日任务 ============ */
const taskVisible = ref(false)
const taskSubmitting = ref(false)
const taskFormRef = ref<FormInstance>()
const taskForm = reactive<CampTaskPayload>({ date: '', title: '', content: '' })

const taskRules: FormRules<CampTaskPayload> = {
  date: [{ required: true, message: '请选择任务日期', trigger: 'change' }],
  title: [{ required: true, message: '请输入任务标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入任务内容', trigger: 'blur' }],
}

function openTask() {
  Object.assign(taskForm, { date: '', title: '', content: '' })
  taskVisible.value = true
}

async function handleCreateTask() {
  if (!taskFormRef.value) return
  const valid = await taskFormRef.value.validate().catch(() => false)
  if (!valid) return

  taskSubmitting.value = true
  try {
    // 接口：POST /camps/{campId}/tasks（发布集训每日任务，权限：ASSISTANT_COACH+）
    await createCampTask(campId, { ...taskForm })
    ElMessage.success('每日任务已发布')
    taskVisible.value = false
    await load()
  } finally {
    taskSubmitting.value = false
  }
}

/* ============ 提交集训报名 ============ */
const registerVisible = ref(false)
const registerSubmitting = ref(false)
const registerRemark = ref('')

function openRegister() {
  registerRemark.value = ''
  registerVisible.value = true
}

async function handleRegister() {
  registerSubmitting.value = true
  try {
    // 接口：POST /camps/{campId}/register（提交集训报名，权限：会员类角色）
    await registerCamp(campId, registerRemark.value)
    ElMessage.success('报名申请已提交，请等待审核')
    registerVisible.value = false
    await load()
  } finally {
    registerSubmitting.value = false
  }
}

/* ============ 审核报名 ============ */
async function handleAudit(record: CampRegistration, action: 'APPROVE' | 'REJECT') {
  const label = action === 'APPROVE' ? '通过' : '驳回'
  const confirmed = await ElMessageBox.confirm(
    `确认${label}「${record.memberName}」的集训报名？`,
    '审核报名',
    { type: 'warning', confirmButtonText: '确认', cancelButtonText: '取消' },
  ).catch(() => false)
  if (!confirmed) return

  // 接口：POST /camps/{campId}/registrations/{applyId}/audit（权限：干事及以上）
  await auditCampRegistration(campId, record.applyId, action, '')
  ElMessage.success(`已${label}该报名申请`)
  await loadRegistrations()
}

/* ============ 签到打卡 ============ */
const checkinVisible = ref(false)
const checkinSubmitting = ref(false)
const checkinForm = reactive<{ date: string; remark: string }>({ date: '', remark: '' })

function today(): string {
  const now = new Date()
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`
}

function openCheckin() {
  checkinForm.date = today()
  checkinForm.remark = ''
  checkinVisible.value = true
}

async function handleCheckin() {
  if (!checkinForm.date) {
    ElMessage.warning('请选择打卡日期')
    return
  }
  checkinSubmitting.value = true
  try {
    // 接口：POST /camps/{campId}/checkin（签到打卡，权限：参训名单内成员）
    await checkinCamp(campId, { date: checkinForm.date, remark: checkinForm.remark })
    ElMessage.success('签到打卡成功')
    checkinVisible.value = false
    await loadAttendance()
  } finally {
    checkinSubmitting.value = false
  }
}

/* ============ 补签 / 请假 / 标记缺勤 ============ */
const attendanceVisible = ref(false)
const attendanceSubmitting = ref(false)
const attendanceTarget = ref<AttendanceRecord | null>(null)
const attendanceForm = reactive<{ action: AttendanceStatus; remark: string }>({
  action: 'MAKEUP',
  remark: '',
})

const attendanceActions: Array<{ value: AttendanceStatus; label: string }> = [
  { value: 'MAKEUP', label: '补签' },
  { value: 'LEAVE', label: '请假' },
  { value: 'ABSENT', label: '标记缺勤' },
]

function openAttendance(record: AttendanceRecord, action: AttendanceStatus) {
  attendanceTarget.value = record
  attendanceForm.action = action
  attendanceForm.remark = record.remark ?? ''
  attendanceVisible.value = true
}

async function handleAttendance() {
  if (!attendanceTarget.value) return
  attendanceSubmitting.value = true
  try {
    // 接口：PUT /camps/{campId}/attendance/{recordId}（补签 / 请假 / 标记缺勤，权限：干事及以上）
    await updateCampAttendance(
      campId,
      attendanceTarget.value.recordId,
      attendanceForm.action,
      attendanceForm.remark,
    )
    ElMessage.success('考勤记录已更新')
    attendanceVisible.value = false
    await loadAttendance()
  } finally {
    attendanceSubmitting.value = false
  }
}

watch(activeTab, (tab) => {
  if (tab === 'registration' && canManage.value && registrations.value.length === 0) {
    loadRegistrations()
  }
  if (tab === 'attendance') {
    loadAttendance()
  }
})

onMounted(load)
</script>

<template>
<PageHeader
    :title="camp?.name ?? '集训详情'"
    :subtitle="camp ? `${campTypeMap[camp.type]} · ${formatDate(camp.startDate)} ~ ${formatDate(camp.endDate)}` : ''"
  >
    <template #actions>
      <el-button :icon="'ArrowLeft'" @click="router.push('/camps')">返回列表</el-button>
      <el-button
        v-if="canCheckin"
        type="primary"
        :icon="'CircleCheck'"
        @click="openCheckin"
      >
        签到打卡
      </el-button>
      <el-button v-if="canRegister" type="primary" :icon="'EditPen'" @click="openRegister">
        提交集训报名
      </el-button>
      <el-button v-if="canPublishTask" :icon="'Plus'" @click="openTask">发布每日任务</el-button>
    </template>
  </PageHeader>

<div v-loading="loading" class="page">
  <template v-if="camp">
    <div class="ic-card overview">
      <div class="overview-item">
        <span>集训类型</span>
        <strong>{{ campTypeMap[camp.type] }}</strong>
      </div>
      <div class="overview-item">
        <span>集训地点</span>
        <strong>{{ camp.location }}</strong>
      </div>
      <div class="overview-item">
        <span>报名截止</span>
        <strong>{{ formatDateTime(camp.registrationDeadline) }}</strong>
      </div>
      <div class="overview-item">
        <span>发布人</span>
        <strong>{{ camp.creator.name }}</strong>
      </div>
      <p class="overview-desc">{{ camp.description }}</p>
    </div>

    <div class="ic-card tab-card">
      <el-tabs v-model="activeTab">
        <!-- 每日任务 -->
        <el-tab-pane :label="`每日任务（${tasks.length}）`" name="tasks">
          <div v-if="tasks.length" class="task-list">
            <div v-for="task in tasks" :key="task.taskId" class="task-item">
              <div class="task-head">
                <span class="task-date">{{ formatDate(task.date) }}</span>
                <strong class="task-title">{{ task.title }}</strong>
              </div>
              <p class="task-content">{{ task.content }}</p>
              <span class="task-meta">
                发布人：{{ task.publisher?.name ?? '—' }} · {{ formatDateTime(task.publishTime) }}
              </span>
            </div>
          </div>
          <EmptyState
            v-else
            title="暂无每日任务"
            description="助理教练及以上角色可在集训期间发布每日任务"
          />
        </el-tab-pane>

        <!-- 集训报名 -->
        <el-tab-pane
          :label="canManage ? `报名审核（${pendingCount}）` : '我的报名'"
          name="registration"
        >
          <template v-if="canManage">
            <el-table v-if="registrations.length" :data="registrations" row-key="applyId">
              <el-table-column prop="memberName" label="姓名" min-width="110" />
              <el-table-column prop="studentId" label="学号" width="140" />
              <el-table-column prop="grade" label="年级" width="100" />
              <el-table-column label="报名时间" width="170">
                <template #default="{ row }">{{ formatDateTime(row.applyTime) }}</template>
              </el-table-column>
              <el-table-column label="申请说明" min-width="180">
                <template #default="{ row }">
                  <span :class="{ muted: !row.remark }">{{ row.remark || '—' }}</span>
                </template>
              </el-table-column>
              <el-table-column label="状态" width="100">
                <template #default="{ row }">
                  <StatusTag
                    :label="applyStatusMap[row.status as ApplyStatus].label"
                    :tone="applyStatusMap[row.status as ApplyStatus].tone"
                  />
                </template>
              </el-table-column>
              <el-table-column label="操作" width="150" fixed="right">
                <template #default="{ row }">
                  <template v-if="row.status === 'PENDING'">
                    <el-button link type="primary" @click="handleAudit(row, 'APPROVE')">
                      通过
                    </el-button>
                    <el-button link type="danger" @click="handleAudit(row, 'REJECT')">
                      驳回
                    </el-button>
                  </template>
                  <span v-else class="muted">已处理</span>
                </template>
              </el-table-column>
              <template #empty>
                <EmptyState title="暂无报名记录" description="会员提交报名后将显示在此处" />
              </template>
            </el-table>
            <EmptyState
              v-else
              title="暂无报名记录"
              description="会员提交报名后将显示在此处，可进行通过 / 驳回操作"
            />
          </template>

          <div v-else class="my-registration">
            <template v-if="myRegistration">
              <div class="my-status">
                <span>报名状态</span>
                <StatusTag
                  :label="applyStatusMap[myRegistration.status as ApplyStatus].label"
                  :tone="applyStatusMap[myRegistration.status as ApplyStatus].tone"
                />
              </div>
              <div class="my-status">
                <span>提交时间</span>
                <strong>{{ formatDateTime(myRegistration.applyTime) }}</strong>
              </div>
              <p class="my-remark">申请说明：{{ myRegistration.remark || '—' }}</p>
            </template>
            <EmptyState
              v-else
              :title="registrationOpen ? '尚未报名' : '报名已截止'"
              :description="
                registrationOpen
                  ? '点击「提交集训报名」报名参加本次集训'
                  : `报名已于 ${formatDateTime(camp.registrationDeadline)} 截止`
              "
            />
          </div>
        </el-tab-pane>

        <!-- 考勤记录 -->
        <el-tab-pane label="考勤记录" name="attendance">
          <el-table v-if="attendance.length" :data="attendance" row-key="recordId">
            <el-table-column v-if="canManage" prop="memberName" label="姓名" min-width="110" />
            <el-table-column v-if="canManage" prop="studentId" label="学号" width="140" />
            <el-table-column label="日期" width="130">
              <template #default="{ row }">{{ formatDate(row.date) }}</template>
            </el-table-column>
            <el-table-column label="打卡时间" width="170">
              <template #default="{ row }">{{ formatDateTime(row.checkinTime) }}</template>
            </el-table-column>
            <el-table-column label="考勤状态" width="110">
              <template #default="{ row }">
                <StatusTag
                  :label="attendanceStatusMap[row.status as AttendanceStatus]?.label ?? row.status"
                  :tone="attendanceStatusMap[row.status as AttendanceStatus]?.tone ?? 'info'"
                />
              </template>
            </el-table-column>
            <el-table-column label="备注" min-width="160">
              <template #default="{ row }">
                <span :class="{ muted: !row.remark }">{{ row.remark || '—' }}</span>
              </template>
            </el-table-column>
            <el-table-column v-if="canManage" label="操作" width="220" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="openAttendance(row, 'MAKEUP')">
                  补签
                </el-button>
                <el-button link type="info" @click="openAttendance(row, 'LEAVE')">请假</el-button>
                <el-button link type="danger" @click="openAttendance(row, 'ABSENT')">
                  标记缺勤
                </el-button>
              </template>
            </el-table-column>
            <template #empty>
              <EmptyState title="暂无考勤记录" description="集训期间完成签到打卡后生成考勤记录" />
            </template>
          </el-table>
          <EmptyState
            v-else
            title="暂无考勤记录"
            :description="
              canCheckin ? '点击右上角「签到打卡」记录当日出勤' : '集训期间完成签到打卡后生成考勤记录'
            "
          />
        </el-tab-pane>
      </el-tabs>
    </div>
  </template>
</div>

<!-- 发布集训每日任务 -->
<el-dialog v-model="taskVisible" title="发布集训每日任务" width="620px" destroy-on-close>
  <el-form ref="taskFormRef" :model="taskForm" :rules="taskRules" label-width="88px">
    <el-form-item label="任务日期" prop="date">
      <el-date-picker
        v-model="taskForm.date"
        type="date"
        placeholder="选择任务日期"
        value-format="YYYY-MM-DD"
        style="width: 100%"
      />
    </el-form-item>
    <el-form-item label="任务标题" prop="title">
      <el-input v-model="taskForm.title" placeholder="如 Day2 动态规划专题" />
    </el-form-item>
    <el-form-item label="任务内容" prop="content">
      <el-input
        v-model="taskForm.content"
        type="textarea"
        :rows="4"
        placeholder="讲解内容、训练题目、注意事项等"
      />
    </el-form-item>
  </el-form>
  <template #footer>
    <el-button @click="taskVisible = false">取消</el-button>
    <el-button type="primary" :loading="taskSubmitting" @click="handleCreateTask">
      发布任务
    </el-button>
  </template>
</el-dialog>

<!-- 提交集训报名 -->
<el-dialog v-model="registerVisible" title="提交集训报名" width="520px" destroy-on-close>
  <el-form label-width="88px">
    <el-form-item label="申请说明">
      <el-input
        v-model="registerRemark"
        type="textarea"
        :rows="3"
        placeholder="简要说明报名原因、训练目标等（选填）"
      />
    </el-form-item>
  </el-form>
  <div class="register-tip">
    <el-icon><InfoFilled /></el-icon>
    报名需经助理教练及以上角色审核，审核通过后进入参训名单即可签到打卡。
  </div>
  <template #footer>
    <el-button @click="registerVisible = false">取消</el-button>
    <el-button type="primary" :loading="registerSubmitting" @click="handleRegister">
      提交报名
    </el-button>
  </template>
</el-dialog>

<!-- 签到打卡 -->
<el-dialog v-model="checkinVisible" title="签到打卡" width="480px" destroy-on-close>
  <el-form label-width="88px">
    <el-form-item label="打卡日期">
      <el-date-picker
        v-model="checkinForm.date"
        type="date"
        placeholder="选择打卡日期"
        value-format="YYYY-MM-DD"
        style="width: 100%"
      />
    </el-form-item>
    <el-form-item label="备注">
      <el-input v-model="checkinForm.remark" placeholder="选填" />
    </el-form-item>
  </el-form>
  <template #footer>
    <el-button @click="checkinVisible = false">取消</el-button>
    <el-button type="primary" :loading="checkinSubmitting" @click="handleCheckin">
      确认打卡
    </el-button>
  </template>
</el-dialog>

<!-- 补签 / 请假 / 标记缺勤 -->
<el-dialog v-model="attendanceVisible" title="调整考勤记录" width="480px" destroy-on-close>
  <el-form label-width="88px">
    <el-form-item label="考勤人">
      <span>{{ attendanceTarget?.memberName }}（{{ attendanceTarget?.studentId }}）</span>
    </el-form-item>
    <el-form-item label="考勤日期">
      <span>{{ formatDate(attendanceTarget?.date) }}</span>
    </el-form-item>
    <el-form-item label="操作">
      <el-select v-model="attendanceForm.action" style="width: 100%">
        <el-option
          v-for="item in attendanceActions"
          :key="item.value"
          :label="item.label"
          :value="item.value"
        />
      </el-select>
    </el-form-item>
    <el-form-item label="备注">
      <el-input v-model="attendanceForm.remark" type="textarea" :rows="2" placeholder="选填" />
    </el-form-item>
  </el-form>
  <template #footer>
    <el-button @click="attendanceVisible = false">取消</el-button>
    <el-button type="primary" :loading="attendanceSubmitting" @click="handleAttendance">
      确认
    </el-button>
  </template>
</el-dialog>
</template>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.overview {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  padding: 20px 22px;
}

.overview-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.overview-item span {
  font-size: 12px;
  color: var(--text-muted);
}

.overview-item strong {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-main);
}

.overview-desc {
  grid-column: 1 / -1;
  margin: 4px 0 0;
  font-size: 12.5px;
  line-height: 1.75;
  color: var(--text-sub);
}

.tab-card {
  padding: 8px 22px 20px;
}

.task-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.task-item {
  padding: 14px 16px;
  border: 1px solid var(--border);
  border-radius: var(--radius-card);
  background: var(--bg-card-soft, #fbfdff);
}

.task-head {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}

.task-date {
  font-size: 12px;
  color: var(--primary-deep);
  padding: 2px 10px;
  border-radius: var(--radius-pill);
  background: var(--primary-lighter);
}

.task-title {
  font-size: 14px;
  color: var(--text-main);
}

.task-content {
  margin: 0 0 8px;
  font-size: 12.5px;
  line-height: 1.75;
  color: var(--text-sub);
}

.task-meta {
  font-size: 11.5px;
  color: var(--text-muted);
}

.my-registration {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 8px 2px;
}

.my-status {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 13px;
  color: var(--text-sub);
}

.my-status strong {
  color: var(--text-main);
}

.my-remark {
  margin: 0;
  font-size: 12.5px;
  color: var(--text-sub);
}

.muted {
  color: var(--text-muted);
}

.register-tip {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--primary-deep);
}

@media (max-width: 1200px) {
  .overview {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
