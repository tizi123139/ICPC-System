<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules, type UploadRequestOptions } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import StatusTag from '@/components/StatusTag.vue'
import EmptyState from '@/components/EmptyState.vue'
import { uploadFile } from '@/api/files'
import {
  getProblem,
  getProblemSubmissions,
  submitProblemCode,
  updateSolution,
} from '@/api/problems'
import {
  BANK_PRIVATE_ROLES,
  CONTENT_MANAGER_ROLES,
  difficultyMap,
  formatDateTime,
  judgeResultMap,
  languages,
  visibilityMap,
} from '@/utils/dict'
import { useAuthStore } from '@/stores/auth'
import type {
  Difficulty,
  JudgeResult,
  ProblemInfo,
  ProblemVisibility,
  SubmitRecord,
  UploadResult,
} from '@/types'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const problemId = Number(route.params.id)

const loading = ref(false)
const problem = ref<ProblemInfo | null>(null)
const activeTab = ref<'description' | 'solution' | 'submissions'>('description')

const solutionDialog = ref(false)
const submitting = ref(false)
const solutionForm = reactive({
  content: '',
  referenceCode: '',
  /** 题解附件（POST /files/upload 上传成功后累计，保存题解时一并提交） */
  attachments: [] as UploadResult[],
})
const uploading = ref(false)

/* ============ 题解附件上传 ============ */
const MAX_ATTACHMENT_SIZE = 10 * 1024 * 1024

/** el-upload 自定义上传：调用通用文件上传接口 POST /files/upload */
async function handleUploadRequest(options: UploadRequestOptions) {
  uploading.value = true
  try {
    const result = await uploadFile(options.file)
    solutionForm.attachments.push(result)
    ElMessage.success(`附件「${result.filename}」上传成功`)
  } catch {
    ElMessage.error('附件上传失败，请重试')
  } finally {
    uploading.value = false
  }
}

/** 上传前校验：单个附件不超过 10MB */
function beforeAttachmentUpload(file: File) {
  if (file.size > MAX_ATTACHMENT_SIZE) {
    ElMessage.warning('附件大小不能超过 10MB')
    return false
  }
  return true
}

function removeAttachment(index: number) {
  solutionForm.attachments.splice(index, 1)
}

function formatSize(size: number) {
  return size >= 1024 * 1024 ? `${(size / 1024 / 1024).toFixed(1)} MB` : `${Math.max(1, Math.round(size / 1024))} KB`
}

/* ============ 代码提交 ============ */
const submissions = ref<SubmitRecord[]>([])
const submitVisible = ref(false)
const submitLoading = ref(false)
const submitFormRef = ref<FormInstance>()
const submitLanguage = ref('C++17')
const submitSource = ref('')

/** 表单校验模型（仅用于 el-form rules 绑定） */
const submitFormModel = computed(() => ({
  language: submitLanguage.value,
  source: submitSource.value,
}))

const submitRules: FormRules = {
  source: [{ required: true, message: '请粘贴代码', trigger: 'blur' }],
}

async function loadSubmissions() {
  // 接口：GET /problems/{problemId}/submissions
  submissions.value = await getProblemSubmissions(problemId)
}

function openSubmitDialog() {
  submitLanguage.value = 'C++17'
  submitSource.value = ''
  submitVisible.value = true
}

async function handleSubmitCode() {
  if (!submitFormRef.value) return
  const valid = await submitFormRef.value.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    // 接口：POST /problems/{problemId}/submit
    await submitProblemCode(problemId, {
      problemId,
      language: submitLanguage.value,
      code: submitSource.value,
    })
    ElMessage.success('代码已提交，正在评测中')
    submitVisible.value = false
    activeTab.value = 'submissions'
    await loadSubmissions()
    // TODO(backend): 判题由 OJ 异步完成，可改为轮询 /problems/{problemId}/submissions
    // 直到 result 不再是 PENDING / JUDGING
    window.setTimeout(loadSubmissions, 1600)
  } finally {
    submitLoading.value = false
  }
}

