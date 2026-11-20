/**
 * 认证模块 —— 对应接口文档第 1 章
 * 接口清单：POST /auth/login、POST /auth/logout、GET /auth/profile
 */
import { get, post, TOKEN_KEY } from './request'
import { USE_MOCK } from './mode'
import { createMockLoginResult, mockDelay, resolveMockTokenUser } from '@/mock'
import type { LoginRequest, LoginResult, UserInfo } from '@/types'

/** POST /auth/login —— 用户登录（学号 + 密码），权限：公开 */
export async function login(payload: LoginRequest): Promise<LoginResult> {
  // TODO(backend): 联调时删除 mock 分支，直接使用下方真实请求
  if (USE_MOCK) {
    await mockDelay(400)
    // Mock 仅按学号映射固定身份，密码不校验（真实密码校验与 JWT 签发由后端负责）
    return createMockLoginResult(payload.studentId)
  }
  return post<LoginResult>('/auth/login', payload)
}

/** POST /auth/logout —— 用户登出，权限：已登录 */
export async function logout(): Promise<void> {
  // TODO(backend): 后端登出接口联调
  if (USE_MOCK) {
    await mockDelay(200)
    return
  }
  await post<void>('/auth/logout')
}

/** GET /auth/profile —— 获取当前登录用户信息，权限：已登录 */
export async function getProfile(): Promise<UserInfo> {
  // TODO(backend): 联调时删除 mock 分支
  if (USE_MOCK) {
    await mockDelay(300)
    // 按 token 反查登录身份，保证刷新页面后角色不丢失
    const user = resolveMockTokenUser(localStorage.getItem(TOKEN_KEY) ?? '')
    if (!user) throw new Error('登录状态已失效，请重新登录')
    return user
  }
  return get<UserInfo>('/auth/profile')
}
