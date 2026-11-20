/**
 * 集训管理模块（寒假 / 暑假共用接口）—— 对应接口文档第 6 章
 * 接口清单：
 *   GET  /camps                                     查询集训列表（type=WINTER / SUMMER）（已登录）
 *   POST /camps                                     发布集训通知（ASSISTANT_COACH+）
 *   GET  /camps/{campId}                            集训详情（含任务列表）（已登录）
 *   POST /camps/{campId}/register                   提交集训报名（已登录会员）
 *   GET  /camps/{campId}/registrations              报名列表（ASSISTANT_COACH+）
 *   POST /camps/{campId}/registrations/{applyId}/audit  审核报名（ASSISTANT_COACH+）
 *   POST /camps/{campId}/tasks                      发布集训每日任务（ASSISTANT_COACH+）
 *   POST /camps/{campId}/checkin                    签到打卡（参训名单内成员）
 *   GET  /camps/{campId}/attendance                 考勤记录查询（本人 / ASSISTANT_COACH+）
 *   PUT  /camps/{campId}/attendance/{recordId}      补签 / 请假 / 标记缺勤（ASSISTANT_COACH+）
 */
import { get, post, put, toLocalDateTime } from './request'
import { USE_MOCK } from './mode'
import {
  getMockCurrentUser,
  mockAttendance,
  mockCampRegistrations,
  mockCamps,
  mockCampTasks,
  mockDelay,
  queryCamps,
} from '@/mock'
import type {
  AttendanceRecord,
  AttendanceStatus,
  CampDetail,
  CampInfo,
  CampPayload,
  CampQuery,
  CampRegistration,
  CampTask,
  CampTaskPayload,
  CampType,
  CheckinPayload,
  PageResult,
  UserInfo,
} from '@/types'

/* ===================== 后端传输层结构（与 CampDtos 对齐） ===================== */

interface RawUserBrief {
  memberId: number
  studentId?: string | null
  name: string
}

interface RawCampVO {
  campId: number
  name: string
  type: CampType
  startTime: string
  endTime: string
  location?: string | null
  registrationDeadline: string
  description?: string | null
  requirements?: string | null
  status?: string
  creator?: RawUserBrief | null
  createTime?: string
}

interface RawTaskVO {
  taskId: number
  title: string
  date: string
  content: string
  problemIds?: number[]
  problems?: Array<{ problemId: number; title: string }>
}

interface RawCampDetailVO {
  basicInfo: RawCampVO
  tasks?: RawTaskVO[]
}

interface RawRegistrationVO {
  applyId: number
  campId: number
  applicantId: number
  applicantName?: string | null
  studentId?: string | null
  applyData?: Record<string, unknown> | null
  status: 'PENDING' | 'APPROVED' | 'REJECTED'
  auditRemark?: string | null
  applyTime: string
}

interface RawAttendanceVO {
  recordId: number
  campId: number
  userId: number
  userName?: string | null
  studentId?: string | null
  checkInDate: string
  checkInTime?: string | null
  status: AttendanceStatus
  remark?: string | null
}

function toUserInfo(raw?: RawUserBrief | null): UserInfo {
  return {
    userId: raw?.memberId ?? 0,
    studentId: raw?.studentId ?? '',
    name: raw?.name ?? '',
    roles: [],
  }
}

/** 后端 CampVO → 视图层 CampInfo（startTime/endTime → startDate/endDate） */
function toCampInfo(raw: RawCampVO): CampInfo {
  return {
    campId: raw.campId,
    name: raw.name,
    type: raw.type,
    startDate: raw.startTime,
    endDate: raw.endTime,
    registrationDeadline: raw.registrationDeadline,
    location: raw.location ?? '',
    description: raw.description ?? '',
    creator: toUserInfo(raw.creator),
    createTime: raw.createTime ?? '',
  }
}

/** 后端 TaskVO → 视图层 CampTask */
function toCampTask(campId: number, raw: RawTaskVO): CampTask {
  return {
    taskId: raw.taskId,
    campId,
    date: raw.date,
    title: raw.title,
    content: raw.content,
  }
}

/** 后端 RegistrationVO → 视图层 CampRegistration */
function toCampRegistration(raw: RawRegistrationVO): CampRegistration {
  const data = raw.applyData ?? {}
  const text = (key: string) => (data[key] == null ? '' : String(data[key]))
  return {
    applyId: raw.applyId,
    campId: raw.campId,
    memberId: raw.applicantId,
    memberName: raw.applicantName ?? text('name'),
    studentId: raw.studentId ?? text('studentId'),
    grade: text('grade'),
    applyTime: raw.applyTime,
    status: raw.status,
    remark: text('reason') || raw.auditRemark || null,
  }
}

