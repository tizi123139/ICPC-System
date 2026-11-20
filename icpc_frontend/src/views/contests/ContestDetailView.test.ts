import { fireEvent, render, screen, waitFor } from '@testing-library/vue'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { Pinia } from 'pinia'
import type { Router } from 'vue-router'
import ContestDetailView from './ContestDetailView.vue'
import { permission } from '@/directives/permission'
import { createTestRouter, resetAuth, setupAuth } from '@/test/utils'
import type {
  CertificateBundle,
  ContestInfo,
  ContestRegistration,
  ContestResult,
  LeaderboardResult,
  PageResult,
  ProblemInfo,
  RoleCode,
  TeamInfo,
  UserInfo,
} from '@/types'

vi.mock('@/api/contests', () => ({
  getContest: vi.fn(),
  getMyContestRegistration: vi.fn(),
  getContestRegistrations: vi.fn(),
  registerContest: vi.fn(),
  auditContestRegistration: vi.fn(),
  getContestLeaderboard: vi.fn(),
  submitContestCode: vi.fn(),
  getContestResults: vi.fn(),
  updateContestResults: vi.fn(),
  generateCertificates: vi.fn(),
  downloadContestCertificates: vi.fn(),
}))

vi.mock('@/api/problems', () => ({
  getProblems: vi.fn(),
}))

vi.mock('@/api/teams', () => ({
  getTeams: vi.fn(),
}))

import * as contestsApi from '@/api/contests'
import * as problemsApi from '@/api/problems'
import * as teamsApi from '@/api/teams'

const getContest = vi.mocked(contestsApi.getContest)
const getMyContestRegistration = vi.mocked(contestsApi.getMyContestRegistration)
const getContestRegistrations = vi.mocked(contestsApi.getContestRegistrations)
const registerContest = vi.mocked(contestsApi.registerContest)
const auditContestRegistration = vi.mocked(contestsApi.auditContestRegistration)
const getContestLeaderboard = vi.mocked(contestsApi.getContestLeaderboard)
const submitContestCode = vi.mocked(contestsApi.submitContestCode)
const getContestResults = vi.mocked(contestsApi.getContestResults)
const updateContestResults = vi.mocked(contestsApi.updateContestResults)
const generateCertificates = vi.mocked(contestsApi.generateCertificates)
const downloadContestCertificates = vi.mocked(contestsApi.downloadContestCertificates)
const getProblems = vi.mocked(problemsApi.getProblems)
const getTeams = vi.mocked(teamsApi.getTeams)

const makeUser = (name: string, userId = 4): UserInfo => ({
  userId,
  studentId: `100${userId}`,
  name,
  roles: ['ASSISTANT_COACH'],
})

const contest: ContestInfo = {
  contestId: 1,
  name: '2027 年寒假校内热身赛',
  category: 'XCPC',
  startTime: '2027-01-15T09:00:00+08:00',
  endTime: '2027-01-15T14:00:00+08:00',
  registrationDeadline: '2099-06-30T23:59:59+08:00',
  description: '寒假集训前的热身赛。',
  maxTeamSize: 3,
  problemIds: [1],
  freezeMinutes: 60,
  status: 'ONGOING',
  creator: makeUser('周雨桐'),
  createTime: '2027-01-01T10:00:00+08:00',
}

const problem: ProblemInfo = {
  problemId: 1,
  title: '两数之和',
  ojProblemId: 'A',
  difficulty: 'EASY',
  timeLimit: 1000,
  memoryLimit: 256,
  description: '求两数之和。',
  inputFormat: '两个整数',
  outputFormat: '一个整数',
  sampleInput: '1 2',
  sampleOutput: '3',
  hint: null,
  tags: [],
  solution: null,
  visibility: 'PUBLIC',
}

const pendingRegistration: ContestRegistration = {
  applyId: 1,
  contestId: 1,
  applyType: 'SOLO',
  teamId: null,
  teamName: null,
  memberId: 2,
  memberName: '李知远',
  studentId: '2024120002',
  grade: '2024级',
  memberCount: 1,
  applyTime: '2027-01-02T10:00:00+08:00',
  status: 'PENDING',
  remark: '希望热身。',
}

const approvedRegistration: ContestRegistration = {
  ...pendingRegistration,
  status: 'APPROVED',
}

const contestResult: ContestResult = {
  resultId: 1,
  contestId: 1,
  memberId: 2,
  memberName: '李知远',
  studentId: '2024120002',
  teamName: null,
  rank: 1,
  award: '一等奖',
  score: 300,
  remark: '全场唯一 AK。',
}

const leaderboard: LeaderboardResult = {
  frozen: false,
  entries: [
    {
      rank: 1,
      userId: 2,
      userName: '李知远',
      teamId: null,
      teamName: null,
      solvedCount: 3,
      penalty: 120,
    },
  ],
}

