import { fireEvent, render, screen, waitFor } from '@testing-library/vue'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { Pinia } from 'pinia'
import type { Router } from 'vue-router'
import TeamDetailView from './TeamDetailView.vue'
import { permission } from '@/directives/permission'
import { createTestRouter, resetAuth, setupAuth } from '@/test/utils'
import type { MemberInfo, PageResult, RoleCode, TeamInfo, TeamMember, UserInfo } from '@/types'

vi.mock('@/api/teams', () => ({
  getTeam: vi.fn(),
  updateTeam: vi.fn(),
  inviteTeamMember: vi.fn(),
  removeTeamMember: vi.fn(),
  confirmTeam: vi.fn(),
}))

vi.mock('@/api/members', () => ({
  getMembers: vi.fn(),
}))

vi.mock('@/components/BaseChart.vue', () => ({
  default: { template: '<div data-test="chart" />' },
}))

import * as teamsApi from '@/api/teams'
import * as membersApi from '@/api/members'

const getTeam = vi.mocked(teamsApi.getTeam)
const updateTeam = vi.mocked(teamsApi.updateTeam)
const inviteTeamMember = vi.mocked(teamsApi.inviteTeamMember)
const removeTeamMember = vi.mocked(teamsApi.removeTeamMember)
const confirmTeam = vi.mocked(teamsApi.confirmTeam)
const getMembers = vi.mocked(membersApi.getMembers)

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

const team: TeamInfo = {
  teamId: 1,
  name: '深夜 AC 小分队',
  description: '备战 2026 校赛。',
  status: 'PENDING',
  // 队长 userId = 1，与 setupAuth 默认登录用户一致（isLeader 为 true）
  leader: makeUser('测试用户', 1),
  members: [makeMember(1, '测试用户', true), makeMember(2, '李知远')],
  dimensions: [],
  createTime: '2026-09-12T19:00:00+08:00',
}

const memberInfo = (memberId: number, name: string): MemberInfo => ({
  memberId,
  name,
  studentId: `2024${String(memberId).padStart(6, '0')}`,
  major: '计算机科学与技术',
  grade: '2024级',
  phone: '13800000000',
  email: 'demo@example.com',
  memberType: 'MEMBER',
  department: '竞赛部',
  joinDate: '2026-10-01',
  status: 'ACTIVE',
  createTime: '2026-10-01T09:00:00+08:00',
})

const makePage = <T,>(records: T[]): PageResult<T> => ({
  total: records.length,
  page: 1,
  size: 100,
  records,
})

/** 邀请成员弹窗的下拉替身：点击后回填一个候选人 id */
const ElSelectStub = {
  name: 'ElSelect',
  props: { modelValue: { default: undefined } },
  emits: ['update:modelValue', 'change'],
  template: `<button type="button" class="el-select-stub" @click="pick">选择会员</button>`,
  methods: {
    pick(this: { $emit: (event: string, payload: unknown) => void }) {
      this.$emit('update:modelValue', 42)
      this.$emit('change', 42)
    },
  },
}

let router: Router
let pinia: Pinia

/** 预置路由到 /teams/1 后再渲染，保证组件内 teamId 解析正确 */
async function renderView(role: RoleCode = 'TEAM_LEADER') {
  resetAuth()
  pinia = setupAuth(role).pinia
  router = createTestRouter()
  await router.push('/teams/1')
  return render(TeamDetailView, {
    global: {
      plugins: [pinia, router],
      directives: { permission },
      stubs: { ElSelect: ElSelectStub },
    },
  })
}

beforeEach(async () => {
  vi.clearAllMocks()
  vi.spyOn(ElMessage, 'success').mockImplementation(() => ({ close: () => {} }) as never)
  vi.spyOn(ElMessage, 'warning').mockImplementation(() => ({ close: () => {} }) as never)
  vi.spyOn(ElMessage, 'error').mockImplementation(() => ({ close: () => {} }) as never)
  vi.spyOn(ElMessageBox, 'confirm').mockResolvedValue('confirm' as never)

  getTeam.mockResolvedValue({ ...team })
  getMembers.mockResolvedValue(makePage([memberInfo(2, '李知远'), memberInfo(42, '新候选人')]))
  updateTeam.mockResolvedValue(undefined)
  inviteTeamMember.mockResolvedValue(undefined)
  removeTeamMember.mockResolvedValue(undefined)
  confirmTeam.mockResolvedValue(undefined)
})