/** 后端 AttendanceVO → 视图层 AttendanceRecord */
function toAttendanceRecord(raw: RawAttendanceVO): AttendanceRecord {
  return {
    recordId: raw.recordId,
    campId: raw.campId,
    memberId: raw.userId,
    memberName: raw.userName ?? '',
    studentId: raw.studentId ?? '',
    date: raw.checkInDate,
    status: raw.status,
    checkinTime: raw.checkInTime ?? null,
    remark: raw.remark ?? null,
  }
}

/** GET /camps —— 查询集训列表，权限：已登录 */
export async function getCamps(query: CampQuery): Promise<PageResult<CampInfo>> {
  // TODO(backend): 联调时删除 mock 分支
  if (USE_MOCK) {
    await mockDelay()
    return queryCamps(query)
  }
  // 后端按 type 必填返回数组；「全部」时分别查询寒假 / 暑假再合并，由前端分页
  const types: CampType[] = query.type ? [query.type] : ['WINTER', 'SUMMER']
  const lists = await Promise.all(types.map((type) => get<RawCampVO[]>('/camps', { type })))
  let records = lists.flat().map(toCampInfo)
  const keyword = query.keyword?.trim()
  if (keyword) {
    records = records.filter((item) => item.name.includes(keyword))
  }
  records.sort((a, b) => b.startDate.localeCompare(a.startDate))
  const total = records.length
  const page = query.page ?? 1
  const size = query.size ?? 9
  return {
    total,
    page,
    size,
    records: records.slice((page - 1) * size, page * size),
  }
}

/** GET /camps/{campId} —— 集训详情（含任务列表），权限：已登录 */
export async function getCamp(campId: number): Promise<CampDetail> {
  // TODO(backend): 联调时删除 mock 分支
  if (USE_MOCK) {
    await mockDelay(300)
    const camp = mockCamps.find((item) => item.campId === campId)
    if (!camp) throw new Error('集训不存在')
    const tasks = mockCampTasks
      .filter((item) => item.campId === campId)
      .sort((a, b) => a.date.localeCompare(b.date))
    return { ...camp, tasks }
  }
  const raw = await get<RawCampDetailVO>(`/camps/${campId}`)
  const info = toCampInfo(raw.basicInfo)
  return {
    ...info,
    tasks: (raw.tasks ?? []).map((task) => toCampTask(campId, task)),
  }
}

/** POST /camps —— 发布集训通知，权限：ASSISTANT_COACH+ */
export async function createCamp(payload: CampPayload): Promise<CampInfo> {
  // TODO(backend): 联调时删除 mock 分支
  if (USE_MOCK) {
    await mockDelay(400)
    const camp: CampInfo = {
      ...payload,
      campId: Math.max(0, ...mockCamps.map((item) => item.campId)) + 1,
      creator: getMockCurrentUser(),
      createTime: new Date().toISOString(),
    }
    mockCamps.unshift(camp)
    return camp
  }
  const raw = await post<RawCampVO>('/camps', {
    name: payload.name,
    type: payload.type,
    startTime: toLocalDateTime(payload.startDate),
    endTime: toLocalDateTime(payload.endDate),
    registrationDeadline: toLocalDateTime(payload.registrationDeadline),
    location: payload.location,
    description: payload.description,
  })
  return toCampInfo(raw)
}

/** GET /camps/{campId}/registrations —— 报名列表，权限：ASSISTANT_COACH+ */
export async function getCampRegistrations(campId: number): Promise<CampRegistration[]> {
  // TODO(backend): 联调时删除 mock 分支
  if (USE_MOCK) {
    await mockDelay(300)
    return mockCampRegistrations.filter((item) => item.campId === campId)
  }
  const raw = await get<RawRegistrationVO[]>(`/camps/${campId}/registrations`)
  return (raw ?? []).map(toCampRegistration)
}

/**
 * 查询当前登录人在该集训的报名记录（用于判断是否可签到打卡）
 * TODO(backend): 后端如另有「我的报名」接口请替换此处；当前按报名列表中的本人记录推断。
 */
export async function getMyCampRegistration(campId: number): Promise<CampRegistration | null> {
  // TODO(backend): 联调时删除 mock 分支
  if (USE_MOCK) {
    await mockDelay(200)
    const user = getMockCurrentUser()
    return (
      mockCampRegistrations.find(
        (item) => item.campId === campId && item.memberId === user.userId,
      ) ?? null
    )
  }
  const raw = await get<RawRegistrationVO | null>(`/camps/${campId}/registrations/mine`)
  return raw ? toCampRegistration(raw) : null
}

