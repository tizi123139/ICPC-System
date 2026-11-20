import { fireEvent, render, screen, waitFor } from '@testing-library/vue'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ElMessage } from 'element-plus'
import TeamsView from './TeamsView.vue'
import { permission } from '@/directives/permission'
import { createTestRouter, resetAuth, setupAuth } from '@/test/utils'
import type { PageResult, RoleCode, TeamInfo, TeamMember, UserInfo } from '@/types'

vi.mock('@/api/teams', () => ({
  getTeams: vi.fn(),
  createTeam: vi.fn(),
}))

import * as teamsApi from '@/api/teams'

const getTeams = vi.mocked(teamsApi.getTeams)
const createTeam = vi.mocked(teamsApi.createTeam)

const makeUser = (name: string, userId = 1): UserInfo => ({
  userId,
  studentId: `100${userId}`,
  name,
  roles: ['TEAM_LEADER'],
})

const makeMember = (memberId: number, name: string, isLeader = false): TeamMember => ({
  memberId,
  name,
  studentId: `2024${String(memberId).padStart(6, '0')}`,
  memberType: 'MEMBER',
  isLeader,
  joinTime: '2026-10-20T10:00:00+08:00',
})

const makeTeam = (overrides: Partial<TeamInfo> = {}): TeamInfo => ({
  teamId: 1,
  name: '深夜 AC 小分队',
  description: '备战 2026 校赛，主攻动态规划与图论。',
  status: 'CONFIRMED',
  leader: makeUser('黄启帆', 11),
  members: [makeMember(11, '黄启帆', true), makeMember(2, '李知远')],
  dimensions: [],
  createTime: '2026-09-12T19:00:00+08:00',
  ...overrides,
})

const makePage = <T,>(records: T[]): PageResult<T> => ({
  total: records.length,
  page: 1,
  size: 9,
  records,
})

function renderView(role: RoleCode = 'MEMBER') {
  resetAuth()
  const { pinia } = setupAuth(role)
  const router = createTestRouter()
  return render(TeamsView, {
    global: {
      plugins: [pinia, router],
      directives: { permission },
    },
  })
}

describe('TeamsView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.spyOn(ElMessage, 'success').mockImplementation(() => ({ close: () => {} }) as never)
    vi.spyOn(ElMessage, 'warning').mockImplementation(() => ({ close: () => {} }) as never)
    vi.spyOn(ElMessage, 'error').mockImplementation(() => ({ close: () => {} }) as never)

    getTeams.mockResolvedValue(makePage([makeTeam()]))
  })

  it('【组队·查询队伍列表】渲染队伍名称、状态、队长与成员数', async () => {
    getTeams.mockResolvedValue(
      makePage([
        makeTeam(),
        makeTeam({
          teamId: 3,
          name: '新生冲锋队',
          description: '新生自发组队，等待助理教练确认。',
          status: 'PENDING',
          leader: makeUser('王一鸣', 3),
          members: [makeMember(3, '王一鸣', true)],
        }),
      ]),
    )

    renderView()

    await waitFor(() => expect(screen.getByText('深夜 AC 小分队')).toBeInTheDocument())
    expect(screen.getByText('新生冲锋队')).toBeInTheDocument()
    expect(screen.getAllByText('已确认').length).toBeGreaterThan(0)
    expect(screen.getAllByText('待教练确认').length).toBeGreaterThan(0)
    expect(screen.getByText('队长：黄启帆')).toBeInTheDocument()
    expect(screen.getByText('2 名成员')).toBeInTheDocument()
    expect(getTeams).toHaveBeenCalledWith(expect.objectContaining({ page: 1, size: 9 }))
  })

  it('【组队·查询队伍列表】按状态筛选：切换「待教练确认」后按 PENDING 查询', async () => {
    renderView()

    await waitFor(() => expect(screen.getByText('深夜 AC 小分队')).toBeInTheDocument())

    fireEvent.click(screen.getByRole('button', { name: '待教练确认' }))

    await waitFor(() =>
      expect(getTeams).toHaveBeenLastCalledWith(
        expect.objectContaining({ status: 'PENDING', page: 1 }),
      ),
    )
  })

  it('【组队·创建队伍】会员填写队伍名称后调用 createTeam', async () => {
    createTeam.mockResolvedValue(makeTeam({ teamId: 9, name: '新建队伍', status: 'PENDING' }))

    renderView('MEMBER')

    await waitFor(() => expect(screen.getByText('深夜 AC 小分队')).toBeInTheDocument())

    fireEvent.click(screen.getByRole('button', { name: '创建队伍' }))

    const nameInput = await screen.findByPlaceholderText('如 深夜 AC 小分队')
    await fireEvent.update(nameInput, '新建队伍')

    const buttons = screen.getAllByRole('button', { name: '创建队伍' })
    fireEvent.click(buttons[buttons.length - 1]!)

    await waitFor(() => expect(createTeam).toHaveBeenCalledTimes(1))
    expect(createTeam).toHaveBeenCalledWith(expect.objectContaining({ name: '新建队伍' }))
  })

  it('【权限·3.2 组队】干事及以上角色不显示「创建队伍」按钮', async () => {
    renderView('OFFICER')

    await waitFor(() => expect(screen.getByText('深夜 AC 小分队')).toBeInTheDocument())

    expect(screen.queryByRole('button', { name: '创建队伍' })).toBeNull()
  })
})
