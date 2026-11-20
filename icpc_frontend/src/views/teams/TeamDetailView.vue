<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import type { EChartsOption } from 'echarts'
import PageHeader from '@/components/PageHeader.vue'
import StatusTag from '@/components/StatusTag.vue'
import EmptyState from '@/components/EmptyState.vue'
import BaseChart from '@/components/BaseChart.vue'
import {
  confirmTeam,
  getTeam,
  inviteTeamMember,
  removeTeamMember,
  updateTeam,
} from '@/api/teams'
import { getMembers } from '@/api/members'
import {
  CONTENT_MANAGER_ROLES,
  TEAM_CREATE_ROLES,
  formatDate,
  memberTypeMap,
  teamStatusMap,
} from '@/utils/dict'
import { useAuthStore } from '@/stores/auth'
import type {
  MemberInfo,
  MemberType,
  TeamInfo,
  TeamMember,
  TeamPayload,
  TeamStatus,
} from '@/types'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const teamId = Number(route.params.id)

const loading = ref(false)
const team = ref<TeamInfo | null>(null)
const candidates = ref<MemberInfo[]>([])

/** 队长权限：PUT /teams/{teamId}、邀请成员、移除成员 */
const isLeader = computed(() => team.value?.leader.userId === auth.user?.userId)
/** 教练确认队伍：POST /teams/{teamId}/confirm（ASSISTANT_COACH+） */
const canConfirm = computed(() => auth.hasRole(CONTENT_MANAGER_ROLES))
/** 3.2「组队」一行：正式会员 / 队长可组队（观察会员不可） */
const canTeam = computed(() => auth.hasRole(TEAM_CREATE_ROLES))

/** 可邀请的会员：不在当前队伍中的会员 */
const invitableMembers = computed(() => {
  const joined = new Set(team.value?.members.map((item) => item.memberId) ?? [])
  return candidates.value.filter((item) => !joined.has(item.memberId))
})

const radarOption = computed<EChartsOption>(() => {
  const dimensions = team.value?.dimensions ?? []
  return {
    tooltip: { trigger: 'item' },
    radar: {
      indicator: dimensions.map((item) => ({ name: item.name, max: 100 })),
      radius: '66%',
      center: ['50%', '52%'],
      splitNumber: 4,
      axisName: { color: '#52646f', fontSize: 12 },
      splitLine: { lineStyle: { color: '#e3eef7' } },
      splitArea: {
        areaStyle: { color: ['rgba(241,247,253,0.6)', 'rgba(253,252,247,0.6)'] },
      },
      axisLine: { lineStyle: { color: '#e3eef7' } },
    },
    series: [
      {
        type: 'radar',
        symbol: 'circle',
        symbolSize: 6,
        lineStyle: { color: '#5b9bd5', width: 2 },
        itemStyle: { color: '#5b9bd5' },
        areaStyle: { color: 'rgba(91,155,213,0.22)' },
        data: [{ name: '队伍能力', value: dimensions.map((item) => item.value) }],
      },
    ],
  }
})

async function load() {
  loading.value = true
  try {
    // 接口：GET /teams/{teamId}（队伍详情，含成员与能力画像）
    team.value = await getTeam(teamId)
  } finally {
    loading.value = false
  }
}

async function loadCandidates() {
  if (!isLeader.value) return
  // 接口：GET /members（邀请成员时选择候选人）
  const result = await getMembers({ page: 1, size: 100, status: 'ACTIVE' })
  candidates.value = result.records
}

/* ============ 更新队伍信息 ============ */
const editVisible = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance>()
const form = reactive<TeamPayload>({ name: '', description: null })

const rules: FormRules<TeamPayload> = {
  name: [{ required: true, message: '请输入队伍名称', trigger: 'blur' }],
}

function openEdit() {
  if (!team.value) return
  Object.assign(form, { name: team.value.name, description: team.value.description })
  editVisible.value = true
}

