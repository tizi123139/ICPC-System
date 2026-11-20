/**
 * 会员管理模块 —— 对应接口文档第 2 章
 * 接口清单：
 *   GET    /members                        分页查询会员（已登录）
 *   POST   /members                        新增会员（OFFICER+）
 *   GET    /members/{memberId}             会员详情（已登录）
 *   PUT    /members/{memberId}             更新会员（OFFICER+）
 *   DELETE /members/{memberId}             逻辑删除会员（PRESIDENT）
 *   GET    /members/{memberId}/archive      会员完整档案（本人 / OFFICER+）
 *   POST   /members/apply/join              提交入会申请（公开）
 *   POST   /members/apply/resign            提交退会申请（已登录）
 *   GET    /members/approvals               待审批申请列表（OFFICER+）
 *   POST   /members/approvals/{applyId}/audit  审批（OFFICER+，退会/除名需 PRESIDENT+）
 */
import { del, get, post, put } from './request'
import { USE_MOCK } from './mode'
import {
  getMockCurrentUser,
  mockDelay,
  mockMembers,
  mockApprovals,
  mockTrainings,
  queryMembers,
} from '@/mock'
import type {
  ApplyRecord,
  ApplyStatus,
  MemberArchive,
  MemberInfo,
  MemberQuery,
  PageResult,
} from '@/types'

/** 新增 / 更新会员的请求体（后端字段以后续 DTO 为准） */
export type MemberPayload = Omit<MemberInfo, 'memberId' | 'createTime'>

/* ===================== 后端传输层结构（与 MemberDtos 对齐） ===================== */

/** 后端 ApplyVO：申请人信息存放在 applyData 中 */
interface RawApplyVO {
  applyId: number
  type: string
  applicantId: number
  applicantName?: string | null
  applyData?: Record<string, unknown> | null
  status: ApplyStatus
  auditRemark?: string | null
  auditorId?: number | null
  auditorName?: string | null
  applyTime: string
  auditTime?: string | null
}

/** 后端 ArchiveVO：trainingRecords 为按训练聚合的 Map 列表 */
interface RawArchiveVO {
  basicInfo: MemberInfo
  trainingRecords?: Array<{
    trainingId: number
    trainingName: string
    solvedCount: number
    submitCount: number
    lastSubmitTime: string
  }>
  contestRecords?: Array<Record<string, unknown>>
  rewards?: Array<Record<string, unknown>>
  punishments?: Array<Record<string, unknown>>
}

/** 将后端申请单转换为视图层 ApplyRecord */
function toApplyRecord(raw: RawApplyVO): ApplyRecord {
  const data = raw.applyData ?? {}
  const text = (key: string) => (data[key] == null ? '' : String(data[key]))
  return {
    applyId: raw.applyId,
    memberId: raw.applicantId,
    memberName: raw.applicantName ?? text('name'),
    studentId: text('studentId'),
    // 后端 JOIN_MEMBER / RESIGN；视图仅区分入会 / 退会
    type: raw.type === 'JOIN_MEMBER' || raw.type === 'JOIN' ? 'JOIN' : 'RESIGN',
    reason: text('reason') || String(raw.auditRemark ?? ''),
    applyTime: raw.applyTime,
    status: raw.status,
  }
}

/** 将后端档案聚合转换为视图层 MemberArchive */
function toMemberArchive(raw: RawArchiveVO): MemberArchive {
  const records = raw.trainingRecords ?? []
  const submissionCount = records.reduce((sum, item) => sum + (item.submitCount ?? 0), 0)
  const solvedCount = records.reduce((sum, item) => sum + (item.solvedCount ?? 0), 0)
  const acRate = submissionCount === 0 ? 0 : Math.round((solvedCount / submissionCount) * 1000) / 10
  return {
    member: raw.basicInfo,
    trainingCount: records.length,
    submissionCount,
    acRate,
    recentTrainings: records.map((item) => ({
      trainingId: item.trainingId,
      name: item.trainingName,
      solvedCount: item.solvedCount ?? 0,
      submitTime: item.lastSubmitTime,
    })),
  }
}

/** GET /members —— 分页查询会员列表，权限：已登录 */
export async function getMembers(query: MemberQuery): Promise<PageResult<MemberInfo>> {
  // TODO(backend): 联调时删除 mock 分支，参数与接口文档 2.1 一致
  if (USE_MOCK) {
    await mockDelay()
    return queryMembers(query)
  }
  return get<PageResult<MemberInfo>>('/members', query as Record<string, unknown>)
}

/** GET /members/{memberId} —— 获取会员详情，权限：已登录 */
export async function getMember(memberId: number): Promise<MemberInfo> {
  // TODO(backend): 联调时删除 mock 分支
  if (USE_MOCK) {
    await mockDelay(300)
    const member = mockMembers.find((item) => item.memberId === memberId)
    if (!member) throw new Error('会员不存在')
    return member
  }
  return get<MemberInfo>(`/members/${memberId}`)
}

