/**
 * 奖惩管理模块 —— 对应接口手册第 12 章（奖励 / 处分为两组独立接口）
 * 接口清单：
 *   GET  /rewards         分页查询奖励记录（权限：本人 / OFFICER+）
 *   POST /rewards         新增奖励记录（权限：干事、会长/副会长、指导老师）
 *   GET  /punishments     分页查询处分记录（权限：本人 / OFFICER+）
 *   POST /punishments     新增处分记录（权限：干事、会长/副会长、指导老师）
 */
import { get, post } from './request'
import { USE_MOCK } from './mode'
import { mockDelay, mockMembers, mockPunishments, mockRewards } from '@/mock'
import type { PunishmentPayload, PunishmentRecord, RewardPayload, RewardRecord } from '@/types'

/** GET /rewards —— 奖励记录列表 */
export async function getRewards(): Promise<RewardRecord[]> {
  // TODO(backend): 联调时删除 mock 分支，改走真实接口
  if (USE_MOCK) {
    await mockDelay(300)
    return [...mockRewards]
  }
  return get<RewardRecord[]>('/rewards')
}

/** POST /rewards —— 新增奖励记录，请求体见接口手册 12.1 */
export async function createReward(payload: RewardPayload): Promise<RewardRecord> {
  // TODO(backend): 联调时删除 mock 分支
  if (USE_MOCK) {
    await mockDelay(300)
    const member = mockMembers.find((item) => item.memberId === payload.memberId)
    if (!member) throw new Error('会员不存在')
    const record: RewardRecord = {
      ...payload,
      rewardId: Math.max(...mockRewards.map((item) => item.rewardId), 0) + 1,
      memberName: member.name,
      studentId: member.studentId,
      createTime: new Date().toISOString(),
    }
    mockRewards.unshift(record)
    return record
  }
  return post<RewardRecord>('/rewards', payload)
}

/** GET /punishments —— 处分记录列表 */
export async function getPunishments(): Promise<PunishmentRecord[]> {
  // TODO(backend): 联调时删除 mock 分支，改走真实接口
  if (USE_MOCK) {
    await mockDelay(300)
    return [...mockPunishments]
  }
  return get<PunishmentRecord[]>('/punishments')
}

/** POST /punishments —— 新增处分记录，请求体见接口手册 12.2 */
export async function createPunishment(payload: PunishmentPayload): Promise<PunishmentRecord> {
  // TODO(backend): 联调时删除 mock 分支
  if (USE_MOCK) {
    await mockDelay(300)
    const member = mockMembers.find((item) => item.memberId === payload.memberId)
    if (!member) throw new Error('会员不存在')
    const record: PunishmentRecord = {
      ...payload,
      punishmentId: Math.max(...mockPunishments.map((item) => item.punishmentId), 0) + 1,
      memberName: member.name,
      studentId: member.studentId,
      createTime: new Date().toISOString(),
    }
    mockPunishments.unshift(record)
    return record
  }
  return post<PunishmentRecord>('/punishments', payload)
}
