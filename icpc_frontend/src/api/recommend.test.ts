/**
 * 个性化训练推荐模块测试
 * 覆盖：mock 推荐算法（薄弱维度匹配 / 排序 / 过滤）+ 接口 mock 分支
 */
import { describe, expect, it } from 'vitest'
import { getRecommendedProblems, getRecommendedTrainings } from '@/api/recommend'
import { buildProblemRecommendations, buildTrainingRecommendations, mockAbility } from '@/mock'
import type { AbilityResult } from '@/types'

const ability: AbilityResult = mockAbility

describe('buildTrainingRecommendations —— 基于能力评估推荐专项训练', () => {
  it('只推荐已发布/进行中的训练，并匹配薄弱维度', () => {
    const result = buildTrainingRecommendations(ability)

    // mock 数据最薄弱三个维度：数学(55) / 字符串(61) / 图论(64)
    // 训练 3「图论专题训练」（PUBLISHED）命中图论；训练 4「字符串与哈希」（PUBLISHED）命中字符串
    expect(result.map((item) => item.trainingId)).toEqual([3, 4])
    expect(result[0]).toMatchObject({ trainingId: 3, matchedDimensions: ['图论'] })
    expect(result[1]).toMatchObject({ trainingId: 4, matchedDimensions: ['字符串'] })

    // 草稿（训练 5）与已结束（训练 1 / 6）不参与推荐
    expect(result.every((item) => item.status === 'PUBLISHED' || item.status === 'ONGOING')).toBe(
      true,
    )
  })

  it('命中薄弱维度越多的训练排序越靠前，并生成推荐理由', () => {
    const customized: AbilityResult = {
      ...ability,
      dimensions: [
        { name: '基础算法', value: 90 },
        { name: '动态规划', value: 88 },
        { name: '图论', value: 70 },
        { name: '数据结构', value: 60 },
        { name: '数学', value: 65 },
        { name: '字符串', value: 75 },
      ],
    }

    // 最薄弱：数据结构(60) / 数学(65) / 图论(70)
    // 训练 3 命中「图论 + 数据结构」2 个维度，训练 2 仅命中「数据结构」1 个
    const result = buildTrainingRecommendations(customized)
    expect(result[0]).toMatchObject({
      trainingId: 3,
      matchedDimensions: expect.arrayContaining(['图论', '数据结构']),
    })
    expect(result[0]!.reason).toContain('图论')
    expect(result).toHaveLength(2)
  })
})

describe('buildProblemRecommendations —— 基于薄弱知识点推荐习题', () => {
  it('按薄弱维度分组，排除已 AC / 评测中的题目，组内难度由易到难', () => {
    const result = buildProblemRecommendations(ability)

    // 分组顺序按维度得分升序：数学(55) → 字符串(61) → 图论(64)
    expect(result.map((group) => group.tagName)).toEqual(['数学', '字符串', '图论'])
    expect(result[0]!.score).toBe(55)

    // 数学分组仅命中「组合数取模」（题目 110），未被排除
    expect(result[0]!.problems.map((item) => item.problemId)).toEqual([110])
    expect(result[0]!.problems[0]!.tagNames).toEqual(['组合数学', '数论'])

    // 字符串分组：108(中等) → 109(困难)；本人已 AC 的题目（100/101/102/104）不出现在任何分组
    expect(result[1]!.problems.map((item) => item.problemId)).toEqual([108, 109])

    // 图论分组：103 与 107 均为中等难度，按题目编号升序
    expect(result[2]!.problems.map((item) => item.problemId)).toEqual([103, 107])
    expect(result.every((group) => group.problems.length > 0)).toBe(true)
  })
})

describe('推荐接口（mock 分支）', () => {
  it('GET /trainings/recommend/trainings/me 返回推荐专项训练', async () => {
    await expect(getRecommendedTrainings('me')).resolves.toHaveLength(2)
  })

  it('GET /trainings/recommend/problems/me 返回薄弱知识点分组', async () => {
    const groups = await getRecommendedProblems('me')
    expect(groups.map((group) => group.tagName)).toEqual(['数学', '字符串', '图论'])
  })
})