async function handleUpdate() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    // 接口：PUT /teams/{teamId}（更新队伍信息，权限：队长）
    await updateTeam(teamId, { ...form })
    ElMessage.success('队伍信息已更新')
    editVisible.value = false
    await load()
  } finally {
    submitting.value = false
  }
}

/* ============ 邀请成员 ============ */
const inviteVisible = ref(false)
const inviteMemberId = ref<number | null>(null)

async function openInvite() {
  inviteMemberId.value = null
  await loadCandidates()
  inviteVisible.value = true
}

async function handleInvite() {
  if (inviteMemberId.value === null) {
    ElMessage.warning('请选择要邀请的会员')
    return
  }
  // 接口：POST /teams/{teamId}/members（邀请成员，权限：队长）
  await inviteTeamMember(teamId, inviteMemberId.value)
  ElMessage.success('已邀请该会员加入队伍')
  inviteVisible.value = false
  await load()
}

/* ============ 移除成员 / 离队 ============ */
async function handleRemove(member: TeamMember, isSelf = false) {
  const confirmed = await ElMessageBox.confirm(
    isSelf ? '确认退出该队伍？' : `确认将「${member.name}」移出队伍？`,
    isSelf ? '离队确认' : '移除成员',
    { type: 'warning', confirmButtonText: '确认', cancelButtonText: '取消' },
  ).catch(() => false)
  if (!confirmed) return

  // 接口：DELETE /teams/{teamId}/members?memberId=（移除成员 / 离队）
  await removeTeamMember(teamId, member.memberId)
  ElMessage.success(isSelf ? '已退出队伍' : '成员已移出队伍')
  if (isSelf) {
    router.push('/teams')
    return
  }
  await load()
}

/* ============ 教练确认队伍 ============ */
async function handleConfirm() {
  const confirmed = await ElMessageBox.confirm(
    '确认该队伍？确认后队伍可报名参加校赛 / XCPC 赛事。',
    '确认队伍',
    { type: 'info', confirmButtonText: '确认', cancelButtonText: '取消' },
  ).catch(() => false)
  if (!confirmed) return

  // 接口：POST /teams/{teamId}/confirm（教练确认队伍，权限：ASSISTANT_COACH+）
  await confirmTeam(teamId)
  ElMessage.success('队伍已确认')
  await load()
}

onMounted(async () => {
  await load()
  await loadCandidates()
})
</script>

<template>
<PageHeader
    :title="team?.name ?? '队伍详情'"
    :subtitle="team ? `队长 ${team.leader.name} · ${team.members.length} 名成员` : ''"
  >
    <template #actions>
      <el-button :icon="'ArrowLeft'" @click="router.push('/teams')">返回列表</el-button>
      <el-button
        v-if="isLeader"
        :icon="'Edit'"
        @click="openEdit"
      >
        更新队伍信息
      </el-button>
      <el-button v-if="isLeader" :icon="'Plus'" @click="openInvite">邀请成员</el-button>
      <el-button
        v-if="canConfirm && team?.status === 'PENDING'"
        type="primary"
        :icon="'CircleCheck'"
        @click="handleConfirm"
      >
        教练确认队伍
      </el-button>
    </template>
</PageHeader>

