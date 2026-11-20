/**
 * 赛事管理模块（校赛 / XCPC / 非 XCPC 共用基础）—— 对应接口文档第 7 章
 * 接口清单：
 *   GET  /contests                                            查询赛事列表（支持 category 筛选）（已登录）
 *   POST /contests                                            创建 / 发布赛事（ASSISTANT_COACH+）
 *   GET  /contests/{contestId}                                赛事详情（已登录）
 *   POST /contests/{contestId}/register                       队伍 / 个人报名（符合条件的会员）
 *   GET  /contests/{contestId}/registrations                  报名列表（赛事组织者 / ASSISTANT_COACH+）
 *   POST /contests/{contestId}/registrations/{applyId}/audit   审核报名（ASSISTANT_COACH+）
 *   GET  /contests/{contestId}/leaderboard                    实时榜单（自动封榜）（已登录）
 *   POST /contests/{contestId}/submit                         比赛中提交代码（已报名队伍成员）
 *   PUT  /contests/{contestId}/results                        赛后登记获奖 / 成绩（ASSISTANT_COACH+）
 *   POST /contests/{contestId}/certificates                   批量生成获奖电子证书（PRESIDENT+）
 *   GET  /export/contest-certificates/{contestId}             下载赛事获奖证书 ZIP（本人 / PRESIDENT+）
 */
import http, { get, post, put, toLocalDateTime } from './request'
import { USE_MOCK } from './mode'
import {
  getMockCurrentUser,
  mockContestRegistrations,
  mockContestResults,
  mockContests,
  mockDelay,
  mockLeaderboard,
  mockTeams,
  queryContests,
} from '@/mock'
import type {
  CertificateBundle,
  ContestCertificate,
  ContestCategory,
  ContestInfo,
  ContestPayload,
  ContestQuery,
  ContestRegistration,
  ContestRegisterPayload,
  ContestResult,
  ContestResultPayload,
  ContestStatus,
  LeaderboardResult,
  PageResult,
  SubmitPayload,
  SubmitResult,
  UserInfo,
} from '@/types'

/* ===================== 后端传输层结构（与 ContestDtos 对齐） ===================== */

interface RawUserBrief {
  memberId: number
  studentId?: string | null
  name: string
}

interface RawContestVO {
  contestId: number
  name: string
  category: ContestCategory
  startTime: string
  endTime: string
  registrationDeadline: string
  description?: string | null
  requirements?: string | null
  maxTeamSize?: number
  freezeMinutes?: number
  status: string
  problemIds?: number[]
  creator?: RawUserBrief | null
  createTime?: string
}

interface RawRegistrationVO {
  applyId: number
  contestId: number
  teamId?: number | null
  teamName?: string | null
  applicantId: number
  applicantName?: string | null
  studentId?: string | null
  applyData?: Record<string, unknown> | null
  status: 'PENDING' | 'APPROVED' | 'REJECTED'
  auditRemark?: string | null
  applyTime: string
}

interface RawCertificateVO {
  generatedCount?: number
  downloadUrl?: string | null
}

function toUserInfo(raw?: RawUserBrief | null): UserInfo {
  return {
    userId: raw?.memberId ?? 0,
    studentId: raw?.studentId ?? '',
    name: raw?.name ?? '',
    roles: [],
  }
}

/** 后端赛事状态 → 视图层状态机 */
function toContestStatus(raw: string): ContestStatus {
  if (raw === 'ONGOING' || raw === 'ENDED' || raw === 'DRAFT') {
    return raw as ContestStatus
  }
  // ANNOUNCED / REGISTERING 在视图中统一归为已发布
  return 'PUBLISHED'
}

function toContestInfo(raw: RawContestVO): ContestInfo {
  return {
    contestId: raw.contestId,
    name: raw.name,
    category: raw.category,
    startTime: raw.startTime,
    endTime: raw.endTime,
    registrationDeadline: raw.registrationDeadline,
    description: raw.description ?? '',
    maxTeamSize: raw.maxTeamSize ?? 3,
    problemIds: raw.problemIds ?? [],
    freezeMinutes: raw.freezeMinutes ?? 0,
    status: toContestStatus(raw.status),
    creator: toUserInfo(raw.creator),
    createTime: raw.createTime ?? '',
  }
}

