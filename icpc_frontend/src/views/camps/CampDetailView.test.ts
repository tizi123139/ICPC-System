import { fireEvent, render, screen, waitFor } from '@testing-library/vue'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { Pinia } from 'pinia'
import type { Router } from 'vue-router'
import CampDetailView from './CampDetailView.vue'
import { permission } from '@/directives/permission'
import { createTestRouter, resetAuth, setupAuth } from '@/test/utils'
import type {
  AttendanceRecord,
  CampDetail,
  CampRegistration,
  RoleCode,
  UserInfo,
} from '@/types'

vi.mock('@/api/camps', () => ({
  getCamp: vi.fn(),
  getMyCampRegistration: vi.fn(),
  getCampRegistrations: vi.fn(),
  registerCamp: vi.fn(),
  auditCampRegistration: vi.fn(),
  createCampTask: vi.fn(),
  checkinCamp: vi.fn(),
  getCampAttendance: vi.fn(),
  updateCampAttendance: vi.fn(),
}))

import * as campsApi from '@/api/camps'

const getCamp = vi.mocked(campsApi.getCamp)
const getMyCampRegistration = vi.mocked(campsApi.getMyCampRegistration)
const getCampRegistrations = vi.mocked(campsApi.getCampRegistrations)
const registerCamp = vi.mocked(campsApi.registerCamp)
const auditCampRegistration = vi.mocked(campsApi.auditCampRegistration)
const createCampTask = vi.mocked(campsApi.createCampTask)
const checkinCamp = vi.mocked(campsApi.checkinCamp)
const getCampAttendance = vi.mocked(campsApi.getCampAttendance)
const updateCampAttendance = vi.mocked(campsApi.updateCampAttendance)

const makeUser = (name: string, userId = 4): UserInfo => ({
  userId,
  studentId: `100${userId}`,
  name,
  roles: ['ASSISTANT_COACH'],
})

const camp: CampDetail = {
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
  tasks: [
    {
      taskId: 1,
      campId: 1,
      date: '2027-01-10',
      title: 'Day1 开营与热身赛',
      content: '上午开营宣讲，下午热身赛。',
      publisher: makeUser('周雨桐'),
      publishTime: '2027-01-09T20:00:00+08:00',
    },
  ],
}

const pendingRegistration: CampRegistration = {
  applyId: 1,
  campId: 1,
  memberId: 2,
  memberName: '李知远',
  studentId: '2024120002',
  grade: '2024级',
  applyTime: '2026-12-20T10:00:00+08:00',
  status: 'PENDING',
  remark: '希望利用寒假补习动态规划。',
}

const attendanceRecord: AttendanceRecord = {
  recordId: 1,
  campId: 1,
  memberId: 2,
  memberName: '李知远',
  studentId: '2024120002',
  date: '2027-01-10',
  status: 'ABSENT',
  checkinTime: null,
  remark: '未签到',
}

const ElDatePickerStub = {
  name: 'ElDatePicker',
  props: {
    modelValue: { default: undefined },
    placeholder: { type: String, default: '' },
  },
  emits: ['update:modelValue'],
  template: `<button type="button" class="el-date-picker-stub" @click="pick">{{ placeholder || '选择日期' }}</button>`,
  methods: {
    pick(this: { $emit: (event: string, payload: unknown) => void }) {
      this.$emit('update:modelValue', '2027-01-15')
    },
  },
}

let router: Router
let pinia: Pinia

/** 预置路由到 /camps/1 后再渲染，保证组件内 campId 解析正确 */
async function renderView(role: RoleCode = 'MEMBER') {
  resetAuth()
  pinia = setupAuth(role).pinia
  router = createTestRouter()
  await router.push('/camps/1')
  return render(CampDetailView, {
    global: {
      plugins: [pinia, router],
      directives: { permission },
      stubs: { ElDatePicker: ElDatePickerStub },
    },
  })
}