<div v-loading="loading" class="page">
  <template v-if="team">
    <div class="ic-card overview">
      <div class="overview-item">
        <span>队伍状态</span>
        <StatusTag
          :label="teamStatusMap[team.status as TeamStatus].label"
          :tone="teamStatusMap[team.status as TeamStatus].tone"
        />
      </div>
      <div class="overview-item">
        <span>队长</span>
        <strong>{{ team.leader.name }}</strong>
      </div>
      <div class="overview-item">
        <span>成员人数</span>
        <strong>{{ team.members.length }} 人</strong>
      </div>
      <div class="overview-item">
        <span>创建时间</span>
        <strong>{{ formatDate(team.createTime) }}</strong>
      </div>
    </div>

    <div class="detail-grid">
      <div class="ic-card members-card">
        <h2 class="section-title">队伍成员</h2>
        <el-table :data="team.members" row-key="memberId">
          <el-table-column label="姓名" min-width="120">
            <template #default="{ row }">
              <div class="member-cell">
                <div class="avatar">{{ row.name.slice(0, 1) }}</div>
                <span>{{ row.name }}</span>
                <el-tag v-if="row.isLeader" size="small" effect="light" round>队长</el-tag>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="studentId" label="学号" width="130" />
          <el-table-column label="会员类型" width="130">
            <template #default="{ row }">
              <StatusTag
                :label="memberTypeMap[row.memberType as MemberType]?.label ?? row.memberType"
                :tone="memberTypeMap[row.memberType as MemberType]?.tone ?? 'info'"
              />
            </template>
          </el-table-column>
          <el-table-column label="加入时间" width="140">
            <template #default="{ row }">{{ formatDate(row.joinTime) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="130" fixed="right">
            <template #default="{ row }">
              <el-button
                v-if="isLeader && !row.isLeader"
                link
                type="danger"
                @click="handleRemove(row)"
              >
                移除成员
              </el-button>
              <el-button
                v-else-if="!row.isLeader && row.memberId === auth.user?.userId"
                link
                type="warning"
                @click="handleRemove(row, true)"
              >
                离队
              </el-button>
              <span v-else class="muted">—</span>
            </template>
          </el-table-column>
          <template #empty>
            <EmptyState title="暂无成员" description="队长可通过「邀请成员」添加队友" />
          </template>
        </el-table>
      </div>

      <div class="ic-card profile-card">
        <h2 class="section-title">队伍能力画像</h2>
        <BaseChart v-if="team.dimensions.length" :option="radarOption" height="320px" />
        <EmptyState
          v-else
          title="暂无能力数据"
          description="队伍成员完成训练后自动生成能力画像"
        />
      </div>
    </div>
  </template>
</div>

<!-- 更新队伍信息 -->
<el-dialog v-model="editVisible" title="更新队伍信息" width="560px" destroy-on-close>
  <el-form ref="formRef" :model="form" :rules="rules" label-width="88px">
    <el-form-item label="队伍名称" prop="name">
      <el-input v-model="form.name" placeholder="请输入队伍名称" />
    </el-form-item>
    <el-form-item label="队伍简介">
      <el-input v-model="form.description" type="textarea" :rows="3" placeholder="选填" />
    </el-form-item>
  </el-form>
  <template #footer>
    <el-button @click="editVisible = false">取消</el-button>
    <el-button type="primary" :loading="submitting" @click="handleUpdate">保存</el-button>
  </template>
</el-dialog>

<!-- 邀请成员 -->
<el-dialog v-model="inviteVisible" title="邀请成员" width="520px" destroy-on-close>
  <el-form label-width="88px">
    <el-form-item label="选择会员">
      <el-select
        v-model="inviteMemberId"
        filterable
        placeholder="按姓名 / 学号搜索会员"
        style="width: 100%"
      >
        <el-option
          v-for="member in invitableMembers"
          :key="member.memberId"
          :label="`${member.name} · ${member.studentId}`"
          :value="member.memberId"
        />
      </el-select>
    </el-form-item>
  </el-form>
  <div v-if="canTeam" class="invite-tip">
    <el-icon><InfoFilled /></el-icon>
    邀请后成员立即加入队伍，移除成员仅能由队长或本人发起。
  </div>
  <template #footer>
    <el-button @click="inviteVisible = false">取消</el-button>
    <el-button type="primary" @click="handleInvite">发送邀请</el-button>
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

.detail-grid {
  display: grid;
  grid-template-columns: 1.35fr 1fr;
  gap: 18px;
  align-items: start;
}

.members-card,
.profile-card {
  padding: 20px 22px;
}

.member-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  color: var(--primary-deep);
  background: var(--primary-light);
}

.invite-tip {
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
  .detail-grid {
    grid-template-columns: 1fr;
  }
}
</style>
