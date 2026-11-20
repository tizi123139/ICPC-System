<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import StatusTag from '@/components/StatusTag.vue'
import EmptyState from '@/components/EmptyState.vue'
import {
  createProblem,
  deleteProblem,
  getProblems,
  importProblemsFromOj,
  updateProblem,
} from '@/api/problems'
import { createTag, deleteTag, getTagTree, updateTag } from '@/api/tags'
import {
  BANK_PRIVATE_ROLES,
  CONTENT_MANAGER_ROLES,
  difficultyMap,
  visibilityMap,
} from '@/utils/dict'
import { useAuthStore } from '@/stores/auth'
import type {
  Difficulty,
  ProblemInfo,
  ProblemPayload,
  ProblemQuery,
  ProblemVisibility,
  TagNode,
} from '@/types'

const router = useRouter()
const auth = useAuthStore()

const loading = ref(false)
const list = ref<ProblemInfo[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const tagTree = ref<TagNode[]>([])
const selectedTagId = ref<number | null>(null)

const query = reactive<ProblemQuery>({
  keyword: '',
  tagId: undefined,
  difficulty: '',
  visibility: '',
  page: 1,
  size: 10,
})

const difficultyOptions = Object.entries(difficultyMap) as Array<[Difficulty, { label: string }]>
/** 集训私题库仅对 正式会员/队长/助理教练/指导老师 可见（权限矩阵 v2） */
const canSeePrivate = computed(() => auth.hasRole(BANK_PRIVATE_ROLES))
const visibilityOptions = computed(() =>
  (Object.entries(visibilityMap) as Array<[ProblemVisibility, { label: string }]>).filter(
    ([code]) => code === 'PUBLIC' || canSeePrivate.value,
  ),
)
const allTags = computed(() =>
  tagTree.value.flatMap((node) => [node, ...(node.children ?? [])]),
)

async function loadTags() {
  // 接口：GET /tags（知识点树）
  tagTree.value = await getTagTree()
}

async function load() {
  loading.value = true
  try {
    // 接口：GET /problems（分页 + 知识点 / 难度 / 可见范围筛选）
    const result = await getProblems({ ...query, page: page.value, size: size.value })
    // 无私题库权限（观察会员等）时过滤掉集训私有题目
    list.value = canSeePrivate.value
      ? result.records
      : result.records.filter((item) => item.visibility === 'PUBLIC')
    total.value = canSeePrivate.value ? result.total : list.value.length
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  page.value = 1
  load()
}

function handleReset() {
  query.keyword = ''
  query.difficulty = ''
  query.visibility = ''
  selectedTagId.value = null
  query.tagId = undefined
  page.value = 1
  load()
}

function handleTagClick(node: TagNode) {
  selectedTagId.value = node.tagId
  query.tagId = node.tagId
  handleSearch()
}

/* ============ 知识点增删改 ============ */
const canManageTags = computed(() => auth.hasRole(CONTENT_MANAGER_ROLES))
const canDeleteTag = computed(() => auth.hasRole(['PRESIDENT', 'ADVISOR']))
const tagDialogVisible = ref(false)
const tagSubmitting = ref(false)
const tagFormRef = ref<FormInstance>()
const editingTagId = ref<number | null>(null)
const parentTag = ref<TagNode | null>(null)
const tagForm = reactive({ name: '', category: '' })

const categoryOptions = computed(() =>
  Array.from(new Set(tagTree.value.map((node) => node.category))),
)

const tagRules: FormRules = {
  name: [{ required: true, message: '请输入知识点名称', trigger: 'blur' }],
  category: [{ required: true, message: '请选择或输入分类', trigger: 'change' }],
}

/** 新增知识点：parent 为空表示新增一级分类 */
function openTagCreate(parent: TagNode | null = null) {
  editingTagId.value = null
  parentTag.value = parent
  tagForm.name = ''
  tagForm.category = parent?.category ?? ''
  tagDialogVisible.value = true
}

function openTagEdit(node: TagNode) {
  editingTagId.value = node.tagId
  parentTag.value = null
  tagForm.name = node.name
  tagForm.category = node.category
  tagDialogVisible.value = true
}

async function handleTagSubmit() {
  if (!tagFormRef.value) return
  const valid = await tagFormRef.value.validate().catch(() => false)
  if (!valid) return

  tagSubmitting.value = true
  try {
    if (editingTagId.value) {
      // 接口：PUT /tags/{tagId}（修改知识点）
      await updateTag(editingTagId.value, { ...tagForm })
      ElMessage.success('知识点已更新')
    } else {
      // 接口：POST /tags（新增知识点）
      await createTag({
        name: tagForm.name,
        category: tagForm.category,
        parentId: parentTag.value?.tagId ?? null,
      })
      ElMessage.success('知识点已新增')
    }
    tagDialogVisible.value = false
    await loadTags()
  } finally {
    tagSubmitting.value = false
  }
}

async function handleTagDelete(node: TagNode) {
  const confirmed = await ElMessageBox.confirm(
    `确认删除知识点「${node.name}」？${node.children?.length ? '其子知识点也会一并删除。' : ''}`,
    '删除知识点',
    { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' },
  ).catch(() => false)
  if (!confirmed) return

  // 接口：DELETE /tags/{tagId}，权限：PRESIDENT+
  // TODO(backend): 后端需处理关联题目的知识点引用（置空或同步删除）
  await deleteTag(node.tagId)
  ElMessage.success('知识点已删除')
  await loadTags()
  if (selectedTagId.value === node.tagId) {
    handleReset()
    return
  }
  load()
}

watch(page, load)

/* ============ 新增 / 编辑题目 ============ */
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const submitting = ref(false)
const formRef = ref<FormInstance>()

const emptyForm = (): ProblemPayload => ({
  title: '',
  ojProblemId: '',
  difficulty: 'EASY',
  timeLimit: 1000,
  memoryLimit: 131072,
  description: '',
  inputFormat: '',
  outputFormat: '',
  sampleInput: '',
  sampleOutput: '',
  hint: null,
  tagIds: [],
  visibility: 'PUBLIC',
})

const form = reactive<ProblemPayload>(emptyForm())

const rules: FormRules<ProblemPayload> = {
  title: [{ required: true, message: '请输入题目标题', trigger: 'blur' }],
  ojProblemId: [{ required: true, message: '请输入 OJ 题目编号', trigger: 'blur' }],
  description: [{ required: true, message: '请输入题目描述', trigger: 'blur' }],
}

function openCreate() {
  editingId.value = null
  Object.assign(form, emptyForm())
  dialogVisible.value = true
}

function openEdit(row: ProblemInfo) {
  editingId.value = row.problemId
  Object.assign(form, {
    title: row.title,
    ojProblemId: row.ojProblemId,
    difficulty: row.difficulty,
    timeLimit: row.timeLimit,
    memoryLimit: row.memoryLimit,
    description: row.description,
    inputFormat: row.inputFormat,
    outputFormat: row.outputFormat,
    sampleInput: row.sampleInput,
    sampleOutput: row.sampleOutput,
    hint: row.hint,
    tagIds: row.tags.map((tag) => tag.tagId),
    visibility: row.visibility,
  })
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    // 接口：POST /problems（新增）/ PUT /problems/{problemId}（修改）
    if (editingId.value) {
      await updateProblem(editingId.value, { ...form })
      ElMessage.success('题目已更新')
    } else {
      await createProblem({ ...form })
      ElMessage.success('题目已创建')
    }
    dialogVisible.value = false
    load()
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row: ProblemInfo) {
  const confirmed = await ElMessageBox.confirm(
    `确认删除题目「${row.title}」？`,
    '删除题目',
    { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' },
  ).catch(() => false)
  if (!confirmed) return
  // 接口：DELETE /problems/{problemId}，权限：PRESIDENT+
  await deleteProblem(row.problemId)
  ElMessage.success('已删除')
  load()
}

/* ============ OJ 批量导入 ============ */
const importVisible = ref(false)
const importText = ref('')
const importing = ref(false)

async function handleImport() {
  const ids = importText.value
    .split(/[\s,，]+/)
    .map((item) => item.trim())
    .filter(Boolean)
  if (ids.length === 0) {
    ElMessage.warning('请填写至少一个 OJ 题目编号')
    return
  }
  importing.value = true
  try {
    // 接口：POST /problems/import（从 OJ 批量导入）
    const result = await importProblemsFromOj(ids)
    ElMessage.success(`已导入 ${result.imported} 道题目`)
    importVisible.value = false
    importText.value = ''
    load()
  } finally {
    importing.value = false
  }
}

onMounted(() => {
  loadTags()
  load()
})
</script>

<template>
<PageHeader title="题目管理" subtitle="按知识点维护题库，支持从自建 OJ 批量导入">
    <template #actions>
      <el-button v-permission="CONTENT_MANAGER_ROLES" :icon="'Download'" @click="importVisible = true">
        从 OJ 导入
      </el-button>
      <el-button v-permission="CONTENT_MANAGER_ROLES" type="primary" :icon="'Plus'" @click="openCreate">
        新增题目
      </el-button>
    </template>
</PageHeader>

<div class="page problem-page">
  <!-- 知识点树 -->
  <aside class="ic-card tag-card">
    <div class="tag-head">
      <h2 class="section-title">知识点</h2>
      <div class="tag-head-actions">
        <el-button link type="primary" @click="handleReset">清除筛选</el-button>
        <el-button
          v-permission="CONTENT_MANAGER_ROLES"
          link
          type="primary"
          :icon="'Plus'"
          @click="openTagCreate()"
        >
          新增
        </el-button>
      </div>
    </div>
    <el-tree
      :data="tagTree"
      node-key="tagId"
      :props="{ label: 'name', children: 'children' }"
      default-expand-all
      highlight-current
      :expand-on-click-node="false"
      @node-click="handleTagClick"
    >
      <template #default="{ data }">
        <span class="tag-node">
          <span class="tag-node-name">{{ data.name }}</span>
          <span v-if="canManageTags" class="tag-node-actions">
            <el-icon title="新增子知识点" @click.stop="openTagCreate(data)">
              <Plus />
            </el-icon>
            <el-icon title="编辑" @click.stop="openTagEdit(data)">
              <EditPen />
            </el-icon>
            <el-icon
              v-if="canDeleteTag"
              class="danger"
              title="删除"
              @click.stop="handleTagDelete(data)"
            >
              <Delete />
            </el-icon>
          </span>
        </span>
      </template>
    </el-tree>
  </aside>

  <!-- 题目列表 -->
  <div class="main-col">
    <div class="ic-card filter-card">
      <el-form :inline="true" :model="query" @submit.prevent="handleSearch">
        <el-form-item label="关键词">
          <el-input
            v-model="query.keyword"
            placeholder="题目标题 / OJ 编号"
            clearable
            style="width: 210px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="难度">
          <el-select v-model="query.difficulty" placeholder="全部" clearable style="width: 120px">
            <el-option v-for="[code, item] in difficultyOptions" :key="code" :label="item.label" :value="code" />
          </el-select>
        </el-form-item>
        <el-form-item label="可见范围">
          <el-select v-model="query.visibility" placeholder="全部" clearable style="width: 130px">
            <el-option v-for="[code, item] in visibilityOptions" :key="code" :label="item.label" :value="code" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="'Search'" @click="handleSearch">查询</el-button>
          <el-button :icon="'Refresh'" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div v-loading="loading" class="ic-card table-card">
      <div class="table-head">
        <h2 class="section-title">
          题目列表
          <span v-if="selectedTagId" class="tag-filter">
            已按知识点筛选
            <el-icon><Close @click="handleReset" /></el-icon>
          </span>
        </h2>
        <span class="muted">共 {{ total }} 道题目</span>
      </div>

      <el-table :data="list" row-key="problemId">
        <el-table-column label="题目" min-width="240">
          <template #default="{ row }">
            <a class="problem-title" @click="router.push(`/problems/${row.problemId}`)">
              <span class="oj-id mono">{{ row.ojProblemId }}</span>
              {{ row.title }}
            </a>
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
        <el-table-column label="知识点" min-width="200">
          <template #default="{ row }">
            <div class="tag-list">
              <el-tag v-for="tag in row.tags" :key="tag.tagId" size="small" effect="plain" round>
                {{ tag.name }}
              </el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="可见范围" width="110">
          <template #default="{ row }">
            <StatusTag
              :label="visibilityMap[row.visibility as ProblemVisibility].label"
              :tone="visibilityMap[row.visibility as ProblemVisibility].tone"
            />
          </template>
        </el-table-column>
        <el-table-column label="限制" width="140">
          <template #default="{ row }">
            <span class="muted mono">{{ row.timeLimit }}ms / {{ Math.round(row.memoryLimit / 1024) }}MB</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="190" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="router.push(`/problems/${row.problemId}`)">
              查看
            </el-button>
            <el-button v-permission="CONTENT_MANAGER_ROLES" link @click="openEdit(row)">编辑</el-button>
            <el-button
              v-if="auth.hasRole(['PRESIDENT', 'ADVISOR'])"
              link
              type="danger"
              @click="handleDelete(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
        <template #empty>
          <EmptyState title="未找到题目" description="调整筛选条件，或从 OJ 批量导入题目" />
        </template>
      </el-table>

      <el-pagination
        v-model:current-page="page"
        v-model:page-size="size"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        background
        @size-change="handleSearch"
      />
    </div>
  </div>
</div>

<!-- 新增 / 编辑题目 -->
<el-dialog
  v-model="dialogVisible"
  :title="editingId ? '编辑题目' : '新增题目'"
  width="780px"
  top="6vh"
  destroy-on-close
>
  <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
    <div class="form-grid">
      <el-form-item label="题目标题" prop="title">
        <el-input v-model="form.title" placeholder="如 A + B Problem" />
      </el-form-item>
      <el-form-item label="OJ 编号" prop="ojProblemId">
        <el-input v-model="form.ojProblemId" placeholder="对应自建 OJ 题目编号" />
      </el-form-item>
      <el-form-item label="难度">
        <el-select v-model="form.difficulty" style="width: 100%">
          <el-option v-for="[code, item] in difficultyOptions" :key="code" :label="item.label" :value="code" />
        </el-select>
      </el-form-item>
      <el-form-item label="可见范围">
        <el-select v-model="form.visibility" style="width: 100%">
          <el-option v-for="[code, item] in visibilityOptions" :key="code" :label="item.label" :value="code" />
        </el-select>
      </el-form-item>
      <el-form-item label="时间限制">
        <el-input-number v-model="form.timeLimit" :min="100" :max="10000" :step="100" style="width: 100%" />
      </el-form-item>
      <el-form-item label="内存限制">
        <el-input-number v-model="form.memoryLimit" :min="16384" :max="1048576" :step="16384" style="width: 100%" />
      </el-form-item>
    </div>

    <el-form-item label="知识点">
      <el-select
        v-model="form.tagIds"
        multiple
        filterable
        collapse-tags
        collapse-tags-tooltip
        placeholder="选择关联知识点"
        style="width: 100%"
      >
        <el-option v-for="tag in allTags" :key="tag.tagId" :label="tag.name" :value="tag.tagId" />
      </el-select>
    </el-form-item>
    <el-form-item label="题目描述" prop="description">
      <el-input v-model="form.description" type="textarea" :rows="4" placeholder="支持 Markdown 格式" />
    </el-form-item>
    <div class="form-grid">
      <el-form-item label="输入格式">
        <el-input v-model="form.inputFormat" type="textarea" :rows="2" />
      </el-form-item>
      <el-form-item label="输出格式">
        <el-input v-model="form.outputFormat" type="textarea" :rows="2" />
      </el-form-item>
      <el-form-item label="样例输入">
        <el-input v-model="form.sampleInput" type="textarea" :rows="3" class="mono-input" />
      </el-form-item>
      <el-form-item label="样例输出">
        <el-input v-model="form.sampleOutput" type="textarea" :rows="3" class="mono-input" />
      </el-form-item>
    </div>
    <el-form-item label="提示">
      <el-input v-model="form.hint" type="textarea" :rows="2" placeholder="可选" />
    </el-form-item>
  </el-form>
  <template #footer>
    <el-button @click="dialogVisible = false">取消</el-button>
    <el-button type="primary" :loading="submitting" @click="handleSubmit">
      {{ editingId ? '保存修改' : '创建题目' }}
    </el-button>
  </template>
</el-dialog>

<!-- OJ 批量导入 -->
<el-dialog v-model="importVisible" title="从 OJ 批量导入题目" width="520px">
  <p class="muted import-desc">
    填写自建 OJ 的题目编号，多个编号用逗号或换行分隔。导入后可在题目详情中补充题解与知识点。
  </p>
  <el-input
    v-model="importText"
    type="textarea"
    :rows="5"
    placeholder="1000, 1001, 1002"
  />
  <div class="audit-tip">
    <el-icon><InfoFilled /></el-icon>
    <!-- TODO(backend): 该接口需后端调用 OJ 的 GET /oj/problems?ids=[] 同步题目数据 -->
    导入依赖 OJ 接口协议，联调时由后端完成题目同步。
  </div>
  <template #footer>
    <el-button @click="importVisible = false">取消</el-button>
    <el-button type="primary" :loading="importing" @click="handleImport">开始导入</el-button>
  </template>
</el-dialog>

<!-- 新增 / 编辑知识点 -->
<el-dialog
  v-model="tagDialogVisible"
  :title="editingTagId ? '编辑知识点' : '新增知识点'"
  width="500px"
  destroy-on-close
>
  <el-form ref="tagFormRef" :model="tagForm" :rules="tagRules" label-width="86px">
    <el-form-item v-if="parentTag" label="父知识点">
      <el-input :model-value="parentTag.name" disabled />
    </el-form-item>
    <el-form-item label="名称" prop="name">
      <el-input v-model="tagForm.name" placeholder="如 并查集" />
    </el-form-item>
    <el-form-item label="所属分类" prop="category">
      <el-select
        v-model="tagForm.category"
        filterable
        allow-create
        default-first-option
        placeholder="选择或输入分类"
        style="width: 100%"
      >
        <el-option
          v-for="item in categoryOptions"
          :key="item"
          :label="item"
          :value="item"
        />
      </el-select>
    </el-form-item>
  </el-form>
  <template #footer>
    <el-button @click="tagDialogVisible = false">取消</el-button>
    <el-button type="primary" :loading="tagSubmitting" @click="handleTagSubmit">
      {{ editingTagId ? '保存修改' : '确认新增' }}
    </el-button>
  </template>
</el-dialog>
</template>

<style scoped>
.problem-page {
  display: grid;
  grid-template-columns: 250px 1fr;
  gap: 18px;
  align-items: start;
}

.tag-card {
  padding: 18px 14px 14px;
  position: sticky;
  top: 0;
}

.tag-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 6px;
}

.tag-head .section-title {
  margin-bottom: 10px;
}

.tag-head-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.tag-node {
  display: flex;
  flex: 1;
  align-items: center;
  gap: 8px;
  min-width: 0;
  padding-right: 6px;
}

.tag-node-name {
  flex: 1;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.tag-node-actions {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: var(--text-muted);
  opacity: 0.4;
  transition: opacity 0.16s ease;
}

.tag-node:hover .tag-node-actions,
.el-tree :deep(.el-tree-node.is-current > .el-tree-node__content) .tag-node-actions {
  opacity: 1;
}

.tag-node-actions .el-icon {
  font-size: 14px;
  cursor: pointer;
}

.tag-node-actions .el-icon:hover {
  color: var(--primary-deep);
}

.tag-node-actions .danger:hover {
  color: var(--el-color-danger);
}

.el-tree {
  --el-tree-node-hover-bg-color: var(--primary-lighter);
  background: transparent;
  font-size: 13px;
  color: var(--text-sub);
}

.el-tree :deep(.el-tree-node__content) {
  height: 32px;
  border-radius: var(--radius-sm);
}

.el-tree :deep(.is-current .el-tree-node__content) {
  background: var(--primary-lighter);
  color: var(--primary-deep);
  font-weight: 600;
}

.main-col {
  display: flex;
  flex-direction: column;
  gap: 18px;
  min-width: 0;
}

.filter-card {
  padding: 18px 20px 0;
}

.filter-card :deep(.el-form-item) {
  margin-bottom: 18px;
}

.table-card {
  padding: 18px 20px 16px;
}

.table-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.table-head .section-title {
  margin-bottom: 14px;
}

.tag-filter {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin-left: 10px;
  font-size: 11px;
  font-weight: 400;
  color: var(--primary-deep);
  background: var(--primary-lighter);
  padding: 2px 8px;
  border-radius: var(--radius-pill);
}

.tag-filter .el-icon {
  cursor: pointer;
}

.problem-title {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: var(--text-main);
  font-weight: 500;
}

.problem-title:hover {
  color: var(--primary-deep);
}

.oj-id {
  font-size: 11px;
  color: var(--primary-deep);
  background: var(--primary-lighter);
  padding: 1px 7px;
  border-radius: var(--radius-pill);
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  column-gap: 18px;
}

.mono-input :deep(textarea) {
  font-family: var(--font-mono);
  font-size: 12px;
}

.import-desc {
  margin: 0 0 12px;
  line-height: 1.7;
}

.audit-tip {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 12px;
  font-size: 12px;
  color: var(--primary-deep);
}
</style>
