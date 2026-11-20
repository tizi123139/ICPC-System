<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import StatusTag from '@/components/StatusTag.vue'
import EmptyState from '@/components/EmptyState.vue'
import { createMember, deleteMember, getMembers, updateMember, type MemberPayload } from '@/api/members'
import { MEMBER_INFO_MANAGER_ROLES, memberStatusMap, memberTypeMap } from '@/utils/dict'
import { useAuthStore } from '@/stores/auth'
import type { MemberInfo, MemberQuery, MemberStatus, MemberType } from '@/types'

const router = useRouter()
const auth = useAuthStore()

const loading = ref(false)
const list = ref<MemberInfo[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)

const query = reactive<MemberQuery>({
  keyword: '',
  memberType: '',
  grade: '',
  department: '',
  status: '',
  page: 1,
  size: 10,
})

const gradeOptions = ['2022级', '2023级', '2024级', '2025级']
const departmentOptions = ['竞赛部', '组织部', '宣传部', '秘书部']
const memberTypeOptions = Object.entries(memberTypeMap) as Array<[MemberType, { label: string }]>
const statusOptions = Object.entries(memberStatusMap) as Array<[MemberStatus, { label: string }]>

async function load() {
  loading.value = true
  try {
    // 接口：GET /members（分页 + 多条件筛选）
    const result = await getMembers({ ...query, page: page.value, size: size.value })
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

function handleReset() {
  query.keyword = ''
  query.memberType = ''
  query.grade = ''
  query.department = ''
  query.status = ''
  page.value = 1
  load()
}

watch(page, load)

/* ============ 新增 / 编辑会员 ============ */
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const submitting = ref(false)
const formRef = ref<FormInstance>()

const emptyForm = (): MemberPayload => ({
  name: '',
  studentId: '',
  major: '',
  grade: '2023级',
  phone: '',
  email: '',
  memberType: 'OBSERVE_MEMBER',
  department: null,
  joinDate: new Date().toISOString().slice(0, 10),
  status: 'ACTIVE',
})

const form = reactive<MemberPayload>(emptyForm())

const rules: FormRules<MemberPayload> = {
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  studentId: [
    { required: true, message: '请输入学号', trigger: 'blur' },
    { pattern: /^\d{6,12}$/, message: '学号格式不正确', trigger: 'blur' },
  ],
  major: [{ required: true, message: '请输入专业', trigger: 'blur' }],
  phone: [{ required: true, message: '请输入手机号', trigger: 'blur' }],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }],
}

function openCreate() {
  editingId.value = null
  Object.assign(form, emptyForm())
  dialogVisible.value = true
}

