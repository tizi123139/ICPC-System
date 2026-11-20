import { beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor, within } from '@testing-library/vue'
import type { Pinia } from 'pinia'
import ProblemsView from './ProblemsView.vue'
import { getProblems } from '@/api/problems'
import { createTag, deleteTag, getTagTree, updateTag } from '@/api/tags'
import { permission } from '@/directives/permission'
import { createTestRouter, resetAuth, setupAuth } from '@/test/utils'
import type { TagNode } from '@/types'

const { confirmMock } = vi.hoisted(() => ({ confirmMock: vi.fn() }))

vi.mock('element-plus', async () => {
  const actual = await vi.importActual<typeof import('element-plus')>('element-plus')
  return {
    ...actual,
    ElMessageBox: {
      ...(actual.ElMessageBox as unknown as Record<string, unknown>),
      confirm: confirmMock,
    },
  }
})

vi.mock('@/api/problems', () => ({
  getProblems: vi.fn(),
  createProblem: vi.fn(),
  updateProblem: vi.fn(),
  deleteProblem: vi.fn(),
  importProblemsFromOj: vi.fn(),
}))

vi.mock('@/api/tags', () => ({
  getTagTree: vi.fn(),
  createTag: vi.fn(),
  updateTag: vi.fn(),
  deleteTag: vi.fn(),
}))

class ResizeObserverStub {
  observe() {}
  unobserve() {}
  disconnect() {}
}
if (!('ResizeObserver' in globalThis)) {
  globalThis.ResizeObserver = ResizeObserverStub as unknown as typeof ResizeObserver
}

/** 两级知识点树：父节点 + 子节点 */
const tagTree: TagNode[] = [
  {
    tagId: 1,
    name: '基础算法',
    category: '算法',
    children: [
      { tagId: 11, name: '枚举', category: '基础算法' },
      { tagId: 12, name: '模拟', category: '基础算法' },
    ],
  },
  {
    tagId: 2,
    name: '动态规划',
    category: '算法',
    children: [{ tagId: 21, name: '线性 DP', category: '动态规划' }],
  },
]

let pinia: Pinia

function renderView() {
  const router = createTestRouter()
  return render(ProblemsView, {
    global: {
      plugins: [pinia, router],
      directives: { permission },
    },
  })
}

/** 在知识点弹窗中填写「所属分类」下拉框 */
async function selectCategory(dialog: HTMLElement, value: string) {
  const combobox = within(dialog).getByRole('combobox')
  await fireEvent.update(combobox, value)
  const option = await screen.findByRole('option', { name: value })
  fireEvent.click(option)
}

beforeEach(() => {
  resetAuth()
  pinia = setupAuth('PRESIDENT').pinia

  vi.mocked(getProblems).mockReset()
  vi.mocked(getProblems).mockResolvedValue({ total: 0, page: 1, size: 10, records: [] })
  vi.mocked(getTagTree).mockReset()
  vi.mocked(getTagTree).mockResolvedValue(tagTree)
  vi.mocked(createTag).mockReset()
  vi.mocked(createTag).mockResolvedValue(undefined)
  vi.mocked(updateTag).mockReset()
  vi.mocked(updateTag).mockResolvedValue(undefined)
  vi.mocked(deleteTag).mockReset()
  vi.mocked(deleteTag).mockResolvedValue(undefined)
  confirmMock.mockReset()
})

describe('ProblemsView 知识点管理', () => {
  it('【清单 9】获取知识点树：渲染父节点与子节点名称', async () => {
    renderView()

    expect(await screen.findByText('基础算法')).toBeInTheDocument()
    expect(screen.getByText('枚举')).toBeInTheDocument()
    expect(screen.getByText('模拟')).toBeInTheDocument()
    expect(screen.getByText('动态规划')).toBeInTheDocument()
    expect(screen.getByText('线性 DP')).toBeInTheDocument()

    expect(vi.mocked(getTagTree)).toHaveBeenCalled()
  })

  it('【清单 8】新增知识点：树头“新增”弹窗填写名称/分类后调用 createTag', async () => {
    renderView()
    await screen.findByText('基础算法')

    fireEvent.click(screen.getByRole('button', { name: '新增' }))

    const dialog = await screen.findByRole('dialog', { name: '新增知识点' })
    const nameInput = within(dialog).getByPlaceholderText('如 并查集')
    fireEvent.update(nameInput, '并查集')
    await selectCategory(dialog, '算法')

    fireEvent.click(within(dialog).getByRole('button', { name: '确认新增' }))

    await waitFor(() => expect(vi.mocked(createTag)).toHaveBeenCalledTimes(1))
    expect(vi.mocked(createTag)).toHaveBeenCalledWith({
      name: '并查集',
      category: '算法',
      parentId: null,
    })
  })

  it('【清单 7】修改知识点：节点“编辑”回填原名，保存后调用 updateTag', async () => {
    renderView()
    await screen.findByText('基础算法')

    fireEvent.click(screen.getAllByTitle('编辑')[0] as HTMLElement)

    const dialog = await screen.findByRole('dialog', { name: '编辑知识点' })
    const nameInput = within(dialog).getByPlaceholderText('如 并查集') as HTMLInputElement
    expect(nameInput.value).toBe('基础算法')

    fireEvent.update(nameInput, '基础算法进阶')
    fireEvent.click(within(dialog).getByRole('button', { name: '保存修改' }))

    await waitFor(() => expect(vi.mocked(updateTag)).toHaveBeenCalledTimes(1))
    expect(vi.mocked(updateTag)).toHaveBeenCalledWith(1, {
      name: '基础算法进阶',
      category: '算法',
    })
  })

  it('【清单 6】删除知识点：节点“删除”确认后调用 deleteTag', async () => {
    confirmMock.mockResolvedValue('confirm')
    renderView()
    await screen.findByText('基础算法')

    fireEvent.click(screen.getAllByTitle('删除')[0] as HTMLElement)

    await waitFor(() => expect(vi.mocked(deleteTag)).toHaveBeenCalledWith(1))
    await waitFor(() => expect(vi.mocked(getTagTree)).toHaveBeenCalledTimes(2))
  })
})