const limits = computed(() =>
  problem.value
    ? `${problem.value.timeLimit} ms / ${Math.round(problem.value.memoryLimit / 1024)} MB`
    : '',
)

async function load() {
  loading.value = true
  try {
    // 接口：GET /problems/{problemId}
    problem.value = await getProblem(problemId)
    await loadSubmissions()
  } finally {
    loading.value = false
  }
}

function openSolutionDialog() {
  solutionForm.content = problem.value?.solution?.content ?? ''
  solutionForm.referenceCode = problem.value?.solution?.referenceCode ?? ''
  solutionForm.attachments = [...(problem.value?.solution?.attachments ?? [])]
  solutionDialog.value = true
}

async function saveSolution() {
  if (!solutionForm.content.trim()) {
    ElMessage.warning('题解内容不能为空')
    return
  }
  submitting.value = true
  try {
    // 接口：PUT /problems/{problemId}/solution（Markdown + 参考代码 + 附件）
    await updateSolution(problemId, {
      content: solutionForm.content,
      referenceCode: solutionForm.referenceCode || null,
      attachments: solutionForm.attachments,
    })
    ElMessage.success('题解已保存')
    solutionDialog.value = false
    load()
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  await load()
  // 权限矩阵 v2：集训私题仅 正式会员/队长/助理教练/指导老师 可见
  if (problem.value?.visibility === 'TRAINING_PRIVATE' && !auth.hasRole(BANK_PRIVATE_ROLES)) {
    ElMessage.warning('集训私有题目仅对正式会员、队长、助理教练与指导老师开放')
    router.replace('/problems')
  }
})
</script>

<template>
<PageHeader
    :title="problem?.title ?? '题目详情'"
    :subtitle="problem ? `OJ 编号 ${problem.ojProblemId} · ${limits}` : ''"
  >
    <template #actions>
      <el-button :icon="'ArrowLeft'" @click="router.push('/problems')">返回题库</el-button>
      <el-button type="primary" :icon="'Upload'" @click="openSubmitDialog">提交代码</el-button>
      <el-button v-permission="CONTENT_MANAGER_ROLES" @click="openSolutionDialog">
        {{ problem?.solution ? '编辑题解' : '补充题解' }}
      </el-button>
    </template>
</PageHeader>

