/**
 * 测试公共工具：Pinia / Router / 登录态预置
 * 说明：组件测试统一从这里拿测试上下文，避免每个用例重复样板代码。
 */
import { createPinia, setActivePinia } from 'pinia'
import { createMemoryHistory, createRouter, type Router } from 'vue-router'
import { TOKEN_KEY } from '@/api/request'
import type { RoleCode, UserInfo } from '@/types'

/** auth store 中用户信息的 localStorage key（与 stores/auth.ts 保持一致） */
const USER_KEY = 'icpc_user'

/** 各视图会跳转到的路由，统一用空组件占位 */
const routes = [
  { path: '/', name: 'home', component: { template: '<div />' } },
  { path: '/login', name: 'login', component: { template: '<div />' } },
  { path: '/dashboard', name: 'dashboard', component: { template: '<div />' } },
  { path: '/members', name: 'members', component: { template: '<div />' } },
  { path: '/members/approvals', name: 'approvals', component: { template: '<div />' } },
  { path: '/members/:id', name: 'member-detail', component: { template: '<div />' } },
  { path: '/problems', name: 'problems', component: { template: '<div />' } },
  { path: '/problems/:id', name: 'problem-detail', component: { template: '<div />' } },
  { path: '/trainings', name: 'trainings', component: { template: '<div />' } },
  { path: '/trainings/ability/:userId', name: 'ability', component: { template: '<div />' } },
  { path: '/trainings/:id', name: 'training-detail', component: { template: '<div />' } },
  { path: '/teams', name: 'teams', component: { template: '<div />' } },
  { path: '/teams/:id', name: 'team-detail', component: { template: '<div />' } },
  { path: '/camps', name: 'camps', component: { template: '<div />' } },
  { path: '/camps/:id', name: 'camp-detail', component: { template: '<div />' } },
  { path: '/contests', name: 'contests', component: { template: '<div />' } },
  { path: '/contests/:id', name: 'contest-detail', component: { template: '<div />' } },
  { path: '/mbti', name: 'mbti', component: { template: '<div />' } },
  { path: '/:pathMatch(.*)*', name: 'not-found', component: { template: '<div />' } },
]

/** 创建内存路由实例 */
export function createTestRouter(): Router {
  return createRouter({ history: createMemoryHistory(), routes })
}

/** 清空登录态（每个用例 beforeEach 建议调用） */
export function resetAuth() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
}

/**
 * 预置登录态并激活 Pinia。
 * 注意：store 创建时读取 localStorage，因此必须先写存储再 setActivePinia。
 */
export function setupAuth(role: RoleCode = 'PRESIDENT', patch: Partial<UserInfo> = {}) {
  const user: UserInfo = {
    userId: 1,
    studentId: '1001',
    name: '测试用户',
    roles: [role],
    ...patch,
  }
  localStorage.setItem(TOKEN_KEY, 'mock-token-1001')
  localStorage.setItem(USER_KEY, JSON.stringify(user))
  const pinia = createPinia()
  setActivePinia(pinia)
  return { pinia, user }
}

/** 仅创建并激活 Pinia（未登录场景） */
export function setupPinia() {
  const pinia = createPinia()
  setActivePinia(pinia)
  return pinia
}

/** 等待所有微任务 / 组件异步加载完成 */
export function flushAsync(times = 2): Promise<void> {
  return new Promise((resolve) => {
    let count = 0
    const tick = () => {
      count += 1
      if (count >= times) resolve()
      else setTimeout(tick, 0)
    }
    setTimeout(tick, 0)
  })
}
