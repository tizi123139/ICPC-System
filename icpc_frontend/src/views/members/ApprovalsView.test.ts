import { beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor, within } from '@testing-library/vue'
import type { Pinia } from 'pinia'
import { ElMessage } from 'element-plus'
import ApprovalsView from './ApprovalsView.vue'
import { auditApply, getApprovals } from '@/api/members'
import { permission } from '@/directives/permission'
import { createTestRouter, resetAuth, setupAuth } from '@/test/utils'
import type { ApplyRecord } from '@/types'

vi.mock('@/api/members', () => ({
  getApprovals: vi.fn(),
  auditApply: vi.fn(),
}))

class ResizeObserverStub {
  observe() {}
  unobserve() {}
  disconnect() {}
}
if (!('ResizeObserver' in globalThis)) {
  globalThis.ResizeObserver = ResizeObserverStub as unknown as typeof ResizeObserver
}

const pendingJoin: ApplyRecord = {
  applyId: 1,
  memberId: 13,
  memberName: '刘星宇',
  studentId: '2025210333',
  type: 'JOIN',
  reason: '对算法竞赛有浓厚兴趣，希望加入协会接受系统训练。',
  applyTime: '2026-11-03T10:22:00+08:00',
  status: 'PENDING',
}

const pendingResign: ApplyRecord = {
  applyId: 2,
  memberId: 8,
  memberName: '何思远',
  studentId: '2022320118',
  type: 'RESIGN',
  reason: '课业压力较大，暂时无法参加日常训练，申请退会。',
  applyTime: '2026-10-30T09:12:00+08:00',
  status: 'PENDING',
}

const approvedRecord: ApplyRecord = {
  applyId: 3,
  memberId: 16,
  memberName: '唐可',
  studentId: '2023310222',
  type: 'JOIN',
  reason: '已完成观察期考核，申请转为正式会员。',
  applyTime: '2026-10-28T14:30:00+08:00',
  status: 'APPROVED',
}

const rejectedRecord: ApplyRecord = {
  applyId: 4,
  memberId: 17,
  memberName: '高子恒',
  studentId: '2024310405',
  type: 'JOIN',
  reason: '希望通过协会的训练体系备战明年的校赛。',
  applyTime: '2026-11-01T20:05:00+08:00',
  status: 'REJECTED',
}

let pinia: Pinia
let successSpy: ReturnType<typeof vi.spyOn>

function renderView() {
  const router = createTestRouter()
  return render(ApprovalsView, {
    global: {
      plugins: [pinia, router],
      directives: { permission },
    },
  })
}

function tableRows(): Element[] {
  return Array.from(document.querySelectorAll('.el-table__row'))
}

beforeEach(() => {
  resetAuth()
  pinia = setupAuth('PRESIDENT').pinia

  vi.mocked(getApprovals).mockReset()
  vi.mocked(getApprovals).mockResolvedValue([pendingJoin, pendingResign])
  vi.mocked(auditApply).mockReset()
  vi.mocked(auditApply).mockResolvedValue(undefined)

  successSpy = vi
    .spyOn(ElMessage, 'success')
    .mockImplementation(() => ({ close: () => {} }) as never)
})

describe('ApprovalsView', () => {
  it('【清单 12】查询待审批申请列表：渲染申请人、申请类型与申请说明', async () => {
    renderView()

    expect(await screen.findByText('刘星宇')).toBeInTheDocument()
    expect(screen.getByText('何思远')).toBeInTheDocument()
    expect(screen.getAllByText('入会申请').length).toBeGreaterThan(0)
    expect(screen.getAllByText('退会申请').length).toBeGreaterThan(0)
    expect(screen.getByText(pendingJoin.reason)).toBeInTheDocument()
    expect(screen.getByText(pendingResign.reason)).toBeInTheDocument()
    expect(screen.getByText(/共\s*2\s*条记录/)).toBeInTheDocument()

    expect(vi.mocked(getApprovals)).toHaveBeenCalledTimes(1)
    expect(vi.mocked(getApprovals).mock.calls[0]).toEqual([])
  })

  it('【清单 12】按状态筛选：切换“已通过/已驳回”后仅展示对应状态记录', async () => {
    vi.mocked(getApprovals).mockResolvedValue([
      pendingJoin,
      approvedRecord,
      rejectedRecord,
    ])
    renderView()

    await screen.findByText('刘星宇')

    fireEvent.click(screen.getByText('已通过'))
    await waitFor(() => expect(screen.getByText('唐可')).toBeInTheDocument())
    expect(screen.queryByText('刘星宇')).not.toBeInTheDocument()
    expect(screen.queryByText('高子恒')).not.toBeInTheDocument()
    expect(screen.getByText(/共\s*1\s*条记录/)).toBeInTheDocument()

    fireEvent.click(screen.getByText('已驳回'))
    await waitFor(() => expect(screen.getByText('高子恒')).toBeInTheDocument())
    expect(screen.queryByText('唐可')).not.toBeInTheDocument()
    expect(screen.queryByText('刘星宇')).not.toBeInTheDocument()
    expect(screen.getByText(/共\s*1\s*条记录/)).toBeInTheDocument()
  })

  it('【清单 11】审批申请：填写审批意见并通过后调用 auditApply(APPROVE)', async () => {
    renderView()

    await screen.findByText('刘星宇')
    const rows = tableRows()
    expect(rows.length).toBe(2)

    fireEvent.click(within(rows[0] as HTMLElement).getByText('通过'))
    expect(await screen.findByText('审批申请')).toBeInTheDocument()

    const remarkInput = screen.getByPlaceholderText('填写审批备注，将同步给申请人')
    fireEvent.update(remarkInput, '材料齐全，同意入会')

    fireEvent.click(screen.getByRole('button', { name: '确认通过' }))

    await waitFor(() => expect(vi.mocked(auditApply)).toHaveBeenCalledTimes(1))
    expect(vi.mocked(auditApply)).toHaveBeenCalledWith(
      pendingJoin.applyId,
      'APPROVE',
      '材料齐全，同意入会',
    )
    expect(successSpy).toHaveBeenCalledWith('已通过申请')
  })

  it('【清单 11】驳回申请：确认驳回后调用 auditApply(REJECT)', async () => {
    renderView()

    await screen.findByText('刘星宇')
    const rows = tableRows()

    fireEvent.click(within(rows[0] as HTMLElement).getByText('驳回'))
    expect(await screen.findByText('审批申请')).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: '确认驳回' }))

    await waitFor(() => expect(vi.mocked(auditApply)).toHaveBeenCalledTimes(1))
    expect(vi.mocked(auditApply)).toHaveBeenCalledWith(pendingJoin.applyId, 'REJECT', '')
    expect(successSpy).toHaveBeenCalledWith('已驳回申请')
  })
})
