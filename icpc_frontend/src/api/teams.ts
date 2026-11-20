/**
 * 组队管理模块 —— 对应接口文档第 5 章
 * 接口清单：
 *   GET    /teams                                查询队伍列表（已登录）
 *   POST   /teams                                创建队伍（3.2「组队」：观察会员 / 正式会员 / 实验室成员 / 队长）
 *   GET    /teams/{teamId}                       队伍详情（含成员、能力画像）（已登录）
 *   PUT    /teams/{teamId}                       更新队伍信息（队长）
 *   POST   /teams/{teamId}/members               邀请成员（队长）
 *   DELETE /teams/{teamId}/members?memberId=     移除成员 / 离队（队长 / 本人）
 *   POST   /teams/{teamId}/confirm               教练确认队伍（ASSISTANT_COACH+）
 */
import { del, get, post, put } from './request'
import { USE_MOCK } from './mode'
import { getMockCurrentUser, mockDelay, mockMembers, mockTeams, queryTeams } from '@/mock'
import type {
  MemberInfo,
  MemberType,
  PageResult,
  TeamInfo,
  TeamMember,
  TeamPayload,
  TeamQuery,
  TeamStatus,
  UserInfo,
} from '@/types'

/* ===================== 后端传输层结构（与 TeamDtos 对齐） ===================== */

interface RawTeamVO {
  teamId: number
  name: string
  leaderId: number
  memberIds?: number[]
  contestTarget?: string | null
  description?: string | null
  status: string
  createTime?: string
}

interface RawTeamMemberVO {
  memberId: number
  name: string
  studentId: string
  major?: string | null
  grade?: string | null
  memberType: string
  department?: string | null
  joinTime: string
}

interface RawTeamDetailVO {
  basicInfo: RawTeamVO
  members?: RawTeamMemberVO[]
  abilitySummary?: {
    memberCount?: number
    totalSolved?: number
    topTags?: Array<{ tagId: number; tagName: string; solvedCount: number }>
  } | null
}

/** 后端队伍状态 → 视图状态（PENDING_CONFIRM 统一显示为待确认） */
function toTeamStatus(raw: string): TeamStatus {
  if (raw === 'PENDING_CONFIRM' || raw === 'DRAFT') return 'PENDING'
  if (raw === 'CONFIRMED') return 'CONFIRMED'
  if (raw === 'DISSOLVED') return 'DISSOLVED'
  return 'PENDING'
}

function toUserInfo(memberId: number, member?: RawTeamMemberVO): UserInfo {
  return {
    userId: memberId,
    studentId: member?.studentId ?? '',
    name: member?.name ?? '',
    roles: [],
  }
}

/** 队伍详情 → 视图层 TeamInfo */
function toTeamInfo(raw: RawTeamDetailVO): TeamInfo {
  const basic = raw.basicInfo
  const rawMembers = raw.members ?? []
  const members: TeamMember[] = rawMembers.map((member) => ({
    memberId: member.memberId,
    name: member.name,
    studentId: member.studentId,
    memberType: member.memberType as MemberType,
    isLeader: member.memberId === basic.leaderId,
    joinTime: member.joinTime,
  }))
  const leaderMember = rawMembers.find((member) => member.memberId === basic.leaderId)
  return {
    teamId: basic.teamId,
    name: basic.name,
    description: basic.description ?? null,
    status: toTeamStatus(basic.status),
    leader: toUserInfo(basic.leaderId, leaderMember),
    members,
    dimensions: (raw.abilitySummary?.topTags ?? []).map((tag) => ({
      name: tag.tagName,
      value: tag.solvedCount,
    })),
    createTime: basic.createTime ?? '',
  }
}

/** 列表摘要 → 视图层 TeamInfo（无成员姓名时仅保留 id） */
function toTeamInfoBrief(raw: RawTeamVO): TeamInfo {
  const members: TeamMember[] = (raw.memberIds ?? []).map((memberId) => ({
    memberId,
    name: '',
    studentId: '',
    memberType: 'MEMBER',
    isLeader: memberId === raw.leaderId,
    joinTime: '',
  }))
  return {
    teamId: raw.teamId,
    name: raw.name,
    description: raw.description ?? null,
    status: toTeamStatus(raw.status),
    leader: toUserInfo(raw.leaderId),
    members,
    dimensions: [],
    createTime: raw.createTime ?? '',
  }
}

/** 依据会员信息生成队伍成员（用于 Mock 创建队伍 / 邀请成员） */
function toTeamMember(member: MemberInfo, isLeader = false): TeamMember {
  return {
    memberId: member.memberId,
    name: member.name,
    studentId: member.studentId,
    memberType: member.memberType,
    isLeader,
    joinTime: new Date().toISOString(),
  }
}

