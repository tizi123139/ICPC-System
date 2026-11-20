<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { EChartsOption } from 'echarts'
import PageHeader from '@/components/PageHeader.vue'
import BaseChart from '@/components/BaseChart.vue'
import StatusTag from '@/components/StatusTag.vue'
import { getAbility } from '@/api/trainings'
import { getRecommendedProblems, getRecommendedTrainings } from '@/api/recommend'
import { useAuthStore } from '@/stores/auth'
import { difficultyMap, formatDateTime, SELF_EVAL_ROLES, trainingTypeMap } from '@/utils/dict'
import type {
  AbilityResult,
  RecommendedTraining,
  WeakTagProblemGroup,
} from '@/types'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

/** 能力评估（雷达图/维度得分/建议）仅参与角色可见：干事与指导老师不参与（权限表「能力评估」一行） */
const canSelfEval = computed(() => auth.hasRole(SELF_EVAL_ROLES))

const loading = ref(false)
const ability = ref<AbilityResult | null>(null)
/** 推荐专项训练（GET /trainings/recommend/trainings/{userId}） */
const trainings = ref<RecommendedTraining[]>([])
/** 薄弱知识点习题推荐分组（GET /trainings/recommend/problems/{userId}） */
const problemGroups = ref<WeakTagProblemGroup[]>([])

/** 路由参数为 'me' 时表示当前登录用户，后端对应 /trainings/ability/me */
const userId = computed(() => {
  const param = String(route.params.userId)
  return param === 'me' ? 'me' : Number(param)
})

const radarOption = computed<EChartsOption>(() => {
  const dimensions = ability.value?.dimensions ?? []
  return {
    tooltip: { trigger: 'item' },
    radar: {
      indicator: dimensions.map((item) => ({ name: item.name, max: 100 })),
      radius: '68%',
      center: ['50%', '52%'],
      splitNumber: 4,
      axisName: { color: '#52646f', fontSize: 12 },
      splitLine: { lineStyle: { color: '#e3eef7' } },
      splitArea: {
        areaStyle: { color: ['rgba(241,247,253,0.6)', 'rgba(253,252,247,0.6)'] },
      },
      axisLine: { lineStyle: { color: '#e3eef7' } },
    },
    series: [
      {
        type: 'radar',
        symbol: 'circle',
        symbolSize: 6,
        lineStyle: { color: '#5b9bd5', width: 2 },
        itemStyle: { color: '#5b9bd5' },
        areaStyle: { color: 'rgba(91,155,213,0.22)' },
        data: [
          {
            name: '能力值',
            value: dimensions.map((item) => item.value),
          },
        ],
      },
    ],
  }
})

const strongest = computed(() =>
  [...(ability.value?.dimensions ?? [])].sort((a, b) => b.value - a.value).slice(0, 2),
)
const weakest = computed(() =>
  [...(ability.value?.dimensions ?? [])].sort((a, b) => a.value - b.value).slice(0, 2),
)