<div v-loading="loading" class="page">
  <template v-if="problem">
    <div class="ic-card meta-card">
      <div class="meta-left">
        <StatusTag
          :label="difficultyMap[problem.difficulty as Difficulty].label"
          :tone="difficultyMap[problem.difficulty as Difficulty].tone"
        />
        <StatusTag
          :label="visibilityMap[problem.visibility as ProblemVisibility].label"
          :tone="visibilityMap[problem.visibility as ProblemVisibility].tone"
        />
        <el-tag v-for="tag in problem.tags" :key="tag.tagId" size="small" effect="plain" round>
          {{ tag.name }}
        </el-tag>
      </div>
      <div class="meta-right">
        <span class="muted">题解更新于 {{ formatDateTime(problem.solution?.updateTime) }}</span>
      </div>
    </div>

    <div class="ic-card content-card">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="题目描述" name="description">
          <div class="statement">
            <section>
              <h3>题目描述</h3>
              <p class="rich-text">{{ problem.description }}</p>
            </section>
            <section v-if="problem.inputFormat">
              <h3>输入格式</h3>
              <p class="rich-text">{{ problem.inputFormat }}</p>
            </section>
            <section v-if="problem.outputFormat">
              <h3>输出格式</h3>
              <p class="rich-text">{{ problem.outputFormat }}</p>
            </section>
            <div class="samples">
              <div class="sample">
                <h3>样例输入</h3>
                <pre>{{ problem.sampleInput }}</pre>
              </div>
              <div class="sample">
                <h3>样例输出</h3>
                <pre>{{ problem.sampleOutput }}</pre>
              </div>
            </div>
            <section v-if="problem.hint">
              <h3>提示</h3>
              <p class="rich-text">{{ problem.hint }}</p>
            </section>
          </div>
        </el-tab-pane>

        <el-tab-pane label="题解" name="solution">
          <div v-if="problem.solution" class="solution">
            <h3>解题思路</h3>
            <p class="rich-text">{{ problem.solution.content }}</p>
            <template v-if="problem.solution.referenceCode">
              <h3>参考代码</h3>
              <pre class="code-block">{{ problem.solution.referenceCode }}</pre>
            </template>
            <template v-if="problem.solution.attachments?.length">
              <h3>附件</h3>
              <div class="attachment-list" data-test="solution-attachments">
                <div
                  v-for="file in problem.solution.attachments"
                  :key="file.url"
                  class="attachment-item"
                >
                  <el-icon><Document /></el-icon>
                  <a :href="file.url" target="_blank" rel="noopener" class="attachment-name">
                    {{ file.filename }}
                  </a>
                  <span class="muted">{{ formatSize(file.size) }}</span>
                </div>
              </div>
            </template>
          </div>
          <EmptyState
            v-else
            title="暂无题解"
            description="具备题库维护权限的成员可补充题解与参考代码"
          >
            <template #action>
              <el-button v-permission="CONTENT_MANAGER_ROLES" type="primary" @click="openSolutionDialog">
                补充题解
              </el-button>
            </template>
          </EmptyState>
        </el-tab-pane>

        <el-tab-pane label="提交记录" name="submissions">
          <el-table :data="submissions" row-key="submitId">
            <el-table-column label="提交人" width="120">
              <template #default="{ row }">{{ row.submitter.name }}</template>
            </el-table-column>
            <el-table-column label="语言" width="120">
              <template #default="{ row }">{{ row.language }}</template>
            </el-table-column>
            <el-table-column label="结果" width="110">
              <template #default="{ row }">
                <StatusTag
                  :label="judgeResultMap[row.result as JudgeResult].label"
                  :tone="judgeResultMap[row.result as JudgeResult].tone"
                />
              </template>
            </el-table-column>
            <el-table-column label="耗时" width="110">
              <template #default="{ row }">
                {{ row.timeUsed ? `${row.timeUsed} ms` : '—' }}
              </template>
            </el-table-column>
            <el-table-column label="内存" width="110">
              <template #default="{ row }">
                {{ row.memoryUsed ? `${Math.round(row.memoryUsed / 1024)} MB` : '—' }}
              </template>
            </el-table-column>
            <el-table-column label="提交时间" width="170">
              <template #default="{ row }">{{ formatDateTime(row.submitTime) }}</template>
            </el-table-column>
            <template #empty>
              <EmptyState title="还没有提交记录" description="点击右上角「提交代码」开始作答" />
            </template>
          </el-table>
          <div class="poll-tip">
            <el-icon><InfoFilled /></el-icon>
            <!-- TODO(backend): 判题由自建 OJ 异步完成，可轮询提交记录直到结果落定 -->
            PENDING / JUDGING 状态表示评测中，稍后会自动刷新结果。
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>
  </template>
</div>

