import { beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor, within } from '@testing-library/vue'
import type { Pinia } from 'pinia'
import { ElMessage } from 'element-plus'
import MembersView from './MembersView.vue'
import { createMember, deleteMember, getMembers, updateMember } from '@/api/members'
import { permission } from '@/directives/permission'
import { createTestRouter, resetAuth, setupAuth } from '@/test/utils'
import type { MemberInfo } from '@/types'

const { confirmMock } = vi.hoisted(() => ({ confirmMock: vi.fn() }))

vi.mock('element-plus', async () => {
  const actual = await vi.importActual<typeof import('element-plus')>('element-plus')
  return {
    ...actual,
    ElMessageBox: {
      ...(actual.ElMessageBox as unknown as Record<string, unknown>),
      confirm: confirmMock,
    },
  }
})

vi.mock('@/api/members', () => ({
  getMembers: vi.fn(),
  getMember: vi.fn(),
  getMemberArchive: vi.fn(),
  createMember: vi.fn(),
  updateMember: vi.fn(),
  deleteMember: vi.fn(),
}))

class ResizeObserverStub {
  observe() {}
  unobserve() {}
  disconnect() {}
}
if (!('ResizeObserver' in globalThis)) {
  globalThis.ResizeObserver = ResizeObserverStub as unknown as typeof ResizeObserver
}

const memberA: MemberInfo = {
  memberId: 1,
  name: '张三',
  studentId: '20230001',
  major: '计算机科学与技术',
  grade: '2023级',
  phone: '13800138000',
  email: 'zhangsan@example.com',
  memberType: 'MEMBER',
  department: '竞赛部',
  joinDate: '2023-09-01',
  status: 'ACTIVE',
  createTime: '2023-09-01T10:00:00+08:00',
}

const memberB: MemberInfo = {
  memberId: 2,
  name: '李四',
  studentId: '20240002',
  major: '软件工程',
  grade: '2024级',
  phone: '13900139000',
  email: 'lisi@example.com',
  memberType: 'MEMBER',
  department: '组织部',
  joinDate: '2024-09-01',
  status: 'INACTIVE',
  createTime: '2024-09-01T10:00:00+08:00',
}

function renderView(pinia: Pinia) {
  const router = createTestRouter()
  return render(MembersView, {
    global: {
      plugins: [pinia, router],
      directives: { permission },
    },
  })
}

function tableRows(): Element[] {
  return Array.from(document.querySelectorAll('.el-table__row'))
}

let pinia: Pinia

beforeEach(() => {
  resetAuth()
  pinia = setupAuth('PRESIDENT').pinia

  vi.mocked(getMembers).mockReset()
  vi.mocked(getMembers).mockResolvedValue({ total: 2, page: 1, size: 10, records: [memberA, memberB] })
  vi.mocked(createMember).mockReset()
  vi.mocked(createMember).mockResolvedValue(memberA)
  vi.mocked(updateMember).mockReset()
  vi.mocked(updateMember).mockResolvedValue(memberA)
  vi.mocked(deleteMember).mockReset()
  vi.mocked(deleteMember).mockResolvedValue(undefined)
  confirmMock.mockReset()
})

describe('MembersView', () => {
  it('【清单 15】分页查询会员列表：渲染姓名、学号与总数', async () => {
    renderView(pinia)

    expect(await screen.findByText('张三')).toBeInTheDocument()
    expect(screen.getByText('李四')).toBeInTheDocument()
    expect(screen.getAllByText('20230001').length).toBeGreaterThan(0)
    expect(screen.getAllByText('20240002').length).toBeGreaterThan(0)
    expect(screen.getByText(/共\s*2\s*名会员/)).toBeInTheDocument()

    expect(vi.mocked(getMembers)).toHaveBeenCalledTimes(1)
    expect(vi.mocked(getMembers)).toHaveBeenCalledWith(
      expect.objectContaining({ page: 1, size: 10 }),
    )
  })

  it('【清单 20】新增会员：填写必填项后调用 createMember 且参数正确', async () => {
    const successSpy = vi.spyOn(ElMessage, 'success')
    renderView(pinia)

    await screen.findByText('张三')
    fireEvent.click(screen.getByRole('button', { name: /新增会员/ }))

    const nameInput = await screen.findByPlaceholderText('请输入姓名')
    fireEvent.update(nameInput, '王五')
    fireEvent.update(screen.getByPlaceholderText('请输入学号'), '20250001')
    fireEvent.update(screen.getByPlaceholderText('请输入专业'), '人工智能')
    fireEvent.update(screen.getByPlaceholderText('请输入手机号'), '13700137000')
    fireEvent.update(screen.getByPlaceholderText('请输入邮箱'), 'wangwu@example.com')

    fireEvent.click(screen.getByRole('button', { name: '创建会员' }))

    await waitFor(() => expect(vi.mocked(createMember)).toHaveBeenCalledTimes(1))
    expect(vi.mocked(createMember)).toHaveBeenCalledWith(
      expect.objectContaining({
        name: '王五',
        studentId: '20250001',
        major: '人工智能',
        phone: '13700137000',
        email: 'wangwu@example.com',
        memberType: 'OBSERVE_MEMBER',
        status: 'ACTIVE',
      }),
    )
    expect(successSpy).toHaveBeenCalledWith('会员已创建')
    successSpy.mockRestore()
  })

  it('【清单 18】更新会员信息：编辑弹窗回填原值，保存后调用 updateMember', async () => {
    renderView(pinia)

    await screen.findByText('张三')
    const rows = tableRows()
    expect(rows).toHaveLength(2)

    fireEvent.click(within(rows[0] as HTMLElement).getByText('编辑'))

    const nameInput = (await screen.findByPlaceholderText('请输入姓名')) as HTMLInputElement
    expect(nameInput.value).toBe('张三')
    expect((screen.getByPlaceholderText('请输入学号') as HTMLInputElement).value).toBe('20230001')

    fireEvent.update(nameInput, '张三丰')
    fireEvent.click(screen.getByRole('button', { name: '保存修改' }))

    await waitFor(() => expect(vi.mocked(updateMember)).toHaveBeenCalledTimes(1))
    expect(vi.mocked(updateMember)).toHaveBeenCalledWith(
      1,
      expect.objectContaining({ name: '张三丰', studentId: '20230001' }),
    )
  })

  it('【清单 19】逻辑删除会员：确认后调用 deleteMember 并重新加载列表', async () => {
    const successSpy = vi.spyOn(ElMessage, 'success')
    confirmMock.mockResolvedValue('confirm')
    renderView(pinia)

    await screen.findByText('张三')
    const rows = tableRows()
    fireEvent.click(within(rows[0] as HTMLElement).getByText('删除'))

    await waitFor(() => expect(vi.mocked(deleteMember)).toHaveBeenCalledWith(1))
    await waitFor(() => expect(vi.mocked(getMembers)).toHaveBeenCalledTimes(2))
    expect(successSpy).toHaveBeenCalledWith('已删除')
    successSpy.mockRestore()
  })
})
