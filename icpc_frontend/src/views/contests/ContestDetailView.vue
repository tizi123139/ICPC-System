<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import StatusTag from '@/components/StatusTag.vue'
import EmptyState from '@/components/EmptyState.vue'
import {
  auditContestRegistration,
  downloadContestCertificates,
  generateCertificates,
  getContest,
  getContestLeaderboard,
  getContestRegistrations,
  getContestResults,
  getMyContestRegistration,
  registerContest,
  submitContestCode,
  updateContestResults,
} from '@/api/contests'
import { getProblems } from '@/api/problems'
import { getTeams } from '@/api/teams'
import {
  CERTIFICATE_MANAGER_ROLES,
  CONTEST_MANAGER_ROLES,
  CONTEST_REGISTER_ROLES,
  applyStatusMap,
  contestApplyTypeMap,
  contestCategoryMap,
  contestStatusMap,
  difficultyMap,
  formatDateTime,
  languages,
} from '@/utils/dict'
import { useAuthStore } from '@/stores/auth'
import type {
  ApplyStatus,
  CertificateBundle,
  ContestApplyType,
  ContestCategory,
  ContestInfo,
  ContestRegistration,
  ContestRegisterPayload,
  ContestResult,
  ContestStatus,
  Difficulty,
  LeaderboardResult,
  ProblemInfo,
  TeamInfo,
} from '@/types'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const contestId = Number(route.params.id)

const loading = ref(false)
const contest = ref<ContestInfo | null>(null)
const problems = ref<ProblemInfo[]>([])
const leaderboard = ref<LeaderboardResult | null>(null)
const myRegistration = ref<ContestRegistration | null>(null)
const registrations = ref<ContestRegistration[]>([])
const results = ref<ContestResult[]>([])
const activeTab = ref<'problems' | 'registration' | 'leaderboard' | 'results'>('problems')

/** 报名审核 / 报名列表 / 成绩登记：3.2 权限矩阵「赛事报名审核」「报名列表查询」「赛后登记获奖/成绩」行（助理教练、会长/副会长、指导老师） */
const canManage = computed(() => auth.hasRole(CONTEST_MANAGER_ROLES))
/** 批量生成证书：权限表「批量生成获奖证书」一行（仅会长/副会长）；下载证书本人可用 */
const canGenerateCert = computed(() => auth.hasRole(CERTIFICATE_MANAGER_ROLES))
/** 队伍 / 个人报名：3.2 权限矩阵「校赛 / XCPC 报名」一行（正式会员、队长） */
const canRegisterRole = computed(() => auth.hasRole(CONTEST_REGISTER_ROLES))

const registrationOpen = computed(() =>
  contest.value ? Date.now() <= new Date(contest.value.registrationDeadline).getTime() : false,
)
const canRegister = computed(
  () => canRegisterRole.value && !myRegistration.value && registrationOpen.value,
)
/** 已通过报名且在比赛进行中可提交代码 */
const canSubmit = computed(
  () =>
    myRegistration.value?.status === 'APPROVED' &&
    (contest.value?.status === 'ONGOING' || contest.value?.status === 'PUBLISHED'),
)

const problemsInContest = computed(() => {
  if (!contest.value) return []
  return contest.value.problemIds
    .map((id) => problems.value.find((item) => item.problemId === id))
    .filter((item): item is ProblemInfo => Boolean(item))
})
const pendingCount = computed(
  () => registrations.value.filter((item) => item.status === 'PENDING').length,
)

async function load() {
  loading.value = true
  try {
    // 接口：GET /contests/{contestId}、GET /problems、GET /contests/{contestId}/leaderboard
    const [contestData, problemData, leaderboardData, myReg] = await Promise.all([
      getContest(contestId),
      getProblems({ page: 1, size: 100 }),
      getContestLeaderboard(contestId),
      getMyContestRegistration(contestId),
    ])
    contest.value = contestData
    problems.value = problemData.records
    leaderboard.value = leaderboardData
    myRegistration.value = myReg
  } finally {
    loading.value = false
  }
}

async function loadRegistrations() {
  // 接口：GET /contests/{contestId}/registrations（权限：干事及以上）
  if (!canManage.value) return
  registrations.value = await getContestRegistrations(contestId)
}

async function loadResults() {
  // 接口：GET /contests/{contestId}/results（成绩登记回显）
  if (!canManage.value) return
  results.value = await getContestResults(contestId)
}