function toContestRegistration(raw: RawRegistrationVO): ContestRegistration {
  const data = raw.applyData ?? {}
  const text = (key: string) => (data[key] == null ? '' : String(data[key]))
  return {
    applyId: raw.applyId,
    contestId: raw.contestId,
    applyType: raw.teamId ? 'TEAM' : 'SOLO',
    teamId: raw.teamId ?? null,
    teamName: raw.teamName ?? null,
    memberId: raw.applicantId,
    memberName: raw.applicantName ?? text('name'),
    studentId: raw.studentId ?? text('studentId'),
    grade: text('grade'),
    memberCount: data.memberCount == null ? (raw.teamId ? 0 : 1) : Number(data.memberCount),
    applyTime: raw.applyTime,
    status: raw.status,
    remark: text('remark') || raw.auditRemark || null,
  }
}

/** 触发浏览器下载 Blob 文件 */
function downloadBlob(blob: Blob, filename: string) {
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  link.style.display = 'none'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  window.setTimeout(() => URL.revokeObjectURL(url), 1000)
}

/** GET /contests —— 查询赛事列表，权限：已登录 */
export async function getContests(query: ContestQuery): Promise<PageResult<ContestInfo>> {
  // TODO(backend): 联调时删除 mock 分支
  if (USE_MOCK) {
    await mockDelay()
    return queryContests(query)
  }
  const raw = await get<PageResult<RawContestVO>>('/contests', query as Record<string, unknown>)
  return { ...raw, records: (raw.records ?? []).map(toContestInfo) }
}

/** GET /contests/{contestId} —— 赛事详情，权限：已登录 */
export async function getContest(contestId: number): Promise<ContestInfo> {
  // TODO(backend): 联调时删除 mock 分支
  if (USE_MOCK) {
    await mockDelay(300)
    const contest = mockContests.find((item) => item.contestId === contestId)
    if (!contest) throw new Error('赛事不存在')
    return contest
  }
  const raw = await get<RawContestVO>(`/contests/${contestId}`)
  return toContestInfo(raw)
}

/** POST /contests —— 创建 / 发布赛事，权限：ASSISTANT_COACH+ */
export async function createContest(payload: ContestPayload): Promise<ContestInfo> {
  // TODO(backend): 联调时删除 mock 分支
  if (USE_MOCK) {
    await mockDelay(400)
    const contest: ContestInfo = {
      ...payload,
      contestId: Math.max(0, ...mockContests.map((item) => item.contestId)) + 1,
      status: 'PUBLISHED',
      creator: getMockCurrentUser(),
      createTime: new Date().toISOString(),
    }
    mockContests.unshift(contest)
    return contest
  }
  const raw = await post<RawContestVO>('/contests', {
    ...payload,
    startTime: toLocalDateTime(payload.startTime),
    endTime: toLocalDateTime(payload.endTime),
    registrationDeadline: toLocalDateTime(payload.registrationDeadline),
  })
  return toContestInfo(raw)
}

/** POST /contests/{contestId}/register —— 队伍 / 个人报名，权限：符合条件的会员 */
export async function registerContest(
  contestId: number,
  payload: ContestRegisterPayload,
): Promise<void> {
  // TODO(backend): 联调时删除 mock 分支
  if (USE_MOCK) {
    await mockDelay(300)
    const user = getMockCurrentUser()
    const existing = mockContestRegistrations.find(
      (item) => item.contestId === contestId && item.memberId === user.userId,
    )
    if (existing) throw new Error('已提交过该赛事的报名申请')
    const team =
      payload.applyType === 'TEAM' && payload.teamId
        ? mockTeams.find((item) => item.teamId === payload.teamId)
        : undefined
    mockContestRegistrations.push({
      applyId: Date.now(),
      contestId,
      applyType: payload.applyType,
      teamId: team?.teamId ?? null,
      teamName: team?.name ?? null,
      memberId: user.userId,
      memberName: user.name,
      studentId: user.studentId,
      grade: '',
      memberCount: team ? team.members.length : 1,
      applyTime: new Date().toISOString(),
      status: 'PENDING',
      remark: payload.remark || null,
    })
    return
  }
  // 后端按 teamId 是否为空区分队伍 / 个人报名，请求体不含 applyType
  await post<void>(`/contests/${contestId}/register`, {
    teamId: payload.applyType === 'TEAM' ? payload.teamId : null,
    remark: payload.remark,
  })
}

