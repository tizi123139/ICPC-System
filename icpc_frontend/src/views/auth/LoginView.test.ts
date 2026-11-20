import { fireEvent, render, screen, waitFor } from '@testing-library/vue'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { ElMessage } from 'element-plus'
import LoginView from './LoginView.vue'
import { login as apiLogin } from '@/api/auth'
import { createTestRouter, resetAuth, setupPinia } from '@/test/utils'
import type { LoginResult } from '@/types'

// 伪造认证接口，禁止测试期间发起真实请求
vi.mock('@/api/auth', () => ({
  login: vi.fn(),
  logout: vi.fn(),
  getProfile: vi.fn(),
}))

vi.mock('element-plus', async () => {
  const actual = await vi.importActual<typeof import('element-plus')>('element-plus')
  return {
    ...actual,
    ElMessage: {
      success: vi.fn(),
      error: vi.fn(),
    },
  }
})

const loginMock = vi.mocked(apiLogin)

const loginResult: LoginResult = {
  token: 'mock-token-1001',
  expiresIn: 7200,
  user: { userId: 1, studentId: '1001', name: '会长', roles: ['PRESIDENT'] },
}

/** 渲染登录页并注入内存路由 */
function renderLoginPage() {
  const router = createTestRouter()
  render(LoginView, {
    global: {
      plugins: [router],
    },
  })
  return router
}

/** 填写学号 / 密码并点击登录按钮 */
async function submitLogin(studentId: string, password: string) {
  await fireEvent.update(screen.getByPlaceholderText(/请输入学号/i), studentId)
  await fireEvent.update(screen.getByPlaceholderText(/请输入密码/i), password)
  await fireEvent.click(screen.getByRole('button', { name: /登\s*录/ }))
}

describe('LoginView', () => {
  beforeEach(() => {
    resetAuth()
    setupPinia()
    vi.clearAllMocks()
  })

  afterEach(() => {
    vi.restoreAllMocks()
    vi.clearAllMocks()
  })

  it('【清单 1】登录页正常渲染：标题、输入框与演示账号', () => {
    renderLoginPage()

    expect(screen.getByRole('heading', { name: /登录/i })).toBeInTheDocument()
    expect(screen.getByText(/使用学号与密码进入系统/i)).toBeInTheDocument()
    expect(screen.getByPlaceholderText(/请输入学号/i)).toBeInTheDocument()
    expect(screen.getByPlaceholderText(/请输入密码/i)).toBeInTheDocument()

    // 演示账号 chip：学号 + 身份（1001/1002 均为会长/副会长）
    expect(screen.getByRole('button', { name: /1001/ })).toBeInTheDocument()
    expect(screen.getAllByText('会长/副会长').length).toBeGreaterThan(0)
  })

  it('【清单 4】用户登录：填写学号与密码后携带参数调用登录接口并提示成功', async () => {
    loginMock.mockResolvedValue(loginResult)
    const successSpy = vi.spyOn(ElMessage, 'success')

    renderLoginPage()
    await submitLogin('1001', 'any-password')

    await waitFor(() => expect(loginMock).toHaveBeenCalledTimes(1))
    expect(loginMock).toHaveBeenCalledWith({ studentId: '1001', password: 'any-password' })
    await waitFor(() => expect(successSpy).toHaveBeenCalledWith('登录成功，欢迎回来'))
  })

  it('【清单 4】登录失败：登录接口抛错时展示错误提示', async () => {
    loginMock.mockRejectedValue(new Error('该学号未注册'))
    const errorSpy = vi.spyOn(ElMessage, 'error')

    renderLoginPage()
    await submitLogin('1001', 'any-password')

    await waitFor(() => expect(loginMock).toHaveBeenCalledTimes(1))
    await waitFor(() => expect(errorSpy).toHaveBeenCalledWith('该学号未注册'))
  })
})
