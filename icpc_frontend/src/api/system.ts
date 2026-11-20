/**
 * 系统管理模块 —— 对应接口手册第 14 章
 * 接口清单（权限均为 PRESIDENT / ADVISOR）：
 *   GET    /system/logs                        分页查询操作日志
 *   GET    /system/logs/{logId}                操作日志详情
 *   GET    /system/roles                       查询角色字典
 *   PUT    /system/members/{memberId}/roles    调整会员角色
 *   GET    /system/configs                     查询系统配置
 *   PUT    /system/configs                     批量更新系统配置
 *   GET    /system/files                       分页查询文件记录
 *   DELETE /system/files/{fileId}              删除文件记录
 */
import { del, get, put } from './request'
import { USE_MOCK } from './mode'
import { mockAccounts, mockDelay, mockMembers, mockSystemConfigs, mockSystemFiles, mockSystemLogs } from '@/mock'
import { roleLabels } from '@/utils/dict'
import type {
  RoleCode,
  RoleOption,
  SystemConfigsPayload,
  SystemConfig,
  SystemFile,
  SystemLog,
  SystemRolesPayload,
} from '@/types'

/** GET /system/logs —— 操作日志列表 */
export async function getSystemLogs(): Promise<SystemLog[]> {
  // TODO(backend): 联调时删除 mock 分支，改走真实接口
  if (USE_MOCK) {
    await mockDelay(300)
    return [...mockSystemLogs]
  }
  return get<SystemLog[]>('/system/logs')
}

/** GET /system/logs/{logId} —— 操作日志详情 */
export async function getSystemLogDetail(logId: number): Promise<SystemLog> {
  // TODO(backend): 联调时删除 mock 分支
  if (USE_MOCK) {
    await mockDelay(200)
    const log = mockSystemLogs.find((item) => item.logId === logId)
    if (!log) throw new Error('日志不存在')
    return log
  }
  return get<SystemLog>(`/system/logs/${logId}`)
}

/** GET /system/roles —— 查询角色字典 */
export async function getRoleOptions(): Promise<RoleOption[]> {
  // TODO(backend): 联调时删除 mock 分支，改走真实接口
  if (USE_MOCK) {
    await mockDelay(200)
    return (Object.keys(roleLabels) as RoleCode[]).map((roleCode) => ({
      roleCode,
      roleName: roleLabels[roleCode],
    }))
  }
  return get<RoleOption[]>('/system/roles')
}

/** GET /system/configs —— 查询系统配置 */
export async function getSystemConfigs(): Promise<SystemConfig[]> {
  // TODO(backend): 联调时删除 mock 分支，改走真实接口
  if (USE_MOCK) {
    await mockDelay(300)
    return mockSystemConfigs.map((item) => ({ ...item }))
  }
  return get<SystemConfig[]>('/system/configs')
}

/** PUT /system/configs —— 批量更新系统配置，请求体见接口手册 14 */
export async function updateSystemConfigs(payload: SystemConfigsPayload): Promise<void> {
  // TODO(backend): 联调时删除 mock 分支
  if (USE_MOCK) {
    await mockDelay(300)
    for (const item of payload.configs) {
      const target = mockSystemConfigs.find((entry) => entry.configKey === item.configKey)
      if (target) {
        target.configValue = item.configValue
        target.updateTime = new Date().toISOString()
      } else {
        mockSystemConfigs.push({
          configKey: item.configKey,
          configValue: item.configValue,
          description: null,
          updateTime: new Date().toISOString(),
        })
      }
    }
    return
  }
  await put<void>('/system/configs', payload)
}

/** GET /system/files —— 文件记录列表 */
export async function getSystemFiles(): Promise<SystemFile[]> {
  // TODO(backend): 联调时删除 mock 分支，改走真实接口
  if (USE_MOCK) {
    await mockDelay(300)
    return [...mockSystemFiles]
  }
  return get<SystemFile[]>('/system/files')
}

/** DELETE /system/files/{fileId} —— 删除文件记录 */
export async function deleteSystemFile(fileId: number): Promise<void> {
  // TODO(backend): 联调时删除 mock 分支
  if (USE_MOCK) {
    await mockDelay(300)
    const index = mockSystemFiles.findIndex((item) => item.fileId === fileId)
    if (index >= 0) mockSystemFiles.splice(index, 1)
    return
  }
  await del<void>(`/system/files/${fileId}`)
}

/** PUT /system/members/{memberId}/roles —— 调整会员角色，请求体见接口手册 14 */
export async function updateMemberRoles(
  memberId: number,
  payload: SystemRolesPayload,
): Promise<void> {
  // TODO(backend): 联调时删除 mock 分支
  if (USE_MOCK) {
    await mockDelay(300)
    const member = mockMembers.find((item) => item.memberId === memberId)
    if (!member) throw new Error('会员不存在')
    const accountEntry = Object.entries(mockAccounts).find(([, user]) => user.userId === memberId)
    if (!accountEntry) throw new Error('该账号未注册演示身份，无法调整角色')
    const [, user] = accountEntry
    user.roles = [...payload.roles]
    return
  }
  await put<void>(`/system/members/${memberId}/roles`, payload)
}
