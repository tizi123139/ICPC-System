import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/vue'
import type { Pinia } from 'pinia'
import MemberDetailView from './MemberDetailView.vue'
import { getMember, getMemberArchive } from '@/api/members'
import { createTestRouter, resetAuth, setupAuth } from '@/test/utils'
import type { MemberArchive, MemberInfo } from '@/types'

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

const member: MemberInfo = {
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

const archive: MemberArchive = {
  member,
  trainingCount: 18,
  submissionCount: 126,
  acRate: 68.4,
  recentTrainings: [
    {
      trainingId: 11,
      name: '春季个人训练赛',
      solvedCount: 5,
      submitTime: '2026-10-20T18:00:00+08:00',
    },
    {
      trainingId: 12,
      name: '暑期组队训练赛',
      solvedCount: 4,
      submitTime: '2026-10-22T18:00:00+08:00',
    },
  ],
}

let pinia: Pinia

async function renderDetail() {
  const router = createTestRouter()
  await router.push('/members/1')
  await router.isReady()
  return render(MemberDetailView, {
    global: {
      plugins: [pinia, router],
    },
  })
}

beforeEach(() => {
  resetAuth()
  pinia = setupAuth('PRESIDENT').pinia

  vi.mocked(getMemberArchive).mockReset()
  vi.mocked(getMemberArchive).mockResolvedValue(archive)
  vi.mocked(getMember).mockReset()
  vi.mocked(getMember).mockResolvedValue(member)
})

describe('MemberDetailView', () => {
  it('【清单 17】获取会员详情：展示姓名、学号、专业等基本信息', async () => {
    await renderDetail()

    const names = await screen.findAllByText('张三')
    expect(names.length).toBeGreaterThan(0)
    expect(screen.getByText('20230001 · 计算机科学与技术 · 2023级')).toBeInTheDocument()
    expect(screen.getAllByText('计算机科学与技术').length).toBeGreaterThan(0)
    expect(screen.getByText('学号')).toBeInTheDocument()
    expect(screen.getByText('姓名')).toBeInTheDocument()
    expect(screen.getByText('专业')).toBeInTheDocument()

    expect(vi.mocked(getMemberArchive)).toHaveBeenCalledWith(1)
  })

  it('【清单 16】获取会员完整档案：展示训练次数、提交次数、AC 率与近期训练', async () => {
    await renderDetail()

    expect(await screen.findByText('参与训练')).toBeInTheDocument()
    expect(screen.getByText('18')).toBeInTheDocument()
    expect(screen.getByText('代码提交')).toBeInTheDocument()
    expect(screen.getByText('126')).toBeInTheDocument()
    expect(screen.getByText('AC 率')).toBeInTheDocument()
    expect(screen.getByText('68.4%')).toBeInTheDocument()

    expect(screen.getByText('近期训练')).toBeInTheDocument()
    expect(await screen.findByText('春季个人训练赛')).toBeInTheDocument()
    expect(screen.getByText('暑期组队训练赛')).toBeInTheDocument()
    expect(screen.getByText('5')).toBeInTheDocument()
    expect(screen.getByText('4')).toBeInTheDocument()
  })
})
