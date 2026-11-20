import { render, screen, waitFor, fireEvent } from '@testing-library/vue'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import type { Pinia } from 'pinia'
import type { Router } from 'vue-router'
import AbilityView from './AbilityView.vue'
import { createTestRouter, resetAuth, setupAuth } from '@/test/utils'
import type { AbilityResult, RecommendedTraining, WeakTagProblemGroup } from '@/types'

vi.mock('@/api/trainings', () => ({
  getAbility: vi.fn(),
}))

vi.mock('@/api/recommend', () => ({
  getRecommendedTrainings: vi.fn(),
  getRecommendedProblems: vi.fn(),
}))

vi.mock('@/components/BaseChart.vue', () => ({
  default: { template: '<div data-test="chart" />' },
}))

import * as trainingsApi from '@/api/trainings'
import * as recommendApi from '@/api/recommend'

const getAbility = vi.mocked(trainingsApi.getAbility)
const getRecommendedTrainings = vi.mocked(recommendApi.getRecommendedTrainings)
const getRecommendedProblems = vi.mocked(recommendApi.getRecommendedProblems)

const ability: AbilityResult = {
  userId: 1,
  userName: '测试用户',
  dimensions: [
    { name: '算法基础', value: 88 },
    { name: '数据结构', value: 76 },
    { name: '动态规划', value: 62 },
  ],
  report: '近期训练中算法基础表现稳定，动态规划题目通过率偏低。建议加强区间 DP 与状态压缩专项练习。',
}

let router: Router
let pinia: Pinia

describe('AbilityView', () => {
  beforeEach(async () => {
    resetAuth()
    vi.clearAllMocks()
    pinia = setupAuth('PRESIDENT').pinia
    router = createTestRouter()
    await router.push('/trainings/ability/me')

    getAbility.mockResolvedValue(ability)
    // 推荐接口默认返回空列表，避免影响既有断言
    getRecommendedTrainings.mockResolvedValue([])
    getRecommendedProblems.mockResolvedValue([])
  })

  it('【清单 22】个人能力雷达图数据与文字报告渲染', async () => {
    render(AbilityView, {
      global: {
        plugins: [pinia, router],
      },
    })

    await waitFor(() => expect(getAbility).toHaveBeenCalledWith('me'))
    await waitFor(() => expect(screen.getByText('算法基础')).toBeInTheDocument())

    // 能力维度（雷达图数据来源）及分数
    expect(screen.getByText('数据结构')).toBeInTheDocument()
    expect(screen.getByText('动态规划')).toBeInTheDocument()
    expect(screen.getByText('88')).toBeInTheDocument()
    expect(screen.getByText('62')).toBeInTheDocument()

    // 文字报告
    expect(screen.getByText(ability.report)).toBeInTheDocument()

    // 雷达图使用 BaseChart 替身渲染，未触发 ECharts 初始化
    expect(document.querySelector('[data-test="chart"]')).toBeInTheDocument()

    // 当前登录人的综合信息
    expect(screen.getByText('本人（测试用户）')).toBeInTheDocument()
  })

  it('【清单 65】基于能力评估推荐专项训练：渲染推荐卡片并支持跳转', async () => {
    const trainings: RecommendedTraining[] = [
      {
        trainingId: 3,
        name: '图论专题训练',
        type: 'TEAM',
        startTime: '2026-11-08T19:00:00+08:00',
        endTime: '2026-11-08T22:00:00+08:00',
        status: 'PUBLISHED',
        matchedDimensions: ['图论'],
        reason: '针对薄弱维度「图论」设计，建议优先完成',
      },
    ]
    getRecommendedTrainings.mockResolvedValue(trainings)

    const testRouter = createTestRouter()
    await testRouter.push('/trainings/ability/me')
    const pushSpy = vi.spyOn(testRouter, 'push').mockResolvedValue(undefined as never)
    render(AbilityView, {
      global: {
        plugins: [pinia, testRouter],
      },
    })

    await waitFor(() =>
      expect(getRecommendedTrainings).toHaveBeenCalledWith('me'),
    )
    await waitFor(() => expect(screen.getByText('推荐专项训练')).toBeInTheDocument())
    expect(screen.getByText('图论专题训练')).toBeInTheDocument()
    expect(screen.getByText('组队训练')).toBeInTheDocument()
    expect(screen.getByText('针对薄弱维度「图论」设计，建议优先完成')).toBeInTheDocument()

    // 点击推荐项跳转训练详情
    fireEvent.click(screen.getByText('图论专题训练'))
    expect(pushSpy).toHaveBeenCalledWith('/trainings/3')
  })

  it('【清单 66】基于薄弱知识点推荐习题：按知识点分组渲染', async () => {
    const groups: WeakTagProblemGroup[] = [
      {
        tagName: '数学',
        score: 55,
        problems: [
          {
            problemId: 110,
            title: '组合数取模',
            difficulty: 'MEDIUM',
            tagNames: ['组合数学', '数论'],
            reason: '数学得分 55，建议由易到难练习',
          },
        ],
      },
    ]
    getRecommendedProblems.mockResolvedValue(groups)

    const testRouter = createTestRouter()
    await testRouter.push('/trainings/ability/me')
    render(AbilityView, {
      global: {
        plugins: [pinia, testRouter],
      },
    })

    await waitFor(() => expect(getRecommendedProblems).toHaveBeenCalledWith('me'))
    await waitFor(() => expect(screen.getByText('薄弱知识点习题推荐')).toBeInTheDocument())
    expect(screen.getByText('数学')).toBeInTheDocument()
    expect(screen.getByText('能力值 55')).toBeInTheDocument()
    expect(screen.getByText('组合数取模')).toBeInTheDocument()
    expect(screen.getByText('中等')).toBeInTheDocument()
    expect(screen.getByText('数学得分 55，建议由易到难练习')).toBeInTheDocument()
  })
})
