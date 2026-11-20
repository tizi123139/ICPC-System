/**
 * 侧边导航配置
 * - roles 为空表示所有登录用户可见；
 * - 未实现的模块不在此注册导航（需要时应先补齐接口设计与路由）。
 */
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

export interface NavItem {
  label: string
  path?: string
  icon: string
  roles?: RoleCode[]
}

export interface NavGroup {
  title: string
  items: NavItem[]
}

export const navGroups: NavGroup[] = [
  {
    title: '概览',
    items: [{ label: '工作台', path: '/dashboard', icon: 'Odometer' }],
  },
  {
    title: '会员',
    items: [
      { label: '会员列表', path: '/members', icon: 'User', roles: MEMBER_INFO_MANAGER_ROLES },
      {
        label: '入会审批',
        path: '/members/approvals',
        icon: 'Checked',
        roles: APPROVAL_MANAGER_ROLES,
      },
      { label: '奖惩管理', path: '/rewards', icon: 'Medal', roles: REWARD_MANAGER_ROLES },
    ],
  },
  {
    title: '题库',
    items: [
      {
        label: '题目管理',
        path: '/problems',
        icon: 'Document',
        roles: BANK_PUBLIC_ROLES,
      },
    ],
  },
  {
    title: '训练',
    items: [
      { label: '训练列表', path: '/trainings', icon: 'DataAnalysis' },
      {
        label: '能力评估',
        path: '/trainings/ability/me',
        icon: 'TrendCharts',
      },
      {
        label: '考核管理',
        path: '/assessments',
        icon: 'List',
      },
    ],
  },
  {
    title: '组队与集训',
    items: [
      { label: '组队管理', path: '/teams', icon: 'UserFilled' },
      { label: '寒假/暑假集训', path: '/camps', icon: 'Sunny' },
    ],
  },
  {
    title: '赛事与测评',
    items: [
      { label: '赛事管理', path: '/contests', icon: 'Trophy' },
      { label: 'MBTI 测评', path: '/mbti', icon: 'MagicStick', roles: SELF_EVAL_ROLES },
    ],
  },
  {
    title: '统计与系统',
    items: [
      {
        label: '数据统计',
        path: '/statistics',
        icon: 'PieChart',
        roles: STATISTICS_ROLES,
      },
      {
        label: '系统管理',
        path: '/system',
        icon: 'Setting',
        roles: SYSTEM_MANAGER_ROLES,
      },
    ],
  },
]
