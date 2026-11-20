/**
 * 文件上传模块 —— 对应接口文档第 11 章
 * POST /files/upload  通用文件上传（题解附件、证书、证明材料等），权限：已登录
 */
import http from './request'
import { USE_MOCK } from './mode'
import { mockDelay } from '@/mock'
import type { UploadResult } from '@/types'

/** POST /files/upload —— multipart/form-data 上传 */
export async function uploadFile(file: File): Promise<UploadResult> {
  // TODO(backend): 联调时删除 mock 分支；真实实现如下：
  // const formData = new FormData()
  // formData.append('file', file)
  // const { data } = await http.post<ApiResponse<UploadResult>>('/files/upload', formData, {
  //   headers: { 'Content-Type': 'multipart/form-data' },
  // })
  // return data.data
  if (USE_MOCK) {
    await mockDelay(400)
    return {
      url: URL.createObjectURL(file),
      filename: file.name,
      size: file.size,
    }
  }
  const formData = new FormData()
  formData.append('file', file)
  const response = await http.post('/files/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
  return response.data.data as UploadResult
}