describe('TeamDetailView', () => {
  it('【组队·队伍详情】渲染队伍状态、队长与成员列表', async () => {
    await renderView()

    await waitFor(() => expect(getTeam).toHaveBeenCalledWith(1))
    expect(await screen.findByText('李知远')).toBeInTheDocument()
    expect(screen.getByText('2 人')).toBeInTheDocument()
    expect(screen.getByText('待教练确认')).toBeInTheDocument()
  })

  it('【组队·更新队伍信息】队长修改名称后调用 updateTeam', async () => {
    await renderView()

    await waitFor(() => expect(screen.getByText('李知远')).toBeInTheDocument())

    fireEvent.click(screen.getByRole('button', { name: '更新队伍信息' }))

    const nameInput = await screen.findByPlaceholderText('请输入队伍名称')
    expect(nameInput).toHaveValue('深夜 AC 小分队')
    await fireEvent.update(nameInput, '改名后的队伍')
    fireEvent.click(screen.getByRole('button', { name: '保存' }))

    await waitFor(() =>
      expect(updateTeam).toHaveBeenCalledWith(
        1,
        expect.objectContaining({ name: '改名后的队伍' }),
      ),
    )
  })

  it('【组队·邀请成员】队长选择会员后调用 inviteTeamMember', async () => {
    await renderView()

    await waitFor(() => expect(screen.getByText('李知远')).toBeInTheDocument())

    fireEvent.click(screen.getByRole('button', { name: '邀请成员' }))

    const selectStub = await screen.findByRole('button', { name: '选择会员' })
    fireEvent.click(selectStub)
    fireEvent.click(screen.getByRole('button', { name: '发送邀请' }))

    await waitFor(() => expect(inviteTeamMember).toHaveBeenCalledWith(1, 42))
  })

  it('【组队·移除成员】队长移除成员后调用 removeTeamMember', async () => {
    await renderView()

    await waitFor(() => expect(screen.getByText('李知远')).toBeInTheDocument())

    fireEvent.click(screen.getByRole('button', { name: '移除成员' }))

    await waitFor(() => expect(removeTeamMember).toHaveBeenCalledWith(1, 2))
  })

  it('【组队·教练确认队伍】助理教练确认待确认队伍时调用 confirmTeam', async () => {
    await renderView('ASSISTANT_COACH')

    await waitFor(() => expect(screen.getByText('李知远')).toBeInTheDocument())

    fireEvent.click(screen.getByRole('button', { name: '教练确认队伍' }))

    await waitFor(() => expect(confirmTeam).toHaveBeenCalledWith(1))
  })

  it('【权限·3.2 组队】非队长的普通会员看不到更新信息与邀请成员入口', async () => {
    // MEMBER 角色默认 userId=1，队伍队长 userId 改为 11，此时 isLeader=false
    getTeam.mockResolvedValue({ ...team, leader: makeUser('黄启帆', 11) })

    await renderView('MEMBER')

    await waitFor(() => expect(screen.getByText('李知远')).toBeInTheDocument())

    expect(screen.queryByRole('button', { name: '更新队伍信息' })).toBeNull()
    expect(screen.queryByRole('button', { name: '邀请成员' })).toBeNull()
  })

  it('【权限·3.2 组队】教练确认入口仅对 ASSISTANT_COACH+ 且队伍待确认时显示', async () => {
    // 队长（非教练）不可见确认按钮
    await renderView('TEAM_LEADER')
    await waitFor(() => expect(screen.getByText('李知远')).toBeInTheDocument())
    expect(screen.queryByRole('button', { name: '教练确认队伍' })).toBeNull()
  })
})
