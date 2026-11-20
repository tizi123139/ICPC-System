/**
 * 全局类型定义 —— 字段与《ICPC 协会管理系统-接口文档.yaml》中的 schema 一一对应。
 * TODO(backend): 接口字段调整时，请同步修改该文件。
 */

/** 通用响应体：GET / POST 等接口统一返回 */
export interface ApiResponse<T = unknown> {
  code: number
  message: string
  data: T
}

/** 分页响应体（ApiResponse.data 的结构） */
export interface PageResult<T> {
  total: number
  page: number
  size: number
  records: T[]
}

/** 通用分页查询参数 */
export interface PageQuery {
  page?: number
  size?: number
}

/**
 * 角色编码。
 * 权限矩阵 v2：正式会员已合并实验室成员（LAB_MEMBER）、
 * 会长/副会长已合并部长/副部长（MINISTER），统一为 7 类角色。
 */
export type RoleCode =
  | 'OBSERVE_MEMBER'
  | 'MEMBER'
  | 'TEAM_LEADER'
  | 'OFFICER'
  | 'ASSISTANT_COACH'
  | 'PRESIDENT'
  | 'ADVISOR'

/** 会员类型 / 会员状态 / 申请状态 */
export type MemberType = 'OBSERVE_MEMBER' | 'MEMBER'
export type MemberStatus = 'ACTIVE' | 'INACTIVE' | 'RESIGNED' | 'REMOVED'
export type ApplyStatus = 'PENDING' | 'APPROVED' | 'REJECTED'

/** 训练状态 / 训练类型 / 题目可见范围 / 难度 */
export type TrainingStatus = 'DRAFT' | 'PUBLISHED' | 'ONGOING' | 'ENDED'
export type TrainingType = 'SOLO' | 'TEAM'
export type ProblemVisibility = 'PUBLIC' | 'TRAINING_PRIVATE'
export type Difficulty = 'EASY' | 'MEDIUM' | 'HARD'

/** 判题结果 */
export type JudgeResult = 'PENDING' | 'JUDGING' | 'AC' | 'WA' | 'TLE' | 'MLE' | 'RE' | 'CE'

/** 登录用户信息 */
export interface UserInfo {
  userId: number
  studentId: string
  name: string
  roles: RoleCode[]
  avatar?: string | null
}

/** POST /auth/login 响应 data */
export interface LoginResult {
  token: string
  expiresIn: number
  user: UserInfo
}

/** 登录请求体 */
export interface LoginRequest {
  studentId: string
  password: string
}

/** 会员信息（GET /members、GET /members/{memberId}） */
export interface MemberInfo {
  memberId: number
  name: string
  studentId: string
  major: string
  grade: string
  phone: string
  email: string
  memberType: MemberType
  department: string | null
  joinDate: string
  status: MemberStatus
  roles?: RoleCode[]
  createTime: string
}

/** 会员列表查询参数 */
export interface MemberQuery extends PageQuery {
  keyword?: string
  memberType?: MemberType | ''
  grade?: string
  department?: string
  status?: MemberStatus | ''
}

/** 入会 / 退会申请记录 */
export interface ApplyRecord {
  applyId: number
  memberId: number
  memberName: string
  studentId: string
  /** JOIN 入会、RESIGN 退会 */
  type: 'JOIN' | 'RESIGN'
  reason: string
  applyTime: string
  status: ApplyStatus
}

/** 会员完整档案（GET /members/{memberId}/archive） */
export interface MemberArchive {
  member: MemberInfo
  trainingCount: number
  submissionCount: number
  acRate: number
  recentTrainings: Array<{
    trainingId: number
    name: string
    solvedCount: number
    submitTime: string
  }>
}

/** 知识点 */
export interface Tag {
  tagId: number
  name: string
  category: string
}

/** 知识点树节点（GET /tags 返回层级结构） */
export interface TagNode extends Tag {
  children?: TagNode[]
}

/** 题解 */
export interface Solution {
  solutionId: number
  content: string
  referenceCode: string | null
  /** 题解附件（POST /files/upload 上传后返回的文件列表，可选） */
  attachments?: UploadResult[]
  updateTime: string
}

/** 题目信息 */
export interface ProblemInfo {
  problemId: number
  title: string
  ojProblemId: string
  difficulty: Difficulty
  timeLimit: number
  memoryLimit: number
  description: string
  inputFormat: string
  outputFormat: string
  sampleInput: string
  sampleOutput: string
  hint: string | null
  tags: Tag[]
  solution: Solution | null
  visibility: ProblemVisibility
}

