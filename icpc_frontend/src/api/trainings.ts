/**
 * 训练管理模块 —— 对应接口文档第 4 章
 * 接口清单：
 *   GET  /trainings                                 分页查询训练（已登录）
 *   POST /trainings                                 创建训练（ASSISTANT_COACH+）
 *   GET  /trainings/{trainingId}                    训练详情（已登录）
 *   PUT  /trainings/{trainingId}                    修改训练（仅 DRAFT）
 *   DELETE /trainings/{trainingId}                  删除训练
 *   POST /trainings/{trainingId}/publish            发布训练
 *   POST /trainings/{trainingId}/submit             提交代码
 *   GET  /trainings/{trainingId}/submissions        提交记录
 *   GET  /trainings/{trainingId}/leaderboard        实时榜单（自动识别封榜状态）
 *   GET  /trainings/ability/{userId}                个人能力雷达图数据
 */
import { del, get, post, put, getStoredUser, toLocalDateTime } from './request'
import { USE_MOCK } from './mode'
import {
  getMockCurrentUser,
  mockAbility,
  mockDelay,
  mockLeaderboard,
  mockSubmissions,
  mockTrainings,
  queryTrainings,
} from '@/mock'
import type {
  AbilityResult,
  JudgeResult,
  LeaderboardResult,
  PageResult,
  ProblemInfo,
  SubmitPayload,
  SubmitRecord,
  SubmitResult,
  TrainingInfo,
  TrainingPayload,
  TrainingQuery,
  TrainingStatus,
  TrainingType,
  UserInfo,
} from '@/types'

/* ===================== 后端传输层结构（与 TrainingDtos 对齐） ===================== */

interface RawUserBrief {
  memberId: number
  studentId?: string | null
  name: string
}

interface RawTrainingVO {
  trainingId: number
  name: string
  type: TrainingType
  description?: string | null
  startTime: string
  endTime: string
  leaderboardVisible?: boolean
  freezeTime?: number
  problemIds?: number[]
  status: TrainingStatus
  creator?: RawUserBrief | null
  createTime?: string
  updateTime?: string
}

interface RawTrainingDetailVO {
  basicInfo: RawTrainingVO
  problems?: ProblemInfo[]
}

interface RawSubmissionVO {
  submitId: number
  trainingId: number
  problemId: number
  problemTitle?: string | null
  submitterId: number
  submitterName?: string | null
  code?: string | null
  language: string
  result: JudgeResult
  timeUsed?: number | null
  memoryUsed?: number | null
  submitTime: string
}

interface RawAbilityVO {
  tagScores?: Array<{
    tagName: string
    score: number
    solvedCount?: number
    attemptCount?: number
  }>
  recentTrend?: Array<{ day: string; acCount: number; submitCount: number }>
  report?: string | null
  totalSolved?: number
}

/** 后端 UserBrief → 视图层 UserInfo */
function toUserInfo(raw?: RawUserBrief | null): UserInfo {
  return {
    userId: raw?.memberId ?? 0,
    studentId: raw?.studentId ?? '',
    name: raw?.name ?? '',
    roles: [],
  }
}

/** 后端 TrainingVO → 视图层 TrainingInfo */
function toTrainingInfo(raw: RawTrainingVO): TrainingInfo {
  return {
    trainingId: raw.trainingId,
    name: raw.name,
    type: raw.type,
    startTime: raw.startTime,
    endTime: raw.endTime,
    leaderboardVisible: raw.leaderboardVisible ?? true,
    freezeTime: raw.freezeTime ?? 0,
    problemIds: raw.problemIds ?? [],
    status: raw.status,
    creator: toUserInfo(raw.creator),
    createTime: raw.createTime ?? '',
  }
}

/** 后端 SubmissionVO → 视图层 SubmitRecord */
function toSubmitRecord(raw: RawSubmissionVO): SubmitRecord {
  return {
    submitId: raw.submitId,
    trainingId: raw.trainingId,
    problemId: raw.problemId,
    submitter: {
      userId: raw.submitterId,
      studentId: '',
      name: raw.submitterName ?? '',
      roles: [],
    },
    language: raw.language,
    result: raw.result,
    timeUsed: raw.timeUsed ?? 0,
    memoryUsed: raw.memoryUsed ?? 0,
    submitTime: raw.submitTime,
  }
}

/** GET /trainings —— 分页查询训练列表，权限：已登录 */
export async function getTrainings(query: TrainingQuery): Promise<PageResult<TrainingInfo>> {
  // TODO(backend): 联调时删除 mock 分支
  if (USE_MOCK) {
    await mockDelay()
    return queryTrainings(query)
  }
  const raw = await get<PageResult<RawTrainingVO>>('/trainings', query as Record<string, unknown>)
  return { ...raw, records: (raw.records ?? []).map(toTrainingInfo) }
}

/** GET /trainings/{trainingId} —— 获取训练详情（含题目列表），权限：已登录 */
export async function getTraining(trainingId: number): Promise<TrainingInfo> {
  // TODO(backend): 联调时删除 mock 分支
  if (USE_MOCK) {
    await mockDelay(300)
    const training = mockTrainings.find((item) => item.trainingId === trainingId)
    if (!training) throw new Error('训练不存在')
    return training
  }
  // 后端返回 { basicInfo, problems }，视图列表页只需要 basicInfo（题目由题库列表关联）
  const raw = await get<RawTrainingDetailVO>(`/trainings/${trainingId}`)
  return toTrainingInfo(raw.basicInfo)
}

