<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { navGroups } from '@/config/nav'
import { roleLabels } from '@/utils/dict'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const collapsed = ref(false)
const currentTitle = computed(() => route.meta.title ?? '工作台')
const currentRoleLabel = computed(() =>
  auth.roles[0] ? roleLabels[auth.roles[0]] : '未分配角色',
)
const visibleGroups = computed(() =>
  navGroups
    .map((group) => ({
      ...group,
      items: group.items.filter((item) => !item.roles || auth.hasRole(item.roles)),
    }))
    .filter((group) => group.items.length > 0),
)

async function handleLogout() {
  const confirmed = await ElMessageBox.confirm('确认退出当前账号？', '退出登录', {
    confirmButtonText: '退出',
    cancelButtonText: '取消',
    type: 'warning',
  }).catch(() => false)
  if (!confirmed) return
  await auth.logout()
  router.push('/login')
}

function handleSelect(item: { path?: string }) {
  if (!item.path) return
  router.push(item.path)
}
</script>

<template>
  <div class="layout" :class="{ collapsed }">
    <!-- 侧边导航 -->
    <aside class="sidebar">
      <div class="brand">
        <div class="logo">IC</div>
        <div v-show="!collapsed" class="brand-text">
          <strong>ICPC 协会</strong>
          <span>管理系统</span>
        </div>
      </div>

      <nav class="nav">
        <div v-for="group in visibleGroups" :key="group.title" class="nav-group">
          <p v-show="!collapsed" class="nav-group-title">{{ group.title }}</p>
          <button
            v-for="item in group.items"
            :key="item.label"
            class="nav-item"
            :class="{ active: item.path === route.path }"
            type="button"
            @click="handleSelect(item)"
          >
            <el-icon class="nav-icon"><component :is="item.icon" /></el-icon>
            <span v-show="!collapsed" class="nav-label">{{ item.label }}</span>
          </button>
        </div>
      </nav>
    </aside>

    <!-- 右侧主体 -->
    <div class="main">
      <header class="topbar">
        <div class="topbar-left">
          <button class="collapse-btn" type="button" @click="collapsed = !collapsed">
            <el-icon><component :is="collapsed ? 'Expand' : 'Fold'" /></el-icon>
          </button>
          <div class="crumb">
            <span>首页</span>
            <span class="sep">/</span>
            <strong>{{ currentTitle }}</strong>
          </div>
        </div>

        <div class="topbar-right">
          <el-tag class="role-tag" effect="plain" round>{{ currentRoleLabel }}</el-tag>

          <el-dropdown trigger="click">
            <div class="user">
              <el-avatar :size="32">{{ auth.user?.name?.slice(0, 1) }}</el-avatar>
              <div class="user-meta">
                <strong>{{ auth.user?.name }}</strong>
                <span>{{ currentRoleLabel }}</span>
              </div>
              <el-icon><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item disabled>学号：{{ auth.user?.studentId }}</el-dropdown-item>
                <el-dropdown-item divided @click="handleLogout">
                  <el-icon><SwitchButton /></el-icon>退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <main class="content">
        <router-view />
      </main>
    </div>
  </div>
</template>

<style scoped>
.layout {
  display: flex;
  height: 100vh;
  overflow: hidden;
}

/* ============ 侧边栏 ============ */
.sidebar {
  width: var(--sidebar-width);
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  padding: 20px 14px 16px;
  background: linear-gradient(180deg, #eaf4fc 0%, #f4f9fe 45%, #fdfbf4 100%);
  border-right: 1px solid var(--border);
  transition: width 0.2s ease;
}

.layout.collapsed .sidebar {
  width: var(--sidebar-width-collapsed);
  padding-left: 10px;
  padding-right: 10px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 4px 6px 20px;
}

.logo {
  width: 38px;
  height: 38px;
  flex-shrink: 0;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 15px;
  letter-spacing: 0.5px;
  color: #fff;
  background: linear-gradient(140deg, #6fa9dd, #3d7cb0);
  box-shadow: 0 6px 14px rgba(91, 155, 213, 0.35);
}

.brand-text {
  display: flex;
  flex-direction: column;
  line-height: 1.3;
}

.brand-text strong {
  font-size: 15px;
  color: var(--text-main);
}

.brand-text span {
  font-size: 11px;
  color: var(--text-muted);
  letter-spacing: 1px;
}

.nav {
  flex: 1;
  overflow-y: auto;
  padding-right: 2px;
}

.nav-group {
  margin-bottom: 14px;
}

.nav-group-title {
  margin: 0 0 6px;
  padding-left: 12px;
  font-size: 11px;
  letter-spacing: 1px;
  color: var(--text-muted);
}

.nav-item {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  margin-bottom: 4px;
  border: none;
  border-radius: var(--radius-sm);
  background: transparent;
  color: var(--text-sub);
  font-size: 13px;
  cursor: pointer;
  text-align: left;
  transition:
    background 0.18s ease,
    color 0.18s ease;
}

.nav-item:hover {
  background: rgba(255, 255, 255, 0.75);
  color: var(--primary-deep);
}

.nav-item.active {
  background: #ffffff;
  color: var(--primary-deep);
  font-weight: 600;
  box-shadow: 0 4px 14px rgba(91, 155, 213, 0.16);
}

.nav-item.active .nav-icon {
  color: var(--primary);
}

.nav-icon {
  font-size: 17px;
  flex-shrink: 0;
}

.nav-label {
  flex: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* ============ 顶栏 ============ */
.main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.topbar {
  height: var(--header-height);
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  background: rgba(253, 252, 247, 0.8);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid var(--border);
}

.topbar-left {
  display: flex;
  align-items: center;
  gap: 14px;
}

.collapse-btn {
  width: 32px;
  height: 32px;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  background: #fff;
  color: var(--text-sub);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}

.collapse-btn:hover {
  color: var(--primary-deep);
  border-color: var(--primary-light);
}

.crumb {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: var(--text-muted);
}

.crumb strong {
  color: var(--text-main);
  font-weight: 600;
}

.sep {
  color: var(--border-strong);
}

.topbar-right {
  display: flex;
  align-items: center;
  gap: 14px;
}

.role-tag {
  color: var(--primary-deep);
  background: #fff;
  border-color: var(--primary-light);
  font-weight: 500;
}

.user {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  padding: 6px 10px;
  border-radius: var(--radius-sm);
  transition: background 0.18s ease;
}

.user:hover {
  background: #fff;
}

.user-meta {
  display: flex;
  flex-direction: column;
  line-height: 1.25;
}

.user-meta strong {
  font-size: 13px;
  color: var(--text-main);
}

.user-meta span {
  font-size: 11px;
  color: var(--text-muted);
}

/* ============ 内容区 ============ */
.content {
  flex: 1;
  overflow-y: auto;
  min-height: 0;
}
</style>
