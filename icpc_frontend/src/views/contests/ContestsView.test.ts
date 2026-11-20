import { fireEvent, render, screen, waitFor } from '@testing-library/vue'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ElMessage } from 'element-plus'
import ContestsView from './ContestsView.vue'
import { permission } from '@/directives/permission'
import { createTestRouter, resetAuth, setupAuth } from '@/test/utils'
import type { ContestInfo, PageResult, ProblemInfo, RoleCode, UserInfo } from '@/types'

vi.mock('@/api/contests', () => ({
  getContests: vi.fn(),
  createContest: vi.fn(),
}))

vi.mock('@/api/problems', () => ({
  getProblems: vi.fn(),
}))

import * as contestsApi from '@/api/contests'
import * as problemsApi from '@/api/problems'

const getContests = vi.mocked(contestsApi.getContests)
const createContest = vi.mocked(contestsApi.createContest)
const getProblems = vi.mocked(problemsApi.getProblems)

const makeUser = (name: string, userId = 4): UserInfo => ({
  userId,
  studentId: `100${userId}`,
  name,
  roles: ['ASSISTANT_COACH'],
})

const makeContest = (overrides: Partial<ContestInfo> = {}): ContestInfo => ({
  contestId: 1,
  name: '中南民族大学第十届校内程序设计竞赛',
  category: 'SCHOLASTIC',
  startTime: '2026-11-15T09:00:00+08:00',
  endTime: '2026-11-15T14:00:00+08:00',
  registrationDeadline: '2026-11-10T23:59:59+08:00',
  description: '面向全校学生的程序设计竞赛。',
  maxTeamSize: 3,
  problemIds: [1, 2, 3],
  freezeMinutes: 60,
  status: 'PUBLISHED',
  creator: makeUser('张承宇'),
  createTime: '2026-11-01T10:00:00+08:00',
  ...overrides,
})

const makeProblem = (overrides: Partial<ProblemInfo> = {}): ProblemInfo => ({
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
  ...overrides,
})

const makePage = <T,>(records: T[]): PageResult<T> => ({
  total: records.length,
  page: 1,
  size: 9,
  records,
})

/** 时间选择器替身：点击后回填一个合法时间，避免依赖 Element Plus 弹层在 jsdom 中的交互细节 */
const ElDatePickerStub = {
  name: 'ElDatePicker',
  props: {
    modelValue: { default: undefined },
    placeholder: { type: String, default: '' },
  },
  emits: ['update:modelValue'],
  template: `<button type="button" class="el-date-picker-stub" @click="pick">{{ placeholder || '选择时间' }}</button>`,
  methods: {
    pick(this: { $emit: (event: string, payload: unknown) => void }) {
      this.$emit('update:modelValue', '2026-12-01T09:00:00+08:00')
    },
  },
}

let router: ReturnType<typeof createTestRouter>

function renderView(role: RoleCode = 'ASSISTANT_COACH') {
  resetAuth()
  const { pinia } = setupAuth(role)
  router = createTestRouter()
  return render(ContestsView, {
    global: {
      plugins: [pinia, router],
      directives: { permission },
      stubs: { ElDatePicker: ElDatePickerStub },
    },
  })
}

describe('ContestsView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.spyOn(ElMessage, 'success').mockImplementation(() => ({ close: () => {} }) as never)
    vi.spyOn(ElMessage, 'warning').mockImplementation(() => ({ close: () => {} }) as never)
    vi.spyOn(ElMessage, 'error').mockImplementation(() => ({ close: () => {} }) as never)

    getContests.mockResolvedValue(makePage([makeContest()]))
    getProblems.mockResolvedValue(makePage([makeProblem()]))
  })

  it('【赛事·查询赛事列表】渲染赛事名称、分类、状态与报名截止', async () => {
    getContests.mockResolvedValue(
      makePage([
        makeContest(),
        makeContest({
          contestId: 2,
          name: '2026 XCPC 网络预选赛',
          category: 'XCPC',
          status: 'ONGOING',
        }),
      ]),
    )

    renderView()

    await waitFor(() =>
      expect(screen.getByText('中南民族大学第十届校内程序设计竞赛')).toBeInTheDocument(),
    )
    expect(screen.getByText('2026 XCPC 网络预选赛')).toBeInTheDocument()
    expect(screen.getAllByText('校赛').length).toBeGreaterThan(0)
    expect(screen.getAllByText('XCPC 系列').length).toBeGreaterThan(0)
    expect(screen.getByText('已发布')).toBeInTheDocument()
    expect(screen.getByText('进行中')).toBeInTheDocument()
    expect(screen.getAllByText(/报名截止：2026-11-10/).length).toBeGreaterThan(0)
    expect(getContests).toHaveBeenCalledWith(expect.objectContaining({ page: 1, size: 9 }))
  })

  it('【赛事·查询赛事列表】按分类筛选：切换「XCPC 系列」后按 XCPC 查询', async () => {
    renderView()

    await waitFor(() =>
      expect(screen.getByText('中南民族大学第十届校内程序设计竞赛')).toBeInTheDocument(),
    )

    fireEvent.click(screen.getByRole('button', { name: 'XCPC 系列' }))

    await waitFor(() =>
      expect(getContests).toHaveBeenLastCalledWith(
        expect.objectContaining({ category: 'XCPC', page: 1 }),
      ),
    )
  })

  it('【赛事·创建/发布赛事】助理教练填写信息后调用 createContest', async () => {
    createContest.mockResolvedValue(makeContest({ contestId: 9 }))

    renderView('ASSISTANT_COACH')

    await waitFor(() =>
      expect(screen.getByText('中南民族大学第十届校内程序设计竞赛')).toBeInTheDocument(),
    )

    fireEvent.click(screen.getByRole('button', { name: '创建赛事' }))

    await fireEvent.update(
      await screen.findByPlaceholderText('如 中南民族大学第十届校内程序设计竞赛'),
      '2027 年新生程序设计选拔赛',
    )
    fireEvent.click(screen.getByText('选择开始时间'))
    fireEvent.click(screen.getByText('选择结束时间'))
    fireEvent.click(screen.getByText('选择报名截止时间'))

    fireEvent.click(screen.getByRole('button', { name: '发布赛事' }))

    await waitFor(() => expect(createContest).toHaveBeenCalledTimes(1))
    expect(createContest).toHaveBeenCalledWith(
      expect.objectContaining({
        name: '2027 年新生程序设计选拔赛',
        category: 'SCHOLASTIC',
        startTime: '2026-12-01T09:00:00+08:00',
        registrationDeadline: '2026-12-01T09:00:00+08:00',
      }),
    )
  })

  it('【权限·3.2 创建/发布赛事】普通会员不显示「创建赛事」按钮', async () => {
    renderView('MEMBER')

    await waitFor(() =>
      expect(screen.getByText('中南民族大学第十届校内程序设计竞赛')).toBeInTheDocument(),
    )

    expect(screen.queryByRole('button', { name: '创建赛事' })).toBeNull()
  })
})
