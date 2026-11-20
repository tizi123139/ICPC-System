/**
 * 考核管理模块 —— 对应接口手册第 13 章
 * 接口清单：
 *   GET    /assessments                                  分页查询考核（已登录，普通会员仅见已发布）
 *   POST   /assessments                                  创建考核（ASSISTANT_COACH+）
 *   POST   /assessments/{assessmentId}/publish           发布考核（ASSISTANT_COACH+）
 *   GET    /assessments/{assessmentId}/results           查询考核结果（本人 / ASSISTANT_COACH+）
 *   PUT    /assessments/{assessmentId}/results           批量录入 / 更新考核成绩（ASSISTANT_COACH+）
 *   POST   /assessments/{assessmentId}/results/{resultId}/audit  审核考核结果（PRESIDENT / ADVISOR）
 */
import { get, post, put } from './request'
import { USE_MOCK } from './mode'
import { mockAssessmentList, mockAssessmentResults, mockDelay, mockMembers } from '@/mock'
import type {
  AssessmentAuditPayload,
  AssessmentInfo,
  AssessmentPayload,
  AssessmentResultRecord,
  AssessmentResultsPayload,
} from '@/types'

/** GET /assessments —— 考核列表（普通会员仅见已发布） */
export async function getAssessments(): Promise<AssessmentInfo[]> {
  // TODO(backend): 联调时删除 mock 分支，改走真实接口
  if (USE_MOCK) {
    await mockDelay(300)
    return [...mockAssessmentList]
  }
  return get<AssessmentInfo[]>('/assessments')
}

/** POST /assessments —— 创建考核，请求体见接口手册 13 */
export async function createAssessment(payload: AssessmentPayload): Promise<AssessmentInfo> {
  // TODO(backend): 联调时删除 mock 分支
  if (USE_MOCK) {
    await mockDelay(300)
    const record: AssessmentInfo = {
      assessmentId: Math.max(...mockAssessmentList.map((item) => item.assessmentId), 0) + 1,
      ...payload,
      status: 'DRAFT',
      createTime: new Date().toISOString(),
    }
    mockAssessmentList.unshift(record)
    return record
  }
  return post<AssessmentInfo>('/assessments', payload)
}

/** POST /assessments/{assessmentId}/publish —— 发布考核 */
export async function publishAssessment(assessmentId: number): Promise<void> {
  // TODO(backend): 联调时删除 mock 分支
  if (USE_MOCK) {
    await mockDelay(300)
    const target = mockAssessmentList.find((item) => item.assessmentId === assessmentId)
    if (!target) throw new Error('考核不存在')
    target.status = 'PUBLISHED'
    return
  }
  await post<void>(`/assessments/${assessmentId}/publish`)
}

/** GET /assessments/{assessmentId}/results —— 查询考核结果 */
export async function getAssessmentResults(
  assessmentId: number,
): Promise<AssessmentResultRecord[]> {
  // TODO(backend): 联调时删除 mock 分支
  if (USE_MOCK) {
    await mockDelay(300)
    return mockAssessmentResults.filter((item) => item.assessmentId === assessmentId)
  }
  return get<AssessmentResultRecord[]>(`/assessments/${assessmentId}/results`)
}

/** PUT /assessments/{assessmentId}/results —— 批量录入 / 更新考核成绩 */
export async function updateAssessmentResults(
  assessmentId: number,
  payload: AssessmentResultsPayload,
): Promise<void> {
  // TODO(backend): 联调时删除 mock 分支
  if (USE_MOCK) {
    await mockDelay(300)
    for (const item of payload.results) {
      const member = mockMembers.find((entry) => entry.memberId === item.memberId)
      if (!member) throw new Error(`会员 ${item.memberId} 不存在`)
      const existing = mockAssessmentResults.find(
        (entry) => entry.assessmentId === assessmentId && entry.memberId === item.memberId,
      )
      if (existing) {
        existing.score = item.score
        existing.remark = item.remark ?? null
      } else {
        mockAssessmentResults.push({
          resultId: Math.max(...mockAssessmentResults.map((entry) => entry.resultId), 0) + 1,
          assessmentId,
          memberId: member.memberId,
          memberName: member.name,
          studentId: member.studentId,
          score: item.score,
          remark: item.remark ?? null,
          auditStatus: 'PENDING',
          auditRemark: null,
        })
      }
    }
    return
  }
  await put<void>(`/assessments/${assessmentId}/results`, payload)
}

/** POST /assessments/{assessmentId}/results/{resultId}/audit —— 审核考核结果（PRESIDENT / ADVISOR） */
export async function auditAssessmentResult(
  assessmentId: number,
  resultId: number,
  payload: AssessmentAuditPayload,
): Promise<void> {
  // TODO(backend): 联调时删除 mock 分支
  if (USE_MOCK) {
    await mockDelay(300)
    const target = mockAssessmentResults.find(
      (item) => item.assessmentId === assessmentId && item.resultId === resultId,
    )
    if (!target) throw new Error('考核成绩不存在')
    target.auditStatus = payload.status
    target.auditRemark = payload.auditRemark || null
    return
  }
  await post<void>(`/assessments/${assessmentId}/results/${resultId}/audit`, payload)
}
