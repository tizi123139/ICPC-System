/**
 * 认证状态（Pinia）
 * - token 持久化到 localStorage，供 Axios 请求拦截器读取；
 * - roles 用于路由守卫、菜单渲染与按钮级权限判断；
 * - 角色由登录学号决定（Mock 见 src/mock/index.ts），前端不提供角色切换。
 */
import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { getProfile, login as apiLogin, logout as apiLogout } from '@/api/auth'
import { TOKEN_KEY } from '@/api/request'
import type { LoginRequest, RoleCode, UserInfo } from '@/types'

const USER_KEY = 'icpc_user'

function readStoredUser(): UserInfo | null {
  try {
    const raw = localStorage.getItem(USER_KEY)
    return raw ? (JSON.parse(raw) as UserInfo) : null
  } catch {
    return null
  }
}

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem(TOKEN_KEY) ?? '')
  const user = ref<UserInfo | null>(readStoredUser())

  const isLoggedIn = computed(() => Boolean(token.value))
  const roles = computed<RoleCode[]>(() => user.value?.roles ?? [])

  function persist() {
    if (token.value) localStorage.setItem(TOKEN_KEY, token.value)
    if (user.value) localStorage.setItem(USER_KEY, JSON.stringify(user.value))
  }

  /** 判断当前用户是否拥有任一指定角色；未指定角色时默认放行 */
  function hasRole(allowed?: RoleCode[]): boolean {
    if (!allowed || allowed.length === 0) return true
    return roles.value.some((role) => allowed.includes(role))
  }

  /** 登录：POST /auth/login */
  async function login(payload: LoginRequest) {
    const result = await apiLogin(payload)
    token.value = result.token
    user.value = result.user
    persist()
  }

  /** 拉取当前用户信息：GET /auth/profile */
  async function fetchProfile() {
    const profile = await getProfile()
    user.value = profile
    localStorage.setItem(USER_KEY, JSON.stringify(profile))
  }

  /** 登出：POST /auth/logout */
  async function logout() {
    try {
      await apiLogout()
    } finally {
      token.value = ''
      user.value = null
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(USER_KEY)
    }
  }

  return {
    token,
    user,
    roles,
    isLoggedIn,
    hasRole,
    login,
    fetchProfile,
    logout,
  }
})
