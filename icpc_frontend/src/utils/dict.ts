/**
 * 统一字典 —— 与接口文档 0.6「业务状态枚举速查」保持一致。
 * TODO(backend): 若后端新增枚举值，请同步在此处补充文案与颜色。
 */
import type {
  ApplyStatus,
  AttendanceStatus,
  CampType,
  ContestCategory,
  ContestStatus,
  Difficulty,
  JudgeResult,
  MemberStatus,
  MemberType,
  MbtiType,
  ProblemVisibility,
  RoleCode,
  TeamStatus,
  TrainingStatus,
} from '@/types'

type Tone = 'primary' | 'success' | 'warning' | 'danger' | 'info'

export const roleLabels: Record<RoleCode, string> = {
  OBSERVE_MEMBER: '观察会员',
  MEMBER: '正式会员',
  TEAM_LEADER: '队长',
  OFFICER: '干事',
  ASSISTANT_COACH: '助理教练',
  PRESIDENT: '会长/副会长',
  ADVISOR: '指导老师',
}

export const memberTypeMap: Record<MemberType, { label: string; tone: Tone }> = {
  OBSERVE_MEMBER: { label: '观察会员', tone: 'info' },
  MEMBER: { label: '正式会员', tone: 'primary' },
}

export const memberStatusMap: Record<MemberStatus, { label: string; tone: Tone }> = {
  ACTIVE: { label: '活跃', tone: 'success' },
  INACTIVE: { label: '不活跃', tone: 'info' },
  RESIGNED: { label: '已退会', tone: 'warning' },
  REMOVED: { label: '已除名', tone: 'danger' },
}

export const applyStatusMap: Record<ApplyStatus, { label: string; tone: Tone }> = {
  PENDING: { label: '待审核', tone: 'warning' },
  APPROVED: { label: '已通过', tone: 'success' },
  REJECTED: { label: '已驳回', tone: 'danger' },
}

export const trainingStatusMap: Record<TrainingStatus, { label: string; tone: Tone }> = {
  DRAFT: { label: '草稿', tone: 'info' },
  PUBLISHED: { label: '已发布', tone: 'primary' },
  ONGOING: { label: '进行中', tone: 'success' },
  ENDED: { label: '已结束', tone: 'info' },
}

export const difficultyMap: Record<Difficulty, { label: string; tone: Tone }> = {
  EASY: { label: '简单', tone: 'success' },
  MEDIUM: { label: '中等', tone: 'warning' },
  HARD: { label: '困难', tone: 'danger' },
}

export const visibilityMap: Record<ProblemVisibility, { label: string; tone: Tone }> = {
  PUBLIC: { label: '公开', tone: 'primary' },
  TRAINING_PRIVATE: { label: '集训私有', tone: 'warning' },
}

export const judgeResultMap: Record<JudgeResult, { label: string; tone: Tone }> = {
  PENDING: { label: '等待中', tone: 'info' },
  JUDGING: { label: '评测中', tone: 'primary' },
  AC: { label: 'AC', tone: 'success' },
  WA: { label: 'WA', tone: 'danger' },
  TLE: { label: 'TLE', tone: 'warning' },
  MLE: { label: 'MLE', tone: 'warning' },
  RE: { label: 'RE', tone: 'danger' },
  CE: { label: 'CE', tone: 'info' },
}

export const trainingTypeMap: Record<'SOLO' | 'TEAM', string> = {
  SOLO: '单人训练',
  TEAM: '组队训练',
}

export const teamStatusMap: Record<TeamStatus, { label: string; tone: Tone }> = {
  PENDING: { label: '待教练确认', tone: 'warning' },
  PENDING_CONFIRM: { label: '待教练确认', tone: 'warning' },
  CONFIRMED: { label: '已确认', tone: 'success' },
  DRAFT: { label: '草稿', tone: 'info' },
  DISSOLVED: { label: '已解散', tone: 'info' },
}

export const campTypeMap: Record<CampType, string> = {
  WINTER: '寒假集训',
  SUMMER: '暑假集训',
}

export const attendanceStatusMap: Record<AttendanceStatus, { label: string; tone: Tone }> = {
  PRESENT: { label: '已签到', tone: 'success' },
  MAKEUP: { label: '补签', tone: 'primary' },
  LEAVE: { label: '请假', tone: 'info' },
  ABSENT: { label: '缺勤', tone: 'danger' },
}

export const contestCategoryMap: Record<ContestCategory, { label: string; tone: Tone }> = {
  SCHOLASTIC: { label: '校赛', tone: 'primary' },
  XCPC: { label: 'XCPC 系列', tone: 'success' },
  NON_XCPC: { label: '非 XCPC 校外赛', tone: 'warning' },
}

export const contestStatusMap: Record<ContestStatus, { label: string; tone: Tone }> = {
  DRAFT: { label: '草稿', tone: 'info' },
  PUBLISHED: { label: '已发布', tone: 'primary' },
  ONGOING: { label: '进行中', tone: 'success' },
  ENDED: { label: '已结束', tone: 'info' },
}

/** 赛事报名方式 */
export const contestApplyTypeMap: Record<'TEAM' | 'SOLO', string> = {
  TEAM: '队伍报名',
  SOLO: '个人报名',
}

/** MBTI 16 型人格中文名称 */
export const mbtiTypeNameMap: Record<MbtiType, string> = {
  INTJ: '建筑师',
  INTP: '逻辑学家',
  ENTJ: '指挥官',
  ENTP: '辩论家',
  INFJ: '提倡者',
  INFP: '调停者',
  ENFJ: '主人公',
  ENFP: '竞选者',
  ISTJ: '物流师',
  ISFJ: '守卫者',
  ESTJ: '总经理',
  ESFJ: '执政官',
  ISTP: '鉴赏家',
  ISFP: '探险家',
  ESTP: '企业家',
  ESFP: '表演者',
}