/** GET /teams —— 查询队伍列表，权限：已登录 */
export async function getTeams(query: TeamQuery): Promise<PageResult<TeamInfo>> {
  // TODO(backend): 联调时删除 mock 分支
  if (USE_MOCK) {
    await mockDelay()
    return queryTeams(query)
  }
  // 后端列表仅返回 leaderId / memberIds，视图展示需要姓名：小分页时逐条补全详情
  const params: Record<string, unknown> = { ...query }
  if (query.status === 'PENDING') {
    params.status = 'PENDING_CONFIRM'
  }
  const raw = await get<PageResult<RawTeamVO>>('/teams', params)
  const briefs = raw.records ?? []
  const records =
    briefs.length <= 20
      ? await Promise.all(
          briefs.map(async (item) => {
            try {
              const detail = await get<RawTeamDetailVO>(`/teams/${item.teamId}`)
              return toTeamInfo(detail)
            } catch {
              return toTeamInfoBrief(item)
            }
          }),
        )
      : briefs.map(toTeamInfoBrief)
  return { ...raw, records }
}

/** GET /teams/{teamId} —— 队伍详情（含成员、能力画像），权限：已登录 */
export async function getTeam(teamId: number): Promise<TeamInfo> {
  // TODO(backend): 联调时删除 mock 分支
  if (USE_MOCK) {
    await mockDelay(300)
    const team = mockTeams.find((item) => item.teamId === teamId)
    if (!team) throw new Error('队伍不存在')
    return team
  }
  const raw = await get<RawTeamDetailVO>(`/teams/${teamId}`)
  return toTeamInfo(raw)
}

/** POST /teams —— 创建队伍（发起组队申请），权限：3.2「组队」一行（观察会员及以上会员类角色） */
export async function createTeam(payload: TeamPayload): Promise<TeamInfo> {
  // TODO(backend): 联调时删除 mock 分支
  if (USE_MOCK) {
    await mockDelay(400)
    const user = getMockCurrentUser()
    const member = mockMembers.find((item) => item.studentId === user.studentId)
    const leaderMember: TeamMember = member
      ? toTeamMember(member, true)
      : {
          memberId: user.userId,
          name: user.name,
          studentId: user.studentId,
          memberType: 'MEMBER',
          isLeader: true,
          joinTime: new Date().toISOString(),
        }
    const team: TeamInfo = {
      teamId: Math.max(0, ...mockTeams.map((item) => item.teamId)) + 1,
      name: payload.name,
      description: payload.description,
      // 新建队伍需等待助理教练确认后方可参赛
      status: 'PENDING',
      leader: user,
      members: [leaderMember],
      dimensions: [],
      createTime: new Date().toISOString(),
    }
    mockTeams.unshift(team)
    return team
  }
  // 后端创建接口必填 contestTarget（前端表单未采集），返回新建队伍 id
  const teamId = await post<number>('/teams', {
    name: payload.name,
    description: payload.description,
    contestTarget: '待定',
  })
  const detail = await get<RawTeamDetailVO>(`/teams/${teamId}`)
  return toTeamInfo(detail)
}

/** PUT /teams/{teamId} —— 更新队伍信息，权限：队长 */
export async function updateTeam(teamId: number, payload: TeamPayload): Promise<void> {
  // TODO(backend): 联调时删除 mock 分支
  if (USE_MOCK) {
    await mockDelay(400)
    const target = mockTeams.find((item) => item.teamId === teamId)
    if (!target) throw new Error('队伍不存在')
    Object.assign(target, payload)
    return
  }
  await put<void>(`/teams/${teamId}`, payload)
}

/** POST /teams/{teamId}/members —— 邀请成员，权限：队长 */
export async function inviteTeamMember(teamId: number, memberId: number): Promise<void> {
  // TODO(backend): 联调时删除 mock 分支
  if (USE_MOCK) {
    await mockDelay(300)
    const team = mockTeams.find((item) => item.teamId === teamId)
    if (!team) throw new Error('队伍不存在')
    const member = mockMembers.find((item) => item.memberId === memberId)
    if (!member) throw new Error('会员不存在')
    if (team.members.some((item) => item.memberId === memberId)) {
      throw new Error('该成员已在队伍中')
    }
    team.members.push(toTeamMember(member))
    return
  }
  await post<void>(`/teams/${teamId}/members`, { memberId })
}

/** DELETE /teams/{teamId}/members —— 移除成员 / 离队，权限：队长 / 本人 */
export async function removeTeamMember(teamId: number, memberId: number): Promise<void> {
  // TODO(backend): 联调时删除 mock 分支
  if (USE_MOCK) {
    await mockDelay(300)
    const team = mockTeams.find((item) => item.teamId === teamId)
    if (!team) throw new Error('队伍不存在')
    if (team.leader.userId === memberId) throw new Error('队长不能直接移出队伍')
    team.members = team.members.filter((item) => item.memberId !== memberId)
    return
  }
  await del<void>(`/teams/${teamId}/members`, { memberId })
}

/** POST /teams/{teamId}/confirm —— 教练确认队伍，权限：ASSISTANT_COACH+ */
export async function confirmTeam(teamId: number): Promise<void> {
  // TODO(backend): 联调时删除 mock 分支
  if (USE_MOCK) {
    await mockDelay(300)
    const team = mockTeams.find((item) => item.teamId === teamId)
    if (!team) throw new Error('队伍不存在')
    team.status = 'CONFIRMED'
    return
  }
  await post<void>(`/teams/${teamId}/confirm`)
}