/** 题目列表查询参数 */
export interface ProblemQuery extends PageQuery {
  keyword?: string
  tagId?: number
  difficulty?: Difficulty | ''
  visibility?: ProblemVisibility | ''
}

/** 新增 / 修改题目请求体 */
export interface ProblemPayload {
  title: string
  ojProblemId: string
  difficulty: Difficulty
  timeLimit: number
  memoryLimit: number
  description: string
  inputFormat: string
  outputFormat: string
  sampleInput: string
  sampleOutput: string
  hint: string | null
  tagIds: number[]
  visibility: ProblemVisibility
}

/** 训练信息 */
export interface TrainingInfo {
  trainingId: number
  name: string
  type: TrainingType
  startTime: string
  endTime: string
  leaderboardVisible: boolean
  freezeTime: number
  problemIds: number[]
  status: TrainingStatus
  creator: UserInfo
  createTime: string
}

/** 训练列表查询参数 */
export interface TrainingQuery extends PageQuery {
  keyword?: string
  status?: TrainingStatus | ''
  type?: TrainingType | ''
}

/** 创建训练请求体 */
export interface TrainingPayload {
  name: string
  type: TrainingType
  startTime: string
  endTime: string
  problemIds: number[]
  leaderboardVisible: boolean
  freezeTime: number
}

/** 代码提交记录 */
export interface SubmitRecord {
  submitId: number
  trainingId: number
  problemId: number
  submitter: UserInfo
  language: string
  result: JudgeResult
  timeUsed: number
  memoryUsed: number
  submitTime: string
}

/** 提交代码请求体 */
export interface SubmitPayload {
  problemId: number
  code: string
  language: string
}

/** 提交接口响应 data */
export interface SubmitResult {
  submitId: number
  result: JudgeResult
}

/** 榜单条目 */
export interface LeaderboardEntry {
  rank: number
  userId: number
  userName: string
  teamId: number | null
  teamName: string | null
  solvedCount: number
  penalty: number
}

/** 榜单响应 data */
export interface LeaderboardResult {
  frozen: boolean
  entries: LeaderboardEntry[]
}

/** 能力雷达维度 */
export interface AbilityDimension {
  name: string
  value: number
}

/** 能力评估响应 data（雷达图 + 文字报告） */
export interface AbilityResult {
  userId: number
  userName: string
  dimensions: AbilityDimension[]
  report: string
}

/** 推荐专项训练（基于能力评估薄弱维度匹配） */
export interface RecommendedTraining {
  trainingId: number
  name: string
  type: TrainingType
  startTime: string
  endTime: string
  status: TrainingStatus
  /** 匹配到的薄弱维度名称 */
  matchedDimensions: string[]
  /** 推荐理由 */
  reason: string
}

/** 推荐习题（按薄弱知识点分组返回） */
export interface RecommendedProblem {
  problemId: number
  title: string
  difficulty: Difficulty
  /** 所属薄弱知识点名称 */
  tagNames: string[]
  /** 推荐理由 */
  reason: string
}

/** 薄弱知识点习题推荐分组 */
export interface WeakTagProblemGroup {
  /** 知识点名称（与能力雷达维度对应） */
  tagName: string
  /** 该知识点能力得分（0-100） */
  score: number
  /** 推荐习题（难度由易到难） */
  problems: RecommendedProblem[]
}

/** 会员结构统计 */
export interface MemberOverview {
  total: number
  byType: Array<{ name: MemberType; value: number }>
  byGrade: Array<{ name: string; value: number }>
  byDepartment: Array<{ name: string; value: number }>
}

/** 训练数据汇总 */
export interface TrainingSummary {
  totalTrainings: number
  totalSubmissions: number
  acRate: number
  trend: Array<{ date: string; submissions: number; ac: number }>
}

/** 文件上传响应 data */
export interface UploadResult {
  url: string
  filename: string
  size: number
}

/**
 * 队伍状态。
 * 视图层沿用 PENDING / CONFIRMED；后端实际枚举为
 * DRAFT / PENDING_CONFIRM / CONFIRMED / DISSOLVED，由 api 层做映射。
 */
export type TeamStatus =
  | 'PENDING'
  | 'PENDING_CONFIRM'
  | 'CONFIRMED'
  | 'DRAFT'
  | 'DISSOLVED'