/** MBTI 维度双向标签 */
export const mbtiPoleLabelMap: Record<'EI' | 'SN' | 'TF' | 'JP', [string, string]> = {
  EI: ['外向 E', '内向 I'],
  SN: ['实感 S', '直觉 N'],
  TF: ['思考 T', '情感 F'],
  JP: ['判断 J', '知觉 P'],
}

export const languages = ['C++17', 'C++20', 'Java 17', 'Python 3.10', 'Go 1.21']

/**
 * 角色常量 —— 严格依据 3.2 权限矩阵（合并版：7 类角色）。
 * 正式会员已含原实验室成员；会长/副会长已含原部长/副部长。
 */

/** 会员类角色 —— 「提交集训报名 / 集训考勤打卡 / 提交代码（训练/比赛）/ 提交退会申请」（干事及以上不参与） */
export const MEMBER_ROLES: RoleCode[] = [
  'OBSERVE_MEMBER',
  'MEMBER',
  'TEAM_LEADER',
]
/** 组队角色 —— 权限表「组队」一行：正式会员、队长（观察会员不可组队） */
export const TEAM_CREATE_ROLES: RoleCode[] = ['MEMBER', 'TEAM_LEADER']
/** 集训私题库可见角色 —— 正式会员及以上（观察会员不可见，干事按权限表可见） */
export const BANK_PRIVATE_ROLES: RoleCode[] = [
  'MEMBER',
  'TEAM_LEADER',
  'OFFICER',
  'ASSISTANT_COACH',
  'PRESIDENT',
  'ADVISOR',
]
/** 题库（公开部分）可见角色 —— 全员只读访问 */
export const BANK_PUBLIC_ROLES: RoleCode[] = [
  'OBSERVE_MEMBER',
  'MEMBER',
  'TEAM_LEADER',
  'OFFICER',
  'ASSISTANT_COACH',
  'PRESIDENT',
  'ADVISOR',
]
/** 会员信息管理角色（新增 / 编辑会员：干事、会长/副会长；逻辑删除仅会长/副会长，页面内单独限定） */
export const MEMBER_INFO_MANAGER_ROLES: RoleCode[] = ['OFFICER', 'PRESIDENT']
/** 入会审批角色（干事、会长/副会长、指导老师；退会 / 除名审批页面内进一步限定 RESIGN_APPROVAL_ROLES） */
export const APPROVAL_MANAGER_ROLES: RoleCode[] = ['OFFICER', 'PRESIDENT', 'ADVISOR']
/** 奖惩管理角色（干事、会长/副会长、指导老师） */
export const REWARD_MANAGER_ROLES: RoleCode[] = ['OFFICER', 'PRESIDENT', 'ADVISOR']
/** 可维护题库 / 训练 / 考核的角色（助理教练、会长/副会长、指导老师） */
export const CONTENT_MANAGER_ROLES: RoleCode[] = ['ASSISTANT_COACH', 'PRESIDENT', 'ADVISOR']
/** 集训报名审核与考勤补签 / 请假 / 缺勤角色（助理教练、会长/副会长、指导老师） */
export const CAMP_MANAGER_ROLES: RoleCode[] = ['ASSISTANT_COACH', 'PRESIDENT', 'ADVISOR']
/** 赛事报名审核角色（与集训审核一致） */
export const CONTEST_MANAGER_ROLES: RoleCode[] = CAMP_MANAGER_ROLES
/** 校赛 / XCPC 报名角色（正式会员、队长） */
export const CONTEST_REGISTER_ROLES: RoleCode[] = ['MEMBER', 'TEAM_LEADER']
/** 退会 / 除名审批角色（仅会长/副会长、指导老师） */
export const RESIGN_APPROVAL_ROLES: RoleCode[] = ['PRESIDENT', 'ADVISOR']
/** 数据统计分析角色（干事、助理教练、会长/副会长、指导老师） */
export const STATISTICS_ROLES: RoleCode[] = ['OFFICER', 'ASSISTANT_COACH', 'PRESIDENT', 'ADVISOR']
/** 系统管理角色（会长/副会长、指导老师） */
export const SYSTEM_MANAGER_ROLES: RoleCode[] = ['PRESIDENT', 'ADVISOR']
/** 批量生成获奖电子证书角色（仅会长/副会长）；证书下载允许本人，见赛事详情页 */
export const CERTIFICATE_MANAGER_ROLES: RoleCode[] = ['PRESIDENT']
/**
 * 能力评估 / MBTI 测评可用角色 —— 干事与指导老师不参与（产品确认）。
 * 查看他人 MBTI 结果为本人 / ASSISTANT_COACH+（见 MBTI_RESULT_VIEW_ROLES）。
 */
export const SELF_EVAL_ROLES: RoleCode[] = [
  'OBSERVE_MEMBER',
  'MEMBER',
  'TEAM_LEADER',
  'ASSISTANT_COACH',
  'PRESIDENT',
]
/** 查看他人 MBTI 结果为本人 / ASSISTANT_COACH+（权限表：指导老师不参与 MBTI） */
export const MBTI_RESULT_VIEW_ROLES: RoleCode[] = ['ASSISTANT_COACH', 'PRESIDENT']

/** 格式化时间：2026-11-15T19:00:00+08:00 -> 2026-11-15 19:00 */
export function formatDateTime(value?: string | null): string {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

/** 格式化日期：2026-11-15 */
export function formatDate(value?: string | null): string {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}
