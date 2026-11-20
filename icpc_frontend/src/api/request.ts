/**
 * Axios 实例 + 统一响应处理
 * 后端基础路径：/api/v1（见 OpenAPI servers）
 *
 * 后端预留说明：
 * - 请求拦截器已预留 JWT 注入位置；
 * - 响应拦截器已按接口文档 0.2 的 { code, message, data } 结构解包；
 * - 错误码 400/401/403/404/409/422/500 已做统一提示。
 * TODO(backend): 联调时若后端错误码字段有调整，修改 handleCode 即可。
 */
import axios, { type AxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import type { ApiResponse, UserInfo } from '@/types'

export const TOKEN_KEY = 'icpc_token'
export const USER_KEY = 'icpc_user'

/** 读取登录时持久化的当前用户（供 api 适配层做本人相关判断） */
export function getStoredUser(): UserInfo | null {
  try {
    const raw = localStorage.getItem(USER_KEY)
    return raw ? (JSON.parse(raw) as UserInfo) : null
  } catch {
    return null
  }
}

/**
 * 后端 LocalDateTime 采用 ISO_LOCAL_DATE_TIME（不带时区）解析，
 * 前端日期选择器值可能带 +08:00 / Z，这里统一去掉时区后缀。
 */
export function toLocalDateTime(value?: string | null): string | undefined {
  if (!value) return undefined
  return value.replace(' ', 'T').replace(/([+-]\d{2}:\d{2}|Z)$/, '')
}

const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api/v1',
  timeout: 15000,
})

/** 请求拦截：附加 Authorization: Bearer <token> */
http.interceptors.request.use((config) => {
  // TODO(backend): 登录接口返回 token 后，写入 localStorage（见 stores/auth.ts）
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

/** 响应拦截：HTTP 层异常统一提示 */
http.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error?.response?.status
    if (status === 401) {
      localStorage.removeItem(TOKEN_KEY)
      ElMessage.error('登录已过期，请重新登录')
      // TODO(backend): 可在此跳转登录页，例如 window.location.href = '/login'
    } else if (status === 403) {
      ElMessage.error('无权执行此操作')
    } else {
      ElMessage.error(error?.response?.data?.message || '网络异常，请稍后重试')
    }
    return Promise.reject(error)
    },
)

/**
 * 统一请求封装：自动解包 ApiResponse.data
 * 使用示例：request<PageResult<MemberInfo>>({ url: '/members', params })
 */
export async function request<T>(config: AxiosRequestConfig): Promise<T> {
  const response = await http.request<ApiResponse<T>>(config)
  const body = response.data
  if (body.code !== 0) {
    ElMessage.error(body.message || '请求失败')
    throw new Error(body.message || '请求失败')
  }
  return body.data
}

export const get = <T>(url: string, params?: Record<string, unknown>) =>
  request<T>({ url, method: 'get', params })

export const post = <T>(url: string, data?: unknown) => request<T>({ url, method: 'post', data })

export const put = <T>(url: string, data?: unknown) => request<T>({ url, method: 'put', data })

export const del = <T>(url: string, params?: Record<string, unknown>) =>
  request<T>({ url, method: 'delete', params })

export default http
