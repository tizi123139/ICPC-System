<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import PageHeader from '@/components/PageHeader.vue'
import StatusTag from '@/components/StatusTag.vue'
import EmptyState from '@/components/EmptyState.vue'
import { getMemberArchive } from '@/api/members'
import { formatDate, formatDateTime, memberStatusMap, memberTypeMap } from '@/utils/dict'
import type { MemberArchive, MemberStatus, MemberType } from '@/types'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const archive = ref<MemberArchive | null>(null)

const memberId = Number(route.params.id)

async function load() {
  loading.value = true
  try {
    // 接口：GET /members/{memberId}/archive（本人 / OFFICER+）
    archive.value = await getMemberArchive(memberId)
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
<PageHeader
    title="会员档案"
    subtitle="会员基本信息、训练参与与能力概览"
  >
    <template #actions>
      <el-button :icon="'ArrowLeft'" @click="router.push('/members')">返回列表</el-button>
    </template>
</PageHeader>

<div v-loading="loading" class="page">
  <template v-if="archive">
    <div class="ic-card profile">
      <div class="profile-main">
        <div class="avatar">{{ archive.member.name.slice(0, 1) }}</div>
        <div class="identity">
          <div class="name-row">
            <h2>{{ archive.member.name }}</h2>
            <StatusTag
              :label="memberTypeMap[archive.member.memberType as MemberType]?.label ?? archive.member.memberType"
              :tone="memberTypeMap[archive.member.memberType as MemberType]?.tone ?? 'info'"
            />
            <StatusTag
              :label="memberStatusMap[archive.member.status as MemberStatus].label"
              :tone="memberStatusMap[archive.member.status as MemberStatus].tone"
            />
          </div>
          <p class="mono">{{ archive.member.studentId }} · {{ archive.member.major }} · {{ archive.member.grade }}</p>
        </div>
      </div>
      <div class="profile-stats">
        <div class="stat">
          <span>参与训练</span>
          <strong>{{ archive.trainingCount }}</strong>
        </div>
        <div class="stat">
          <span>代码提交</span>
          <strong>{{ archive.submissionCount }}</strong>
        </div>
        <div class="stat">
          <span>AC 率</span>
          <strong>{{ archive.acRate }}%</strong>
        </div>
      </div>
    </div>

    <div class="grid">
      <div class="ic-card info-card">
        <h2 class="section-title">基本信息</h2>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="学号">{{ archive.member.studentId }}</el-descriptions-item>
          <el-descriptions-item label="姓名">{{ archive.member.name }}</el-descriptions-item>
          <el-descriptions-item label="专业">{{ archive.member.major }}</el-descriptions-item>
          <el-descriptions-item label="年级">{{ archive.member.grade }}</el-descriptions-item>
          <el-descriptions-item label="部门">{{ archive.member.department || '未分配' }}</el-descriptions-item>
          <el-descriptions-item label="加入日期">{{ formatDate(archive.member.joinDate) }}</el-descriptions-item>
          <el-descriptions-item label="手机号">{{ archive.member.phone }}</el-descriptions-item>
          <el-descriptions-item label="邮箱">{{ archive.member.email }}</el-descriptions-item>
        </el-descriptions>
      </div>

      <div class="ic-card info-card">
        <h2 class="section-title">近期训练</h2>
        <el-table :data="archive.recentTrainings" :show-header="true">
          <el-table-column label="训练名称" min-width="180">
            <template #default="{ row }">
              <a class="table-link" @click="router.push(`/trainings/${row.trainingId}`)">
                {{ row.name }}
              </a>
            </template>
          </el-table-column>
          <el-table-column prop="solvedCount" label="通过题数" width="90" />
          <el-table-column label="结束时间" width="150">
            <template #default="{ row }">{{ formatDateTime(row.submitTime) }}</template>
          </el-table-column>
          <template #empty>
            <EmptyState title="暂无训练记录" description="该会员尚未参加训练" />
          </template>
        </el-table>
      </div>
    </div>
  </template>
</div>
</template>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.profile {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  padding: 24px 26px;
  background:
    radial-gradient(500px 200px at 100% 0%, #fdf9ee 0%, transparent 60%),
    #fff;
}

.profile-main {
  display: flex;
  align-items: center;
  gap: 16px;
}

.avatar {
  width: 64px;
  height: 64px;
  border-radius: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  font-weight: 600;
  color: var(--primary-deep);
  background: linear-gradient(140deg, #eaf4fc, #cfe4f6);
}

.identity h2 {
  margin: 0 0 6px;
  font-size: 20px;
}

.name-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.identity p {
  margin: 0;
  font-size: 12px;
  color: var(--text-sub);
}

.profile-stats {
  display: flex;
  gap: 28px;
}

.stat {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 2px;
}

.stat span {
  font-size: 12px;
  color: var(--text-muted);
}

.stat strong {
  font-size: 22px;
  color: var(--primary-deep);
}

.grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 18px;
}

.info-card {
  padding: 20px 22px;
}

.table-link {
  color: var(--text-main);
  cursor: pointer;
}

.table-link:hover {
  color: var(--primary-deep);
}

@media (max-width: 1100px) {
  .grid {
    grid-template-columns: 1fr;
  }
  .profile {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
