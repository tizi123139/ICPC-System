import { fireEvent, render, screen, waitFor } from '@testing-library/vue'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ElMessage } from 'element-plus'
import CampsView from './CampsView.vue'
import { permission } from '@/directives/permission'
import { createTestRouter, resetAuth, setupAuth } from '@/test/utils'
import type { CampInfo, PageResult, RoleCode, UserInfo } from '@/types'

vi.mock('@/api/camps', () => ({
  getCamps: vi.fn(),
  createCamp: vi.fn(),
}))

import * as campsApi from '@/api/camps'

const getCamps = vi.mocked(campsApi.getCamps)
const createCamp = vi.mocked(campsApi.createCamp)

const makeUser = (name: string, userId = 4): UserInfo => ({
  userId,
  studentId: `100${userId}`,
  name,
  roles: ['ASSISTANT_COACH'],
})

const makeCamp = (overrides: Partial<CampInfo> = {}): CampInfo => ({
  campId: 1,
  name: '2027 年寒假集训',
  type: 'WINTER',
  startDate: '2027-01-10',
  endDate: '2027-01-24',
  registrationDeadline: '2027-01-05T23:59:59+08:00',
  location: '计算机学院实验楼 A301',
  description: '为期两周的寒假集中训练。',
  creator: makeUser('周雨桐'),
  createTime: '2026-12-15T10:00:00+08:00',
  ...overrides,
})

const makePage = <T,>(records: T[]): PageResult<T> => ({
  total: records.length,
  page: 1,
  size: 9,
  records,
})

/** 时间选择器替身：daterange 回填区间，datetime 回填单个时间 */
const ElDatePickerStub = {
  name: 'ElDatePicker',
  props: {
    modelValue: { default: undefined },
    type: { type: String, default: 'date' },
    startPlaceholder: { type: String, default: '' },
    placeholder: { type: String, default: '' },
  },
  emits: ['update:modelValue'],
  template: `<button type="button" class="el-date-picker-stub" @click="pick">{{ startPlaceholder || placeholder || '选择时间' }}</button>`,
  methods: {
    pick(this: { type: string; $emit: (event: string, payload: unknown) => void }) {
      this.$emit(
        'update:modelValue',
        this.type === 'daterange'
          ? ['2027-01-10', '2027-01-24']
          : '2027-01-05T23:59:59+08:00',
      )
    },
  },
}

let router: ReturnType<typeof createTestRouter>

function renderView(role: RoleCode = 'ASSISTANT_COACH') {
  resetAuth()
  const { pinia } = setupAuth(role)
  router = createTestRouter()
  return render(CampsView, {
    global: {
      plugins: [pinia, router],
      directives: { permission },
      stubs: { ElDatePicker: ElDatePickerStub },
    },
  })
}

describe('CampsView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.spyOn(ElMessage, 'success').mockImplementation(() => ({ close: () => {} }) as never)
    vi.spyOn(ElMessage, 'warning').mockImplementation(() => ({ close: () => {} }) as never)
    vi.spyOn(ElMessage, 'error').mockImplementation(() => ({ close: () => {} }) as never)

    getCamps.mockResolvedValue(makePage([makeCamp()]))
  })

  it('【集训·查询集训列表】渲染集训名称、类型、地点与报名截止', async () => {
    getCamps.mockResolvedValue(
      makePage([
        makeCamp(),
        makeCamp({
          campId: 2,
          name: '2026 年暑假集训',
          type: 'SUMMER',
          startDate: '2026-07-12',
          endDate: '2026-07-26',
          location: '计算机学院实验楼 A305',
        }),
      ]),
    )

    renderView()

    await waitFor(() => expect(screen.getByText('2027 年寒假集训')).toBeInTheDocument())
    expect(screen.getByText('2026 年暑假集训')).toBeInTheDocument()
    expect(screen.getAllByText('寒假集训').length).toBeGreaterThan(0)
    expect(screen.getAllByText('暑假集训').length).toBeGreaterThan(0)
    expect(screen.getByText('计算机学院实验楼 A301')).toBeInTheDocument()
    expect(screen.getAllByText(/报名截止：2027-01-05/).length).toBeGreaterThan(0)
    expect(getCamps).toHaveBeenCalledWith(expect.objectContaining({ page: 1, size: 9 }))
  })

  it('【集训·查询集训列表】按类型筛选：切换「暑假集训」后按 SUMMER 查询', async () => {
    renderView()

    await waitFor(() => expect(screen.getByText('2027 年寒假集训')).toBeInTheDocument())

    fireEvent.click(screen.getByRole('button', { name: '暑假集训' }))

    await waitFor(() =>
      expect(getCamps).toHaveBeenLastCalledWith(
        expect.objectContaining({ type: 'SUMMER', page: 1 }),
      ),
    )
  })

  it('【集训·发布集训通知】助理教练填写信息后调用 createCamp', async () => {
    createCamp.mockResolvedValue(makeCamp({ campId: 9 }))

    renderView('ASSISTANT_COACH')

    await waitFor(() => expect(screen.getByText('2027 年寒假集训')).toBeInTheDocument())

    fireEvent.click(screen.getByRole('button', { name: '发布集训通知' }))

    await fireEvent.update(
      await screen.findByPlaceholderText('如 2027 年寒假集训'),
      '2028 年寒假集训',
    )
    await fireEvent.update(
      screen.getByPlaceholderText('如 计算机学院实验楼 A301'),
      '实验楼 B201',
    )
    fireEvent.click(screen.getByText('开始日期'))
    fireEvent.click(screen.getByText('选择报名截止时间'))

    fireEvent.click(screen.getByRole('button', { name: '发布通知' }))

    await waitFor(() => expect(createCamp).toHaveBeenCalledTimes(1))
    expect(createCamp).toHaveBeenCalledWith(
      expect.objectContaining({
        name: '2028 年寒假集训',
        location: '实验楼 B201',
        startDate: '2027-01-10',
        endDate: '2027-01-24',
        registrationDeadline: '2027-01-05T23:59:59+08:00',
      }),
    )
  })

  it('【权限·3.2 创建/发布训练】普通会员不显示「发布集训通知」按钮', async () => {
    renderView('MEMBER')

    await waitFor(() => expect(screen.getByText('2027 年寒假集训')).toBeInTheDocument())

    expect(screen.queryByRole('button', { name: '发布集训通知' })).toBeNull()
  })
})
