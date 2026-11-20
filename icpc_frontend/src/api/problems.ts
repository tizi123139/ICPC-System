/**
 * 题库管理模块 —— 对应接口文档第 3.2 章
 * 接口清单：
 *   GET    /problems                          分页查询题目（已登录）
 *   POST   /problems                          新增题目（ASSISTANT_COACH+）
 *   GET    /problems/{problemId}              题目详情（集训私有题需 正式会员+）
 *   PUT    /problems/{problemId}              修改题目（ASSISTANT_COACH+）
 *   DELETE /problems/{problemId}              删除题目（PRESIDENT+）
 *   PUT    /problems/{problemId}/solution     更新题解（ASSISTANT_COACH+）
 *   POST   /problems/import                   从 OJ 批量导入题目（ASSISTANT_COACH+）
 *   POST   /problems/{problemId}/submit       题库内提交代码（预留，见函数注释）
 *   GET    /problems/{problemId}/submissions  题目提交记录（预留，见函数注释）
 */
import { del, get, post, put } from './request'
import { USE_MOCK } from './mode'
import {
  getMockCurrentUser,
  mockDelay,
  mockProblems,
  mockSubmissions,
  queryProblems,
  mockTagTree,
} from '@/mock'
import type {
  JudgeResult,
  PageResult,
  ProblemInfo,
  ProblemPayload,
  ProblemQuery,
  Solution,
  SubmitPayload,
  SubmitRecord,
  SubmitResult,
  UploadResult,
} from '@/types'

/** GET /problems —— 分页查询题目（支持知识点 / 难度 / 可见范围筛选） */
export async function getProblems(query: ProblemQuery): Promise<PageResult<ProblemInfo>> {
  // TODO(backend): 联调时删除 mock 分支
  if (USE_MOCK) {
    await mockDelay()
    return queryProblems(query)
  }
  return get<PageResult<ProblemInfo>>('/problems', query as Record<string, unknown>)
}

/** GET /problems/{problemId} —— 获取题目详情，权限：已登录（集训私有题需 正式会员+） */
export async function getProblem(problemId: number): Promise<ProblemInfo> {
  // TODO(backend): 联调时删除 mock 分支
  if (USE_MOCK) {
    await mockDelay(300)
    const problem = mockProblems.find((item) => item.problemId === problemId)
    if (!problem) throw new Error('题目不存在')
    return problem
  }
  return get<ProblemInfo>(`/problems/${problemId}`)
}

/** POST /problems —— 新增题目，权限：ASSISTANT_COACH+ */
export async function createProblem(payload: ProblemPayload): Promise<ProblemInfo> {
  // TODO(backend): 联调时删除 mock 分支
  if (USE_MOCK) {
    await mockDelay(400)
    const tagIds = payload.tagIds
    const allTags = mockTagTree.flatMap((node) => [node, ...(node.children ?? [])])
    const problem: ProblemInfo = {
      ...payload,
      problemId: Math.max(...mockProblems.map((item) => item.problemId)) + 1,
      tags: allTags
        .filter((tag) => tagIds.includes(tag.tagId))
        .map((tag) => ({ tagId: tag.tagId, name: tag.name, category: tag.category })),
      solution: null,
    }
    mockProblems.unshift(problem)
    return problem
  }
  return post<ProblemInfo>('/problems', payload)
}

/** PUT /problems/{problemId} —— 修改题目，权限：ASSISTANT_COACH+ */
export async function updateProblem(
  problemId: number,
  payload: ProblemPayload,
): Promise<void> {
  // TODO(backend): 联调时删除 mock 分支
  if (USE_MOCK) {
    await mockDelay(400)
    const target = mockProblems.find((item) => item.problemId === problemId)
    if (!target) throw new Error('题目不存在')
    Object.assign(target, payload)
    return
  }
  await put<void>(`/problems/${problemId}`, payload)
}