const certificateBundle: CertificateBundle = {
  contestId: 1,
  total: 2,
  generatedTime: '2027-01-16T10:00:00+08:00',
  certificates: [
    { certId: 1, memberId: 2, memberName: '李知远', teamName: null, award: '一等奖', certNo: 'CERT-1-001' },
    { certId: 2, memberId: 3, memberName: '陈昊', teamName: '算法之光', award: '二等奖', certNo: 'CERT-1-002' },
  ],
}

const makeTeam = (overrides: Partial<TeamInfo> = {}): TeamInfo => ({
  teamId: 1,
  name: '算法之光',
  description: '专注图论。',
  status: 'CONFIRMED',
  leader: makeUser('陈昊', 3),
  members: [],
  dimensions: [],
  createTime: '2026-12-01T10:00:00+08:00',
  ...overrides,
})

const makePage = <T,>(records: T[]): PageResult<T> => ({
  total: records.length,
  page: 1,
  size: 100,
  records,
})

/** 下拉替身：点击后回填成员 id，避免依赖 Element Plus 弹层在 jsdom 中的交互细节 */
const ElSelectStub = {
  name: 'ElSelect',
  props: { modelValue: { default: undefined }, placeholder: { type: String, default: '' } },
  emits: ['update:modelValue', 'change'],
  template: `<button type="button" class="el-select-stub" @click="pick">{{ placeholder || '下拉选择' }}</button>`,
  methods: {
    pick(this: { $emit: (event: string, payload: unknown) => void }) {
      this.$emit('update:modelValue', 2)
      this.$emit('change', 2)
    },
  },
}

let router: Router
let pinia: Pinia

/** 预置路由到 /contests/1 后再渲染，保证组件内 contestId 解析正确 */
async function renderView(role: RoleCode = 'MEMBER') {
  resetAuth()
  pinia = setupAuth(role).pinia
  router = createTestRouter()
  await router.push('/contests/1')
  return render(ContestDetailView, {
    global: {
      plugins: [pinia, router],
      directives: { permission },
      stubs: { ElSelect: ElSelectStub },
    },
  })
}

beforeEach(() => {
  vi.clearAllMocks()
  vi.spyOn(ElMessage, 'success').mockImplementation(() => ({ close: () => {} }) as never)
  vi.spyOn(ElMessage, 'warning').mockImplementation(() => ({ close: () => {} }) as never)
  vi.spyOn(ElMessage, 'error').mockImplementation(() => ({ close: () => {} }) as never)
  vi.spyOn(ElMessageBox, 'confirm').mockResolvedValue('confirm' as never)

  getContest.mockResolvedValue({ ...contest })
  getProblems.mockResolvedValue(makePage([problem]))
  getContestLeaderboard.mockResolvedValue(leaderboard)
  getMyContestRegistration.mockResolvedValue(null)
  getContestRegistrations.mockResolvedValue([pendingRegistration])
  registerContest.mockResolvedValue(undefined)
  auditContestRegistration.mockResolvedValue(undefined)
  submitContestCode.mockResolvedValue({ submitId: 1, result: 'JUDGING' })
  getContestResults.mockResolvedValue([contestResult])
  updateContestResults.mockResolvedValue(contestResult)
  generateCertificates.mockResolvedValue(certificateBundle)
  downloadContestCertificates.mockResolvedValue(undefined)
  getTeams.mockResolvedValue(makePage([makeTeam()]))
})

