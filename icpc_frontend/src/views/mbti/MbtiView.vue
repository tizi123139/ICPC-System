<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import EmptyState from '@/components/EmptyState.vue'
import { getMbtiQuestions, getMbtiResult, submitMbti } from '@/api/mbti'
import { getMembers } from '@/api/members'
import { MBTI_RESULT_VIEW_ROLES, formatDateTime } from '@/utils/dict'
import { useAuthStore } from '@/stores/auth'
import type { MemberInfo, MbtiAnswer, MbtiQuestion, MbtiResult } from '@/types'

const auth = useAuthStore()

const loading = ref(false)
const submitting = ref(false)
/** loading（加载中） / quiz（作答） / result（结果展示） */
const mode = ref<'loading' | 'quiz' | 'result'>('loading')

const questions = ref<MbtiQuestion[]>([])
const answers = ref<Record<number, 'A' | 'B'>>({})
const result = ref<MbtiResult | null>(null)
/** 是否正在查看他人结果 */
const viewingOther = ref(false)

/** 查看他人测评结果：接口手册第 8 章（本人 / ASSISTANT_COACH+） */
const canViewOthers = computed(() => auth.hasRole(MBTI_RESULT_VIEW_ROLES))

const answeredCount = computed(() => Object.keys(answers.value).length)
const allAnswered = computed(
  () => questions.value.length > 0 && answeredCount.value === questions.value.length,
)
const progressPercent = computed(() =>
  questions.value.length === 0
    ? 0
    : Math.round((answeredCount.value / questions.value.length) * 100),
)

async function loadMine() {
  loading.value = true
  try {
    // 接口：GET /mbti/questions（获取测评题目列表）
    questions.value = await getMbtiQuestions()
    try {
      // 接口：GET /mbti/results/me（查看本人测评结果）
      result.value = await getMbtiResult('me')
      mode.value = 'result'
    } catch {
      // 尚未测评 → 进入作答
      result.value = null
      mode.value = 'quiz'
    }
  } finally {
    loading.value = false
  }
}

function pickOption(questionId: number, value: 'A' | 'B') {
  answers.value[questionId] = value
}

async function handleSubmit() {
  if (!allAnswered.value) {
    ElMessage.warning('请完成全部题目后再提交')
    return
  }
  submitting.value = true
  try {
    const payload: MbtiAnswer[] = questions.value.map((question) => ({
      questionId: question.questionId,
      value: answers.value[question.questionId]!,
    }))
    // 接口：POST /mbti/submit（提交答案，返回测评结果）
    result.value = await submitMbti(payload)
    viewingOther.value = false
    mode.value = 'result'
    ElMessage.success('测评完成')
  } finally {
    submitting.value = false
  }
}

function restart() {
  answers.value = {}
  viewingOther.value = false
  mode.value = 'quiz'
}

/* ============ 查看某会员测评结果 ============ */
const members = ref<MemberInfo[]>([])
const selectedMemberId = ref<number | null>(null)
const viewing = ref(false)

async function loadMembers() {
  // 接口：GET /members（选择会员）
  const pageData = await getMembers({ page: 1, size: 100 })
  members.value = pageData.records
}

async function handleViewMember() {
  if (selectedMemberId.value === null) {
    ElMessage.warning('请选择要查看的会员')
    return
  }
  viewing.value = true
  try {
    // 接口：GET /mbti/results/{userId}（查看某会员测评结果，权限：本人 / ASSISTANT_COACH+）
    result.value = await getMbtiResult(selectedMemberId.value)
    viewingOther.value = true
    mode.value = 'result'
  } finally {
    viewing.value = false
  }
}

onMounted(async () => {
  await loadMine()
  if (canViewOthers.value) await loadMembers()
})
</script>

<template>
<PageHeader title="MBTI 测评" subtitle="完成性格测评并查看测评结果，教练可查看会员的测评结果">
    <template #actions>
      <template v-if="canViewOthers">
        <el-select
          v-model="selectedMemberId"
          placeholder="选择会员"
          filterable
          clearable
          style="width: 180px"
        >
          <el-option
            v-for="member in members"
            :key="member.memberId"
            :label="`${member.name}（${member.studentId}）`"
            :value="member.memberId"
          />
        </el-select>
        <el-button :loading="viewing" :icon="'View'" @click="handleViewMember">
          查看测评结果
        </el-button>
      </template>
      <el-button
        v-if="mode === 'result' && !viewingOther"
        :icon="'RefreshRight'"
        @click="restart"
      >
        重新测评
      </el-button>
    </template>
  </PageHeader>