/** 队伍成员 */
export interface TeamMember {
  memberId: number
  name: string
  studentId: string
  memberType: MemberType
  /** 是否为队长 */
  isLeader: boolean
  joinTime: string
}

/** 队伍信息（GET /teams、GET /teams/{teamId}） */
export interface TeamInfo {
  teamId: number
  name: string
  description: string | null
  status: TeamStatus
  leader: UserInfo
  members: TeamMember[]
  /** 队伍能力画像（雷达图数据） */
  dimensions: AbilityDimension[]
  createTime: string
}

/** 队伍列表查询参数 */
export interface TeamQuery extends PageQuery {
  keyword?: string
  status?: TeamStatus | ''
}

/** 创建 / 更新队伍请求体 */
export interface TeamPayload {
  name: string
  description: string | null
}

/** 集训类型：WINTER 寒假 / SUMMER 暑假 */
export type CampType = 'WINTER' | 'SUMMER'

/** 集训信息（GET /camps、GET /camps/{campId}） */
export interface CampInfo {
  campId: number
  name: string
  type: CampType
  startDate: string
  endDate: string
  registrationDeadline: string
  location: string
  description: string
  creator: UserInfo
  createTime: string
}

/** 集训详情（含任务列表） */
export interface CampDetail extends CampInfo {
  tasks: CampTask[]
}

/** 集训列表查询参数 */
export interface CampQuery extends PageQuery {
  keyword?: string
  type?: CampType | ''
}

/** 发布集训通知请求体 */
export interface CampPayload {
  name: string
  type: CampType
  startDate: string
  endDate: string
  registrationDeadline: string
  location: string
  description: string
}

/** 集训每日任务 */
export interface CampTask {
  taskId: number
  campId: number
  /** 任务所属日期，如 2027-01-15 */
  date: string
  title: string
  content: string
  /** 后端任务 VO 暂未返回发布人，可能为空 */
  publisher?: UserInfo
  /** 后端任务 VO 暂未返回发布时间，可能为空 */
  publishTime?: string
}

/** 发布集训每日任务请求体 */
export interface CampTaskPayload {
  date: string
  title: string
  content: string
}

/** 集训报名记录 */
export interface CampRegistration {
  applyId: number
  campId: number
  memberId: number
  memberName: string
  studentId: string
  grade: string
  applyTime: string
  status: ApplyStatus
  remark: string | null
}

/** 考勤状态：PRESENT 已签到 / MAKEUP 补签 / LEAVE 请假 / ABSENT 缺勤 */
export type AttendanceStatus = 'PRESENT' | 'MAKEUP' | 'LEAVE' | 'ABSENT'

/** 考勤记录 */
export interface AttendanceRecord {
  recordId: number
  campId: number
  memberId: number
  memberName: string
  studentId: string
  date: string
  status: AttendanceStatus
  checkinTime: string | null
  remark: string | null
}

/** 签到打卡请求体 */
export interface CheckinPayload {
  date: string
  remark?: string
}

/* ===================== 赛事管理 ===================== */

/** 赛事分类：SCHOLASTIC 校赛 / XCPC XCPC 系列 / NON_XCPC 非 XCPC 校外赛 */
export type ContestCategory = 'SCHOLASTIC' | 'XCPC' | 'NON_XCPC'

/** 赛事状态（沿用训练状态机：草稿 / 已发布 / 进行中 / 已结束） */
export type ContestStatus = 'DRAFT' | 'PUBLISHED' | 'ONGOING' | 'ENDED'

/** 赛事信息（GET /contests、GET /contests/{contestId}） */
export interface ContestInfo {
  contestId: number
  name: string
  category: ContestCategory
  startTime: string
  endTime: string
  registrationDeadline: string
  description: string
  /** 队伍最大人数（个人赛为 1） */
  maxTeamSize: number
  problemIds: number[]
  /** 结束前封榜分钟数，0 表示不封榜 */
  freezeMinutes: number
  status: ContestStatus
  creator: UserInfo
  createTime: string
}

/** 赛事列表查询参数 */
export interface ContestQuery extends PageQuery {
  keyword?: string
  category?: ContestCategory | ''
}

/** 创建 / 发布赛事请求体 */
export interface ContestPayload {
  name: string
  category: ContestCategory
  startTime: string
  endTime: string
  registrationDeadline: string
  description: string
  maxTeamSize: number
  problemIds: number[]
  freezeMinutes: number
}