function openEdit(row: MemberInfo) {
  editingId.value = row.memberId
  Object.assign(form, {
    name: row.name,
    studentId: row.studentId,
    major: row.major,
    grade: row.grade,
    phone: row.phone,
    email: row.email,
    memberType: row.memberType,
    department: row.department,
    joinDate: row.joinDate,
    status: row.status,
  })
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    // 接口：POST /members（新增） / PUT /members/{memberId}（更新）
    if (editingId.value) {
      await updateMember(editingId.value, { ...form })
      ElMessage.success('会员信息已更新')
    } else {
      await createMember({ ...form })
      ElMessage.success('会员已创建')
    }
    dialogVisible.value = false
    load()
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row: MemberInfo) {
  const confirmed = await ElMessageBox.confirm(
    `确认删除会员「${row.name}」？该操作为逻辑删除。`,
    '删除会员',
    { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' },
  ).catch(() => false)
  if (!confirmed) return
  // 接口：DELETE /members/{memberId}，权限：PRESIDENT
  await deleteMember(row.memberId)
  ElMessage.success('已删除')
  load()
}

function viewArchive(row: MemberInfo) {
  router.push(`/members/${row.memberId}`)
}

onMounted(load)
</script>

<template>
<PageHeader title="会员列表" subtitle="管理协会会员信息、分级类型与在会状态">
    <template #actions>
      <el-button v-permission="MEMBER_INFO_MANAGER_ROLES" type="primary" :icon="'Plus'" @click="openCreate">
        新增会员
      </el-button>
    </template>
</PageHeader>

<div class="page">
  <div class="ic-card filter-card">
    <el-form :inline="true" :model="query" class="filter-form" @submit.prevent="handleSearch">
      <el-form-item label="关键词">
        <el-input
          v-model="query.keyword"
          placeholder="姓名 / 学号"
          clearable
          style="width: 200px"
          @keyup.enter="handleSearch"
        />
      </el-form-item>
      <el-form-item label="会员类型">
        <el-select v-model="query.memberType" placeholder="全部" clearable style="width: 150px">
          <el-option v-for="[code, item] in memberTypeOptions" :key="code" :label="item.label" :value="code" />
        </el-select>
      </el-form-item>
      <el-form-item label="年级">
        <el-select v-model="query.grade" placeholder="全部" clearable style="width: 120px">
          <el-option v-for="item in gradeOptions" :key="item" :label="item" :value="item" />
        </el-select>
      </el-form-item>
      <el-form-item label="部门">
        <el-select v-model="query.department" placeholder="全部" clearable style="width: 120px">
          <el-option v-for="item in departmentOptions" :key="item" :label="item" :value="item" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
          <el-option v-for="[code, item] in statusOptions" :key="code" :label="item.label" :value="code" />
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
      <h2 class="section-title">会员信息</h2>
      <span class="muted">共 {{ total }} 名会员</span>
    </div>

    <el-table :data="list" row-key="memberId">
      <el-table-column label="会员" min-width="170">
        <template #default="{ row }">
          <div class="member-cell">
            <div class="avatar">{{ row.name.slice(0, 1) }}</div>
            <div class="member-meta">
              <strong>{{ row.name }}</strong>
              <span class="mono">{{ row.studentId }}</span>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="专业 / 年级" min-width="200">
        <template #default="{ row }">
          <div class="two-line">
            <span>{{ row.major }}</span>
            <span class="muted">{{ row.grade }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="类型" width="130">
        <template #default="{ row }">
          <StatusTag
            :label="memberTypeMap[row.memberType as MemberType]?.label ?? row.memberType"
            :tone="memberTypeMap[row.memberType as MemberType]?.tone ?? 'info'"
          />
        </template>
      </el-table-column>
      <el-table-column label="部门" width="110">
        <template #default="{ row }">{{ row.department || '—' }}</template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <StatusTag
            :label="memberStatusMap[row.status as MemberStatus].label"
            :tone="memberStatusMap[row.status as MemberStatus].tone"
          />
        </template>
      </el-table-column>
      <el-table-column prop="joinDate" label="加入日期" width="120" />
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="viewArchive(row)">查看档案</el-button>
          <el-button v-permission="MEMBER_INFO_MANAGER_ROLES" link @click="openEdit(row)">编辑</el-button>
          <el-button
            v-if="auth.roles.includes('PRESIDENT')"
            link
            type="danger"
            @click="handleDelete(row)"
          >
            删除
          </el-button>
        </template>
      </el-table-column>
      <template #empty>
        <EmptyState title="未找到会员" description="尝试调整筛选条件或新增会员" />
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

<!-- 新增 / 编辑会员弹窗 -->
<el-dialog
  v-model="dialogVisible"
  :title="editingId ? '编辑会员' : '新增会员'"
  width="620px"
  destroy-on-close
>
  <el-form ref="formRef" :model="form" :rules="rules" label-width="86px">
    <div class="form-grid">
      <el-form-item label="姓名" prop="name">
        <el-input v-model="form.name" placeholder="请输入姓名" />
      </el-form-item>
      <el-form-item label="学号" prop="studentId">
        <el-input v-model="form.studentId" placeholder="请输入学号" />
      </el-form-item>
      <el-form-item label="专业" prop="major">
        <el-input v-model="form.major" placeholder="请输入专业" />
      </el-form-item>
      <el-form-item label="年级">
        <el-select v-model="form.grade" style="width: 100%">
          <el-option v-for="item in gradeOptions" :key="item" :label="item" :value="item" />
        </el-select>
      </el-form-item>
      <el-form-item label="手机号" prop="phone">
        <el-input v-model="form.phone" placeholder="请输入手机号" />
      </el-form-item>
      <el-form-item label="邮箱" prop="email">
        <el-input v-model="form.email" placeholder="请输入邮箱" />
      </el-form-item>
      <el-form-item label="会员类型">
        <el-select v-model="form.memberType" style="width: 100%">
          <el-option v-for="[code, item] in memberTypeOptions" :key="code" :label="item.label" :value="code" />
        </el-select>
      </el-form-item>
      <el-form-item label="部门">
        <el-select v-model="form.department" placeholder="未分配" clearable style="width: 100%">
          <el-option v-for="item in departmentOptions" :key="item" :label="item" :value="item" />
        </el-select>
      </el-form-item>
      <el-form-item label="加入日期">
        <el-date-picker
          v-model="form.joinDate"
          type="date"
          value-format="YYYY-MM-DD"
          style="width: 100%"
        />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="form.status" style="width: 100%">
          <el-option v-for="[code, item] in statusOptions" :key="code" :label="item.label" :value="code" />
        </el-select>
      </el-form-item>
    </div>
  </el-form>
  <template #footer>
    <el-button @click="dialogVisible = false">取消</el-button>
    <el-button type="primary" :loading="submitting" @click="handleSubmit">
      {{ editingId ? '保存修改' : '创建会员' }}
    </el-button>
  </template>
</el-dialog>
</template>

<style scoped>
.filter-card {
  padding: 18px 20px 0;
  margin-bottom: 18px;
}

.filter-form :deep(.el-form-item) {
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

.member-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.avatar {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  color: var(--primary-deep);
  background: linear-gradient(140deg, #eaf4fc, #d6e8f7);
}

.member-meta {
  display: flex;
  flex-direction: column;
  line-height: 1.35;
}

.member-meta strong {
  font-size: 13px;
}

.member-meta span {
  font-size: 11px;
  color: var(--text-muted);
}

.two-line {
  display: flex;
  flex-direction: column;
  line-height: 1.4;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  column-gap: 18px;
}
</style>