/* ============ 队伍 / 个人报名 ============ */
const registerVisible = ref(false)
const registerSubmitting = ref(false)
const registerForm = reactive<ContestRegisterPayload>({
  applyType: 'SOLO',
  teamId: null,
  remark: '',
})
const myTeams = ref<TeamInfo[]>([])

async function openRegister() {
  registerForm.applyType = 'SOLO'
  registerForm.teamId = null
  registerForm.remark = ''
  registerVisible.value = true
  if (myTeams.value.length === 0) {
    // 接口：GET /teams（选择要报名的队伍）
    const result = await getTeams({ page: 1, size: 100 })
    myTeams.value = result.records
  }
}

async function handleRegister() {
  if (registerForm.applyType === 'TEAM' && !registerForm.teamId) {
    ElMessage.warning('请选择报名的队伍')
    return
  }
  registerSubmitting.value = true
  try {
    // 接口：POST /contests/{contestId}/register（队伍 / 个人报名）
    await registerContest(contestId, { ...registerForm })
    ElMessage.success('报名申请已提交，请等待审核')
    registerVisible.value = false
    await load()
  } finally {
    registerSubmitting.value = false
  }
}

/* ============ 审核报名 ============ */
async function handleAudit(record: ContestRegistration, action: 'APPROVE' | 'REJECT') {
  const label = action === 'APPROVE' ? '通过' : '驳回'
  const confirmed = await ElMessageBox.confirm(
    `确认${label}「${record.memberName}」的赛事报名？`,
    '审核报名',
    { type: 'warning', confirmButtonText: '确认', cancelButtonText: '取消' },
  ).catch(() => false)
  if (!confirmed) return

  // 接口：POST /contests/{contestId}/registrations/{applyId}/audit（权限：ASSISTANT_COACH+）
  await auditContestRegistration(contestId, record.applyId, action, '')
  ElMessage.success(`已${label}该报名申请`)
  await loadRegistrations()
}

/* ============ 比赛中提交代码 ============ */
const submitVisible = ref(false)
const submitSubmitting = ref(false)
const submitFormRef = ref<FormInstance>()
const submitForm = reactive<{
  problemId: number | null
  language: string
  code: string
}>({
  problemId: null,
  language: 'C++17',
  code: '',
})

const submitRules: FormRules = {
  problemId: [{ required: true, message: '请选择题目', trigger: 'change' }],
  code: [{ required: true, message: '请粘贴代码', trigger: 'blur' }],
}

function openSubmit(problemId?: number) {
  submitForm.problemId = problemId ?? problemsInContest.value[0]?.problemId ?? null
  submitForm.language = 'C++17'
  submitForm.code = ''
  submitVisible.value = true
}

async function handleSubmitCode() {
  if (!submitFormRef.value) return
  const valid = await submitFormRef.value.validate().catch(() => false)
  if (!valid || submitForm.problemId === null) return

  submitSubmitting.value = true
  try {
    // 接口：POST /contests/{contestId}/submit（比赛中提交代码，权限：已报名队伍成员）
    await submitContestCode(contestId, {
      problemId: submitForm.problemId,
      language: submitForm.language,
      code: submitForm.code,
    })
    ElMessage.success('代码已提交，正在评测中')
    submitVisible.value = false
  } finally {
    submitSubmitting.value = false
  }
}

/* ============ 赛后登记获奖 / 成绩 ============ */
const resultVisible = ref(false)
const resultSubmitting = ref(false)
const resultFormRef = ref<FormInstance>()
const resultForm = reactive<{
  memberId: number | null
  rank: number | null
  award: string
  score: number | null
  remark: string
}>({
  memberId: null,
  rank: null,
  award: '',
  score: null,
  remark: '',
})

const awardOptions = ['一等奖', '二等奖', '三等奖', '优秀奖', '参赛奖']

async function openResult(record?: ContestResult) {
  if (record) {
    resultForm.memberId = record.memberId
    resultForm.rank = record.rank
    resultForm.award = record.award ?? ''
    resultForm.score = record.score
    resultForm.remark = record.remark ?? ''
  } else {
    resultForm.memberId = null
    resultForm.rank = null
    resultForm.award = ''
    resultForm.score = null
    resultForm.remark = ''
  }
  resultVisible.value = true
  if (registrations.value.length === 0) await loadRegistrations()
}