/** POST /camps/{campId}/register —— 提交集训报名，权限：已登录会员 */
export async function registerCamp(
  campId: number,
  remark: string,
): Promise<void> {
  // TODO(backend): 联调时删除 mock 分支
  if (USE_MOCK) {
    await mockDelay(300)
    const user = getMockCurrentUser()
    const existing = mockCampRegistrations.find(
      (item) => item.campId === campId && item.memberId === user.userId,
    )
    if (existing) throw new Error('已提交过该集训的报名申请')
    mockCampRegistrations.push({
      applyId: Date.now(),
      campId,
      memberId: user.userId,
      memberName: user.name,
      studentId: user.studentId,
      grade: '',
      applyTime: new Date().toISOString(),
      status: 'PENDING',
      remark: remark || null,
    })
    return
  }
  // 后端报名请求体为 { reason, availableDates }
  await post<void>(`/camps/${campId}/register`, { reason: remark })
}

/** POST /camps/{campId}/registrations/{applyId}/audit —— 审核报名，权限：ASSISTANT_COACH+ */
export async function auditCampRegistration(
  campId: number,
  applyId: number,
  action: 'APPROVE' | 'REJECT',
  remark: string,
): Promise<void> {
  // TODO(backend): 联调时删除 mock 分支
  if (USE_MOCK) {
    await mockDelay(300)
    const target = mockCampRegistrations.find(
      (item) => item.campId === campId && item.applyId === applyId,
    )
    if (!target) throw new Error('报名记录不存在')
    target.status = action === 'APPROVE' ? 'APPROVED' : 'REJECTED'
    target.remark = remark || target.remark
    return
  }
  await post<void>(`/camps/${campId}/registrations/${applyId}/audit`, { action, remark })
}

/** POST /camps/{campId}/tasks —— 发布集训每日任务，权限：ASSISTANT_COACH+ */
export async function createCampTask(
  campId: number,
  payload: CampTaskPayload,
): Promise<CampTask> {
  // TODO(backend): 联调时删除 mock 分支
  if (USE_MOCK) {
    await mockDelay(300)
    const task: CampTask = {
      ...payload,
      taskId: Math.max(0, ...mockCampTasks.map((item) => item.taskId)) + 1,
      campId,
      publisher: getMockCurrentUser(),
      publishTime: new Date().toISOString(),
    }
    mockCampTasks.push(task)
    return task
  }
  const raw = await post<RawTaskVO>(`/camps/${campId}/tasks`, payload)
  return toCampTask(campId, raw)
}

/** POST /camps/{campId}/checkin —— 签到打卡，权限：参训名单内成员 */
export async function checkinCamp(campId: number, payload: CheckinPayload): Promise<void> {
  // TODO(backend): 联调时删除 mock 分支
  if (USE_MOCK) {
    await mockDelay(300)
    const user = getMockCurrentUser()
    const record = mockAttendance.find(
      (item) =>
        item.campId === campId && item.memberId === user.userId && item.date === payload.date,
    )
    if (record) {
      record.status = 'PRESENT'
      record.checkinTime = new Date().toISOString()
      record.remark = payload.remark || null
      return
    }
    mockAttendance.push({
      recordId: Math.max(0, ...mockAttendance.map((item) => item.recordId)) + 1,
      campId,
      memberId: user.userId,
      memberName: user.name,
      studentId: user.studentId,
      date: payload.date,
      status: 'PRESENT',
      checkinTime: new Date().toISOString(),
      remark: payload.remark || null,
    })
    return
  }
  await post<void>(`/camps/${campId}/checkin`, payload)
}

/**
 * GET /camps/{campId}/attendance —— 考勤记录查询
 * 权限：本人 / ASSISTANT_COACH+；all=true 时返回全体记录（仅 ASSISTANT_COACH+ 可用）
 */
export async function getCampAttendance(
  campId: number,
  all = false,
): Promise<AttendanceRecord[]> {
  // TODO(backend): 联调时删除 mock 分支；后端按登录人权限决定返回范围
  if (USE_MOCK) {
    await mockDelay(300)
    const records = mockAttendance.filter((item) => item.campId === campId)
    if (all) return records
    const user = getMockCurrentUser()
    return records.filter((item) => item.memberId === user.userId)
  }
  // 后端按登录人权限决定返回范围，无需 all 参数
  const raw = await get<RawAttendanceVO[]>(`/camps/${campId}/attendance`)
  return (raw ?? []).map(toAttendanceRecord)
}

/** PUT /camps/{campId}/attendance/{recordId} —— 补签 / 请假 / 标记缺勤，权限：ASSISTANT_COACH+ */
export async function updateCampAttendance(
  campId: number,
  recordId: number,
  action: AttendanceStatus,
  remark: string,
): Promise<void> {
  // TODO(backend): 联调时删除 mock 分支
  if (USE_MOCK) {
    await mockDelay(300)
    const target = mockAttendance.find(
      (item) => item.campId === campId && item.recordId === recordId,
    )
    if (!target) throw new Error('考勤记录不存在')
    target.status = action
    target.remark = remark || null
    if (action === 'MAKEUP') target.checkinTime = new Date().toISOString()
    return
  }
  // 后端考勤更新请求体为 { status, remark }
  await put<void>(`/camps/${campId}/attendance/${recordId}`, { status: action, remark })
}
