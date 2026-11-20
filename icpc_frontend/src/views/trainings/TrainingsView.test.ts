import { fireEvent, render, screen, waitFor } from '@testing-library/vue'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { Component } from 'vue'
import type { Router } from 'vue-router'
import TrainingsView from './TrainingsView.vue'
import { permission } from '@/directives/permission'
import { createTestRouter, resetAuth, setupAuth } from '@/test/utils'
import type { PageResult, ProblemInfo, TrainingInfo, UserInfo } from '@/types'

vi.mock('@/api/trainings', () => ({
  getTrainings: vi.fn(),
  createTraining: vi.fn(),
  updateTraining: vi.fn(),
  publishTraining: vi.fn(),
  deleteTraining: vi.fn(),
}))

vi.mock('@/api/problems', () => ({
  getProblems: vi.fn(),
}))

import * as trainingsApi from '@/api/trainings'
import * as problemsApi from '@/api/problems'

const getTrainings = vi.mocked(trainingsApi.getTrainings)
const createTraining = vi.mocked(trainingsApi.createTraining)
const updateTraining = vi.mocked(trainingsApi.updateTraining)
const publishTraining = vi.mocked(trainingsApi.publishTraining)
const deleteTraining = vi.mocked(trainingsApi.deleteTraining)
const getProblems = vi.mocked(problemsApi.getProblems)

const makeUser = (name: string, userId = 1): UserInfo => ({
  userId,
  studentId: `100${userId}`,
  name,
  roles: ['PRESIDENT'],
})

const makeTraining = (overrides: Partial<TrainingInfo> = {}): TrainingInfo => ({
  trainingId: 1,
  name: '第十周队内训练赛',
  type: 'SOLO',
  startTime: '2026-11-15T19:00:00+08:00',
  endTime: '2026-11-15T22:00:00+08:00',
  leaderboardVisible: true,
  freezeTime: 30,
  problemIds: [1, 2, 3],
  status: 'DRAFT',
  creator: makeUser('张老师'),
  createTime: '2026-11-01T10:00:00+08:00',
  ...overrides,
})

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
    tags: [],
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
    tags: [],
    solution: null,
    visibility: 'PUBLIC',
  },
]

const makePage = <T,>(records: T[]): PageResult<T> => ({
  total: records.length,
  page: 1,
  size: 9,
  records,
})

/** 创建弹窗里的下拉 / 时间选择器替身，避免依赖 Element Plus 弹层在 jsdom 中的交互细节 */
const ElSelectStub = {
  name: 'ElSelect',
  props: {
    modelValue: { type: [Array, String, Number], default: undefined },
    placeholder: { type: String, default: '' },
  },
  emits: ['update:modelValue', 'change'],
  template: `<button type="button" class="el-select-stub" @click="pick">{{ placeholder || '下拉选择' }}</button>`,
  methods: {
    pick(this: { modelValue: unknown; $emit: (event: string, payload: unknown) => void }) {
      const value = Array.isArray(this.modelValue) ? [1] : 'SOLO'
      this.$emit('update:modelValue', value)
      this.$emit('change', value)
    },
  },
}

const ElDatePickerStub = {
  name: 'ElDatePicker',
  props: {
    modelValue: { default: undefined },
    startPlaceholder: { type: String, default: '' },
  },
  emits: ['update:modelValue'],
  template: `<button type="button" class="el-date-picker-stub" @click="pick">{{ startPlaceholder || '选择时间' }}</button>`,
  methods: {
    pick(this: { $emit: (event: string, payload: unknown) => void }) {
      this.$emit('update:modelValue', ['2026-11-15T19:00:00', '2026-11-15T22:00:00'])
    },
  },
}

let router: Router

function renderView(stubs?: Record<string, Component>) {
  return render(TrainingsView, {
    global: {
      plugins: [setupAuth('PRESIDENT').pinia, router],
      directives: { permission },
      stubs,
    },
  })
}