/** POST /trainings —— 创建训练（单人 / 组队），权限：ASSISTANT_COACH+ */
export async function createTraining(payload: TrainingPayload): Promise<TrainingInfo> {
  // TODO(backend): 联调时删除 mock 分支
  if (USE_MOCK) {
    await mockDelay(400)
    const training: TrainingInfo = {
      ...payload,
      trainingId: Math.max(...mockTrainings.map((item) => item.trainingId)) + 1,
      status: 'DRAFT',
      creator: getMockCurrentUser(),
      createTime: new Date().toISOString(),
    }
    mockTrainings.unshift(training)
    return training
  }
  const raw = await post<RawTrainingVO>('/trainings', {
    ...payload,
    startTime: toLocalDateTime(payload.startTime),
    endTime: toLocalDateTime(payload.endTime),
  })
  return toTrainingInfo(raw)
}

/** PUT /trainings/{trainingId} —— 修改训练（仅 DRAFT 状态），权限：训练创建者 */
export async function updateTraining(
  trainingId: number,
  payload: TrainingPayload,
): Promise<void> {
  // TODO(backend): 联调时删除 mock 分支
  if (USE_MOCK) {
    await mockDelay(400)
    const target = mockTrainings.find((item) => item.trainingId === trainingId)
    if (!target) throw new Error('训练不存在')
    Object.assign(target, payload)
    return
  }
  await put<void>(`/trainings/${trainingId}`, {
    ...payload,
    startTime: toLocalDateTime(payload.startTime),
    endTime: toLocalDateTime(payload.endTime),
  })
}

/** DELETE /trainings/{trainingId} —— 删除训练，权限：训练创建者 / PRESIDENT+ */
export async function deleteTraining(trainingId: number): Promise<void> {
  // TODO(backend): 联调时删除 mock 分支
  if (USE_MOCK) {
    await mockDelay(300)
    const index = mockTrainings.findIndex((item) => item.trainingId === trainingId)
    if (index >= 0) mockTrainings.splice(index, 1)
    return
  }
  await del<void>(`/trainings/${trainingId}`)
}

/** POST /trainings/{trainingId}/publish —— 发布训练，权限：训练创建者 */
export async function publishTraining(trainingId: number): Promise<void> {
  // TODO(backend): 联调时删除 mock 分支
  if (USE_MOCK) {
    await mockDelay(300)
    const target = mockTrainings.find((item) => item.trainingId === trainingId)
    if (target) target.status = 'PUBLISHED'
    return
  }
  await post<void>(`/trainings/${trainingId}/publish`)
}

/** POST /trainings/{trainingId}/submit —— 提交代码，权限：已登录会员（训练开放时段内） */
export async function submitCode(
  trainingId: number,
  payload: SubmitPayload,
): Promise<SubmitResult> {
  // TODO(backend): 联调时删除 mock 分支；判题由 OJ 异步完成，需轮询提交记录
  if (USE_MOCK) {
    await mockDelay(500)
    mockSubmissions.unshift({
      submitId: Date.now(),
      trainingId,
      problemId: payload.problemId,
      submitter: getMockCurrentUser(),
      language: payload.language,
      result: 'JUDGING',
      timeUsed: 0,
      memoryUsed: 0,
      submitTime: new Date().toISOString(),
    })
    return { submitId: Date.now(), result: 'JUDGING' }
  }
  const raw = await post<RawSubmissionVO>(`/trainings/${trainingId}/submit`, payload)
  return { submitId: raw.submitId, result: raw.result }
}

/** GET /trainings/{trainingId}/submissions —— 提交记录，权限：本人 / ASSISTANT_COACH+ 可查全体 */
export async function getSubmissions(trainingId: number): Promise<SubmitRecord[]> {
  // TODO(backend): 联调时删除 mock 分支，参数分页由后端支持后再补充
  if (USE_MOCK) {
    await mockDelay(300)
    const records = mockSubmissions.filter((item) => item.trainingId === trainingId)
    return records.map((item) => ({
      ...item,
      problemId: item.problemId,
    }))
  }
  const raw = await get<RawSubmissionVO[]>(`/trainings/${trainingId}/submissions`)
  return (raw ?? []).map(toSubmitRecord)
}

/** GET /trainings/{trainingId}/leaderboard —— 实时榜单（自动识别封榜状态） */
export async function getLeaderboard(trainingId: number): Promise<LeaderboardResult> {
  // TODO(backend): 联调时删除 mock 分支
  if (USE_MOCK) {
    await mockDelay(300)
    return { ...mockLeaderboard, frozen: trainingId === 2 }
  }
  return get<LeaderboardResult>(`/trainings/${trainingId}/leaderboard`)
}

/** GET /trainings/ability/{userId} —— 个人能力雷达图数据 + 文字报告 */
export async function getAbility(userId: number | 'me'): Promise<AbilityResult> {
  // TODO(backend): 联调时删除 mock 分支；userId 传 'me' 时后端按当前登录人处理
  if (USE_MOCK) {
    await mockDelay(300)
    return mockAbility
  }
  // 后端路径参数为数值 id，'me' 由前端解析为当前登录人
  const current = getStoredUser()
  const resolvedId = userId === 'me' ? (current?.userId ?? 0) : userId
  // 后端 AbilityVO：{ tagScores, recentTrend, report, totalSolved }
  const raw = await get<RawAbilityVO>(`/trainings/ability/${resolvedId}`)
  return {
    userId: resolvedId,
    userName: current && current.userId === resolvedId ? current.name : '',
    dimensions: (raw.tagScores ?? []).map((item) => ({
      name: item.tagName,
      value: Math.round(item.score ?? 0),
    })),
    report: raw.report ?? '',
  } satisfies AbilityResult
}
