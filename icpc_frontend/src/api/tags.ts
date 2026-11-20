/**
 * 知识点管理模块 —— 对应接口文档第 3.1 章
 * 接口清单：
 *   GET    /tags          获取知识点树（含父子层级），已登录
 *   POST   /tags          新增知识点，ASSISTANT_COACH+
 *   PUT    /tags/{tagId}  修改知识点，ASSISTANT_COACH+
 *   DELETE /tags/{tagId}  删除知识点，PRESIDENT+
 */
import { del, get, post, put } from './request'
import { USE_MOCK } from './mode'
import { mockDelay, mockTagTree } from '@/mock'
import type { TagNode } from '@/types'

/** GET /tags —— 获取知识点树（含父子层级），权限：已登录 */
export async function getTagTree(): Promise<TagNode[]> {
  // TODO(backend): 联调时删除 mock 分支
  if (USE_MOCK) {
    await mockDelay(300)
    // 返回深拷贝，保证增删改后 Vue 能感知到新数据
    return JSON.parse(JSON.stringify(mockTagTree)) as TagNode[]
  }
  return get<TagNode[]>('/tags')
}

/** POST /tags —— 新增知识点，权限：ASSISTANT_COACH+ */
export async function createTag(payload: {
  name: string
  category: string
  parentId?: number | null
}): Promise<void> {
  // TODO(backend): 联调时删除 mock 分支
  if (USE_MOCK) {
    await mockDelay(300)
    const node = { tagId: Date.now(), name: payload.name, category: payload.category }
    if (payload.parentId) {
      const parent = mockTagTree.find((item) => item.tagId === payload.parentId)
      parent?.children?.push(node)
    } else {
      mockTagTree.push({ ...node, children: [] })
    }
    return
  }
  await post<void>('/tags', payload)
}

/** PUT /tags/{tagId} —— 修改知识点，权限：ASSISTANT_COACH+ */
export async function updateTag(
  tagId: number,
  payload: { name: string; category: string },
): Promise<void> {
  // TODO(backend): 联调时删除 mock 分支
  if (USE_MOCK) {
    await mockDelay(300)
    const walk = (nodes: TagNode[]) => {
      for (const node of nodes) {
        if (node.tagId === tagId) {
          Object.assign(node, payload)
          return true
        }
        if (node.children && walk(node.children)) return true
      }
      return false
    }
    walk(mockTagTree)
    return
  }
  await put<void>(`/tags/${tagId}`, payload)
}

/** DELETE /tags/{tagId} —— 删除知识点，权限：PRESIDENT+ */
export async function deleteTag(tagId: number): Promise<void> {
  // TODO(backend): 联调时删除 mock 分支
  if (USE_MOCK) {
    await mockDelay(300)
    const remove = (nodes: TagNode[]): boolean => {
      const index = nodes.findIndex((item) => item.tagId === tagId)
      if (index >= 0) {
        nodes.splice(index, 1)
        return true
      }
      return nodes.some((node) => (node.children ? remove(node.children) : false))
    }
    remove(mockTagTree)
    return
  }
  await del<void>(`/tags/${tagId}`)
}
