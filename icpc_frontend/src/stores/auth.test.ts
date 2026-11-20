import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { getProfile, login as apiLogin, logout as apiLogout } from '@/api/auth'
import { TOKEN_KEY } from '@/api/request'
import { useAuthStore } from './auth'
import { resetAuth, setupPinia } from '@/test/utils'
import type { LoginResult, UserInfo } from '@/types'

// 伪造认证接口，禁止测试期间发起真实请求
vi.mock('@/api/auth', () => ({
  login: vi.fn(),
  logout: vi.fn(),
  getProfile: vi.fn(),
}))

/** 与 stores/auth.ts 保持一致的用户信息存储 key */
const USER_KEY = 'icpc_user'

const profile: UserInfo = {
  userId: 1,
  studentId: '1001',
  name: '测试用户',
  roles: ['PRESIDENT'],
}

const loginResult: LoginResult = {
  token: 'mock-token-1001',
  expiresIn: 7200,
  user: profile,
}

describe('auth store', () => {
  beforeEach(() => {
    resetAuth()
    setupPinia()
    vi.clearAllMocks()
  })

  afterEach(() => {
    vi.restoreAllMocks()
    vi.clearAllMocks()
    resetAuth()
  })

  it('【清单 2】获取当前登录用户信息：fetchProfile 更新 user/roles 并写入 localStorage', async () => {
    vi.mocked(getProfile).mockResolvedValue(profile)
    const store = useAuthStore()

    await store.fetchProfile()

    expect(getProfile).toHaveBeenCalledTimes(1)
    expect(store.user).toEqual(profile)
    expect(store.roles).toEqual(['PRESIDENT'])
    expect(JSON.parse(localStorage.getItem(USER_KEY) ?? 'null')).toEqual(profile)
  })

  it('【清单 1】登录成功后 token 写入 localStorage', async () => {
    vi.mocked(apiLogin).mockResolvedValue(loginResult)
    const store = useAuthStore()

    await store.login({ studentId: '1001', password: '123456' })

    expect(apiLogin).toHaveBeenCalledWith({ studentId: '1001', password: '123456' })
    expect(store.token).toBe('mock-token-1001')
    expect(store.isLoggedIn).toBe(true)
    expect(store.user).toEqual(profile)
    expect(localStorage.getItem(TOKEN_KEY)).toBe('mock-token-1001')
  })

  it('【清单 3】用户登出：清空 token/user 与 localStorage', async () => {
    vi.mocked(apiLogin).mockResolvedValue(loginResult)
    vi.mocked(apiLogout).mockResolvedValue()
    const store = useAuthStore()
    await store.login({ studentId: '1001', password: '123456' })

    expect(localStorage.getItem(TOKEN_KEY)).toBe('mock-token-1001')

    await store.logout()

    expect(apiLogout).toHaveBeenCalledTimes(1)
    expect(store.token).toBe('')
    expect(store.user).toBeNull()
    expect(store.isLoggedIn).toBe(false)
    expect(localStorage.getItem(TOKEN_KEY)).toBeNull()
    expect(localStorage.getItem(USER_KEY)).toBeNull()
  })

  it('【清单 3】登出接口异常时仍清空本地登录态', async () => {
    const store = useAuthStore()
    store.token = 'mock-token-1001'
    store.user = profile
    localStorage.setItem(TOKEN_KEY, 'mock-token-1001')
    localStorage.setItem(USER_KEY, JSON.stringify(profile))

    vi.mocked(apiLogout).mockRejectedValue(new Error('network error'))

    await expect(store.logout()).rejects.toThrow('network error')

    expect(store.token).toBe('')
    expect(store.user).toBeNull()
    expect(localStorage.getItem(TOKEN_KEY)).toBeNull()
    expect(localStorage.getItem(USER_KEY)).toBeNull()
  })
})