async function handleResultSubmit() {
  if (resultForm.memberId === null) {
    ElMessage.warning('请选择获奖 / 成绩所属成员')
    return
  }
  resultSubmitting.value = true
  try {
    // 接口：PUT /contests/{contestId}/results（赛后登记获奖 / 成绩，权限：ASSISTANT_COACH+）
    await updateContestResults(contestId, {
      memberId: resultForm.memberId,
      rank: resultForm.rank,
      award: resultForm.award || null,
      score: resultForm.score,
      remark: resultForm.remark || null,
    })
    ElMessage.success('成绩已登记')
    resultVisible.value = false
    await Promise.all([loadResults(), loadRegistrations()])
  } finally {
    resultSubmitting.value = false
  }
}

/* ============ 批量生成获奖电子证书 ============ */
const certVisible = ref(false)
const certGenerating = ref(false)
const certificateBundle = ref<CertificateBundle | null>(null)

async function handleGenerate() {
  certGenerating.value = true
  try {
    // 接口：POST /contests/{contestId}/certificates（批量生成获奖电子证书，权限：PRESIDENT+）
    certificateBundle.value = await generateCertificates(contestId)
    if (certificateBundle.value.total === 0) {
      ElMessage.warning('暂无获奖记录，请先登记成绩与奖项')
      return
    }
    certVisible.value = true
    ElMessage.success(`已生成 ${certificateBundle.value.total} 份获奖电子证书`)
  } finally {
    certGenerating.value = false
  }
}

const downloading = ref(false)

async function handleDownload() {
  downloading.value = true
  try {
    // 接口：GET /export/contest-certificates/{contestId}（下载获奖证书，权限：本人 / PRESIDENT+）
    await downloadContestCertificates(contestId)
    ElMessage.success('获奖证书已开始下载')
  } finally {
    downloading.value = false
  }
}

watch(activeTab, (tab) => {
  if (tab === 'registration' && canManage.value && registrations.value.length === 0) {
    loadRegistrations()
  }
  if (tab === 'results' && canManage.value && results.value.length === 0) {
    loadResults()
  }
})

onMounted(load)
</script>

<template>
<PageHeader
    :title="contest?.name ?? '赛事详情'"
    :subtitle="
      contest
        ? `${contestCategoryMap[contest.category as ContestCategory]?.label ?? contest.category} · ${formatDateTime(contest.startTime)} 开赛`
        : ''
    "
  >
    <template #actions>
      <el-button :icon="'ArrowLeft'" @click="router.push('/contests')">返回列表</el-button>
      <el-button
        v-if="canGenerateCert"
        :icon="'Download'"
        :loading="downloading"
        @click="handleDownload"
      >
        下载证书
      </el-button>
      <el-button
        v-if="auth.isLoggedIn && !canGenerateCert"
        :icon="'Download'"
        :loading="downloading"
        @click="handleDownload"
      >
        下载我的证书
      </el-button>
      <el-button
        v-if="canGenerateCert"
        :icon="'Medal'"
        :loading="certGenerating"
        @click="handleGenerate"
      >
        批量生成证书
      </el-button>
      <el-button v-if="canRegister" type="primary" :icon="'EditPen'" @click="openRegister">
        队伍 / 个人报名
      </el-button>
      <el-button v-if="canSubmit" type="primary" :icon="'Upload'" @click="openSubmit()">
        提交代码
      </el-button>
    </template>
  </PageHeader>

