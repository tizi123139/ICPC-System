/**
 * 题目详情页测试
 * 覆盖：题解维护对话框（含通用文件上传附件）与题解附件展示
 */
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor } from '@testing-library/vue'
import type { Pinia } from 'pinia'
import type { Router } from 'vue-router'
import ProblemDetailView from './ProblemDetailView.vue'
import { permission } from '@/directives/permission'
import { createTestRouter, resetAuth, setupAuth } from '@/test/utils'
import type { ProblemInfo, UploadResult } from '@/types'

vi.mock('@/api/problems', () => ({
  getProblem: vi.fn(),
  getProblemSubmissions: vi.fn(),
  submitProblemCode: vi.fn(),
  updateSolution: vi.fn(),
}))

vi.mock('@/api/files', () => ({
  uploadFile: vi.fn(),
}))

import * as problemsApi from '@/api/problems'
import * as filesApi from '@/api/files'

const getProblem = vi.mocked(problemsApi.getProblem)
const getProblemSubmissions = vi.mocked(problemsApi.getProblemSubmissions)
const updateSolution = vi.mocked(problemsApi.updateSolution)
const uploadFile = vi.mocked(filesApi.uploadFile)

class ResizeObserverStub {
  observe() {}
  unobserve() {}
  disconnect() {}
}
if (!('ResizeObserver' in globalThis)) {
  globalThis.ResizeObserver = ResizeObserverStub as unknown as typeof ResizeObserver
}

const problem: ProblemInfo = {
  problemId: 103,
  title: '单源最短路',
  ojProblemId: '1003',
  difficulty: 'MEDIUM',
  timeLimit: 1000,
  memoryLimit: 262144,
  description: '给定 n 个点 m 条边的有向图，求 1 号点到 n 号点的最短路长度。',
  inputFormat: '第一行两个整数 n, m；接下来 m 行每行三个整数 u, v, w。',
  outputFormat: '输出一个整数表示最短路长度。',
  sampleInput: '2 1\n1 2 3',
  sampleOutput: '3',
  hint: null,
  tags: [{ tagId: 31, name: '最短路', category: '图论' }],
  solution: null,
  visibility: 'PUBLIC',
}

const uploaded: UploadResult = {
  url: 'blob:http://localhost/f1',
  filename: '题解配图.png',
  size: 20480,
}

let pinia: Pinia
let router: Router

function renderView() {
  return render(ProblemDetailView, {
    global: {
      plugins: [pinia, router],
      directives: { permission },
    },
  })
}

describe('ProblemDetailView 题解维护与附件上传', () => {
  beforeEach(async () => {
    resetAuth()
    vi.clearAllMocks()
    pinia = setupAuth('PRESIDENT').pinia
    router = createTestRouter()
    await router.push('/problems/103')

    getProblem.mockResolvedValue(problem)
    getProblemSubmissions.mockResolvedValue([])
    uploadFile.mockResolvedValue(uploaded)
  })

  /** 打开题解维护对话框（头部按钮与空状态按钮文案相同，取第一个） */
  async function openSolutionDialog() {
    fireEvent.click(screen.getAllByText('补充题解')[0] as HTMLElement)
    return screen.findByPlaceholderText('支持 Markdown 格式，例如：## 思路 / ## 复杂度')
  }

  it('打开题解维护对话框，填写内容后保存调用 updateSolution（含空附件数组）', async () => {
    renderView()

    await waitFor(() => expect(screen.getByText('单源最短路')).toBeInTheDocument())
    const contentBox = await openSolutionDialog()
    fireEvent.update(contentBox, '## 思路\n使用 Dijkstra 算法。')

    fireEvent.click(screen.getByText('保存题解'))

    await waitFor(() =>
      expect(updateSolution).toHaveBeenCalledWith(103, {
        content: '## 思路\n使用 Dijkstra 算法。',
        referenceCode: null,
        attachments: [],
      }),
    )
  })

  it('通过通用文件上传接口上传附件，附件展示并可随题解一并保存', async () => {
    renderView()

    await waitFor(() => expect(screen.getByText('单源最短路')).toBeInTheDocument())
    const contentBox = await openSolutionDialog()
    fireEvent.update(contentBox, '题解正文')

    // 触发 el-upload 隐藏文件输入框的 change 事件 -> 自定义 http-request -> POST /files/upload
    const input = document.querySelector('input[type="file"]') as HTMLInputElement
    expect(input).toBeTruthy()
    const file = new File(['image-bytes'], '题解配图.png', { type: 'image/png' })
    Object.defineProperty(input, 'files', { value: [file] })
    await fireEvent.change(input)

    await waitFor(() => expect(uploadFile).toHaveBeenCalledTimes(1))
    // 上传成功后附件出现在列表中
    await waitFor(() => expect(screen.getAllByText('题解配图.png').length).toBeGreaterThan(0))
    expect(screen.getAllByText('20 KB').length).toBeGreaterThan(0)

    fireEvent.click(screen.getByText('保存题解'))

    await waitFor(() =>
      expect(updateSolution).toHaveBeenCalledWith(103, {
        content: '题解正文',
        referenceCode: null,
        attachments: [uploaded],
      }),
    )
  })

  it('题解 Tab 展示已保存的附件下载链接', async () => {
    getProblem.mockResolvedValue({
      ...problem,
      solution: {
        solutionId: 1,
        content: '使用 Dijkstra + 堆优化。',
        referenceCode: null,
        attachments: [uploaded],
        updateTime: '2026-11-15T10:00:00+08:00',
      },
    })

    renderView()
    await waitFor(() => expect(screen.getByText('单源最短路')).toBeInTheDocument())

    // 切到「题解」Tab
    fireEvent.click(screen.getByText('题解'))

    await waitFor(() =>
      expect(screen.getByText('使用 Dijkstra + 堆优化。')).toBeInTheDocument(),
    )
    expect(screen.getByText('附件')).toBeInTheDocument()
    const link = screen.getByText('题解配图.png') as HTMLElement
    expect(link).toBeInTheDocument()
  })
})
