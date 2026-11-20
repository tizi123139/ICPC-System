import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import {
  APPROVAL_MANAGER_ROLES,
  BANK_PUBLIC_ROLES,
  MEMBER_INFO_MANAGER_ROLES,
  REWARD_MANAGER_ROLES,
  SELF_EVAL_ROLES,
  STATISTICS_ROLES,
  SYSTEM_MANAGER_ROLES,
} from '@/utils/dict'
import type { RoleCode } from '@/types'

/** 路由 meta 约定 */
declare module 'vue-router' {
  interface RouteMeta {
    title?: string
    /** true 表示无需登录 */
    public?: boolean
    /** 允许访问的角色，空表示登录即可 */
    roles?: RoleCode[]
  }
}

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/auth/LoginView.vue'),
      meta: { title: '登录', public: true },
    },
    {
      path: '/',
      component: () => import('@/layouts/AppLayout.vue'),
      redirect: '/dashboard',
      children: [
        {
          path: 'dashboard',
          name: 'dashboard',
          component: () => import('@/views/dashboard/DashboardView.vue'),
          meta: { title: '工作台' },
        },
        {
          path: 'members',
          name: 'members',
          component: () => import('@/views/members/MembersView.vue'),
          // 会员信息管理：干事、会长/副会长（逻辑删除仅会长/副会长，页面内限定）
          meta: { title: '会员列表', roles: MEMBER_INFO_MANAGER_ROLES },
        },
        {
          path: 'members/approvals',
          name: 'member-approvals',
          component: () => import('@/views/members/ApprovalsView.vue'),
          // 入会审批：干事、会长/副会长、指导老师（退会/除名审批按钮在页面内进一步限定为会长/副会长+）
          meta: { title: '入会审批', roles: APPROVAL_MANAGER_ROLES },
        },
        {
          path: 'members/:id',
          name: 'member-detail',
          component: () => import('@/views/members/MemberDetailView.vue'),
          // 查看个人档案：全员
          meta: { title: '会员档案' },
        },
        {
          path: 'problems',
          name: 'problems',
          component: () => import('@/views/problems/ProblemsView.vue'),
          // 题库（公开部分）：除干事外均只读可访问
          meta: { title: '题目管理', roles: BANK_PUBLIC_ROLES },
        },
        {
          path: 'problems/:id',
          name: 'problem-detail',
          component: () => import('@/views/problems/ProblemDetailView.vue'),
          meta: { title: '题目详情', roles: BANK_PUBLIC_ROLES },
        },
        {
          path: 'trainings',
          name: 'trainings',
          component: () => import('@/views/trainings/TrainingsView.vue'),
          // 查看训练榜单：全员；创建/发布/提交等操作在页面内受限
          meta: { title: '训练列表' },
        },
        {
          path: 'trainings/ability/:userId',
          name: 'training-ability',
          component: () => import('@/views/trainings/AbilityView.vue'),
          // 个性化训练推荐：全员可用；能力评估（雷达图/建议）在页面内限定 SELF_EVAL_ROLES（干事、指导老师不参与）
          meta: { title: '能力评估' },
        },
        {
          path: 'trainings/:id',
          name: 'training-detail',
          component: () => import('@/views/trainings/TrainingDetailView.vue'),
          meta: { title: '训练详情' },
        },
        {
          path: 'teams',
          name: 'teams',
          component: () => import('@/views/teams/TeamsView.vue'),
          // 组队操作为 MEMBER_ROLES，页面内受限；列表登录即可查看
          meta: { title: '组队管理' },
        },
        {
          path: 'teams/:id',
          name: 'team-detail',
          component: () => import('@/views/teams/TeamDetailView.vue'),
          meta: { title: '队伍详情' },
        },
        {
          path: 'camps',
          name: 'camps',
          component: () => import('@/views/camps/CampsView.vue'),
          meta: { title: '集训管理' },
        },
        {
          path: 'camps/:id',
          name: 'camp-detail',
          component: () => import('@/views/camps/CampDetailView.vue'),
          meta: { title: '集训详情' },
        },
        {
          path: 'contests',
          name: 'contests',
          component: () => import('@/views/contests/ContestsView.vue'),
          meta: { title: '赛事管理' },
        },
        {
          path: 'contests/:id',
          name: 'contest-detail',
          component: () => import('@/views/contests/ContestDetailView.vue'),
          meta: { title: '赛事详情' },
        },
        {
          path: 'mbti',
          name: 'mbti',
          component: () => import('@/views/mbti/MbtiView.vue'),
          // MBTI 测评：干事与指导老师不参与
          meta: { title: 'MBTI 测评', roles: SELF_EVAL_ROLES },
        },
        {
          path: 'statistics',
          name: 'statistics',
          component: () => import('@/views/statistics/StatisticsView.vue'),
          // 数据统计分析：干事、助理教练、会长/副会长、指导老师
          meta: { title: '数据统计', roles: STATISTICS_ROLES },
        },
        {
          path: 'rewards',
          name: 'rewards',
          component: () => import('@/views/rewards/RewardsView.vue'),
          // 奖惩管理：干事、会长/副会长、指导老师
          meta: { title: '奖惩管理', roles: REWARD_MANAGER_ROLES },
        },
        {
          path: 'assessments',
          name: 'assessments',
          component: () => import('@/views/assessments/AssessmentsView.vue'),
          // 查看考核 / 查询考核结果：全员可见；创建 / 发布 / 录入成绩在页面内限定为助理教练+
          meta: { title: '考核管理' },
        },
        {
          path: 'system',
          name: 'system',
          component: () => import('@/views/system/SystemView.vue'),
          // 系统管理：会长/副会长、指导老师
          meta: { title: '系统管理', roles: SYSTEM_MANAGER_ROLES },
        },
      ],
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'not-found',
      component: () => import('@/views/NotFoundView.vue'),
      meta: { title: '页面不存在' },
    },
  ],
  scrollBehavior: () => ({ top: 0 }),
})

/** 路由守卫：登录校验 + 角色校验（RBAC 第二层） */
router.beforeEach((to) => {
  const auth = useAuthStore()

  if (to.meta.public) {
    return true
  }

  if (!auth.isLoggedIn) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }

  if (to.meta.roles && !auth.hasRole(to.meta.roles)) {
    ElMessage.warning('当前角色无权访问该页面')
    return { name: 'dashboard' }
  }

  return true
})

export default router