<div v-loading="loading" class="page">
  <template v-if="contest">
    <div class="ic-card overview">
      <div class="overview-item">
        <span>赛事状态</span>
        <StatusTag
          :label="contestStatusMap[contest.status as ContestStatus]?.label ?? contest.status"
          :tone="contestStatusMap[contest.status as ContestStatus]?.tone ?? 'info'"
        />
      </div>
      <div class="overview-item">
        <span>报名截止</span>
        <strong>{{ formatDateTime(contest.registrationDeadline) }}</strong>
      </div>
      <div class="overview-item">
        <span>比赛时间</span>
        <strong>{{ formatDateTime(contest.startTime) }} ~ {{ formatDateTime(contest.endTime) }}</strong>
      </div>
      <div class="overview-item">
        <span>封榜设置</span>
        <strong>
          {{ contest.freezeMinutes === 0 ? '不封榜' : `结束前 ${contest.freezeMinutes} 分钟封榜` }}
        </strong>
      </div>
      <div class="overview-item">
        <span>队伍上限</span>
        <strong>{{ contest.maxTeamSize }} 人</strong>
      </div>
      <p class="overview-desc">{{ contest.description }}</p>
    </div>

    <div class="ic-card tab-card">
      <el-tabs v-model="activeTab">
        <!-- 题目列表 -->
        <el-tab-pane :label="`题目列表（${problemsInContest.length}）`" name="problems">
          <el-table :data="problemsInContest" row-key="problemId">
            <el-table-column type="index" label="#" width="60" />
            <el-table-column label="题目" min-width="260">
              <template #default="{ row }">
                <div class="problem-cell">
                  <span class="oj-id mono">{{ row.ojProblemId }}</span>
                  <a class="problem-title" @click="router.push(`/problems/${row.problemId}`)">
                    {{ row.title }}
                  </a>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="难度" width="100">
              <template #default="{ row }">
                <StatusTag
                  :label="difficultyMap[row.difficulty as Difficulty].label"
                  :tone="difficultyMap[row.difficulty as Difficulty].tone"
                />
              </template>
            </el-table-column>
            <el-table-column label="知识点" min-width="180">
              <template #default="{ row }">
                <div class="tag-list">
                  <el-tag v-for="tag in row.tags" :key="tag.tagId" size="small" effect="plain" round>
                    {{ tag.name }}
                  </el-tag>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120" fixed="right">
              <template #default="{ row }">
                <el-button
                  link
                  type="primary"
                  :disabled="!canSubmit"
                  @click="openSubmit(row.problemId)"
                >
                  提交代码
                </el-button>
              </template>
            </el-table-column>
            <template #empty>
              <EmptyState title="赛事暂无题目" description="赛事创建者可在发布赛事时选择题目" />
            </template>
          </el-table>
          <div v-if="!canSubmit" class="tip">
            <el-icon><InfoFilled /></el-icon>
            仅已通过报名的参赛队员可在比赛期间提交代码。
          </div>
        </el-tab-pane>

        <!-- 报名 -->
        <el-tab-pane
          :label="canManage ? `报名审核（${pendingCount}）` : '我的报名'"
          name="registration"
        >
          <template v-if="canManage">
            <el-table v-if="registrations.length" :data="registrations" row-key="applyId">
              <el-table-column label="报名方式" width="110">
                <template #default="{ row }">{{ contestApplyTypeMap[row.applyType as ContestApplyType] }}</template>
              </el-table-column>
              <el-table-column label="报名人 / 队伍" min-width="180">
                <template #default="{ row }">
                  <span>{{ row.teamName || row.memberName }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="memberName" label="联系人" width="110" />
              <el-table-column prop="studentId" label="学号" width="140" />
              <el-table-column prop="memberCount" label="人数" width="80" />
              <el-table-column label="报名时间" width="170">
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
              description="会员提交队伍 / 个人报名后将显示在此处，可进行通过 / 驳回操作"
            />
          </template>

          <div v-else class="my-registration">
            <template v-if="myRegistration">
              <div class="my-status">
                <span>报名方式</span>
                <strong>{{ contestApplyTypeMap[myRegistration.applyType] }}</strong>
              </div>
              <div class="my-status">
                <span>报名队伍</span>
                <strong>{{ myRegistration.teamName || '—' }}</strong>
              </div>
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
                  ? '点击「队伍 / 个人报名」报名参加本次赛事'
                  : `报名已于 ${formatDateTime(contest.registrationDeadline)} 截止`
              "
            />
          </div>
        </el-tab-pane>

        <!-- 实时榜单 -->
        <el-tab-pane label="实时榜单" name="leaderboard">
          <el-alert
            v-if="leaderboard?.frozen"
            class="freeze-alert"
            type="warning"
            :closable="false"
            show-icon
            title="榜单已封榜"
            description="封榜期间的提交结果不在榜单中展示，比赛结束后自动解封。"
          />
          <el-table :data="leaderboard?.entries ?? []" row-key="userId">
            <el-table-column prop="rank" label="排名" width="80">
              <template #default="{ row }">
                <span class="rank" :class="{ top: row.rank <= 3 }">{{ row.rank }}</span>
              </template>
            </el-table-column>
            <el-table-column label="成员 / 队伍" min-width="180">
              <template #default="{ row }">
                <div class="team-cell">
                  <div class="avatar">{{ row.userName.slice(0, 1) }}</div>
                  <span>{{ row.teamName || row.userName }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="solvedCount" label="通过题数" width="110" />
            <el-table-column prop="penalty" label="罚时（分钟）" width="120" />
            <template #empty>
              <EmptyState title="暂无榜单数据" description="比赛开始后自动生成实时榜单" />
            </template>
          </el-table>
        </el-tab-pane>

        <!-- 成绩与证书 -->
        <el-tab-pane v-if="canManage" label="成绩与证书" name="results">
          <div class="result-toolbar">
            <el-button type="primary" size="small" :icon="'Plus'" @click="openResult()">
              登记成绩 / 获奖
            </el-button>
          </div>
          <el-table v-if="results.length" :data="results" row-key="resultId">
            <el-table-column prop="memberName" label="获奖人" width="120" />
            <el-table-column prop="studentId" label="学号" width="140" />
            <el-table-column label="队伍" min-width="150">
              <template #default="{ row }">{{ row.teamName || '—' }}</template>
            </el-table-column>
            <el-table-column label="排名" width="90">
              <template #default="{ row }">
                <span :class="{ muted: row.rank === null }">
                  {{ row.rank === null ? '—' : `第 ${row.rank} 名` }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="奖项" width="110">
              <template #default="{ row }">
                <span :class="{ muted: !row.award }">{{ row.award || '—' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="得分" width="90">
              <template #default="{ row }">
                <span :class="{ muted: row.score === null }">{{ row.score ?? '—' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="备注" min-width="160">
              <template #default="{ row }">
                <span :class="{ muted: !row.remark }">{{ row.remark || '—' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="100" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="openResult(row)">编辑</el-button>
              </template>
            </el-table-column>
            <template #empty>
              <EmptyState title="暂无成绩记录" description="点击「登记成绩 / 获奖」录入赛后结果" />
            </template>
          </el-table>
          <EmptyState
            v-else
            title="暂无成绩记录"
            description="赛后点击「登记成绩 / 获奖」录入排名与奖项，登记后可批量生成电子证书"
          />
        </el-tab-pane>
      </el-tabs>
    </div>
  </template>
</div>

<!-- 队伍 / 个人报名 -->
<el-dialog v-model="registerVisible" title="赛事报名" width="560px" destroy-on-close>
  <el-form label-width="88px">
    <el-form-item label="报名方式">
      <el-radio-group v-model="registerForm.applyType">
        <el-radio-button value="SOLO">个人报名</el-radio-button>
        <el-radio-button value="TEAM">队伍报名</el-radio-button>
      </el-radio-group>
    </el-form-item>
    <el-form-item v-if="registerForm.applyType === 'TEAM'" label="报名队伍">
      <el-select v-model="registerForm.teamId" placeholder="选择队伍" style="width: 100%">
        <el-option
          v-for="team in myTeams"
          :key="team.teamId"
          :label="`${team.name}（${team.members.length} 人）`"
          :value="team.teamId"
        />
      </el-select>
    </el-form-item>
    <el-form-item label="申请说明">
      <el-input
        v-model="registerForm.remark"
        type="textarea"
        :rows="3"
        placeholder="简要说明参赛目标等（选填）"
      />
    </el-form-item>
  </el-form>
  <div class="tip">
    <el-icon><InfoFilled /></el-icon>
    报名需经干事及以上角色审核，审核通过后可在比赛期间提交代码。
  </div>
  <template #footer>
    <el-button @click="registerVisible = false">取消</el-button>
    <el-button type="primary" :loading="registerSubmitting" @click="handleRegister">
      提交报名
    </el-button>
  </template>
</el-dialog>

<!-- 比赛中提交代码 -->
<el-dialog v-model="submitVisible" title="提交代码" width="720px" top="8vh">
  <el-form ref="submitFormRef" :model="submitForm" :rules="submitRules" label-width="80px">
    <div class="form-grid">
      <el-form-item label="题目" prop="problemId">
        <el-select v-model="submitForm.problemId" placeholder="选择题目" style="width: 100%">
          <el-option
            v-for="problem in problemsInContest"
            :key="problem.problemId"
            :label="`${problem.ojProblemId} · ${problem.title}`"
            :value="problem.problemId"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="语言">
        <el-select v-model="submitForm.language" style="width: 100%">
          <el-option v-for="item in languages" :key="item" :label="item" :value="item" />
        </el-select>
      </el-form-item>
    </div>
    <el-form-item label="代码" prop="code">
      <el-input
        v-model="submitForm.code"
        type="textarea"
        :rows="14"
        class="code-input"
        placeholder="在此粘贴源代码"
      />
    </el-form-item>
  </el-form>
  <template #footer>
    <el-button @click="submitVisible = false">取消</el-button>
    <el-button type="primary" :loading="submitSubmitting" @click="handleSubmitCode">
      提交评测
    </el-button>
  </template>
</el-dialog>

<!-- 赛后登记获奖 / 成绩 -->
<el-dialog v-model="resultVisible" title="登记成绩 / 获奖" width="560px" destroy-on-close>
  <el-form ref="resultFormRef" :model="resultForm" label-width="88px">
    <el-form-item label="获奖成员">
      <el-select v-model="resultForm.memberId" placeholder="选择成员" style="width: 100%">
        <el-option
          v-for="record in registrations"
          :key="record.applyId"
          :label="`${record.memberName}（${record.studentId}）${record.teamName ? ' · ' + record.teamName : ''}`"
          :value="record.memberId"
        />
      </el-select>
    </el-form-item>
    <div class="form-grid">
      <el-form-item label="排名">
        <el-input-number v-model="resultForm.rank" :min="1" style="width: 100%" />
      </el-form-item>
      <el-form-item label="得分">
        <el-input-number v-model="resultForm.score" :min="0" style="width: 100%" />
      </el-form-item>
    </div>
    <el-form-item label="奖项">
      <el-select v-model="resultForm.award" clearable placeholder="选择奖项" style="width: 100%">
        <el-option v-for="item in awardOptions" :key="item" :label="item" :value="item" />
      </el-select>
    </el-form-item>
    <el-form-item label="备注">
      <el-input v-model="resultForm.remark" type="textarea" :rows="2" placeholder="选填" />
    </el-form-item>
  </el-form>
  <template #footer>
    <el-button @click="resultVisible = false">取消</el-button>
    <el-button type="primary" :loading="resultSubmitting" @click="handleResultSubmit">
      保存登记
    </el-button>
  </template>
</el-dialog>

<!-- 批量生成获奖电子证书 -->
<el-dialog v-model="certVisible" title="获奖电子证书" width="620px" destroy-on-close>
  <el-alert
    class="cert-alert"
    type="success"
    :closable="false"
    show-icon
    :title="`已生成 ${certificateBundle?.total ?? 0} 份证书`"
    :description="`生成时间：${formatDateTime(certificateBundle?.generatedTime)}`"
  />
  <el-table :data="certificateBundle?.certificates ?? []" row-key="certId" max-height="360">
    <el-table-column prop="certNo" label="证书编号" width="160" />
    <el-table-column prop="memberName" label="获奖人" width="110" />
    <el-table-column label="队伍" min-width="140">
      <template #default="{ row }">{{ row.teamName || '—' }}</template>
    </el-table-column>
    <el-table-column prop="award" label="奖项" width="100" />
  </el-table>
  <template #footer>
    <el-button @click="certVisible = false">关闭</el-button>
    <el-button type="primary" :loading="downloading" @click="handleDownload">下载证书</el-button>
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
  grid-template-columns: repeat(5, minmax(0, 1fr));
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

.problem-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.oj-id {
  font-size: 11px;
  color: var(--primary-deep);
  background: var(--primary-lighter);
  padding: 1px 7px;
  border-radius: var(--radius-pill);
}

.problem-title {
  color: var(--text-main);
  font-weight: 500;
}

.problem-title:hover {
  color: var(--primary-deep);
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.tip {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 12px;
  font-size: 12px;
  color: var(--primary-deep);
}

.freeze-alert {
  margin-bottom: 14px;
}

.cert-alert {
  margin-bottom: 14px;
}

.rank {
  display: inline-flex;
  width: 24px;
  height: 24px;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  font-weight: 600;
  color: var(--text-sub);
  background: var(--bg-blue-soft);
}

.rank.top {
  color: #fff;
  background: linear-gradient(140deg, #7cb1e0, #4b88bd);
}

.team-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.avatar {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  color: var(--primary-deep);
  background: var(--primary-light);
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

.result-toolbar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 12px;
}

.muted {
  color: var(--text-muted);
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  column-gap: 18px;
}

.code-input :deep(textarea) {
  font-family: var(--font-mono);
  font-size: 12px;
  line-height: 1.7;
}

@media (max-width: 1200px) {
  .overview {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
