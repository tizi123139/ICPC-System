<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import EmptyState from '@/components/EmptyState.vue'
import { getMembers } from '@/api/members'
import {
  deleteSystemFile,
  getRoleOptions,
  getSystemConfigs,
  getSystemFiles,
  getSystemLogDetail,
  getSystemLogs,
  updateMemberRoles,
  updateSystemConfigs,
} from '@/api/system'
import { formatDate, formatDateTime } from '@/utils/dict'
import type { MemberInfo, RoleCode, RoleOption, SystemConfig, SystemFile, SystemLog } from '@/types'

const activeTab = ref<'accounts' | 'logs' | 'configs' | 'files'>('accounts')
const loading = ref(false)
const memberOptions = ref<MemberInfo[]>([])
const roleOptions = ref<RoleOption[]>([])

/* ============ 账号角色管理（PUT /system/members/{memberId}/roles） ============ */
const rolesDialogVisible = ref(false)
const rolesSubmitting = ref(false)
const rolesTarget = ref<MemberInfo | null>(null)
const rolesDraft = ref<RoleCode[]>([])

async function loadAccounts() {
  loading.value = true
  try {
    // 接口：GET /members（会员列表，用于角色管理）
    const result = await getMembers({ page: 1, size: 500 })
    memberOptions.value = result.records
  } finally {
    loading.value = false
  }
}

function openRoles(row: MemberInfo) {
  rolesTarget.value = row
  // 以会员当前真实角色作为草稿，避免保存时误清空
  rolesDraft.value = [...(row.roles ?? [])]
  rolesDialogVisible.value = true
}

async function submitRoles() {
  if (!rolesTarget.value) return
  rolesSubmitting.value = true
  try {
    // 接口：PUT /system/members/{memberId}/roles —— 调整会员角色
    await updateMemberRoles(rolesTarget.value.memberId, { roles: rolesDraft.value })
    ElMessage.success('角色已调整')
    rolesDialogVisible.value = false
  } finally {
    rolesSubmitting.value = false
  }
}

/* ============ 操作日志（GET /system/logs、GET /system/logs/{logId}） ============ */
const logs = ref<SystemLog[]>([])
const logDetailVisible = ref(false)
const logDetail = ref<SystemLog | null>(null)

async function loadLogs() {
  loading.value = true
  try {
    // 接口：GET /system/logs
    logs.value = await getSystemLogs()
  } finally {
    loading.value = false
  }
}

async function openLogDetail(row: SystemLog) {
  // 接口：GET /system/logs/{logId}
  logDetail.value = await getSystemLogDetail(row.logId)
  logDetailVisible.value = true
}

/* ============ 系统配置（GET /system/configs、PUT /system/configs） ============ */
const configs = ref<SystemConfig[]>([])
const configsDirty = ref(false)
const configsSubmitting = ref(false)

async function loadConfigs() {
  loading.value = true
  try {
    // 接口：GET /system/configs
    configs.value = await getSystemConfigs()
    configsDirty.value = false
  } finally {
    loading.value = false
  }
}

async function saveConfigs() {
  configsSubmitting.value = true
  try {
    // 接口：PUT /system/configs —— 批量更新
    await updateSystemConfigs({
      configs: configs.value.map((item) => ({
        configKey: item.configKey,
        configValue: item.configValue,
      })),
    })
    ElMessage.success('配置已保存')
    loadConfigs()
  } finally {
    configsSubmitting.value = false
  }
}

/* ============ 文件记录（GET /system/files、DELETE /system/files/{fileId}） ============ */
const files = ref<SystemFile[]>([])

async function loadFiles() {
  loading.value = true
  try {
    // 接口：GET /system/files
    files.value = await getSystemFiles()
  } finally {
    loading.value = false
  }
}

async function handleDeleteFile(row: SystemFile) {
  await ElMessageBox.confirm(`确认删除文件「${row.filename}」？`, '删除确认', { type: 'warning' })
  // 接口：DELETE /system/files/{fileId}
  await deleteSystemFile(row.fileId)
  ElMessage.success('文件已删除')
  loadFiles()
}

function formatSize(size: number): string {
  if (size >= 1024 * 1024) return `${(size / 1024 / 1024).toFixed(1)} MB`
  return `${Math.max(1, Math.round(size / 1024))} KB`
}

function onTabChange(tab: string) {
  if (tab === 'accounts') loadAccounts()
  else if (tab === 'logs') loadLogs()
  else if (tab === 'configs') loadConfigs()
  else if (tab === 'files') loadFiles()
}

onMounted(() => {
  loadAccounts()
  // 接口：GET /system/roles —— 角色字典
  getRoleOptions().then((options) => {
    roleOptions.value = options
  })
})
</script>