beforeEach(async () => {
  vi.clearAllMocks()
  vi.spyOn(ElMessage, 'success').mockImplementation(() => ({ close: () => {} }) as never)
  vi.spyOn(ElMessage, 'warning').mockImplementation(() => ({ close: () => {} }) as never)
  vi.spyOn(ElMessage, 'error').mockImplementation(() => ({ close: () => {} }) as never)
  vi.spyOn(ElMessageBox, 'confirm').mockResolvedValue('confirm' as never)

  getCamp.mockResolvedValue({ ...camp, tasks: [...camp.tasks] })
  getMyCampRegistration.mockResolvedValue(null)
  getCampRegistrations.mockResolvedValue([pendingRegistration])
  registerCamp.mockResolvedValue(undefined)
  auditCampRegistration.mockResolvedValue(undefined)
  createCampTask.mockResolvedValue(camp.tasks[0]!)
  checkinCamp.mockResolvedValue(undefined)
  getCampAttendance.mockResolvedValue([attendanceRecord])
  updateCampAttendance.mockResolvedValue(undefined)
})

describe('CampDetailView', () => {
  it('【集训·集训详情】渲染集训名称、类型、地点与每日任务', async () => {
    await renderView()

    await waitFor(() =>
      expect(screen.getByRole('heading', { name: '2027 年寒假集训' })).toBeInTheDocument(),
    )
    expect(getCamp).toHaveBeenCalledWith(1)
    expect(screen.getAllByText(/寒假集训/).length).toBeGreaterThan(0)
    expect(screen.getByText('计算机学院实验楼 A301')).toBeInTheDocument()
    expect(await screen.findByText('Day1 开营与热身赛')).toBeInTheDocument()
    expect(screen.getByText('上午开营宣讲，下午热身赛。')).toBeInTheDocument()
  })

  it('【集训·提交集训报名】会员提交报名后调用 registerCamp', async () => {
    await renderView('MEMBER')

    await waitFor(() =>
      expect(screen.getByRole('heading', { name: '2027 年寒假集训' })).toBeInTheDocument(),
    )

    fireEvent.click(await screen.findByRole('button', { name: '提交集训报名' }))

    const remarkInput = await screen.findByPlaceholderText(
      '简要说明报名原因、训练目标等（选填）',
    )
    await fireEvent.update(remarkInput, '寒假想集中训练。')
    fireEvent.click(screen.getByRole('button', { name: '提交报名' }))

    await waitFor(() => expect(registerCamp).toHaveBeenCalledWith(1, '寒假想集中训练。'))
  })

  it('【集训·报名列表 + 审核报名】助理教练查看报名列表并通过申请', async () => {
    await renderView('ASSISTANT_COACH')

    await waitFor(() =>
      expect(screen.getByRole('heading', { name: '2027 年寒假集训' })).toBeInTheDocument(),
    )

    fireEvent.click(screen.getByText(/报名审核/))

    await waitFor(() => expect(getCampRegistrations).toHaveBeenCalledWith(1))
    expect(await screen.findByText('李知远')).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: '通过' }))

    await waitFor(() =>
      expect(auditCampRegistration).toHaveBeenCalledWith(1, 1, 'APPROVE', ''),
    )
  })

  it('【集训·发布集训每日任务】助理教练发布任务后调用 createCampTask', async () => {
    await renderView('ASSISTANT_COACH')

    await waitFor(() =>
      expect(screen.getByRole('heading', { name: '2027 年寒假集训' })).toBeInTheDocument(),
    )

    fireEvent.click(screen.getByRole('button', { name: '发布每日任务' }))

    fireEvent.click(await screen.findByText('选择任务日期'))
    await fireEvent.update(await screen.findByPlaceholderText('如 Day2 动态规划专题'), 'Day2 图论专题')
    await fireEvent.update(
      screen.getByPlaceholderText('讲解内容、训练题目、注意事项等'),
      '上午讲解最短路。',
    )
    fireEvent.click(screen.getByRole('button', { name: '发布任务' }))

    await waitFor(() => expect(createCampTask).toHaveBeenCalledTimes(1))
    expect(createCampTask).toHaveBeenCalledWith(
      1,
      expect.objectContaining({
        date: '2027-01-15',
        title: 'Day2 图论专题',
        content: '上午讲解最短路。',
      }),
    )
  })

  it('【集训·签到打卡】已通过报名的会员签到后调用 checkinCamp', async () => {
    getMyCampRegistration.mockResolvedValue({ ...pendingRegistration, status: 'APPROVED' })

    await renderView('MEMBER')

    await waitFor(() =>
      expect(screen.getByRole('heading', { name: '2027 年寒假集训' })).toBeInTheDocument(),
    )

    fireEvent.click(await screen.findByRole('button', { name: '签到打卡' }))
    fireEvent.click(await screen.findByRole('button', { name: '确认打卡' }))

    await waitFor(() =>
      expect(checkinCamp).toHaveBeenCalledWith(1, expect.objectContaining({ remark: '' })),
    )
    expect(checkinCamp.mock.calls[0]![1].date).toMatch(/^\d{4}-\d{2}-\d{2}$/)
  })

  it('【集训·考勤记录查询 + 补签】助理教练查看全体考勤并补签', async () => {
    await renderView('ASSISTANT_COACH')

    await waitFor(() =>
      expect(screen.getByRole('heading', { name: '2027 年寒假集训' })).toBeInTheDocument(),
    )

    fireEvent.click(screen.getByText('考勤记录'))

    await waitFor(() => expect(getCampAttendance).toHaveBeenCalledWith(1, true))
    expect(await screen.findByText('李知远')).toBeInTheDocument()
    expect(screen.getByText('缺勤')).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: '补签' }))
    fireEvent.click(await screen.findByRole('button', { name: '确认' }))

    await waitFor(() =>
      expect(updateCampAttendance).toHaveBeenCalledWith(1, 1, 'MAKEUP', '未签到'),
    )
  })

  it('【集训·请假 / 标记缺勤】助理教练可将考勤标记为请假', async () => {
    await renderView('ASSISTANT_COACH')

    await waitFor(() =>
      expect(screen.getByRole('heading', { name: '2027 年寒假集训' })).toBeInTheDocument(),
    )

    fireEvent.click(screen.getByText('考勤记录'))
    await waitFor(() => expect(getCampAttendance).toHaveBeenCalledWith(1, true))
    await screen.findByText('李知远')

    fireEvent.click(screen.getByRole('button', { name: '请假' }))
    fireEvent.click(await screen.findByRole('button', { name: '确认' }))

    await waitFor(() =>
      expect(updateCampAttendance).toHaveBeenCalledWith(1, 1, 'LEAVE', '未签到'),
    )
  })

  it('【权限·3.2 报名与打卡】普通会员不显示发布任务入口，报名 tab 展示「我的报名」', async () => {
    await renderView('MEMBER')

    await waitFor(() =>
      expect(screen.getByRole('heading', { name: '2027 年寒假集训' })).toBeInTheDocument(),
    )

    expect(screen.queryByRole('button', { name: '发布每日任务' })).toBeNull()
    expect(screen.getByText('我的报名')).toBeInTheDocument()
    expect(getCampRegistrations).not.toHaveBeenCalled()
  })

  it('【权限·3.2 审核报名】普通会员不显示报名审核操作', async () => {
    await renderView('MEMBER')

    await waitFor(() =>
      expect(screen.getByRole('heading', { name: '2027 年寒假集训' })).toBeInTheDocument(),
    )

    expect(screen.queryByRole('button', { name: '通过' })).toBeNull()
    expect(screen.queryByRole('button', { name: '驳回' })).toBeNull()
  })
})