/** GET /members/{memberId}/archive —— 获取会员完整档案，权限：本人 / OFFICER+ */
export async function getMemberArchive(memberId: number): Promise<MemberArchive> {
  // TODO(backend): 联调时删除 mock 分支，档案聚合字段以后端 VO 为准
  if (USE_MOCK) {
    await mockDelay(300)
    const member = mockMembers.find((item) => item.memberId === memberId)
    if (!member) throw new Error('会员不存在')
    return {
      member,
      trainingCount: 18,
      submissionCount: 126,
      acRate: 68.4,
      recentTrainings: mockTrainings.slice(0, 3).map((training, index) => ({
        trainingId: training.trainingId,
        name: training.name,
        solvedCount: 5 - index,
        submitTime: training.endTime,
      })),
    }
  }
  const raw = await get<RawArchiveVO>(`/members/${memberId}/archive`)
  return toMemberArchive(raw)
}

/** POST /members —— 新增会员，权限：OFFICER / PRESIDENT */
export async function createMember(payload: MemberPayload): Promise<MemberInfo> {
  // TODO(backend): 联调时删除 mock 分支
  if (USE_MOCK) {
    await mockDelay(300)
    const member: MemberInfo = {
      ...payload,
      memberId: Math.max(...mockMembers.map((item) => item.memberId)) + 1,
      createTime: new Date().toISOString(),
    }
    mockMembers.unshift(member)
    return member
  }
  return post<MemberInfo>('/members', payload)
}

/** PUT /members/{memberId} —— 更新会员信息，权限：OFFICER+ */
export async function updateMember(
  memberId: number,
  payload: Partial<MemberPayload>,
): Promise<MemberInfo> {
  // TODO(backend): 联调时删除 mock 分支
  if (USE_MOCK) {
    await mockDelay(300)
    const target = mockMembers.find((item) => item.memberId === memberId)
    if (!target) throw new Error('会员不存在')
    Object.assign(target, payload)
    return target
  }
  return put<MemberInfo>(`/members/${memberId}`, payload)
}

/** DELETE /members/{memberId} —— 逻辑删除会员，权限：PRESIDENT */
export async function deleteMember(memberId: number): Promise<void> {
  // TODO(backend): 联调时删除 mock 分支
  if (USE_MOCK) {
    await mockDelay(300)
    const index = mockMembers.findIndex((item) => item.memberId === memberId)
    if (index >= 0) mockMembers.splice(index, 1)
    return
  }
  await del<void>(`/members/${memberId}`)
}

/** GET /members/approvals —— 查询待审批申请列表，权限：OFFICER+ */
export async function getApprovals(type?: 'JOIN' | 'RESIGN'): Promise<ApplyRecord[]> {
  // TODO(backend): 联调时删除 mock 分支
  if (USE_MOCK) {
    await mockDelay(300)
    return type ? mockApprovals.filter((item) => item.type === type) : [...mockApprovals]
  }
  // 后端枚举为 JOIN_MEMBER / RESIGN
  const backendType = type === 'JOIN' ? 'JOIN_MEMBER' : type
  const raw = await get<RawApplyVO[]>(
    '/members/approvals',
    backendType ? { type: backendType } : undefined,
  )
  return (raw ?? []).map(toApplyRecord)
}

/** POST /members/approvals/{applyId}/audit —— 审批申请，权限：OFFICER+ */
export async function auditApply(
  applyId: number,
  action: 'APPROVE' | 'REJECT',
  remark: string,
): Promise<void> {
  // TODO(backend): 联调时删除 mock 分支
  if (USE_MOCK) {
    await mockDelay(300)
    const target = mockApprovals.find((item) => item.applyId === applyId)
    if (target) target.status = action === 'APPROVE' ? 'APPROVED' : 'REJECTED'
    return
  }
  await post<void>(`/members/approvals/${applyId}/audit`, { action, remark })
}

/** POST /members/apply/join —— 提交入会申请，权限：公开（观察期前） */
export async function applyJoin(payload: { name: string; studentId: string; reason: string }) {
  // TODO(backend): 联调时删除 mock 分支
  if (USE_MOCK) {
    await mockDelay(300)
    mockApprovals.unshift({
      applyId: Date.now(),
      memberId: 0,
      memberName: payload.name,
      studentId: payload.studentId,
      type: 'JOIN',
      reason: payload.reason,
      applyTime: new Date().toISOString(),
      status: 'PENDING',
    })
    return
  }
  await post<void>('/members/apply/join', payload)
}

/** POST /members/apply/resign —— 提交退会申请，权限：已登录会员 */
export async function applyResign(payload: { reason: string }) {
  // TODO(backend): 联调时删除 mock 分支；退会审批通过后由后端更新会员状态
  if (USE_MOCK) {
    await mockDelay(300)
    const user = getMockCurrentUser()
    mockApprovals.unshift({
      applyId: Date.now(),
      memberId: user.userId,
      memberName: user.name,
      studentId: user.studentId,
      type: 'RESIGN',
      reason: payload.reason,
      applyTime: new Date().toISOString(),
      status: 'PENDING',
    })
    return
  }
  await post<void>('/members/apply/resign', payload)
}
