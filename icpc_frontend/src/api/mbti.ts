/**
 * MBTI 管理模块（低优先级）—— 对应接口文档第 8 章
 * 接口清单：
 *   GET  /mbti/questions        获取测评题目列表（已登录）
 *   POST /mbti/submit           提交答案，返回测评结果（已登录）
 *   GET  /mbti/results/{userId} 查看某会员测评结果（本人 / ASSISTANT_COACH+）
 */
import { get, post, getStoredUser } from './request'
import { USE_MOCK } from './mode'
import {
  computeMbtiResult,
  getMockCurrentUser,
  mockDelay,
  mockMbtiQuestions,
  mockMbtiResults,
} from '@/mock'
import { mbtiPoleLabelMap, mbtiTypeNameMap } from '@/utils/dict'
import type {
  MbtiAnswer,
  MbtiDimension,
  MbtiDimensionScore,
  MbtiOption,
  MbtiPole,
  MbtiQuestion,
  MbtiResult,
  MbtiType,
} from '@/types'

/* ===================== 后端传输层结构（与 MbtiDtos 对齐） ===================== */

interface RawOptionVO {
  option: 'A' | 'B'
  /** 单极字母，如 E / I */
  dimension: MbtiPole
  value?: number
}

interface RawQuestionVO {
  questionId: number
  content: string
  options: RawOptionVO[]
}

interface RawResultVO {
  resultId: number
  userId: number
  type: MbtiType
  /** key 为 E_I / S_N / T_F / J_P，value 为 A 侧占比 */
  scores?: Record<string, number>
  report?: string | null
  reportUrl?: string | null
  suggestions?: string[]
  testTime: string
}

/** 单极字母 → 维度组 */
function toDimension(pole: MbtiPole): MbtiDimension {
  if (pole === 'E' || pole === 'I') return 'EI'
  if (pole === 'S' || pole === 'N') return 'SN'
  if (pole === 'T' || pole === 'F') return 'TF'
  return 'JP'
}

/** 后端题目 → 视图层题目（补充选项文案与维度组） */
function toQuestion(raw: RawQuestionVO): MbtiQuestion {
  const options = (raw.options ?? []).map((option) => {
    const dimension = toDimension(option.dimension)
    const labels = mbtiPoleLabelMap[dimension]
    const label = option.dimension === dimension[0] ? labels[0] : labels[1]
    return {
      value: option.option,
      label,
      pole: option.dimension,
    } satisfies MbtiOption
  })
  return {
    questionId: raw.questionId,
    dimension: toDimension(raw.options?.[0]?.dimension ?? 'E'),
    content: raw.content,
    options: (options.length >= 2 ? options.slice(0, 2) : options) as [MbtiOption, MbtiOption],
  }
}

/** 后端结果 → 视图层结果（拆解 scores 为双向条） */
function toResult(raw: RawResultVO): MbtiResult {
  const keyMap: Record<string, MbtiDimension> = {
    E_I: 'EI',
    S_N: 'SN',
    T_F: 'TF',
    J_P: 'JP',
    EI: 'EI',
    SN: 'SN',
    TF: 'TF',
    JP: 'JP',
  }
  const dimensions: MbtiDimensionScore[] = Object.entries(raw.scores ?? {}).map(
    ([key, leftPercent]) => {
      const dimension = keyMap[key] ?? 'EI'
      const labels = mbtiPoleLabelMap[dimension]
      const percent = Math.round(leftPercent)
      return {
        dimension,
        leftLabel: labels[0],
        rightLabel: labels[1],
        leftPercent: percent,
        preference: percent >= 50 ? ('left' as const) : ('right' as const),
      }
    },
  )
  const current = getStoredUser()
  return {
    userId: raw.userId,
    userName: current && current.userId === raw.userId ? current.name : '',
    type: raw.type,
    typeName: mbtiTypeNameMap[raw.type] ?? raw.type,
    dimensions,
    description: raw.report ?? '',
    suggestions: raw.suggestions ?? [],
    submitTime: raw.testTime,
  }
}

/** GET /mbti/questions —— 获取测评题目列表，权限：已登录 */
export async function getMbtiQuestions(): Promise<MbtiQuestion[]> {
  // TODO(backend): 联调时删除 mock 分支
  if (USE_MOCK) {
    await mockDelay(300)
    return mockMbtiQuestions
  }
  const raw = await get<RawQuestionVO[]>('/mbti/questions')
  return (raw ?? []).map(toQuestion)
}

/** POST /mbti/submit —— 提交答案，返回测评结果，权限：已登录 */
export async function submitMbti(answers: MbtiAnswer[]): Promise<MbtiResult> {
  // TODO(backend): 联调时删除 mock 分支；真实环境由后端完成计分
  if (USE_MOCK) {
    await mockDelay(500)
    const result = computeMbtiResult(answers)
    const index = mockMbtiResults.findIndex((item) => item.userId === result.userId)
    if (index >= 0) mockMbtiResults.splice(index, 1, result)
    else mockMbtiResults.push(result)
    return result
  }
  // 后端作答项字段为 option（A/B）
  const raw = await post<RawResultVO>('/mbti/submit', {
    answers: answers.map((answer) => ({
      questionId: answer.questionId,
      option: answer.value,
    })),
  })
  return toResult(raw)
}

/**
 * GET /mbti/results/{userId} —— 查看某会员测评结果
 * 权限：本人 / ASSISTANT_COACH+；userId 传 'me' 时后端按当前登录人处理。
 */
export async function getMbtiResult(userId: number | 'me'): Promise<MbtiResult> {
  // TODO(backend): 联调时删除 mock 分支
  if (USE_MOCK) {
    await mockDelay(300)
    const targetId = userId === 'me' ? getMockCurrentUser().userId : userId
    const result = mockMbtiResults.find((item) => item.userId === targetId)
    if (!result) throw new Error('该会员尚未完成 MBTI 测评')
    return result
  }
  // 后端路径参数为数值 id，'me' 由前端解析为当前登录人
  const resolvedId = userId === 'me' ? (getStoredUser()?.userId ?? 0) : userId
  const raw = await get<RawResultVO | null>(`/mbti/results/${resolvedId}`)
  if (!raw) throw new Error('该会员尚未完成 MBTI 测评')
  return toResult(raw)
}