<div v-loading="loading" class="page">
  <!-- 作答 -->
  <template v-if="mode === 'quiz'">
    <div class="ic-card progress-card">
      <div class="progress-head">
        <span>已完成 {{ answeredCount }} / {{ questions.length }} 题</span>
        <span class="progress-value">{{ progressPercent }}%</span>
      </div>
      <el-progress :percentage="progressPercent" :show-text="false" />
    </div>

    <div class="question-list">
      <div v-for="(question, index) in questions" :key="question.questionId" class="ic-card question-card">
        <div class="question-head">
          <span class="question-index">Q{{ index + 1 }}</span>
          <span class="question-content">{{ question.content }}</span>
        </div>
        <div class="option-list">
          <button
            v-for="option in question.options"
            :key="option.value"
            type="button"
            class="option"
            :class="{ selected: answers[question.questionId] === option.value }"
            @click="pickOption(question.questionId, option.value)"
          >
            <span class="option-key">{{ option.value }}</span>
            <span class="option-label">{{ option.label }}</span>
          </button>
        </div>
      </div>
    </div>

    <div class="submit-bar">
      <el-button
        type="primary"
        size="large"
        :disabled="!allAnswered"
        :loading="submitting"
        @click="handleSubmit"
      >
        提交测评
      </el-button>
      <span class="submit-tip">提交后将根据 4 个维度（EI / SN / TF / JP）计算人格类型</span>
    </div>
  </template>

  <!-- 结果 -->
  <template v-else-if="mode === 'result' && result">
    <div class="ic-card result-card">
      <div class="result-type">
        <div class="type-code">{{ result.type }}</div>
        <div class="type-meta">
          <strong>{{ result.userName }} · {{ result.typeName }}</strong>
          <span>测评时间：{{ formatDateTime(result.submitTime) }}</span>
        </div>
      </div>

      <div class="dimension-list">
        <div v-for="dim in result.dimensions" :key="dim.dimension" class="dimension-item">
          <div class="dim-head">
            <span :class="{ active: dim.preference === 'left' }">{{ dim.leftLabel }}</span>
            <span :class="{ active: dim.preference === 'right' }">{{ dim.rightLabel }}</span>
          </div>
          <div class="dim-track">
            <div class="dim-fill" :style="{ width: `${dim.leftPercent}%` }" />
          </div>
          <div class="dim-percent">
            <span>{{ dim.leftPercent }}%</span>
            <span>{{ 100 - dim.leftPercent }}%</span>
          </div>
        </div>
      </div>
    </div>

    <div class="ic-card describe-card">
      <h3>性格解读</h3>
      <p>{{ result.description }}</p>
      <h3>发展建议</h3>
      <ul class="suggestion-list">
        <li v-for="(item, index) in result.suggestions" :key="index">{{ item }}</li>
      </ul>
    </div>
  </template>

  <EmptyState
    v-else-if="mode !== 'loading'"
    title="暂无测评数据"
    description="点击「重新测评」开始 MBTI 性格测评"
  >
    <template #action>
      <el-button type="primary" @click="restart">开始测评</el-button>
    </template>
  </EmptyState>
</div>
</template>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.progress-card {
  padding: 16px 22px;
}

.progress-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
  font-size: 13px;
  color: var(--text-sub);
}

.progress-value {
  font-weight: 600;
  color: var(--primary-deep);
}

.question-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.question-card {
  padding: 18px 22px;
}

.question-head {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  margin-bottom: 14px;
}

.question-index {
  flex-shrink: 0;
  font-size: 12px;
  font-weight: 600;
  color: var(--primary-deep);
  background: var(--primary-lighter);
  padding: 2px 9px;
  border-radius: var(--radius-pill);
}

.question-content {
  font-size: 14px;
  line-height: 1.6;
  color: var(--text-main);
}

.option-list {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.option {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  border: 1px solid var(--border);
  border-radius: var(--radius-card);
  background: var(--bg-card-soft, #fbfdff);
  font-size: 13px;
  color: var(--text-sub);
  text-align: left;
  cursor: pointer;
  transition: all 0.15s ease;
}

.option:hover {
  border-color: var(--primary-light);
  color: var(--primary-deep);
}

.option.selected {
  border-color: var(--primary);
  background: var(--primary-lighter);
  color: var(--primary-deep);
  font-weight: 500;
}

.option-key {
  flex-shrink: 0;
  width: 22px;
  height: 22px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  font-size: 12px;
  background: var(--bg-blue-soft);
}

.option.selected .option-key {
  color: #fff;
  background: linear-gradient(140deg, #7cb1e0, #4b88bd);
}

.submit-bar {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 4px 2px 8px;
}

.submit-tip {
  font-size: 12px;
  color: var(--text-muted);
}

.result-card {
  padding: 24px 26px;
}

.result-type {
  display: flex;
  align-items: center;
  gap: 18px;
  padding-bottom: 20px;
  margin-bottom: 20px;
  border-bottom: 1px dashed var(--border);
}

.type-code {
  font-size: 34px;
  font-weight: 700;
  letter-spacing: 3px;
  background: linear-gradient(140deg, #7cb1e0, #4b88bd);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}

.type-meta {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.type-meta strong {
  font-size: 15px;
  color: var(--text-main);
}

.type-meta span {
  font-size: 12px;
  color: var(--text-muted);
}

.dimension-list {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 18px 32px;
}

.dimension-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.dim-head,
.dim-percent {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 12px;
  color: var(--text-muted);
}

.dim-head .active {
  color: var(--primary-deep);
  font-weight: 600;
}

.dim-track {
  height: 8px;
  border-radius: var(--radius-pill);
  background: var(--bg-blue-soft);
  overflow: hidden;
}

.dim-fill {
  height: 100%;
  border-radius: var(--radius-pill);
  background: linear-gradient(90deg, #a8cdf0, #4b88bd);
  transition: width 0.4s ease;
}

.describe-card {
  padding: 22px 26px;
}

.describe-card h3 {
  margin: 0 0 10px;
  font-size: 14px;
  color: var(--text-main);
}

.describe-card h3:not(:first-child) {
  margin-top: 18px;
}

.describe-card p {
  margin: 0;
  font-size: 13px;
  line-height: 1.8;
  color: var(--text-sub);
}

.suggestion-list {
  margin: 0;
  padding-left: 20px;
  font-size: 13px;
  line-height: 1.9;
  color: var(--text-sub);
}

@media (max-width: 900px) {
  .option-list,
  .dimension-list {
    grid-template-columns: 1fr;
  }
}
</style>
