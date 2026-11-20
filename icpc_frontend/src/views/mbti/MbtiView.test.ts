import { fireEvent, render, screen, waitFor } from '@testing-library/vue'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ElMessage } from 'element-plus'
import MbtiView from './MbtiView.vue'
import { permission } from '@/directives/permission'
import { createTestRouter, resetAuth, setupAuth } from '@/test/utils'
import type { MemberInfo, MbtiQuestion, MbtiResult, PageResult, RoleCode, UserInfo } from '@/types'

vi.mock('@/api/mbti', () => ({
  getMbtiQuestions: vi.fn(),
  submitMbti: vi.fn(),
  getMbtiResult: vi.fn(),
}))

vi.mock('@/api/members', () => ({
  getMembers: vi.fn(),
}))

import * as mbtiApi from '@/api/mbti'
import * as membersApi from '@/api/members'

const getMbtiQuestions = vi.mocked(mbtiApi.getMbtiQuestions)
const submitMbti = vi.mocked(mbtiApi.submitMbti)
const getMbtiResult = vi.mocked(mbtiApi.getMbtiResult)
const getMembers = vi.mocked(membersApi.getMembers)

const makeUser = (name: string, userId = 1): UserInfo => ({
  userId,
  studentId: `100${userId}`,
  name,
  roles: ['MEMBER'],
})

const makeMember = (memberId: number, name: string): MemberInfo => ({
  memberId,
  name,
  studentId: `20241200${memberId}`,
  major: '计算机科学与技术',
  grade: '2024级',
  phone: '13800000000',
  email: 'member@example.com',
  memberType: 'MEMBER',
  department: '算法部',
  joinDate: '2024-09-01',
  status: 'ACTIVE',
  createTime: '2024-09-01T10:00:00+08:00',
})

const questions: MbtiQuestion[] = [
  {
    questionId: 1,
    dimension: 'EI',
    content: '在人群聚会中，你更倾向于：',
    options: [
      { value: 'A', label: '主动与陌生同学交谈', pole: 'E' },
      { value: 'B', label: '和熟悉的队友待在一起', pole: 'I' },
    ],
  },
  {
    questionId: 2,
    dimension: 'SN',
    content: '面对一道新题，你更关注：',
    options: [
      { value: 'A', label: '具体的样例与实现细节', pole: 'S' },
      { value: 'B', label: '题目背后的整体思路', pole: 'N' },
    ],
  },
  {
    questionId: 3,
    dimension: 'TF',
    content: '队伍出现分歧时，你更看重：',
    options: [
      { value: 'A', label: '逻辑与最优方案', pole: 'T' },
      { value: 'B', label: '队友的感受与氛围', pole: 'F' },
    ],
  },
  {
    questionId: 4,
    dimension: 'JP',
    content: '备赛时你更习惯：',
    options: [
      { value: 'A', label: '制定计划并按计划推进', pole: 'J' },
      { value: 'B', label: '保持灵活随机应变', pole: 'P' },
    ],
  },
]

const makeResult = (overrides: Partial<MbtiResult> = {}): MbtiResult => ({
  userId: 1,
  userName: '测试用户',
  type: 'ENTP',
  typeName: '辩论家',
  dimensions: [
    { dimension: 'EI', leftLabel: '外向 E', rightLabel: '内向 I', leftPercent: 70, preference: 'left' },
    { dimension: 'SN', leftLabel: '实感 S', rightLabel: '直觉 N', leftPercent: 40, preference: 'right' },
    { dimension: 'TF', leftLabel: '思考 T', rightLabel: '情感 F', leftPercent: 60, preference: 'left' },
    { dimension: 'JP', leftLabel: '判断 J', rightLabel: '知觉 P', leftPercent: 30, preference: 'right' },
  ],
  description: '你思维敏捷，善于从多角度分析问题。',
  suggestions: ['在讨论中多倾听他人观点', '注意为计划预留收尾时间'],
  submitTime: '2026-11-01T10:00:00+08:00',
  ...overrides,
})

const makePage = <T,>(records: T[]): PageResult<T> => ({
  total: records.length,
  page: 1,
  size: 100,
  records,
})

/** 会员下拉替身：点击后回填一个会员 id */
const ElSelectStub = {
  name: 'ElSelect',
  props: { modelValue: { default: undefined }, placeholder: { type: String, default: '' } },
  emits: ['update:modelValue', 'change'],
  template: `<button type="button" class="el-select-stub" @click="pick">选择会员</button>`,
  methods: {
    pick(this: { $emit: (event: string, payload: unknown) => void }) {
      this.$emit('update:modelValue', 2)
      this.$emit('change', 2)
    },
  },
}

