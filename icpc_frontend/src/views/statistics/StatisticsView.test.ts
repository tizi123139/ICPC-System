/**
 * 数据统计页测试
 * 覆盖：会员结构统计 / 训练数据汇总 KPI 与趋势 / 导出训练成绩报表
 */
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor } from '@testing-library/vue'
import type { Pinia } from 'pinia'
import type { Router } from 'vue-router'
import StatisticsView from './StatisticsView.vue'
import { createTestRouter, resetAuth, setupAuth } from '@/test/utils'
import type { MemberOverview, PageResult, TrainingInfo, TrainingSummary } from '@/types'

vi.mock('@/api/statistics', () => ({
  getMemberOverview: vi.fn(),
  getTrainingSummary: vi.fn(),
  exportTrainingReport: vi.fn(),
}))

vi.mock('@/api/trainings', () => ({
  getTrainings: vi.fn(),
}))

vi.mock('@/components/BaseChart.vue', () => ({
  default: { template: '<div data-testid="chart" />' },
}))

import * as statisticsApi from '@/api/statistics'
import * as trainingsApi from '@/api/trainings'

const getMemberOverview = vi.mocked(statisticsApi.getMemberOverview)
const getTrainingSummary = vi.mocked(statisticsApi.getTrainingSummary)
const exportTrainingReport = vi.mocked(statisticsApi.exportTrainingReport)
const getTrainings = vi.mocked(trainingsApi.getTrainings)

class ResizeObserverStub {
  observe() {}
  unobserve() {}
  disconnect() {}
}
if (!('ResizeObserver' in globalThis)) {
  globalThis.ResizeObserver = ResizeObserverStub as unknown as typeof ResizeObserver
}

const overview: MemberOverview = {
  total: 156,
  byType: [
    { name: 'OBSERVE_MEMBER', value: 38 },
    { name: 'MEMBER', value: 118 },
  ],
  byGrade: [
    { name: '2023级', value: 47 },
    { name: '2024级', value: 58 },
  ],
  byDepartment: [
    { name: '竞赛部', value: 36 },
    { name: '组织部', value: 28 },
  ],
}

const summary: TrainingSummary = {
  totalTrainings: 42,
  totalSubmissions: 1860,
  acRate: 63.5,
  trend: [
    { date: '10-06', submissions: 86, ac: 52 },
    { date: '10-13', submissions: 112, ac: 70 },
  ],
}

const trainingPage: PageResult<TrainingInfo> = {
  total: 1,
  page: 1,
  size: 100,
  records: [
    {
      trainingId: 2,
      name: 'DP 专项训练（一）',
      type: 'SOLO',
      startTime: '2026-11-08T19:00:00+08:00',
      endTime: '2026-11-08T22:00:00+08:00',
      leaderboardVisible: true,
      freezeTime: 60,
      problemIds: [102, 105, 106],
      status: 'PUBLISHED',
      creator: {
        userId: 1,
        studentId: '2024001',
        name: '测试用户',
        roles: ['PRESIDENT'],
      },
      createTime: '2026-11-01T10:00:00+08:00',
    },
  ],
}

let pinia: Pinia
let router: Router

describe('StatisticsView 数据统计', () => {
  beforeEach(async () => {
    resetAuth()
    vi.clearAllMocks()
    pinia = setupAuth('PRESIDENT').pinia
    router = createTestRouter()
    await router.push('/statistics')

    getMemberOverview.mockResolvedValue(overview)
    getTrainingSummary.mockResolvedValue(summary)
    getTrainings.mockResolvedValue(trainingPage)
    exportTrainingReport.mockResolvedValue(undefined)
  })

  it('【清单 70/71】渲染 KPI 指标、提交趋势与会员结构分布', async () => {
    render(StatisticsView, {
      global: {
        plugins: [pinia, router],
      },
    })

    await waitFor(() => expect(getMemberOverview).toHaveBeenCalledTimes(1))
    await waitFor(() => expect(getTrainingSummary).toHaveBeenCalledTimes(1))
    await waitFor(() => expect(screen.getByText('训练提交趋势')).toBeInTheDocument())

    // KPI：会员总数 / 训练总数 / 提交总数 / AC 率（acRate 已是百分数）
    expect(screen.getByText('156')).toBeInTheDocument()
    expect(screen.getByText('42')).toBeInTheDocument()
    expect(screen.getByText('1860')).toBeInTheDocument()
    expect(screen.getByText('63.5')).toBeInTheDocument()

    // 会员结构分布：类型展示中文标签，年级/部门展示原始名称
    expect(screen.getByText('会员类型分布')).toBeInTheDocument()
    expect(screen.getByText('观察会员')).toBeInTheDocument()
    expect(screen.getByText('2024级')).toBeInTheDocument()
    expect(screen.getByText('竞赛部')).toBeInTheDocument()

    // 趋势图已渲染（BaseChart 被桩替换）
    expect(screen.getByTestId('chart')).toBeInTheDocument()
  })

  it('【清单 69】选择训练后导出成绩报表，调用 exportTrainingReport', async () => {
    render(StatisticsView, {
      global: {
        plugins: [pinia, router],
      },
    })

    await waitFor(() => expect(screen.getByText('导出训练成绩报表')).toBeInTheDocument())

    // 未选择训练时点击导出：直接提示且不调用接口
    fireEvent.click(screen.getByTestId('export-btn'))
    await waitFor(() => expect(exportTrainingReport).not.toHaveBeenCalled())

    // 打开下拉选择训练
    fireEvent.click(screen.getByTestId('training-select'))
    await waitFor(() => expect(screen.getByText('DP 专项训练（一）')).toBeInTheDocument())
    fireEvent.click(screen.getByText('DP 专项训练（一）'))

    fireEvent.click(screen.getByTestId('export-btn'))
    await waitFor(() => expect(exportTrainingReport).toHaveBeenCalledWith(2))
  })
})