<el-dialog v-model="solutionDialog" title="题解维护" width="780px" top="6vh">
  <el-form label-position="top">
    <el-form-item label="解题思路（Markdown）">
      <el-input
        v-model="solutionForm.content"
        type="textarea"
        :rows="8"
        placeholder="支持 Markdown 格式，例如：## 思路 / ## 复杂度"
      />
    </el-form-item>
    <el-form-item label="参考代码（可选）">
      <el-input
        v-model="solutionForm.referenceCode"
        type="textarea"
        :rows="8"
        class="mono-input"
        placeholder="粘贴参考代码"
      />
    </el-form-item>
    <el-form-item label="题解附件（可选，单个不超过 10MB）">
      <div class="attachment-editor">
        <el-upload
          data-test="solution-upload"
          :http-request="handleUploadRequest"
          :before-upload="beforeAttachmentUpload"
          :show-file-list="false"
          multiple
        >
          <el-button :icon="'Upload'" :loading="uploading">上传附件</el-button>
        </el-upload>
        <div v-if="solutionForm.attachments.length" class="attachment-list">
          <div
            v-for="(file, index) in solutionForm.attachments"
            :key="file.url"
            class="attachment-item"
            data-test="attachment-item"
          >
            <el-icon><Document /></el-icon>
            <a :href="file.url" target="_blank" rel="noopener" class="attachment-name">
              {{ file.filename }}
            </a>
            <span class="muted">{{ formatSize(file.size) }}</span>
            <el-button link type="danger" @click="removeAttachment(index)">移除</el-button>
          </div>
        </div>
      </div>
    </el-form-item>
  </el-form>
  <template #footer>
    <el-button @click="solutionDialog = false">取消</el-button>
    <el-button type="primary" :loading="submitting" @click="saveSolution">保存题解</el-button>
  </template>
</el-dialog>

<!-- 提交代码 -->
<el-dialog v-model="submitVisible" title="提交代码" width="760px" top="6vh">
  <el-form
    ref="submitFormRef"
    :model="submitFormModel"
    :rules="submitRules"
    label-position="top"
  >
    <el-form-item label="编程语言">
      <el-select v-model="submitLanguage" style="width: 200px">
        <el-option v-for="lang in languages" :key="lang" :label="lang" :value="lang" />
      </el-select>
    </el-form-item>
    <el-form-item label="源代码" prop="source">
      <el-input
        v-model="submitSource"
        type="textarea"
        :rows="14"
        class="mono-input"
        placeholder="粘贴源代码，提交后将进入评测队列"
      />
    </el-form-item>
  </el-form>
  <template #footer>
    <el-button @click="submitVisible = false">取消</el-button>
    <el-button type="primary" :loading="submitLoading" @click="handleSubmitCode">
      提交评测
    </el-button>
  </template>
</el-dialog>
</template>

<style scoped>
.meta-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 16px 22px;
  margin-bottom: 20px;
}

.meta-left {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.content-card {
  padding: 12px 28px 28px;
}

.statement section,
.solution {
  margin-bottom: 26px;
}

h3 {
  margin: 0 0 12px;
  font-size: 15px;
  font-weight: 600;
  line-height: 1.5;
  color: var(--text-main);
}

.rich-text {
  margin: 0;
  font-size: 14px;
  line-height: 2;
  color: var(--text-sub);
  white-space: pre-wrap;
}

.samples {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 18px;
  margin: 24px 0;
}

.sample h3 {
  margin-bottom: 8px;
}

pre {
  margin: 0;
  padding: 14px 16px;
  border-radius: var(--radius-md);
  background: var(--bg-blue-softer);
  border: 1px solid var(--border);
  font-family: var(--font-mono);
  font-size: 12px;
  line-height: 1.7;
  color: var(--text-main);
  overflow-x: auto;
}

.code-block {
  background: #f6fafd;
  max-height: 420px;
  overflow-y: auto;
}

.attachment-editor {
  width: 100%;
}

.attachment-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 8px;
}

.attachment-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  background: var(--bg-blue-softer);
  font-size: 13px;
}

.attachment-name {
  color: var(--primary-deep);
  text-decoration: none;
}

.attachment-name:hover {
  text-decoration: underline;
}

.mono-input :deep(textarea) {
  font-family: var(--font-mono);
  font-size: 12px;
}

.poll-tip {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 16px;
  padding: 10px 14px;
  border-radius: var(--radius-md);
  background: var(--primary-lighter);
  color: var(--text-sub);
  font-size: 12.5px;
  line-height: 1.7;
}

@media (max-width: 900px) {
  .samples {
    grid-template-columns: 1fr;
  }
}
</style>