async function load() {
  loading.value = true
  try {
    // 接口：GET /trainings/ability/{userId}，并行拉取个性化推荐。
    // 推荐接口失败时不应连带整个能力评估页空白，因此使用 allSettled 分别处理。
    // 个性化训练推荐全员可用；能力评估接口仅参与角色请求（干事、指导老师不参与）。
    const [abilityResult, trainingRecs, problemRecs] = await Promise.allSettled([
      canSelfEval.value ? getAbility(userId.value) : Promise.resolve(null),
      getRecommendedTrainings(userId.value),
      getRecommendedProblems(userId.value),
    ])
    if (abilityResult.status === 'fulfilled' && abilityResult.value) {
      ability.value = abilityResult.value
    }
    if (trainingRecs.status === 'fulfilled') {
      trainings.value = trainingRecs.value
    }
    if (problemRecs.status === 'fulfilled') {
      problemGroups.value = problemRecs.value
    }
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
<PageHeader
    title="能力评估"
    :subtitle="ability
      ? `${ability.userName} · 基于近期训练提交数据生成`
      : canSelfEval ? '个人能力雷达图' : '个性化训练推荐'"
  >
    <template #actions>
      <el-button v-if="userId === 'me'" :icon="'DataAnalysis'" @click="router.push('/trainings')">
        训练列表
      </el-button>
      <el-button :icon="'ArrowLeft'" @click="router.push('/trainings')">返回</el-button>
    </template>
</PageHeader>

<div v-loading="loading" class="page ability-page">
  <!-- 能力评估部分：仅参与角色可见（权限表「能力评估」一行，干事、指导老师不参与） -->
  <template v-if="canSelfEval">
    <template v-if="ability">
    <div class="ic-card radar-card">
      <h2 class="section-title">能力雷达图</h2>
      <BaseChart v-if="ability.dimensions.length" :option="radarOption" height="380px" />
      <p v-else class="recommend-empty radar-empty">
        暂无训练提交数据，完成训练题目后即可生成能力雷达图
      </p>
    </div>

    <div class="side-col">
      <div class="ic-card detail-card">
        <h2 class="section-title">维度得分</h2>
        <div v-for="dimension in ability.dimensions" :key="dimension.name" class="dimension-row">
          <div class="dimension-head">
            <span>{{ dimension.name }}</span>
            <strong>{{ dimension.value }}</strong>
          </div>
          <div class="bar-track">
            <div class="bar-fill" :style="{ width: `${dimension.value}%` }" />
          </div>
        </div>
      </div>

      <div class="ic-card report-card">
        <h2 class="section-title">训练建议</h2>
        <p class="report">{{ ability.report }}</p>
      </div>

      <div class="ic-card summary-card">
        <div class="summary-item">
          <span>优势维度</span>
          <strong>{{ strongest.map((item) => item.name).join(' / ') }}</strong>
        </div>
        <div class="summary-item">
          <span>待提升</span>
          <strong>{{ weakest.map((item) => item.name).join(' / ') }}</strong>
        </div>
        <div class="summary-item">
          <span>评估对象</span>
          <strong>{{ userId === 'me' ? `本人（${auth.user?.name}）` : ability.userName }}</strong>
        </div>
      </div>
    </div>
    </template>
    <div v-else-if="!loading" class="ic-card radar-card">
      <p class="recommend-empty radar-empty">能力评估数据加载失败或暂不可用，请稍后重试</p>
    </div>
  </template>

    <!-- 个性化训练推荐：基于能力评估薄弱维度（权限表：全员可用） -->
    <template v-if="!loading">
    <div class="ic-card recommend-card" data-test="recommend-trainings">
      <h2 class="section-title">推荐专项训练</h2>
      <p class="recommend-subtitle">基于能力评估薄弱维度自动匹配</p>
      <div
        v-for="item in trainings"
        :key="item.trainingId"
        class="recommend-item"
        data-test="recommend-training"
        @click="router.push(`/trainings/${item.trainingId}`)"
      >
        <div class="recommend-item-head">
          <span class="recommend-name">{{ item.name }}</span>
          <el-tag size="small" effect="plain" round>{{ trainingTypeMap[item.type] }}</el-tag>
        </div>
        <p class="recommend-reason">{{ item.reason }}</p>
        <p class="recommend-meta">
          {{ formatDateTime(item.startTime) }} ~ {{ formatDateTime(item.endTime) }}
        </p>
      </div>
      <p v-if="!loading && trainings.length === 0" class="recommend-empty">暂无匹配的专项训练</p>
    </div>

    <div class="ic-card recommend-card" data-test="recommend-problems">
      <h2 class="section-title">薄弱知识点习题推荐</h2>
      <p class="recommend-subtitle">按薄弱知识点分组，难度由易到难</p>
      <div v-for="group in problemGroups" :key="group.tagName" class="weak-group">
        <div class="weak-group-head">
          <span class="weak-name">{{ group.tagName }}</span>
          <span class="weak-score">能力值 {{ group.score }}</span>
        </div>
        <div
          v-for="problem in group.problems"
          :key="problem.problemId"
          class="recommend-item"
          data-test="recommend-problem"
          @click="router.push(`/problems/${problem.problemId}`)"
        >
          <div class="recommend-item-head">
            <span class="recommend-name">{{ problem.title }}</span>
            <StatusTag
              :label="difficultyMap[problem.difficulty].label"
              :tone="difficultyMap[problem.difficulty].tone"
            />
          </div>
          <p class="recommend-reason">{{ problem.reason }}</p>
        </div>
      </div>
      <p v-if="!loading && problemGroups.length === 0" class="recommend-empty">
        暂无薄弱知识点推荐
      </p>
    </div>
    </template>
</div>
</template>

<style scoped>
.ability-page {
  display: grid;
  grid-template-columns: 1.1fr 1fr;
  gap: 18px;
  align-items: start;
}

.radar-card {
  padding: 20px 22px 10px;
}

.radar-empty {
  padding: 70px 0;
  text-align: center;
}

.side-col {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.detail-card,
.report-card,
.summary-card,
.recommend-card {
  padding: 20px 22px;
}

.recommend-subtitle {
  margin: -6px 0 14px;
  font-size: 12px;
  color: var(--text-muted);
}

.recommend-item {
  padding: 10px 12px;
  margin-bottom: 10px;
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  background: var(--bg-blue-softer);
  cursor: pointer;
  transition: border-color 0.2s ease, transform 0.2s ease;
}

.recommend-item:hover {
  border-color: var(--primary);
  transform: translateY(-1px);
}

.recommend-item:last-child {
  margin-bottom: 0;
}

.recommend-item-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.recommend-name {
  font-size: 13.5px;
  font-weight: 600;
  color: var(--text-main);
}

.recommend-reason {
  margin: 6px 0 0;
  font-size: 12.5px;
  line-height: 1.7;
  color: var(--text-sub);
}

.recommend-meta {
  margin: 4px 0 0;
  font-size: 12px;
  color: var(--text-muted);
}

.recommend-empty {
  margin: 0;
  font-size: 12.5px;
  color: var(--text-muted);
}

.weak-group {
  margin-bottom: 14px;
}

.weak-group:last-child {
  margin-bottom: 0;
}

.weak-group-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
  font-size: 12.5px;
}

.weak-name {
  font-weight: 600;
  color: var(--primary-deep);
}

.weak-score {
  color: var(--text-muted);
}

.dimension-row {
  margin-bottom: 14px;
}

.dimension-row:last-child {
  margin-bottom: 0;
}

.dimension-head {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  margin-bottom: 6px;
}

.dimension-head span {
  color: var(--text-sub);
}

.dimension-head strong {
  color: var(--primary-deep);
}

.bar-track {
  height: 8px;
  border-radius: var(--radius-pill);
  background: var(--bg-blue-soft);
  overflow: hidden;
}

.bar-fill {
  height: 100%;
  border-radius: var(--radius-pill);
  background: linear-gradient(90deg, #a9cbe8, #5b9bd5);
  transition: width 0.4s ease;
}

.report {
  margin: 0;
  font-size: 13px;
  line-height: 1.9;
  color: var(--text-sub);
}

.summary-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 0;
  border-bottom: 1px dashed var(--border);
  font-size: 13px;
}

.summary-item:last-child {
  border-bottom: none;
}

.summary-item span {
  color: var(--text-muted);
}

@media (max-width: 1100px) {
  .ability-page {
    grid-template-columns: 1fr;
  }
}
</style>