<template>
  <PageHeader title="系统管理" subtitle="会员角色、操作日志、系统配置与文件记录（会长/副会长、指导老师）" />

  <div class="page">
    <el-tabs v-model="activeTab" class="ic-card system-tabs" @tab-change="onTabChange">
      <el-tab-pane label="账号角色" name="accounts">
        <el-table v-loading="loading" :data="memberOptions" row-key="memberId">
          <el-table-column label="会员" min-width="160">
            <template #default="{ row }">
              <strong>{{ row.name }}</strong>
              <span class="mono">{{ row.studentId }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="major" label="专业" min-width="160" show-overflow-tooltip />
          <el-table-column prop="grade" label="年级" width="100" />
          <el-table-column label="会员类型" width="110">
            <template #default="{ row }">
              <el-tag size="small" effect="plain">
                {{ row.memberType === 'OBSERVE_MEMBER' ? '观察会员' : '正式会员' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="入会时间" width="120">
            <template #default="{ row }">{{ formatDate(row.joinDate) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="120" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openRoles(row)">调整角色</el-button>
            </template>
          </el-table-column>
          <template #empty>
            <EmptyState title="暂无会员" />
          </template>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="操作日志" name="logs">
        <el-table v-loading="loading" :data="logs" row-key="logId">
          <el-table-column prop="userName" label="操作人" width="120" />
          <el-table-column prop="action" label="动作" width="220" show-overflow-tooltip />
          <el-table-column prop="detail" label="详情" min-width="280" show-overflow-tooltip />
          <el-table-column prop="ip" label="IP" width="130" />
          <el-table-column label="时间" width="160">
            <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="90" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openLogDetail(row)">详情</el-button>
            </template>
          </el-table-column>
          <template #empty>
            <EmptyState title="暂无日志" />
          </template>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="系统配置" name="configs">
        <div class="configs-toolbar">
          <el-button type="primary" :loading="configsSubmitting" :disabled="!configsDirty" @click="saveConfigs">
            保存修改
          </el-button>
          <span v-if="configsDirty" class="dirty-tip">有未保存的修改</span>
        </div>
        <el-table v-loading="loading" :data="configs" row-key="configKey">
          <el-table-column prop="configKey" label="配置项" min-width="220" />
          <el-table-column label="配置值" width="220">
            <template #default="{ row }">
              <el-input
                v-model="row.configValue"
                @input="configsDirty = true"
              />
            </template>
          </el-table-column>
          <el-table-column prop="description" label="说明" min-width="220">
            <template #default="{ row }">{{ row.description ?? '—' }}</template>
          </el-table-column>
          <el-table-column label="更新时间" width="160">
            <template #default="{ row }">{{ formatDateTime(row.updateTime) }}</template>
          </el-table-column>
          <template #empty>
            <EmptyState title="暂无配置" />
          </template>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="文件记录" name="files">
        <el-table v-loading="loading" :data="files" row-key="fileId">
          <el-table-column prop="filename" label="文件名" min-width="240" show-overflow-tooltip />
          <el-table-column prop="uploaderName" label="上传人" width="120" />
          <el-table-column label="大小" width="110">
            <template #default="{ row }">{{ formatSize(row.size) }}</template>
          </el-table-column>
          <el-table-column label="上传时间" width="160">
            <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="110" fixed="right">
            <template #default="{ row }">
              <el-button link type="danger" @click="handleDeleteFile(row)">删除</el-button>
            </template>
          </el-table-column>
          <template #empty>
            <EmptyState title="暂无文件" />
          </template>
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </div>

  <el-dialog v-model="rolesDialogVisible" title="调整会员角色" width="480px">
    <p class="dialog-tip">
      为 <strong>{{ rolesTarget?.name }}</strong>（{{ rolesTarget?.studentId }}）分配角色
    </p>
    <el-select v-model="rolesDraft" multiple style="width: 100%" placeholder="选择角色（可多选）">
      <el-option
        v-for="option in roleOptions"
        :key="option.roleCode"
        :label="option.roleName"
        :value="option.roleCode"
      />
    </el-select>
    <p class="dialog-warn">注意：调整角色会立即影响该会员的系统访问权限。</p>
    <template #footer>
      <el-button @click="rolesDialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="rolesSubmitting" @click="submitRoles">保存</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="logDetailVisible" title="日志详情" width="560px">
    <el-descriptions v-if="logDetail" :column="1" border>
      <el-descriptions-item label="日志编号">{{ logDetail.logId }}</el-descriptions-item>
      <el-descriptions-item label="操作人">{{ logDetail.userName }}</el-descriptions-item>
      <el-descriptions-item label="动作">{{ logDetail.action }}</el-descriptions-item>
      <el-descriptions-item label="详情">{{ logDetail.detail }}</el-descriptions-item>
      <el-descriptions-item label="IP">{{ logDetail.ip ?? '—' }}</el-descriptions-item>
      <el-descriptions-item label="时间">{{ formatDateTime(logDetail.createTime) }}</el-descriptions-item>
    </el-descriptions>
  </el-dialog>
</template>

<style scoped>
.system-tabs {
  padding: 4px 16px 16px;
}

.mono {
  font-family: var(--ic-mono, 'JetBrains Mono', Consolas, monospace);
  font-size: 12px;
  color: var(--ic-text-muted, #8a94a6);
  margin-left: 6px;
}

.configs-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.dirty-tip {
  font-size: 12px;
  color: var(--ic-warning, #d97706);
}

.dialog-tip {
  margin: 0 0 12px;
}

.dialog-warn {
  margin-top: 12px;
  font-size: 12px;
  color: var(--ic-danger, #e5484d);
}
</style>