describe('ContestDetailView', () => {
  it('【赛事·赛事详情】渲染赛事名称、报名截止与题目列表', async () => {
    await renderView()

    await waitFor(() =>
      expect(screen.getByRole('heading', { name: '2027 年寒假校内热身赛' })).toBeInTheDocument(),
    )
    expect(getContest).toHaveBeenCalledWith(1)
    expect(screen.getAllByText(/报名截止/).length).toBeGreaterThan(0)
    expect(await screen.findByText('两数之和')).toBeInTheDocument()
    expect(screen.getByText('A')).toBeInTheDocument()
  })

  it('【赛事·实时榜单】点击「实时榜单」后展示封榜提示与排名', async () => {
    getContestLeaderboard.mockResolvedValue({ ...leaderboard, frozen: true })

    await renderView()

    await waitFor(() =>
      expect(screen.getByRole('heading', { name: '2027 年寒假校内热身赛' })).toBeInTheDocument(),
    )

    fireEvent.click(screen.getByText('实时榜单'))

    expect(await screen.findByText('榜单已封榜')).toBeInTheDocument()
    expect(getContestLeaderboard).toHaveBeenCalledWith(1)
    expect(screen.getByText('李知远')).toBeInTheDocument()
  })

  it('【赛事·队伍/个人报名】会员提交报名后调用 registerContest', async () => {
    await renderView('MEMBER')

    await waitFor(() =>
      expect(screen.getByRole('heading', { name: '2027 年寒假校内热身赛' })).toBeInTheDocument(),
    )

    fireEvent.click(await screen.findByRole('button', { name: '队伍 / 个人报名' }))

    fireEvent.click(await screen.findByRole('button', { name: '提交报名' }))

    await waitFor(() =>
      expect(registerContest).toHaveBeenCalledWith(
        1,
        expect.objectContaining({ applyType: 'SOLO' }),
      ),
    )
  })

  it('【赛事·报名列表 + 审核报名】助理教练查看报名列表并通过申请', async () => {
    await renderView('ASSISTANT_COACH')

    await waitFor(() =>
      expect(screen.getByRole('heading', { name: '2027 年寒假校内热身赛' })).toBeInTheDocument(),
    )

    fireEvent.click(screen.getByText(/报名审核/))

    await waitFor(() => expect(getContestRegistrations).toHaveBeenCalledWith(1))
    expect(await screen.findByText('李知远')).toBeInTheDocument()

    fireEvent.click(await screen.findByText('通过'))

    await waitFor(() =>
      expect(auditContestRegistration).toHaveBeenCalledWith(1, 1, 'APPROVE', ''),
    )
  })

  it('【赛事·赛后登记获奖/成绩】助理教练选择成员登记奖项后调用 updateContestResults', async () => {
    await renderView('ASSISTANT_COACH')

    await waitFor(() =>
      expect(screen.getByRole('heading', { name: '2027 年寒假校内热身赛' })).toBeInTheDocument(),
    )

    fireEvent.click(screen.getByText('成绩与证书'))
    await waitFor(() => expect(getContestResults).toHaveBeenCalledWith(1))

    fireEvent.click(screen.getByRole('button', { name: '登记成绩 / 获奖' }))
    fireEvent.click(await screen.findByText('选择成员'))
    fireEvent.click(screen.getByRole('button', { name: '保存登记' }))

    await waitFor(() => expect(updateContestResults).toHaveBeenCalledTimes(1))
    expect(updateContestResults).toHaveBeenCalledWith(1, expect.objectContaining({ memberId: 2 }))
  })

  it('【赛事·批量生成获奖电子证书】会长生成证书后调用 generateCertificates', async () => {
    await renderView('PRESIDENT')

    await waitFor(() =>
      expect(screen.getByRole('heading', { name: '2027 年寒假校内热身赛' })).toBeInTheDocument(),
    )

    fireEvent.click(screen.getByRole('button', { name: '批量生成证书' }))

    await waitFor(() => expect(generateCertificates).toHaveBeenCalledWith(1))
    expect(await screen.findByText('已生成 2 份证书')).toBeInTheDocument()
  })

  it('【赛事·比赛中提交代码】已通过报名的会员提交代码后调用 submitContestCode', async () => {
    getMyContestRegistration.mockResolvedValue(approvedRegistration)

    await renderView('MEMBER')

    await waitFor(() =>
      expect(screen.getByRole('heading', { name: '2027 年寒假校内热身赛' })).toBeInTheDocument(),
    )

    fireEvent.click(await screen.findByRole('button', { name: '提交代码' }))

    await fireEvent.update(
      await screen.findByPlaceholderText('在此粘贴源代码'),
      'int main() { return 0; }',
    )
    fireEvent.click(screen.getByRole('button', { name: '提交评测' }))

    await waitFor(() => expect(submitContestCode).toHaveBeenCalledTimes(1))
    expect(submitContestCode).toHaveBeenCalledWith(
      1,
      expect.objectContaining({ problemId: 1, code: 'int main() { return 0; }' }),
    )
  })

  it('【权限·3.2 赛事报名审核】普通会员不显示审核操作与证书入口', async () => {
    await renderView('MEMBER')

    await waitFor(() =>
      expect(screen.getByRole('heading', { name: '2027 年寒假校内热身赛' })).toBeInTheDocument(),
    )

    expect(screen.getByText('我的报名')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: '通过' })).toBeNull()
    expect(screen.queryByRole('button', { name: '驳回' })).toBeNull()
    expect(screen.queryByRole('button', { name: '批量生成证书' })).toBeNull()
    expect(screen.queryByRole('button', { name: '下载证书' })).toBeNull()
    expect(getContestRegistrations).not.toHaveBeenCalled()
  })

  it('【权限·证书生成】干事不显示报名审核与证书入口（审核为助理教练+，证书仅会长/副会长）', async () => {
    await renderView('OFFICER')

    await waitFor(() =>
      expect(screen.getByRole('heading', { name: '2027 年寒假校内热身赛' })).toBeInTheDocument(),
    )

    expect(screen.queryByText(/报名审核/)).toBeNull()
    expect(screen.queryByRole('button', { name: '批量生成证书' })).toBeNull()
    expect(screen.queryByRole('button', { name: '下载证书' })).toBeNull()
  })
})