/**
 * 查询当前登录人在该赛事的报名记录（用于判断是否可提交代码）
 * TODO(backend): 后端如另有「我的报名」接口请替换此处；当前按报名列表中的本人记录推断。
 */
export async function getMyContestRegistration(
  contestId: number,
): Promise<ContestRegistration | null> {
  // TODO(backend): 联调时删除 mock 分支
  if (USE_MOCK) {
    await mockDelay(200)
    const user = getMockCurrentUser()
    return (
      mockContestRegistrations.find(
        (item) => item.contestId === contestId && item.memberId === user.userId,
      ) ?? null
    )
  }
  const raw = await get<RawRegistrationVO | null>(`/contests/${contestId}/registrations/mine`)
  return raw ? toContestRegistration(raw) : null
}

/** GET /contests/{contestId}/registrations —— 报名列表，权限：赛事组织者 / ASSISTANT_COACH+ */
export async function getContestRegistrations(
  contestId: number,
): Promise<ContestRegistration[]> {
  // TODO(backend): 联调时删除 mock 分支
  if (USE_MOCK) {
    await mockDelay(300)
    return mockContestRegistrations.filter((item) => item.contestId === contestId)
  }
  const raw = await get<RawRegistrationVO[]>(`/contests/${contestId}/registrations`)
  return (raw ?? []).map(toContestRegistration)
}

/** POST /contests/{contestId}/registrations/{applyId}/audit —— 审核报名，权限：ASSISTANT_COACH+ */
export async function auditContestRegistration(
  contestId: number,
  applyId: number,
  action: 'APPROVE' | 'REJECT',
  remark: string,
): Promise<void> {
  // TODO(backend): 联调时删除 mock 分支
  if (USE_MOCK) {
    await mockDelay(300)
    const target = mockContestRegistrations.find(
      (item) => item.contestId === contestId && item.applyId === applyId,
    )
    if (!target) throw new Error('报名记录不存在')
    target.status = action === 'APPROVE' ? 'APPROVED' : 'REJECTED'
    target.remark = remark || target.remark
    return
  }
  await post<void>(`/contests/${contestId}/registrations/${applyId}/audit`, { action, remark })
}

/** GET /contests/{contestId}/leaderboard —— 实时榜单（自动封榜），权限：已登录 */
export async function getContestLeaderboard(contestId: number): Promise<LeaderboardResult> {
  // TODO(backend): 联调时删除 mock 分支
  if (USE_MOCK) {
    await mockDelay(300)
    const contest = mockContests.find((item) => item.contestId === contestId)
    const frozen = Boolean(contest && contest.freezeMinutes > 0 && contest.status === 'ONGOING')
    return { ...mockLeaderboard, frozen }
  }
  return get<LeaderboardResult>(`/contests/${contestId}/leaderboard`)
}

/** POST /contests/{contestId}/submit —— 比赛中提交代码，权限：已报名队伍成员 */
export async function submitContestCode(
  contestId: number,
  payload: SubmitPayload,
): Promise<SubmitResult> {
  // TODO(backend): 联调时删除 mock 分支；判题由 OJ 异步完成，可轮询榜单 / 提交记录获取结果
  if (USE_MOCK) {
    await mockDelay(500)
    return { submitId: Date.now(), result: 'JUDGING' }
  }
  return post<SubmitResult>(`/contests/${contestId}/submit`, payload)
}

/**
 * 查询赛事成绩 / 获奖记录（用于成绩登记 Tab 回显）
 * TODO(backend): 接口手册仅列出 PUT /contests/{contestId}/results，如后端未提供 GET，
 * 可改为由赛事详情响应携带 results 字段。
 */
export async function getContestResults(contestId: number): Promise<ContestResult[]> {
  // TODO(backend): 联调时删除 mock 分支
  if (USE_MOCK) {
    await mockDelay(300)
    return mockContestResults.filter((item) => item.contestId === contestId)
  }
  // 后端未提供 GET /contests/{contestId}/results，登记后回显由页面内数据承接
  return []
}