/** 报名方式：TEAM 队伍 / SOLO 个人 */
export type ContestApplyType = 'TEAM' | 'SOLO'

/** 赛事报名记录 */
export interface ContestRegistration {
  applyId: number
  contestId: number
  applyType: ContestApplyType
  teamId: number | null
  teamName: string | null
  memberId: number
  memberName: string
  studentId: string
  grade: string
  /** 队伍人数（个人报名为 1） */
  memberCount: number
  applyTime: string
  status: ApplyStatus
  remark: string | null
}

/** 队伍 / 个人报名请求体 */
export interface ContestRegisterPayload {
  applyType: ContestApplyType
  /** 队伍报名时传入队伍 id */
  teamId?: number | null
  remark?: string
}

/** 赛后成绩 / 获奖记录 */
export interface ContestResult {
  resultId: number
  contestId: number
  memberId: number
  memberName: string
  studentId: string
  teamName: string | null
  rank: number | null
  award: string | null
  score: number | null
  remark: string | null
}

/** 登记获奖 / 成绩请求体 */
export interface ContestResultPayload {
  memberId: number
  rank: number | null
  award: string | null
  score: number | null
  remark: string | null
}

/** 电子证书 */
export interface ContestCertificate {
  certId: number
  memberId: number
  memberName: string
  teamName: string | null
  award: string
  certNo: string
}

/** 批量生成证书响应 data */
export interface CertificateBundle {
  contestId: number
  total: number
  generatedTime: string
  certificates: ContestCertificate[]
}

/* ===================== MBTI 管理 ===================== */

/** MBTI 维度组：EI 能量 / SN 信息 / TF 决策 / JP 生活 */
export type MbtiDimension = 'EI' | 'SN' | 'TF' | 'JP'

/** MBTI 单极 */
export type MbtiPole = 'E' | 'I' | 'S' | 'N' | 'T' | 'F' | 'J' | 'P'

/** MBTI 16 型人格 */
export type MbtiType =
  | 'INTJ'
  | 'INTP'
  | 'ENTJ'
  | 'ENTP'
  | 'INFJ'
  | 'INFP'
  | 'ENFJ'
  | 'ENFP'
  | 'ISTJ'
  | 'ISFJ'
  | 'ESTJ'
  | 'ESFJ'
  | 'ISTP'
  | 'ISFP'
  | 'ESTP'
  | 'ESFP'

/** 测评选项 */
export interface MbtiOption {
  /** A / B */
  value: 'A' | 'B'
  label: string
  /** 选择该项后计分的维度极 */
  pole: MbtiPole
}

/** 测评题目（GET /mbti/questions） */
export interface MbtiQuestion {
  questionId: number
  dimension: MbtiDimension
  content: string
  options: [MbtiOption, MbtiOption]
}

/** 作答项 */
export interface MbtiAnswer {
  questionId: number
  value: 'A' | 'B'
}

/** 维度得分（用于结果页双向条） */
export interface MbtiDimensionScore {
  dimension: MbtiDimension
  leftLabel: string
  rightLabel: string
  /** 左极占比 0 ~ 100 */
  leftPercent: number
  preference: 'left' | 'right'
}

/** 测评结果（POST /mbti/submit、GET /mbti/results/{userId}） */
export interface MbtiResult {
  userId: number
  userName: string
  type: MbtiType
  typeName: string
  dimensions: MbtiDimensionScore[]
  description: string
  suggestions: string[]
  submitTime: string
}

/* ===================== 奖惩管理 ===================== */

/** 奖励类别（接口手册 12.1 示例为 COMPETITION，其余枚举 TODO(backend) 与后端对齐） */
export type RewardCategory = 'COMPETITION' | 'TRAINING' | 'ACADEMIC' | 'SERVICE' | 'OTHER'

/** 奖励级别（接口手册 12.1 示例为 SCHOOL，其余枚举 TODO(backend) 与后端对齐） */
export type RewardLevel = 'ASSOCIATION' | 'SCHOOL' | 'PROVINCIAL' | 'NATIONAL' | 'INTERNATIONAL'

/** 奖励记录（GET /rewards、POST /rewards） */
export interface RewardRecord {
  rewardId: number
  memberId: number
  memberName: string
  studentId: string
  /** 奖励名称，如「2026 校赛一等奖」 */
  title: string
  category: RewardCategory
  level: RewardLevel
  /** 奖金 / 奖品金额（元），可为 0 */
  amount: number
  description: string
  /** 颁发日期：2026-11-01 */
  grantDate: string
  /** 颁发单位 */
  issuerName: string
  createTime: string
}