function renderView(role: RoleCode = 'MEMBER') {
  resetAuth()
  const { pinia } = setupAuth(role)
  const router = createTestRouter()
  return render(MbtiView, {
    global: {
      plugins: [pinia, router],
      directives: { permission },
      stubs: { ElSelect: ElSelectStub },
    },
  })
}

describe('MbtiView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.spyOn(ElMessage, 'success').mockImplementation(() => ({ close: () => {} }) as never)
    vi.spyOn(ElMessage, 'warning').mockImplementation(() => ({ close: () => {} }) as never)
    vi.spyOn(ElMessage, 'error').mockImplementation(() => ({ close: () => {} }) as never)

    getMbtiQuestions.mockResolvedValue([...questions])
    submitMbti.mockResolvedValue(makeResult())
    // 默认：本人尚未测评 → 进入作答
    getMbtiResult.mockRejectedValue(new Error('该会员尚未完成 MBTI 测评'))
    getMembers.mockResolvedValue(makePage([makeMember(2, '李知远')]))
  })

  it('【MBTI·获取测评题目列表】尚未测评时展示全部题目', async () => {
    renderView('MEMBER')

    await waitFor(() => expect(getMbtiQuestions).toHaveBeenCalledTimes(1))
    expect(await screen.findByText('在人群聚会中，你更倾向于：')).toBeInTheDocument()
    expect(screen.getByText('面对一道新题，你更关注：')).toBeInTheDocument()
    expect(screen.getByText('队伍出现分歧时，你更看重：')).toBeInTheDocument()
    expect(screen.getByText('备赛时你更习惯：')).toBeInTheDocument()
    expect(screen.getAllByText(/^Q\d$/).length).toBe(questions.length)
    expect(screen.getByRole('button', { name: '提交测评' })).toBeDisabled()
  })

  it('【MBTI·提交答案返回测评结果】完成全部作答后提交并展示人格类型', async () => {
    const { container } = renderView('MEMBER')

    await waitFor(() => expect(getMbtiQuestions).toHaveBeenCalledTimes(1))
    await screen.findByText('在人群聚会中，你更倾向于：')

    for (const option of container.querySelectorAll('.option')) {
      fireEvent.click(option)
    }

    const submitButton = screen.getByRole('button', { name: '提交测评' })
    await waitFor(() => expect(submitButton).not.toBeDisabled())
    fireEvent.click(submitButton)

    await waitFor(() => expect(submitMbti).toHaveBeenCalledTimes(1))
    expect(submitMbti.mock.calls[0]![0]).toHaveLength(questions.length)
    expect(submitMbti.mock.calls[0]![0][0]).toEqual({ questionId: 1, value: 'B' })

    expect(await screen.findByText('ENTP')).toBeInTheDocument()
    expect(screen.getByText(/辩论家/)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: '重新测评' })).toBeInTheDocument()
  })

  it('【MBTI·查看某会员测评结果】助理教练选择会员后查看其结果', async () => {
    getMbtiResult.mockImplementation((id) =>
      id === 'me'
        ? Promise.reject(new Error('该会员尚未完成 MBTI 测评'))
        : Promise.resolve(makeResult({ userId: 2, userName: '李知远' })),
    )

    renderView('ASSISTANT_COACH')

    await waitFor(() => expect(getMembers).toHaveBeenCalledTimes(1))

    fireEvent.click(await screen.findByText('选择会员'))
    fireEvent.click(screen.getByRole('button', { name: '查看测评结果' }))

    await waitFor(() => expect(getMbtiResult).toHaveBeenCalledWith(2))
    expect(await screen.findByText('ENTP')).toBeInTheDocument()
    expect(screen.getByText(/李知远/)).toBeInTheDocument()
  })

  it('【权限·MBTI 结果查看】普通会员不显示会员下拉与「查看测评结果」按钮', async () => {
    renderView('MEMBER')

    await waitFor(() => expect(getMbtiQuestions).toHaveBeenCalledTimes(1))

    expect(screen.queryByRole('button', { name: '查看测评结果' })).toBeNull()
    expect(screen.queryByText('选择会员')).toBeNull()
    expect(getMembers).not.toHaveBeenCalled()
  })
})