/** PUT /contests/{contestId}/results —— 赛后登记获奖 / 成绩，权限：ASSISTANT_COACH+ */
export async function updateContestResults(
  contestId: number,
  payload: ContestResultPayload,
): Promise<ContestResult> {
  // TODO(backend): 联调时删除 mock 分支
  if (USE_MOCK) {
    await mockDelay(300)
    const existing = mockContestResults.find(
      (item) => item.contestId === contestId && item.memberId === payload.memberId,
    )
    const member = mockContestRegistrations.find(
      (item) => item.contestId === contestId && item.memberId === payload.memberId,
    )
    if (existing) {
      Object.assign(existing, payload)
      return existing
    }
    const created: ContestResult = {
      resultId: Math.max(0, ...mockContestResults.map((item) => item.resultId)) + 1,
      contestId,
      memberId: payload.memberId,
      memberName: member?.memberName ?? '',
      studentId: member?.studentId ?? '',
      teamName: member?.teamName ?? null,
      rank: payload.rank,
      award: payload.award,
      score: payload.score,
      remark: payload.remark,
    }
    mockContestResults.push(created)
    return created
  }
  // 后端成绩以队伍为维度，需通过报名记录把 memberId 映射为 teamId
  const registrations = await get<RawRegistrationVO[]>(`/contests/${contestId}/registrations`)
  const registration = (registrations ?? []).find((item) => item.applicantId === payload.memberId)
  await put<void>(`/contests/${contestId}/results`, {
    results: [
      {
        teamId: registration?.teamId ?? null,
        rank: payload.rank,
        award: payload.award,
        score: payload.score,
      },
    ],
  })
  const mapped = registration ? toContestRegistration(registration) : null
  return {
    resultId: Date.now(),
    contestId,
    memberId: payload.memberId,
    memberName: mapped?.memberName ?? '',
    studentId: mapped?.studentId ?? '',
    teamName: mapped?.teamName ?? null,
    rank: payload.rank,
    award: payload.award,
    score: payload.score,
    remark: payload.remark,
  }
}

/** 依据成绩记录生成证书清单（Mock 用；真实环境由后端生成） */
function buildCertificates(contestId: number): ContestCertificate[] {
  return mockContestResults
    .filter((item) => item.contestId === contestId && item.award)
    .map((item, index) => ({
      certId: item.resultId,
      memberId: item.memberId,
      memberName: item.memberName,
      teamName: item.teamName,
      award: item.award as string,
      certNo: `CERT-${contestId}-${String(index + 1).padStart(3, '0')}`,
    }))
}

/** POST /contests/{contestId}/certificates —— 批量生成获奖电子证书，权限：PRESIDENT+ */
export async function generateCertificates(contestId: number): Promise<CertificateBundle> {
  // TODO(backend): 联调时删除 mock 分支
  if (USE_MOCK) {
    await mockDelay(600)
    const certificates = buildCertificates(contestId)
    return {
      contestId,
      total: certificates.length,
      generatedTime: new Date().toISOString(),
      certificates,
    }
  }
  // 后端请求体为 { templateId, awardRules }，返回 { generatedCount, downloadUrl }
  const raw = await post<RawCertificateVO>(`/contests/${contestId}/certificates`, {})
  return {
    contestId,
    total: raw.generatedCount ?? 0,
    generatedTime: new Date().toISOString(),
    certificates: [],
  }
}

/**
 * GET /export/contest-certificates/{contestId} —— 下载赛事获奖证书 ZIP
 * 权限：本人 / PRESIDENT+
 * TODO(backend): 后端返回文件流（Content-Disposition 指定文件名）。
 * Mock 分支导出证书清单 CSV（带 BOM，Excel 打开中文不乱码）。
 */
export async function downloadContestCertificates(contestId: number): Promise<void> {
  if (USE_MOCK) {
    await mockDelay(400)
    const certificates = buildCertificates(contestId)
    const contest = mockContests.find((item) => item.contestId === contestId)
    const header = ['证书编号', '获奖人', '学号', '队伍', '奖项']
    const rows = certificates.map((item) => [
      item.certNo,
      item.memberName,
      mockContestResults.find((record) => record.memberId === item.memberId)?.studentId ?? '',
      item.teamName ?? '—',
      item.award,
    ])
    const csv = [header, ...rows]
      .map((row) => row.map((cell) => `"${String(cell).replace(/"/g, '""')}"`).join(','))
      .join('\r\n')
    const blob = new Blob([`\uFEFF${csv}`], { type: 'text/csv;charset=utf-8' })
    downloadBlob(blob, `赛事获奖证书-${contest?.name ?? contestId}.csv`)
    return
  }

  const response = await http.get<Blob>(`/export/contest-certificates/${contestId}`, {
    responseType: 'blob',
  })
  downloadBlob(response.data, `赛事获奖证书-${contestId}.zip`)
}