/** 新增奖励请求体（接口手册 12.1） */
export interface RewardPayload {
  memberId: number
  title: string
  category: RewardCategory
  level: RewardLevel
  amount: number
  description: string
  grantDate: string
  issuerName: string
}

/** 处分类型（接口手册 12.2 示例为 WARNING，其余枚举 TODO(backend) 与后端对齐） */
export type PunishmentType = 'WARNING' | 'SERIOUS_WARNING' | 'REVOCATION' | 'EXPULSION'

/** 处分记录（GET /punishments、POST /punishments） */
export interface PunishmentRecord {
  punishmentId: number
  memberId: number
  memberName: string
  studentId: string
  type: PunishmentType
  reason: string
  description: string
  /** 生效日期：2026-11-10 */
  effectiveDate: string
  /** 解除日期：2027-02-10 */
  expireDate: string
  /** 处分单位 */
  issuerName: string
  createTime: string
}

/** 新增处分请求体（接口手册 12.2） */
export interface PunishmentPayload {
  memberId: number
  type: PunishmentType
  reason: string
  description: string
  effectiveDate: string
  expireDate: string
  issuerName: string
}

/* ===================== 考核管理 ===================== */

/** 考核类型（接口手册 13 示例为 OBSERVATION，其余枚举 TODO(backend) 与后端对齐） */
export type AssessmentType = 'OBSERVATION' | 'REGULAR' | 'PROMOTION'

/** 考核状态：创建后为 DRAFT，发布后可录入成绩（PUT 修改仅 DRAFT） */
export type AssessmentStatus = 'DRAFT' | 'PUBLISHED'

/** 考核信息（GET /assessments、POST /assessments） */
export interface AssessmentInfo {
  assessmentId: number
  name: string
  type: AssessmentType
  description: string
  startTime: string
  endTime: string
  /** 通过分数线 */
  passScore: number
  status: AssessmentStatus
  createTime: string
}

/** 创建考核请求体（接口手册 13） */
export interface AssessmentPayload {
  name: string
  type: AssessmentType
  description: string
  startTime: string
  endTime: string
  passScore: number
}

/** 考核成绩记录（GET /assessments/{assessmentId}/results） */
export interface AssessmentResultRecord {
  resultId: number
  assessmentId: number
  memberId: number
  memberName: string
  studentId: string
  score: number
  remark: string | null
  /** 成绩审核状态（POST .../results/{resultId}/audit） */
  auditStatus: ApplyStatus
  auditRemark: string | null
}

/** 批量录入成绩条目 */
export interface AssessmentResultItem {
  memberId: number
  score: number
  remark?: string | null
}

/** 批量录入成绩请求体（接口手册 13：PUT /assessments/{assessmentId}/results） */
export interface AssessmentResultsPayload {
  results: AssessmentResultItem[]
}

/** 审核考核结果请求体（接口手册 13） */
export interface AssessmentAuditPayload {
  status: 'APPROVED' | 'REJECTED'
  auditRemark: string
}

/* ===================== 系统管理 ===================== */

/** 角色字典项（GET /system/roles） */
export interface RoleOption {
  roleCode: RoleCode
  roleName: string
}

/** 操作日志记录（GET /system/logs、GET /system/logs/{logId}） */
export interface SystemLog {
  logId: number
  userId: number
  userName: string
  /** 操作动作，如 LOGIN / CREATE_TRAINING */
  action: string
  detail: string
  ip: string | null
  createTime: string
}

/** 系统配置项（GET /system/configs、PUT /system/configs） */
export interface SystemConfig {
  configKey: string
  configValue: string
  description: string | null
  updateTime: string
}

/** 批量更新配置请求体（接口手册 14） */
export interface SystemConfigsPayload {
  configs: Array<{ configKey: string; configValue: string }>
}

/** 文件记录（GET /system/files、DELETE /system/files/{fileId}） */
export interface SystemFile {
  fileId: number
  filename: string
  url: string
  size: number
  uploaderName: string
  createTime: string
}

/** 调整会员角色请求体（PUT /system/members/{memberId}/roles） */
export interface SystemRolesPayload {
  roles: RoleCode[]
}
