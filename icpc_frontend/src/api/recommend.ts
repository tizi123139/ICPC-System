/**
 * 个性化训练推荐模块
 * 接口清单：
 *   GET /trainings/recommend/trainings/{userId}   基于能力评估推荐专项训练（本人 / ASSISTANT_COACH+）
 *   GET /trainings/recommend/problems/{userId}    基于薄弱知识点推荐习题（本人 / ASSISTANT_COACH+）
 */
import { get } from './request'
import { USE_MOCK } from './mode'
import {
  buildProblemRecommendations,
  buildTrainingRecommendations,
  mockAbility,
  mockDelay,
} from '@/mock'
import type {
  Difficulty,
  RecommendedProblem,
  RecommendedTraining,
  TrainingStatus,
  TrainingType,
  WeakTagProblemGroup,
} from '@/types'

/* ===================== 后端传输层结构（与 RecommendationDtos 对齐） ===================== */

interface RawRecommendedTrainingVO {
  trainingId: number
  name: string
  type: TrainingType
  startTime: string
  endTime: string
  status: TrainingStatus
}

interface RawRecommendedProblemVO {
  problemId: number
  title: string
  ojProblemId?: string | null
  difficulty: Difficulty
  /** 命中的薄弱知识点名称 */
  matchedTag?: string | null
}

/** GET /trainings/recommend/trainings/{userId} —— 基于能力评估推荐专项训练 */
export async function getRecommendedTrainings(
  userId: number | 'me',
): Promise<RecommendedTraining[]> {
  // TODO(backend): 联调时删除 mock 分支；userId 传 'me' 时后端按当前登录人处理
  if (USE_MOCK) {
    await mockDelay(300)
    return buildTrainingRecommendations(mockAbility)
  }
  // 后端：GET /recommendations/trainings?userId=&limit=
  const raw = await get<RawRecommendedTrainingVO[]>('/recommendations/trainings', {
    userId: userId === 'me' ? undefined : userId,
    limit: 5,
  })
  return (raw ?? []).map((item) => ({
    trainingId: item.trainingId,
    name: item.name,
    type: item.type,
    startTime: item.startTime,
    endTime: item.endTime,
    status: item.status,
    matchedDimensions: [],
    reason: '基于能力评估薄弱维度推荐',
  }))
}

/** GET /trainings/recommend/problems/{userId} —— 基于薄弱知识点推荐习题（按知识点分组） */
export async function getRecommendedProblems(
  userId: number | 'me',
): Promise<WeakTagProblemGroup[]> {
  // TODO(backend): 联调时删除 mock 分支；userId 传 'me' 时后端按当前登录人处理
  if (USE_MOCK) {
    await mockDelay(300)
    return buildProblemRecommendations(mockAbility)
  }
  // 后端：GET /recommendations/problems?userId=&limit=，返回扁平列表，前端按命中知识点分组
  const raw = await get<RawRecommendedProblemVO[]>('/recommendations/problems', {
    userId: userId === 'me' ? undefined : userId,
    limit: 10,
  })
  const groups = new Map<string, RecommendedProblem[]>()
  for (const item of raw ?? []) {
    const tagName = item.matchedTag || '薄弱知识点'
    const problems = groups.get(tagName) ?? []
    problems.push({
      problemId: item.problemId,
      title: item.title,
      difficulty: item.difficulty,
      tagNames: [tagName],
      reason: `薄弱知识点：${tagName}`,
    })
    groups.set(tagName, problems)
  }
  return Array.from(groups, ([tagName, problems]) => ({
    tagName,
    score: 0,
    problems,
  })) satisfies WeakTagProblemGroup[]
}
