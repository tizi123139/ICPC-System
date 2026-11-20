import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { exportTrainingReport, getMemberOverview, getTrainingSummary } from './statistics'

const createObjectURL = vi.fn((_blob: Blob) => 'blob:mock-url')
const revokeObjectURL = vi.fn()
let downloadName = ''

beforeEach(() => {
  vi.useFakeTimers()
  downloadName = ''
  createObjectURL.mockClear()
  revokeObjectURL.mockClear()
  Object.defineProperty(URL, 'createObjectURL', {
    configurable: true,
    writable: true,
    value: createObjectURL,
  })
  Object.defineProperty(URL, 'revokeObjectURL', {
    configurable: true,
    writable: true,
    value: revokeObjectURL,
  })
})

afterEach(() => {
  vi.useRealTimers()
  vi.restoreAllMocks()
})

describe('statistics api（Mock 分支）', () => {
  it('【清单 69】导出训练成绩报表：生成 CSV Blob 并触发 a 标签下载', async () => {
    const clickSpy = vi
      .spyOn(HTMLAnchorElement.prototype, 'click')
      .mockImplementation(function (this: HTMLAnchorElement) {
        downloadName = this.download
      })

    const pending = exportTrainingReport(2)
    await vi.advanceTimersByTimeAsync(400)
    await pending

    expect(createObjectURL).toHaveBeenCalledTimes(1)
    const blob = createObjectURL.mock.calls[0]![0]
    expect(blob).toBeInstanceOf(Blob)
    expect(blob.type).toBe('text/csv;charset=utf-8')

    expect(clickSpy).toHaveBeenCalledTimes(1)
    expect(downloadName).toMatch(/^训练成绩报表-.*\.csv$/)

    const content = await blob.text()
    expect(content).toContain('"提交人"')
    expect(content).toContain('"学号"')
    expect(content).toContain('"结果"')
  })

  it('【清单 70】训练数据汇总：返回训练场次、提交数与 AC 率及趋势', async () => {
    const pending = getTrainingSummary()
    await vi.advanceTimersByTimeAsync(300)
    const result = await pending

    expect(result.totalTrainings).toBe(42)
    expect(result.totalSubmissions).toBe(1860)
    expect(result.acRate).toBe(63.5)
    expect(result.trend).toHaveLength(6)
    expect(result.trend[0]).toEqual({ date: '10-06', submissions: 86, ac: 52 })
    expect(result.trend.every((item) => 'date' in item && 'submissions' in item && 'ac' in item)).toBe(
      true,
    )
  })

  it('【清单 71】会员结构统计：返回总数与分类型/年级/专业维度', async () => {
    const pending = getMemberOverview()
    await vi.advanceTimersByTimeAsync(300)
    const result = await pending

    expect(result.total).toBe(156)
    expect(result.byType).toHaveLength(2)
    expect(result.byType).toEqual(
      expect.arrayContaining([
        { name: 'OBSERVE_MEMBER', value: 38 },
        { name: 'MEMBER', value: 118 },
      ]),
    )
    expect(result.byGrade).toEqual(
      expect.arrayContaining([
        { name: '2023级', value: 47 },
        { name: '2024级', value: 58 },
      ]),
    )
    expect(result.byDepartment).toEqual(
      expect.arrayContaining([
        { name: '竞赛部', value: 36 },
        { name: '组织部', value: 28 },
      ]),
    )
  })
})
