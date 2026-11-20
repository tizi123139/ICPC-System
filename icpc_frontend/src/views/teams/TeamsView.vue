<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import StatusTag from '@/components/StatusTag.vue'
import EmptyState from '@/components/EmptyState.vue'
import { createTeam, getTeams } from '@/api/teams'
import { TEAM_CREATE_ROLES, formatDate, teamStatusMap } from '@/utils/dict'
import { useAuthStore } from '@/stores/auth'
import type { TeamInfo, TeamPayload, TeamQuery, TeamStatus } from '@/types'

const router = useRouter()
const auth = useAuthStore()

const loading = ref(false)
const list = ref<TeamInfo[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(9)

const query = reactive<TeamQuery>({
  keyword: '',
  status: '',
  page: 1,
  size: 9,
})

const statusTabs: Array<{ key: TeamStatus | ''; label: string }> = [
  { key: '', label: '全部' },
  { key: 'PENDING', label: '待教练确认' },
  { key: 'CONFIRMED', label: '已确认' },
]

const activeStatus = computed({
  get: () => query.status ?? '',
  set: (value: TeamStatus | '') => {
    query.status = value
    page.value = 1
    load()
  },
})

/** 3.2 权限矩阵「组队」一行：正式会员 / 队长 可组队（观察会员不可） */
const canCreateTeam = computed(() => auth.hasRole(TEAM_CREATE_ROLES))

async function load() {
  loading.value = true
  try {
    // 接口：GET /teams（分页 + 状态筛选）
    const result = await getTeams({ ...query, page: page.value, size: size.value })
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

watch(page, load)

/* ============ 创建队伍 ============ */
const dialogVisible = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance>()

const form = reactive<TeamPayload>({
  name: '',
  description: null,
})

const rules: FormRules<TeamPayload> = {
  name: [{ required: true, message: '请输入队伍名称', trigger: 'blur' }],
}

function openCreate() {
  Object.assign(form, { name: '', description: null })
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    // 接口：POST /teams（创建队伍，发起组队申请）
    const team = await createTeam({ ...form })
    ElMessage.success('队伍已创建，等待助理教练确认')
    dialogVisible.value = false
    router.push(`/teams/${team.teamId}`)
  } finally {
    submitting.value = false
  }
}

onMounted(load)
</script>

<template>
<PageHeader title="组队管理" subtitle="创建队伍、维护成员并查看队伍能力画像，队伍需经教练确认后可参赛">
    <template #actions>
      <el-button v-if="canCreateTeam" type="primary" :icon="'Plus'" @click="openCreate">
        创建队伍
      </el-button>
    </template>
</PageHeader>

<div class="page">
  <div class="ic-card toolbar-card">
    <div class="tabs">
      <button
        v-for="tab in statusTabs"
        :key="tab.key || 'all'"
        type="button"
        class="tab"
        :class="{ active: activeStatus === tab.key }"
        @click="activeStatus = tab.key"
      >
        {{ tab.label }}
      </button>
    </div>
    <div class="toolbar-right">
      <el-input
        v-model="query.keyword"
        placeholder="搜索队伍名称"
        clearable
        style="width: 220px"
        :prefix-icon="'Search'"
        @keyup.enter="handleSearch"
        @clear="handleSearch"
      />
    </div>
  </div>

  <div v-loading="loading" class="team-grid">
    <div v-for="team in list" :key="team.teamId" class="ic-card ic-card--hover team-card">
      <div class="card-top">
        <StatusTag
          :label="teamStatusMap[team.status as TeamStatus].label"
          :tone="teamStatusMap[team.status as TeamStatus].tone"
        />
        <span class="member-badge">{{ team.members.length }} 名成员</span>
      </div>

      <h3 class="team-name" @click="router.push(`/teams/${team.teamId}`)">{{ team.name }}</h3>
      <p class="team-desc">{{ team.description || '暂无队伍简介' }}</p>

      <div class="info-row">
        <el-icon><UserFilled /></el-icon>
        <span>队长：{{ team.leader.name }}</span>
      </div>
      <div class="info-row">
        <el-icon><Calendar /></el-icon>
        <span>创建于 {{ formatDate(team.createTime) }}</span>
      </div>

      <div class="member-preview">
        <el-tag v-for="member in team.members.slice(0, 3)" :key="member.memberId" size="small" effect="plain" round>
          {{ member.name }}{{ member.isLeader ? '（队长）' : '' }}
        </el-tag>
        <span v-if="team.members.length > 3" class="more">+{{ team.members.length - 3 }}</span>
      </div>

      <div class="card-actions">
        <el-button link type="primary" @click="router.push(`/teams/${team.teamId}`)">查看详情</el-button>
      </div>
    </div>

    <EmptyState
      v-if="!loading && list.length === 0"
      class="grid-empty"
      title="暂无队伍"
      description="创建一个队伍并邀请队友，开始参加校赛与 XCPC 赛事"
    />
  </div>

  <el-pagination
    v-if="total > 0"
    v-model:current-page="page"
    v-model:page-size="size"
    :total="total"
    :page-sizes="[9, 18, 36]"
    layout="total, sizes, prev, pager, next"
    background
    class="page-pagination"
  />
</div>

<!-- 创建队伍 -->
<el-dialog v-model="dialogVisible" title="创建队伍" width="560px" destroy-on-close>
  <el-form ref="formRef" :model="form" :rules="rules" label-width="88px">
    <el-form-item label="队伍名称" prop="name">
      <el-input v-model="form.name" placeholder="如 深夜 AC 小分队" />
    </el-form-item>
    <el-form-item label="队伍简介">
      <el-input
        v-model="form.description"
        type="textarea"
        :rows="3"
        placeholder="介绍队伍方向、训练安排等（选填）"
      />
    </el-form-item>
  </el-form>
  <div class="create-tip">
    <el-icon><InfoFilled /></el-icon>
    创建后需等待助理教练确认队伍，确认后方可报名参赛。
  </div>
  <template #footer>
    <el-button @click="dialogVisible = false">取消</el-button>
    <el-button type="primary" :loading="submitting" @click="handleSubmit">创建队伍</el-button>
  </template>
</el-dialog>
</template>

<style scoped>
.toolbar-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 18px;
  margin-bottom: 18px;
}

.tabs {
  display: flex;
  gap: 4px;
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

.toolbar-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.team-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
  min-height: 200px;
}

.team-card {
  display: flex;
  flex-direction: column;
  padding: 18px 20px 12px;
}

.card-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.member-badge {
  font-size: 11px;
  color: var(--text-muted);
  padding: 2px 8px;
  border-radius: var(--radius-pill);
  background: var(--bg-blue-soft);
}

.team-name {
  margin: 0 0 8px;
  font-size: 15px;
  font-weight: 600;
  color: var(--text-main);
  cursor: pointer;
  line-height: 1.5;
}

.team-name:hover {
  color: var(--primary-deep);
}

.team-desc {
  margin: 0 0 12px;
  font-size: 12.5px;
  line-height: 1.7;
  color: var(--text-sub);
  min-height: 42px;
}

.info-row {
  display: flex;
  align-items: center;
  gap: 7px;
  font-size: 12px;
  color: var(--text-sub);
  margin-bottom: 7px;
}

.info-row .el-icon {
  color: var(--primary);
}

.member-preview {
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
  margin: 12px 0 6px;
  min-height: 24px;
}

.more {
  font-size: 11px;
  color: var(--text-muted);
  align-self: center;
}

.card-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  margin-top: auto;
  padding-top: 10px;
  border-top: 1px dashed var(--border);
}

.grid-empty {
  grid-column: 1 / -1;
}

.page-pagination {
  margin-top: 18px;
}

.create-tip {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--primary-deep);
}

@media (max-width: 1280px) {
  .team-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 860px) {
  .team-grid {
    grid-template-columns: 1fr;
  }
  .toolbar-card {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }
}
</style>
