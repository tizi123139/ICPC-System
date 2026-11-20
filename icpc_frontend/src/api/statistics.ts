/**
 * 数据统计模块 —— 对应接口文档第 10 章
 * 接口清单：
 *   GET /statistics/members/overview    会员结构统计（OFFICER+）
 *   GET /statistics/training/summary    训练数据汇总（ASSISTANT_COACH+）
 *   GET /export/training-report/{trainingId}         导出训练成绩报表（Excel）
 *   GET /export/contest-certificates/{contestId}     下载赛事获奖证书（ZIP）
 */
import { USE_MOCK } from './mode'
import http, { get } from './request'
import {
  mockDelay,
  mockMemberOverview,
  mockProblems,
  mockSubmissions,
  mockTrainings,
  mockTrainingSummary,
} from '@/mock'
import type { MemberOverview, MemberType, TrainingSummary } from '@/types'

/* ===================== 后端传输层结构（与 StatisticsDtos 对齐） ===================== */

interface RawCountItem {
  name: string
  count: number
}

interface RawMemberOverviewVO {
  byGrade?: RawCountItem[]
  byMajor?: RawCountItem[]
  byDepartment?: RawCountItem[]
  byMemberType?: RawCountItem[]
}

interface RawTrainingSummaryVO {
  totalTrainings?: number
  totalSubmissions?: number
  avgAcRate?: number
  topMembers?: Array<Record<string, unknown>>
  trend?: Array<{ date?: string; submissions?: number; ac?: number }>
}

function toCountRows(list?: RawCountItem[]): Array<{ name: string; value: number }> {
  return (list ?? []).map((item) => ({ name: item.name, value: item.count }))
}

/** 触发浏览器下载 Blob 文件 */
function downloadBlob(blob: Blob, filename: string) {
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  link.style.display = 'none'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  // 延迟释放，避免部分浏览器在下载开始前就被回收
  window.setTimeout(() => URL.revokeObjectURL(url), 1000)
}

/** GET /statistics/members/overview —— 会员结构统计（年级 / 专业 / 部门 / 类型） */
export async function getMemberOverview(): Promise<MemberOverview> {
  // TODO(backend): 联调时删除 mock 分支
  if (USE_MOCK) {
    await mockDelay(300)
    return mockMemberOverview
  }
  const raw = await get<RawMemberOverviewVO>('/statistics/members/overview')
  const byType = (raw.byMemberType ?? []).map((item) => ({
    name: item.name as MemberType,
    value: item.count,
  }))
  return {
    total: byType.reduce((sum, item) => sum + item.value, 0),
    byType,
    byGrade: toCountRows(raw.byGrade),
    byDepartment: toCountRows(raw.byDepartment),
  }
}

/** GET /statistics/training/summary —— 训练数据汇总 */
export async function getTrainingSummary(): Promise<TrainingSummary> {
  // TODO(backend): 联调时删除 mock 分支
  if (USE_MOCK) {
    await mockDelay(300)
    return mockTrainingSummary
  }
  const raw = await get<RawTrainingSummaryVO>('/statistics/training/summary')
  return {
    totalTrainings: raw.totalTrainings ?? 0,
    totalSubmissions: raw.totalSubmissions ?? 0,
    acRate: raw.avgAcRate ?? 0,
    trend: (raw.trend ?? []).map((item) => ({
      date: item.date ?? '',
      submissions: item.submissions ?? 0,
      ac: item.ac ?? 0,
    })),
  }
}

/**
 * GET /export/training-report/{trainingId} —— 导出训练成绩报表（Excel）
 * 权限：ASSISTANT_COACH+
 * TODO(backend): 后端返回文件流（Content-Disposition 指定文件名）。
 * 若后端包了一层 { code, message, data }，需要改为先解析 JSON 再取下载地址。
 */
export async function exportTrainingReport(trainingId: number): Promise<void> {
  // Mock 分支：按提交记录生成 CSV（带 BOM，Excel 打开中文不乱码）
  if (USE_MOCK) {
    await mockDelay(400)
    const training = mockTrainings.find((item) => item.trainingId === trainingId)
    const records = mockSubmissions.filter((item) => item.trainingId === trainingId)
    const header = ['序号', '提交人', '学号', '题目', '语言', '结果', '耗时(ms)', '内存(MB)', '提交时间']
    const rows = records.map((record, index) => {
      const title =
        mockProblems.find((item) => item.problemId === record.problemId)?.title ??
        `题目 ${record.problemId}`
      return [
        index + 1,
        record.submitter.name,
        record.submitter.studentId,
        title,
        record.language,
        record.result,
        record.timeUsed || '—',
        record.memoryUsed ? Math.round(record.memoryUsed / 1024) : '—',
        record.submitTime.replace('T', ' ').slice(0, 19),
      ]
    })
    const csv = [header, ...rows]
      .map((row) => row.map((cell) => `"${String(cell).replace(/"/g, '""')}"`).join(','))
      .join('\r\n')
    const blob = new Blob([`\uFEFF${csv}`], { type: 'text/csv;charset=utf-8' })
    downloadBlob(blob, `训练成绩报表-${training?.name ?? trainingId}.csv`)
    return
  }

  const response = await http.get<Blob>(`/export/training-report/${trainingId}`, {
    responseType: 'blob',
  })
  downloadBlob(response.data, `训练成绩报表-${trainingId}.xlsx`)
}