describe('TrainingsView', () => {
  beforeEach(() => {
    resetAuth()
    vi.clearAllMocks()
    setupAuth('PRESIDENT')
    router = createTestRouter()

    vi.spyOn(ElMessage, 'success').mockImplementation(() => ({ close: () => {} }) as never)
    vi.spyOn(ElMessage, 'warning').mockImplementation(() => ({ close: () => {} }) as never)
    vi.spyOn(ElMessage, 'error').mockImplementation(() => ({ close: () => {} }) as never)
    vi.spyOn(ElMessageBox, 'confirm').mockResolvedValue('confirm' as never)

    getProblems.mockResolvedValue(makePage(problemList))
    getTrainings.mockResolvedValue(makePage([makeTraining()]))
  })

  it('【清单 31】分页查询训练列表：渲染训练名称、题目数与创建人', async () => {
    getTrainings.mockResolvedValue(
      makePage([
        makeTraining(),
        makeTraining({
          trainingId: 2,
          name: '第三场组队赛',
          type: 'TEAM',
          leaderboardVisible: false,
          problemIds: [1, 2],
          status: 'PUBLISHED',
          creator: makeUser('李老师', 2),
        }),
      ]),
    )

    renderView()

    await waitFor(() => expect(screen.getByText('第十周队内训练赛')).toBeInTheDocument())
    expect(screen.getByText('第三场组队赛')).toBeInTheDocument()
    expect(screen.getByText(/3 道题目 · 公开榜单/)).toBeInTheDocument()
    expect(screen.getByText(/2 道题目 · 不公开榜单/)).toBeInTheDocument()
    expect(screen.getByText('创建人：张老师')).toBeInTheDocument()
    expect(screen.getByText('创建人：李老师')).toBeInTheDocument()
    expect(getTrainings).toHaveBeenCalledWith(expect.objectContaining({ page: 1, size: 9 }))
  })

  it('【清单 31】按状态、类型和关键字筛选训练列表', async () => {
    renderView()

    await waitFor(() => expect(screen.getByText('第十周队内训练赛')).toBeInTheDocument())

    // 状态 tab
    fireEvent.click(screen.getByRole('button', { name: '草稿' }))
    await waitFor(() =>
      expect(getTrainings).toHaveBeenLastCalledWith(
        expect.objectContaining({ status: 'DRAFT', page: 1 }),
      ),
    )

    // 类型下拉
    fireEvent.click(screen.getByText('训练类型'))
    const teamOption = await screen.findByText('组队训练')
    fireEvent.click(teamOption)
    await waitFor(() =>
      expect(getTrainings).toHaveBeenLastCalledWith(
        expect.objectContaining({ status: 'DRAFT', type: 'TEAM' }),
      ),
    )

    // 关键字搜索
    const keywordInput = screen.getByPlaceholderText('搜索训练名称')
    await fireEvent.update(keywordInput, '第十周')
    await fireEvent.keyUp(keywordInput, { key: 'Enter', code: 'Enter' })
    await waitFor(() =>
      expect(getTrainings).toHaveBeenLastCalledWith(
        expect.objectContaining({ status: 'DRAFT', type: 'TEAM', keyword: '第十周' }),
      ),
    )
  })

  it('【清单 30】创建训练：填写名称、题目与时间后调用 createTraining', async () => {
    createTraining.mockResolvedValue(makeTraining({ trainingId: 99, name: '新建训练' }))

    renderView({ ElSelect: ElSelectStub, ElDatePicker: ElDatePickerStub })

    await waitFor(() => expect(screen.getByText('第十周队内训练赛')).toBeInTheDocument())

    fireEvent.click(screen.getByRole('button', { name: '创建训练' }))

    const nameInput = await screen.findByPlaceholderText('如 第十周队内训练赛')
    await fireEvent.update(nameInput, '新建训练')

    fireEvent.click(screen.getByText('从题库中选择训练题目'))
    fireEvent.click(screen.getByText('开始时间'))

    const submitButtons = screen.getAllByRole('button', { name: '创建训练' })
    fireEvent.click(submitButtons[submitButtons.length - 1]!)

    await waitFor(() => expect(createTraining).toHaveBeenCalledTimes(1))
    expect(createTraining).toHaveBeenCalledWith(
      expect.objectContaining({
        name: '新建训练',
        problemIds: [1],
        startTime: '2026-11-15T19:00:00+08:00',
        endTime: '2026-11-15T22:00:00+08:00',
      }),
    )
  })

  it('【清单 28】修改训练：草稿卡片编辑回填并保存调用 updateTraining', async () => {
    const draft = makeTraining({ name: '待修改训练' })
    getTrainings.mockResolvedValue(makePage([draft]))
    updateTraining.mockResolvedValue(undefined)

    renderView()

    await waitFor(() => expect(screen.getByText('待修改训练')).toBeInTheDocument())

    fireEvent.click(screen.getByRole('button', { name: '编辑' }))

    const nameInput = await screen.findByPlaceholderText('如 第十周队内训练赛')
    expect(nameInput).toHaveValue('待修改训练')

    await fireEvent.update(nameInput, '修改后的训练')
    fireEvent.click(screen.getByRole('button', { name: '保存修改' }))

    await waitFor(() =>
      expect(updateTraining).toHaveBeenCalledWith(
        draft.trainingId,
        expect.objectContaining({ name: '修改后的训练' }),
      ),
    )
  })

  it('【清单 26】发布训练：点击发布训练后调用 publishTraining', async () => {
    const draft = makeTraining()
    getTrainings.mockResolvedValue(makePage([draft]))
    publishTraining.mockResolvedValue(undefined)

    renderView()

    await waitFor(() => expect(screen.getByText(draft.name)).toBeInTheDocument())

    fireEvent.click(screen.getByRole('button', { name: '发布训练' }))

    await waitFor(() => expect(publishTraining).toHaveBeenCalledWith(draft.trainingId))
  })

  it('【清单 27】删除训练：确认后调用 deleteTraining', async () => {
    const draft = makeTraining()
    getTrainings.mockResolvedValue(makePage([draft]))
    deleteTraining.mockResolvedValue(undefined)

    renderView()

    await waitFor(() => expect(screen.getByText(draft.name)).toBeInTheDocument())

    fireEvent.click(screen.getByRole('button', { name: '删除' }))

    await waitFor(() => expect(deleteTraining).toHaveBeenCalledWith(draft.trainingId))
  })
})