/** DELETE /problems/{problemId} —— 删除题目，权限：PRESIDENT+ */
export async function deleteProblem(problemId: number): Promise<void> {
  // TODO(backend): 联调时删除 mock 分支
  if (USE_MOCK) {
    await mockDelay(300)
    const index = mockProblems.findIndex((item) => item.problemId === problemId)
    if (index >= 0) mockProblems.splice(index, 1)
    return
  }
  await del<void>(`/problems/${problemId}`)
}

/** PUT /problems/{problemId}/solution —— 更新题解（Markdown + 参考代码 + 附件） */
export async function updateSolution(
  problemId: number,
  payload: {
    content: string
    referenceCode: string | null
    /** 题解附件（POST /files/upload 上传后得到的文件列表，可选） */
    attachments?: UploadResult[]
  },
): Promise<void> {
  // TODO(backend): 联调时删除 mock 分支
  if (USE_MOCK) {
    await mockDelay(400)
    const target = mockProblems.find((item) => item.problemId === problemId)
    if (!target) throw new Error('题目不存在')
    const solution: Solution = {
      solutionId: target.solution?.solutionId ?? Date.now(),
      content: payload.content,
      referenceCode: payload.referenceCode,
      attachments: payload.attachments?.length ? payload.attachments : target.solution?.attachments,
      updateTime: new Date().toISOString(),
    }
    target.solution = solution
    return
  }
  await put<void>(`/problems/${problemId}/solution`, payload)
}

/** POST /problems/import —— 从 OJ 批量导入题目，权限：ASSISTANT_COACH+ */
export async function importProblemsFromOj(ojProblemIds: string[]): Promise<{ imported: number }> {
  // TODO(backend): 联调时删除 mock 分支，后端需返回实际导入条数
  if (USE_MOCK) {
    await mockDelay(600)
    return { imported: ojProblemIds.length }
  }
  return post<{ imported: number }>('/problems/import', { ojProblemIds })
}

/**
 * POST /problems/{problemId}/submit —— 题库内提交代码，权限：已登录
 * TODO(backend): 接口手册目前只定义了训练维度提交（POST /trainings/{trainingId}/submit），
 * 独立题库提交端点需与后端确认；联调时若沿用训练维度，请改为携带 trainingId 调用。
 */
export async function submitProblemCode(
  problemId: number,
  payload: SubmitPayload,
): Promise<SubmitResult> {
  // TODO(backend): 联调时删除 mock 分支；判题应交给 OJ 异步完成
  if (USE_MOCK) {
    await mockDelay(400)
    const submitTime = new Date().toISOString()
    const record: SubmitRecord = {
      submitId: Date.now(),
      trainingId: 0, // Mock 中 0 表示题库内直接提交
      problemId,
      submitter: getMockCurrentUser(),
      language: payload.language,
      result: 'JUDGING',
      timeUsed: 0,
      memoryUsed: 0,
      submitTime,
    }
    mockSubmissions.unshift(record)
    // Mock 模拟异步判题：1.5s 后返回 Accepted（真实判题由 OJ 回调或轮询获取）
    window.setTimeout(() => {
      record.result = 'AC'
      record.timeUsed = 118
      record.memoryUsed = 20480
    }, 1500)
    return { submitId: record.submitId, result: 'JUDGING' as JudgeResult }
  }
  return post<SubmitResult>(`/problems/${problemId}/submit`, payload)
}

/**
 * GET /problems/{problemId}/submissions —— 题目提交记录，权限：已登录
 * TODO(backend): 同 submitProblemCode，端点需与后端确认
 */
export async function getProblemSubmissions(problemId: number): Promise<SubmitRecord[]> {
  if (USE_MOCK) {
    await mockDelay(250)
    return mockSubmissions
      .filter((item) => item.problemId === problemId)
      .sort((a, b) => b.submitTime.localeCompare(a.submitTime))
  }
  return get<SubmitRecord[]>(`/problems/${problemId}/submissions`)
}
