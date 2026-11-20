<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import StatusTag from '@/components/StatusTag.vue'
import EmptyState from '@/components/EmptyState.vue'
import {
  getLeaderboard,
  getSubmissions,
  getTraining,
  publishTraining,
  submitCode,
} from '@/api/trainings'
import { getProblems } from '@/api/problems'
import { exportTrainingReport } from '@/api/statistics'
import {
  CONTENT_MANAGER_ROLES,
  MEMBER_ROLES,
  difficultyMap,
  formatDateTime,
  judgeResultMap,
  languages,
  trainingStatusMap,
  trainingTypeMap,
} from '@/utils/dict'
import { useAuthStore } from '@/stores/auth'
import type {
  Difficulty,
  JudgeResult,
  LeaderboardResult,
  ProblemInfo,
  SubmitRecord,
  TrainingInfo,
  TrainingStatus,
} from '@/types'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const trainingId = Number(route.params.id)

const loading = ref(false)
const training = ref<TrainingInfo | null>(null)
const problems = ref<ProblemInfo[]>([])
const submissions = ref<SubmitRecord[]>([])
const leaderboard = ref<LeaderboardResult | null>(null)
const activeTab = ref<'problems' | 'submissions' | 'leaderboard'>('problems')

const problemsInTraining = computed(() => {
  if (!training.value) return []
  return training.value.problemIds
    .map((id) => problems.value.find((item) => item.problemId === id))
    .filter((item): item is ProblemInfo => Boolean(item))
})

const canManage = computed(() => auth.hasRole(CONTENT_MANAGER_ROLES))
const isDraft = computed(() => training.value?.status === 'DRAFT')
/** 提交代码（训练）：仅会员类角色（观察会员 / 正式会员 / 队长），干事及以上管理角色不参与 */
const canJoin = computed(() => auth.hasRole(MEMBER_ROLES))

async function load() {
  loading.value = true
  try {
    // 并行请求：训练详情 / 题目列表 / 提交记录 / 榜单
    const [trainingData, problemData, submissionData, leaderboardData] = await Promise.all([
      getTraining(trainingId),
      getProblems({ page: 1, size: 100 }),
      getSubmissions(trainingId),
      getLeaderboard(trainingId),
    ])
    training.value = trainingData
    problems.value = problemData.records
    submissions.value = submissionData
    leaderboard.value = leaderboardData
  } finally {
    loading.value = false
  }
}

async function handlePublish() {
  if (!training.value) return
  // 接口：POST /trainings/{trainingId}/publish
  await publishTraining(training.value.trainingId)
  ElMessage.success('训练已发布，参赛成员将收到通知')
  await load()
}

const exporting = ref(false)

async function handleExport() {
  exporting.value = true
  try {
    // 接口：GET /export/training-report/{trainingId}（权限：ASSISTANT_COACH+）
    await exportTrainingReport(trainingId)
    ElMessage.success('成绩报表已导出')
  } catch (error) {
    // 真实环境的网络错误已由 Axios 拦截器提示，这里兜底文件生成失败等异常
    if (error instanceof Error && error.message) ElMessage.error(error.message)
  } finally {
    exporting.value = false
  }
}

/* ============ 提交代码 ============ */
const submitVisible = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance>()
const submitProblemId = ref<number | null>(null)
const submitLanguage = ref('C++17')
const submitSource = ref('')

/** 表单校验模型（仅用于 el-form rules 绑定） */
const submitFormModel = computed(() => ({
  problemId: submitProblemId.value,
  source: submitSource.value,
}))

const submitRules: FormRules = {
  problemId: [{ required: true, message: '请选择题目', trigger: 'change' }],
  source: [{ required: true, message: '请粘贴代码', trigger: 'blur' }],
}

function openSubmit(problemId?: number) {
  submitProblemId.value = problemId ?? problemsInTraining.value[0]?.problemId ?? null
  submitLanguage.value = 'C++17'
  submitSource.value = ''
  submitVisible.value = true
}

