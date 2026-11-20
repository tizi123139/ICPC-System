import { fireEvent, render, screen, waitFor } from '@testing-library/vue'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ElMessage } from 'element-plus'
import type { Pinia } from 'pinia'
import type { Router } from 'vue-router'
import TrainingDetailView from './TrainingDetailView.vue'
import { createTestRouter, resetAuth, setupAuth } from '@/test/utils'
import type {
  LeaderboardResult,
  PageResult,
  ProblemInfo,
  SubmitRecord,
  TrainingInfo,
  UserInfo,
} from '@/types'

vi.mock('@/api/trainings', () => ({
  getTraining: vi.fn(),
  getSubmissions: vi.fn(),
  getLeaderboard: vi.fn(),
  submitCode: vi.fn(),
  publishTraining: vi.fn(),
}))

vi.mock('@/api/problems', () => ({
  getProblems: vi.fn(),
}))

import * as trainingsApi from '@/api/trainings'
import * as problemsApi from '@/api/problems'

const getTraining = vi.mocked(trainingsApi.getTraining)
const getSubmissions = vi.mocked(trainingsApi.getSubmissions)
const getLeaderboard = vi.mocked(trainingsApi.getLeaderboard)
const submitCode = vi.mocked(trainingsApi.submitCode)
const getProblems = vi.mocked(problemsApi.getProblems)

const makeUser = (name: string, userId = 1): UserInfo => ({
  userId,
  studentId: `100${userId}`,
  name,
  roles: ['MEMBER'],
})

const training: TrainingInfo = {
  trainingId: 1,
  name: '第十周队内训练赛',
  type: 'TEAM',
  startTime: '2026-11-15T19:00:00+08:00',
  endTime: '2026-11-15T22:00:00+08:00',
  leaderboardVisible: true,
  freezeTime: 30,
  problemIds: [1, 2],
  status: 'ONGOING',
  creator: makeUser('张老师', 2),
  createTime: '2026-11-01T10:00:00+08:00',
}

const problemList: ProblemInfo[] = [
  {
    problemId: 1,
    title: 'A + B Problem',
    ojProblemId: 'P1001',
    difficulty: 'EASY',
    timeLimit: 1000,
    memoryLimit: 262144,
    description: '',
    inputFormat: '',
    outputFormat: '',
    sampleInput: '',
    sampleOutput: '',
    hint: null,
    tags: [{ tagId: 1, name: '模拟', category: '基础算法' }],
    solution: null,
    visibility: 'PUBLIC',
  },
  {
    problemId: 2,
    title: '区间求和',
    ojProblemId: 'P1002',
    difficulty: 'MEDIUM',
    timeLimit: 1000,
    memoryLimit: 262144,
    description: '',
    inputFormat: '',
    outputFormat: '',
    sampleInput: '',
    sampleOutput: '',
    hint: null,
    tags: [{ tagId: 2, name: '前缀和', category: '基础算法' }],
    solution: null,
    visibility: 'PUBLIC',
  },
]

const submissions: SubmitRecord[] = [
  {
    submitId: 11,
    trainingId: 1,
    problemId: 1,
    submitter: makeUser('小明', 3),
    language: 'C++17',
    result: 'AC',
    timeUsed: 120,
    memoryUsed: 20480,
    submitTime: '2026-11-15T19:20:00+08:00',
  },
]

const leaderboard: LeaderboardResult = {
  frozen: true,
  entries: [
    {
      rank: 1,
      userId: 3,
      userName: '小明',
      teamId: 1,
      teamName: 'A 队',
      solvedCount: 5,
      penalty: 100,
    },
  ],
}

const makePage = <T,>(records: T[]): PageResult<T> => ({
  total: records.length,
  page: 1,
  size: 100,
  records,
})

let router: Router
let pinia: Pinia

function renderView() {
  return render(TrainingDetailView, {
    global: {
      plugins: [pinia, router],
    },
  })
}

describe('TrainingDetailView', () => {
  beforeEach(async () => {
    resetAuth()
    vi.clearAllMocks()
    const auth = setupAuth('PRESIDENT')
    pinia = auth.pinia
    router = createTestRouter()
    await router.push('/trainings/1')

    vi.spyOn(ElMessage, 'success').mockImplementation(() => ({ close: () => {} }) as never)
    vi.spyOn(ElMessage, 'error').mockImplementation(() => ({ close: () => {} }) as never)

    getTraining.mockResolvedValue(training)
    getProblems.mockResolvedValue(makePage(problemList))
    getSubmissions.mockResolvedValue(submissions)
    getLeaderboard.mockResolvedValue(leaderboard)
    submitCode.mockResolvedValue({ submitId: 12, result: 'JUDGING' })
  })

  it('【清单 29】获取训练详情：渲染训练名称与题目列表', async () => {
    renderView()

    await waitFor(() =>
      expect(screen.getByRole('heading', { name: '第十周队内训练赛' })).toBeInTheDocument(),
    )
    expect(getTraining).toHaveBeenCalledWith(1)
    await waitFor(() => expect(screen.getByText('A + B Problem')).toBeInTheDocument())
    expect(screen.getByText('区间求和')).toBeInTheDocument()
    expect(screen.getByText(/创建人 张老师/)).toBeInTheDocument()
  })

  it('【清单 25】提交代码：打开弹窗、填写代码后调用 submitCode', async () => {
    // 权限矩阵 v2：正式会员可参加日常训练并提交代码
    pinia = setupAuth('MEMBER').pinia
    renderView()

    await waitFor(() => expect(screen.getByText('A + B Problem')).toBeInTheDocument())

    fireEvent.click(screen.getAllByRole('button', { name: '提交代码' })[0]!)

    const codeInput = await screen.findByPlaceholderText('// 在此粘贴源代码')
    await fireEvent.update(codeInput, 'int main() { return 0; }')

    fireEvent.click(screen.getByRole('button', { name: '提交评测' }))

    await waitFor(() =>
      expect(submitCode).toHaveBeenCalledWith(
        1,
        expect.objectContaining({
          problemId: 1,
          language: 'C++17',
          code: 'int main() { return 0; }',
        }),
      ),
    )
  })

  it('【清单 24】查询提交记录：切换提交记录 tab 后渲染记录行', async () => {
    renderView()

    await waitFor(() => expect(screen.getByText('A + B Problem')).toBeInTheDocument())

    fireEvent.click(screen.getByText('提交记录'))

    await waitFor(() => expect(getSubmissions).toHaveBeenCalledWith(1))
    expect(screen.getByText('小明')).toBeInTheDocument()
    expect(screen.getByText('C++17')).toBeInTheDocument()
    expect(screen.getByText('AC')).toBeInTheDocument()
  })

  it('【清单 23】实时榜单：切换榜单 tab 后渲染排名与队伍，封榜时展示提示', async () => {
    renderView()

    await waitFor(() => expect(screen.getByText('A + B Problem')).toBeInTheDocument())

    fireEvent.click(screen.getByText('实时榜单'))

    await waitFor(() => expect(getLeaderboard).toHaveBeenCalledWith(1))
    expect(screen.getByText('A 队')).toBeInTheDocument()
    expect(screen.getByText('5')).toBeInTheDocument()
    const ranks = document.querySelectorAll('.rank')
    expect(ranks[0]).toHaveTextContent('1')
    expect(screen.getByText('榜单已封榜')).toBeInTheDocument()
  })
})
