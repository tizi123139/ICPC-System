import { beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor } from '@testing-library/vue'
import type { Pinia } from 'pinia'
import { ElMessage } from 'element-plus'
import DashboardView from './DashboardView.vue'
import { applyJoin, applyResign, getApprovals } from '@/api/members'
import { getMemberOverview, getTrainingSummary } from '@/api/statistics'
import { getTrainings } from '@/api/trainings'
import { permission } from '@/directives/permission'
import { createTestRouter, resetAuth, setupAuth } from '@/test/utils'
import type { MemberOverview, PageResult, TrainingInfo, TrainingSummary } from '@/types'

vi.mock('@/api/statistics', () => ({
  getMemberOverview: vi.fn(),
  getTrainingSummary: vi.fn(),
  exportTrainingReport: vi.fn(),
}))

vi.mock('@/api/members', () => ({
  getApprovals: vi.fn(),
  applyJoin: vi.fn(),
  applyResign: vi.fn(),
}))

vi.mock('@/api/trainings', () => ({
  getTrainings: vi.fn(),
}))

vi.mock('@/components/BaseChart.vue', () => ({
  default: { template: '<div data-test="chart" />' },
}))

class ResizeObserverStub {
  observe() {}
  unobserve() {}
  disconnect() {}
}
if (!('ResizeObserver' in globalThis)) {
  globalThis.ResizeObserver = ResizeObserverStub as unknown as typeof ResizeObserver
}

const overviewData: MemberOverview = {
  total: 156,
  byType: [
    { name: 'OBSERVE_MEMBER', value: 38 },
    { name: 'MEMBER', value: 118 },
  ],
  byGrade: [
    { name: '2022级', value: 21 },
    { name: '2023级', value: 47 },
  ],
  byDepartment: [
    { name: '竞赛部', value: 36 },
    { name: '组织部', value: 28 },
  ],
}

const summaryData: TrainingSummary = {
  totalTrainings: 42,
  totalSubmissions: 1860,
  acRate: 63.5,
  trend: [
    { date: '10-06', submissions: 86, ac: 52 },
    { date: '10-13', submissions: 112, ac: 70 },
    { date: '10-20', submissions: 98, ac: 61 },
    { date: '10-27', submissions: 134, ac: 88 },
    { date: '11-03', submissions: 156, ac: 101 },
    { date: '11-10', submissions: 142, ac: 92 },
  ],
}

const trainingPage: PageResult<TrainingInfo> = {
  total: 0,
  page: 1,
  size: 4,
  records: [],
}

let pinia: Pinia
let successSpy: ReturnType<typeof vi.spyOn>

function renderView() {
  const router = createTestRouter()
  return render(DashboardView, {
    global: {
      plugins: [pinia, router],
      directives: { permission },
    },
  })
}

function chartCount(): number {
  return document.querySelectorAll('[data-test="chart"]').length
}

beforeEach(() => {
  resetAuth()
  vi.clearAllMocks()
  pinia = setupAuth('PRESIDENT').pinia

  vi.mocked(getMemberOverview).mockResolvedValue(overviewData)
  vi.mocked(getTrainingSummary).mockResolvedValue(summaryData)
  vi.mocked(getTrainings).mockResolvedValue(trainingPage)
  vi.mocked(getApprovals).mockResolvedValue([])
  vi.mocked(applyJoin).mockResolvedValue(undefined)
  vi.mocked(applyResign).mockResolvedValue(undefined)

  successSpy = vi
    .spyOn(ElMessage, 'success')
    .mockImplementation(() => ({ close: () => {} }) as never)
})

describe('DashboardView', () => {
  it('【清单 13】提交入会申请：观察会员填写理由后调用 applyJoin', async () => {
    pinia = setupAuth('OBSERVE_MEMBER').pinia
    renderView()

    fireEvent.click(await screen.findByRole('button', { name: '提交入会申请' }))

    const reason = '已完成观察期考核，希望加入协会接受系统训练'
    fireEvent.update(
      await screen.findByPlaceholderText('请说明你的学习情况、竞赛经历以及希望加入协会的原因'),
      reason,
    )

    fireEvent.click(screen.getByRole('button', { name: '提交申请' }))

    await waitFor(() => expect(vi.mocked(applyJoin)).toHaveBeenCalledTimes(1))
    expect(vi.mocked(applyJoin)).toHaveBeenCalledWith({
      name: '测试用户',
      studentId: '1001',
      reason,
    })
    expect(successSpy).toHaveBeenCalledWith('入会申请已提交，等待管理员审批')
  })

  it('【清单 14】提交退会申请：正式会员填写原因后调用 applyResign', async () => {
    pinia = setupAuth('MEMBER').pinia
    renderView()

    fireEvent.click(await screen.findByRole('button', { name: '提交退会申请' }))

    const reason = '课业压力较大，暂时无法参加日常训练'
    fireEvent.update(
      await screen.findByPlaceholderText('请说明退会原因，便于管理员跟进'),
      reason,
    )

    fireEvent.click(screen.getByRole('button', { name: '提交申请' }))

    await waitFor(() => expect(vi.mocked(applyResign)).toHaveBeenCalledTimes(1))
    expect(vi.mocked(applyResign)).toHaveBeenCalledWith({ reason })
    expect(successSpy).toHaveBeenCalledWith('退会申请已提交，等待管理员审批')
  })

  it('【清单 71】会员结构统计：会长可见图表卡片与协会会员总数', async () => {
    renderView()

    expect(await screen.findByText('会员结构')).toBeInTheDocument()
    expect(screen.getByText('协会会员总数')).toBeInTheDocument()
    expect(await screen.findByText('156')).toBeInTheDocument()
    expect(screen.getByText('待审批申请')).toBeInTheDocument()

    await waitFor(() => expect(vi.mocked(getMemberOverview)).toHaveBeenCalledTimes(1))
    expect(vi.mocked(getApprovals)).toHaveBeenCalledTimes(1)
    expect(chartCount()).toBe(2)
  })

  it('【清单 71】会员结构统计：非管理角色不可见且不发起会员统计请求', async () => {
    // 助理教练：可看训练汇总，但不具备会员管理权限
    pinia = setupAuth('ASSISTANT_COACH').pinia
    renderView()

    await waitFor(() => expect(vi.mocked(getTrainingSummary)).toHaveBeenCalledTimes(1))

    expect(screen.queryByText('会员结构')).not.toBeInTheDocument()
    expect(screen.queryByText('待审批申请')).not.toBeInTheDocument()
    expect(screen.queryByText('协会会员总数')).not.toBeInTheDocument()
    expect(screen.getByText('训练活跃度')).toBeInTheDocument()
    expect(chartCount()).toBe(1)

    expect(vi.mocked(getMemberOverview)).not.toHaveBeenCalled()
    expect(vi.mocked(getApprovals)).not.toHaveBeenCalled()
  })

  it('【清单 70】训练数据汇总：渲染训练总场次、累计提交与整体 AC 率', async () => {
    renderView()

    expect(await screen.findByText('累计训练场次')).toBeInTheDocument()
    expect(await screen.findByText('42')).toBeInTheDocument()
    expect(screen.getByText('累计代码提交')).toBeInTheDocument()
    expect(await screen.findByText('1860')).toBeInTheDocument()
    expect(screen.getByText('整体 AC 率')).toBeInTheDocument()
    expect(await screen.findByText('63.5%')).toBeInTheDocument()

    expect(vi.mocked(getTrainingSummary)).toHaveBeenCalledTimes(1)
    expect(vi.mocked(getTrainings)).toHaveBeenCalledWith({ page: 1, size: 4 })
  })
})