async function handleSubmitCode() {
  if (!canJoin.value) {
    ElMessage.warning('当前角色无权参加日常训练')
    return
  }
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  if (submitProblemId.value === null) return

  submitting.value = true
  try {
    // 接口：POST /trainings/{trainingId}/submit
    // TODO(backend): 判题由 OJ 异步完成，可在此处开启轮询
    // （GET /trainings/{trainingId}/submissions）直到 result 不再是 PENDING/JUDGING
    await submitCode(trainingId, {
      problemId: submitProblemId.value,
      language: submitLanguage.value,
      code: submitSource.value,
    })
    ElMessage.success('代码已提交，正在评测中')
    submitVisible.value = false
    submissions.value = await getSubmissions(trainingId)
    activeTab.value = 'submissions'
  } finally {
    submitting.value = false
  }
}

onMounted(load)
</script>

<template>
  <PageHeader :title="training?.name ?? '训练详情'"
    :subtitle="training ? `${trainingTypeMap[training.type]} · 创建人 ${training.creator.name}` : ''">
    <template #actions>
      <el-button :icon="'ArrowLeft'" @click="router.push('/trainings')">返回列表</el-button>
      <el-button v-if="canManage" :icon="'Download'" :loading="exporting" @click="handleExport">
        导出报表
      </el-button>
      <el-button v-if="isDraft && canManage" type="warning" :icon="'Promotion'" @click="handlePublish">
        发布训练
      </el-button>
      <el-button v-if="!isDraft && canJoin" type="primary" :icon="'Upload'" @click="openSubmit()">
        提交代码
      </el-button>
    </template>
  </PageHeader>

  <div v-loading="loading" class="page">
    <template v-if="training">
      <div class="ic-card overview">
        <div class="overview-item">
          <span>状态</span>
          <StatusTag :label="trainingStatusMap[training.status as TrainingStatus].label"
            :tone="trainingStatusMap[training.status as TrainingStatus].tone" />
        </div>
        <div class="overview-item">
          <span>开始时间</span>
          <strong>{{ formatDateTime(training.startTime) }}</strong>
        </div>
        <div class="overview-item">
          <span>结束时间</span>
          <strong>{{ formatDateTime(training.endTime) }}</strong>
        </div>
        <div class="overview-item">
          <span>题目数量</span>
          <strong>{{ training.problemIds.length }} 道</strong>
        </div>
        <div class="overview-item">
          <span>封榜设置</span>
          <strong>{{ training.freezeTime === 0 ? '不封榜' : `结束前 ${training.freezeTime} 分钟封榜` }}</strong>
        </div>
        <div class="overview-item">
          <span>榜单公开</span>
          <strong>{{ training.leaderboardVisible ? '公开' : '不公开' }}</strong>
        </div>
      </div>

      <div class="ic-card content-card">
        <el-tabs v-model="activeTab">
          <!-- 题目列表 -->
          <el-tab-pane label="题目列表" name="problems">
            <el-table :data="problemsInTraining" row-key="problemId">
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
                  <StatusTag :label="difficultyMap[row.difficulty as Difficulty].label"
                    :tone="difficultyMap[row.difficulty as Difficulty].tone" />
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
              <el-table-column label="限制" width="150">
                <template #default="{ row }">
                  <span class="muted mono">{{ row.timeLimit }}ms / {{ Math.round(row.memoryLimit / 1024) }}MB</span>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="120" fixed="right">
                <template #default="{ row }">
                  <el-button v-if="canJoin" link type="primary" @click="openSubmit(row.problemId)">提交代码</el-button>
                </template>
              </el-table-column>
              <template #empty>
                <EmptyState title="训练暂无题目" description="训练创建者可在编辑训练时添加题目" />
              </template>
            </el-table>
          </el-tab-pane>

          <!-- 提交记录 -->
          <el-tab-pane label="提交记录" name="submissions">
            <el-table :data="submissions" row-key="submitId">
              <el-table-column label="题目" min-width="200">
                <template #default="{ row }">
                  <span class="mono">#{{ row.problemId }}</span>
                </template>
              </el-table-column>
              <el-table-column label="提交人" width="120">
                <template #default="{ row }">{{ row.submitter.name }}</template>
              </el-table-column>
              <el-table-column label="语言" width="110">
                <template #default="{ row }">{{ row.language }}</template>
              </el-table-column>
              <el-table-column label="结果" width="100">
                <template #default="{ row }">
                  <StatusTag :label="judgeResultMap[row.result as JudgeResult].label"
                    :tone="judgeResultMap[row.result as JudgeResult].tone" />
                </template>
              </el-table-column>
              <el-table-column label="耗时" width="100">
                <template #default="{ row }">{{ row.timeUsed ? `${row.timeUsed} ms` : '—' }}</template>
              </el-table-column>
              <el-table-column label="内存" width="100">
                <template #default="{ row }">{{ row.memoryUsed ? `${Math.round(row.memoryUsed / 1024)} MB` : '—'
                  }}</template>
              </el-table-column>
              <el-table-column label="提交时间" width="160">
                <template #default="{ row }">{{ formatDateTime(row.submitTime) }}</template>
              </el-table-column>
              <template #empty>
                <EmptyState title="暂无提交" description="训练开始后可提交代码，判题结果将异步返回" />
              </template>
            </el-table>
            <div class="poll-tip">
              <el-icon>
                <InfoFilled />
              </el-icon>
              <!-- TODO(backend): 提交后轮询 GET /trainings/{trainingId}/submissions 获取最终判题结果 -->
              判题由自建 OJ 异步完成，PENDING / JUDGING 状态可稍后刷新查看。
            </div>
          </el-tab-pane>

          <!-- 榜单 -->
          <el-tab-pane label="实时榜单" name="leaderboard">
            <template v-if="training.leaderboardVisible">
              <el-alert v-if="leaderboard?.frozen" class="freeze-alert" type="warning" :closable="false" show-icon
                title="榜单已封榜" description="封榜期间的提交结果不在榜单中展示，比赛结束后自动解封。" />
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
                  <EmptyState title="暂无榜单数据" description="训练开始后自动生成实时榜单" />
                </template>
              </el-table>
            </template>
            <EmptyState v-else title="榜单不公开" description="该训练未开启榜单公开，仅组织者可在后台查看" />
          </el-tab-pane>
        </el-tabs>
      </div>
    </template>
  </div>

  <!-- 提交代码 -->
  <el-dialog v-model="submitVisible" title="提交代码" width="720px" top="8vh">
    <el-form ref="formRef" :model="submitFormModel" :rules="submitRules" label-width="80px">
      <div class="form-grid">
        <el-form-item label="题目" prop="problemId">
          <el-select v-model="submitProblemId" placeholder="选择题目" style="width: 100%">
            <el-option v-for="problem in problemsInTraining" :key="problem.problemId"
              :label="`${problem.ojProblemId} · ${problem.title}`" :value="problem.problemId" />
          </el-select>
        </el-form-item>
        <el-form-item label="语言">
          <el-select v-model="submitLanguage" style="width: 100%">
            <el-option v-for="item in languages" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
      </div>
      <el-form-item label="代码" prop="source">
        <el-input v-model="submitSource" type="textarea" :rows="14" class="code-input" placeholder="// 在此粘贴源代码" />
      </el-form-item>
    </el-form>
    <div class="poll-tip">
      <el-icon>
        <InfoFilled />
      </el-icon>
      提交后将推送至自建 OJ 判题，结果异步返回。
    </div>
    <template #footer>
      <el-button @click="submitVisible = false">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="handleSubmitCode">提交评测</el-button>
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
  grid-template-columns: repeat(6, minmax(0, 1fr));
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

.content-card {
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

.poll-tip {
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
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}
</style>
