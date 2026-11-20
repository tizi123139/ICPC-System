/**
 * 本地 Mock 数据（仅用于后端未就绪时联调/演示）
 * 后端接入方式：将 .env.development 中的 VITE_USE_MOCK 改为 false，
 * 各 api 模块会自动切换到真实请求分支（见 src/api/*.ts）。
 * TODO(backend): 本文件仅作演示数据，联调完成后可整体移除。
 */
import type {
  AbilityResult,
  ApplyRecord,
  AssessmentInfo,
  AssessmentResultRecord,
  AttendanceRecord,
  CampInfo,
  CampQuery,
  CampRegistration,
  CampTask,
  ContestInfo,
  ContestQuery,
  ContestRegistration,
  ContestResult,
  LeaderboardResult,
  LoginResult,
  MemberInfo,
  MemberOverview,
  MemberQuery,
  MbtiAnswer,
  MbtiDimension,
  MbtiDimensionScore,
  MbtiPole,
  MbtiQuestion,
  MbtiResult,
  MbtiType,
  ProblemInfo,
  ProblemQuery,
  RecommendedProblem,
  RecommendedTraining,
  PunishmentRecord,
  RewardRecord,
  RoleCode,
  SubmitRecord,
  SystemConfig,
  SystemFile,
  SystemLog,
  TagNode,
  TeamInfo,
  TeamMember,
  TeamQuery,
  TrainingInfo,
  TrainingQuery,
  TrainingSummary,
  UserInfo,
  WeakTagProblemGroup,
} from '@/types'
import { mbtiPoleLabelMap, mbtiTypeNameMap } from '@/utils/dict'

export const mockDelay = (ms = 500) => new Promise((resolve) => setTimeout(resolve, ms))

export function paginate<T>(list: T[], page = 1, size = 20) {
  const start = (page - 1) * size
  return {
    total: list.length,
    page,
    size,
    records: list.slice(start, start + size),
  }
}

/* ===================== 用户与登录 ===================== */
/**
 * 演示账号：学号 -> 固定身份。
 * 学号按权限表角色顺序编号：1001 观察会员、1002-1003 正式会员、1004 队长、
 * 1005 干事、1006 助理教练、1007-1008 会长/副会长、1009 指导老师。
 * TODO(backend): 正式环境由后端校验账号密码并返回真实角色，本表仅用于前端演示。
 * 密码不参与 Mock 校验（任意密码均可登录），真实鉴权由后端完成。
 */
export const mockAccounts: Record<string, UserInfo> = {
  // 观察会员（R01）
  '1001': { userId: 3, studentId: '1001', name: '王一鸣', roles: ['OBSERVE_MEMBER'], avatar: null },
  // 正式会员（R02）
  '1002': { userId: 6, studentId: '1002', name: '徐清越', roles: ['MEMBER'], avatar: null },
  // 正式会员（原实验室成员，已合并）
  '1003': { userId: 5, studentId: '1003', name: '赵子轩', roles: ['MEMBER'], avatar: null },
  // 队长（R03）
  '1004': { userId: 11, studentId: '1004', name: '黄启帆', roles: ['TEAM_LEADER'], avatar: null },
  // 干事（R04）
  '1005': { userId: 9, studentId: '1005', name: '林可欣', roles: ['OFFICER'], avatar: null },
  // 助理教练（R05）
  '1006': { userId: 4, studentId: '1006', name: '周雨桐', roles: ['ASSISTANT_COACH'], avatar: null },
  // 会长/副会长（R07）
  '1007': { userId: 1, studentId: '1007', name: '陈昊', roles: ['PRESIDENT'], avatar: null },
  // 会长/副会长（原部长，已合并）
  '1008': { userId: 2, studentId: '1008', name: '李知远', roles: ['PRESIDENT'], avatar: null },
  // 指导老师（R06）
  '1009': { userId: 20, studentId: '1009', name: '顾明', roles: ['ADVISOR'], avatar: null },
}

/** 默认演示账号（会长），用于无登录态的 Mock 场景 */
export const mockUser: UserInfo = mockAccounts['1007']!

/**
 * 按学号生成登录结果：学号决定固定身份。
 * TODO(backend): 真实登录需校验密码并签发 JWT，此函数联调时删除。
 */
export function createMockLoginResult(studentId: string): LoginResult {
  const account = mockAccounts[studentId.trim()]
  if (!account) throw new Error('该学号未注册，请使用 1001 - 1009 演示账号')
  return { token: `mock-token-${account.studentId}`, expiresIn: 7200, user: account }
}

/** 根据 Mock token 反查登录用户（token 形如 mock-token-1001） */
export function resolveMockTokenUser(token: string): UserInfo | null {
  const prefix = 'mock-token-'
  if (!token.startsWith(prefix)) return null
  return mockAccounts[token.slice(prefix.length)] ?? null
}

/** 读取当前登录用户（Mock 提交、创建人等场景使用） */
export function getMockCurrentUser(): UserInfo {
  try {
    const raw = localStorage.getItem('icpc_user')
    if (raw) return JSON.parse(raw) as UserInfo
  } catch {
    // 忽略解析异常，回退到默认演示账号
  }
  return mockUser
}

/* ===================== 会员 ===================== */
const memberSeed: Array<Omit<MemberInfo, 'memberId' | 'createTime'>> = [
  {
    name: '陈昊',
    studentId: '1007',
    major: '计算机科学与技术',
    grade: '2023级',
    phone: '13800000001',
    email: 'chenhao@example.com',
    memberType: 'MEMBER',
    department: '竞赛部',
    joinDate: '2023-09-20',
    status: 'ACTIVE',
  },
  {
    name: '李知远',
    studentId: '1008',
    major: '软件工程',
    grade: '2023级',
    phone: '13800000002',
    email: 'lizhiyuan@example.com',
    memberType: 'MEMBER',
    department: '竞赛部',
    joinDate: '2023-10-08',
    status: 'ACTIVE',
  },
  {
    name: '王一鸣',
    studentId: '1001',
    major: '计算机科学与技术',
    grade: '2024级',
    phone: '13800000003',
    email: 'wangyiming@example.com',
    memberType: 'OBSERVE_MEMBER',
    department: null,
    joinDate: '2026-10-02',
    status: 'ACTIVE',
  },
  {
    name: '周雨桐',
    studentId: '1006',
    major: '数据科学与大数据技术',
    grade: '2024级',
    phone: '13800000004',
    email: 'zhouyutong@example.com',
    memberType: 'MEMBER',
    department: '竞赛部',
    joinDate: '2024-09-15',
    status: 'ACTIVE',
  },
  {
    name: '赵子轩',
    studentId: '1003',
    major: '网络工程',
    grade: '2023级',
    phone: '13800000005',
    email: 'zhaozixuan@example.com',
    memberType: 'MEMBER',
    department: '组织部',
    joinDate: '2023-10-12',
    status: 'INACTIVE',
  },
  {
    name: '徐清越',
    studentId: '1002',
    major: '计算机科学与技术',
    grade: '2022级',
    phone: '13800000006',
    email: 'xuqingyue@example.com',
    memberType: 'MEMBER',
    department: '竞赛部',
    joinDate: '2022-09-18',
    status: 'ACTIVE',
  },
  {
    name: '孙嘉树',
    studentId: '2024310211',
    major: '人工智能',
    grade: '2024级',
    phone: '13800000007',
    email: 'sunjiaishu@example.com',
    memberType: 'OBSERVE_MEMBER',
    department: null,
    joinDate: '2026-10-09',
    status: 'ACTIVE',
  },
  {
    name: '何思远',
    studentId: '2022320118',
    major: '软件工程',
    grade: '2022级',
    phone: '13800000008',
    email: 'hesiyuan@example.com',
    memberType: 'MEMBER',
    department: '宣传部',
    joinDate: '2022-10-20',
    status: 'RESIGNED',
  },
  {
    name: '林可欣',
    studentId: '1005',
    major: '计算机科学与技术',
    grade: '2023级',
    phone: '13800000009',
    email: 'linkexin@example.com',
    memberType: 'MEMBER',
    department: '秘书部',
    joinDate: '2023-09-25',
    status: 'ACTIVE',
  },
  {
    name: '郑浩然',
    studentId: '2024220155',
    major: '数据科学与大数据技术',
    grade: '2024级',
    phone: '13800000010',
    email: 'zhenghaoran@example.com',
    memberType: 'OBSERVE_MEMBER',
    department: null,
    joinDate: '2026-10-15',
    status: 'ACTIVE',
  },
  {
    name: '黄启帆',
    studentId: '1004',
    major: '网络工程',
    grade: '2023级',
    phone: '13800000011',
    email: 'huangqifan@example.com',
    memberType: 'MEMBER',
    department: '竞赛部',
    joinDate: '2023-09-28',
    status: 'ACTIVE',
  },
  {
    name: '吴思琪',
    studentId: '2022120144',
    major: '软件工程',
    grade: '2022级',
    phone: '13800000012',
    email: 'wusiqi@example.com',
    memberType: 'MEMBER',
    department: '组织部',
    joinDate: '2022-10-05',
    status: 'REMOVED',
  },
]

export const mockMembers: MemberInfo[] = memberSeed.map((item, index) => ({
  ...item,
  memberId: index + 1,
  createTime: `${item.joinDate}T09:00:00+08:00`,
}))

export const mockApprovals: ApplyRecord[] = [
  {
    applyId: 1,
    memberId: 13,
    memberName: '刘星宇',
    studentId: '2025210333',
    type: 'JOIN',
    reason: '对算法竞赛有浓厚兴趣，已自学完成基础数据结构，希望加入协会接受系统训练。',
    applyTime: '2026-11-03T10:22:00+08:00',
    status: 'PENDING',
  },
  {
    applyId: 2,
    memberId: 14,
    memberName: '马若彤',
    studentId: '2025220211',
    type: 'JOIN',
    reason: '参加过校内程序设计新生赛并获得二等奖，希望加入协会继续提升。',
    applyTime: '2026-11-02T16:40:00+08:00',
    status: 'PENDING',
  },
  {
    applyId: 3,
    memberId: 15,
    memberName: '高子恒',
    studentId: '2024310405',
    type: 'JOIN',
    reason: '希望通过协会的训练体系备战明年的校赛。',
    applyTime: '2026-11-01T20:05:00+08:00',
    status: 'PENDING',
  },
  {
    applyId: 4,
    memberId: 8,
    memberName: '何思远',
    studentId: '2022320118',
    type: 'RESIGN',
    reason: '课业压力较大，暂时无法参加日常训练，申请退会。',
    applyTime: '2026-10-30T09:12:00+08:00',
    status: 'PENDING',
  },
  {
    applyId: 5,
    memberId: 16,
    memberName: '唐可',
    studentId: '2023310222',
    type: 'JOIN',
    reason: '已完成观察期考核，申请转为正式会员。',
    applyTime: '2026-10-28T14:30:00+08:00',
    status: 'APPROVED',
  },
]

/* ===================== 知识点 / 题库 ===================== */
export const mockTagTree: TagNode[] = [
  {
    tagId: 1,
    name: '基础算法',
    category: '算法',
    children: [
      { tagId: 11, name: '枚举', category: '基础算法' },
      { tagId: 12, name: '模拟', category: '基础算法' },
      { tagId: 13, name: '排序', category: '基础算法' },
      { tagId: 14, name: '二分', category: '基础算法' },
      { tagId: 15, name: '前缀和', category: '基础算法' },
    ],
  },
  {
    tagId: 2,
    name: '动态规划',
    category: '算法',
    children: [
      { tagId: 21, name: '线性 DP', category: '动态规划' },
      { tagId: 22, name: '背包问题', category: '动态规划' },
      { tagId: 23, name: '区间 DP', category: '动态规划' },
      { tagId: 24, name: '树形 DP', category: '动态规划' },
    ],
  },
  {
    tagId: 3,
    name: '图论',
    category: '算法',
    children: [
      { tagId: 31, name: '最短路', category: '图论' },
      { tagId: 32, name: '最小生成树', category: '图论' },
      { tagId: 33, name: '拓扑排序', category: '图论' },
      { tagId: 34, name: '网络流', category: '图论' },
    ],
  },
  {
    tagId: 4,
    name: '数据结构',
    category: '算法',
    children: [
      { tagId: 41, name: '并查集', category: '数据结构' },
      { tagId: 42, name: '线段树', category: '数据结构' },
      { tagId: 43, name: '树状数组', category: '数据结构' },
      { tagId: 44, name: '单调栈', category: '数据结构' },
    ],
  },
  {
    tagId: 5,
    name: '数学',
    category: '算法',
    children: [
      { tagId: 51, name: '数论', category: '数学' },
      { tagId: 52, name: '组合数学', category: '数学' },
      { tagId: 53, name: '概率与期望', category: '数学' },
    ],
  },
  {
    tagId: 6,
    name: '字符串',
    category: '算法',
    children: [
      { tagId: 61, name: 'KMP', category: '字符串' },
      { tagId: 62, name: '哈希', category: '字符串' },
      { tagId: 63, name: '字典树', category: '字符串' },
    ],
  },
]

const tagMap = new Map<number, { tagId: number; name: string; category: string }>()
mockTagTree.forEach((node) => {
  tagMap.set(node.tagId, { tagId: node.tagId, name: node.name, category: node.category })
  node.children?.forEach((child) => tagMap.set(child.tagId, { tagId: child.tagId, name: child.name, category: child.category }))
})

const buildTags = (ids: number[]) => ids.map((id) => tagMap.get(id)!).filter(Boolean)

export const mockProblems: ProblemInfo[] = [
  {
    problemId: 100,
    title: 'A + B Problem',
    ojProblemId: '1000',
    difficulty: 'EASY',
    timeLimit: 1000,
    memoryLimit: 131072,
    description: '输入两个整数 A 和 B，输出它们的和。\n\n这是评测系统的入门题目，用于检查代码提交链路是否正常。',
    inputFormat: '一行两个整数 A、B（-10^9 ≤ A, B ≤ 10^9）。',
    outputFormat: '一个整数，表示 A + B。',
    sampleInput: '1 2',
    sampleOutput: '3',
    hint: '注意使用 long long，避免溢出。',
    tags: buildTags([11, 12]),
    visibility: 'PUBLIC',
    solution: {
      solutionId: 1,
      content: '## 思路\n直接读入两个整数并输出它们的和即可。\n\n## 复杂度\n- 时间复杂度：O(1)\n- 空间复杂度：O(1)',
      referenceCode: '#include <iostream>\nusing namespace std;\nint main() {\n    long long a, b;\n    cin >> a >> b;\n    cout << a + b << endl;\n    return 0;\n}',
      updateTime: '2026-10-12T10:00:00+08:00',
    },
  },
  {
    problemId: 101,
    title: '区间求和',
    ojProblemId: '1001',
    difficulty: 'EASY',
    timeLimit: 1000,
    memoryLimit: 131072,
    description: '给定一个长度为 n 的数组，回答 m 次区间 [l, r] 的和。',
    inputFormat: '第一行两个整数 n、m；第二行 n 个整数；接下来 m 行每行两个整数 l、r。',
    outputFormat: '对每次询问输出一行，表示区间和。',
    sampleInput: '5 3\n1 2 3 4 5\n1 3\n2 4\n1 5',
    sampleOutput: '6\n9\n15',
    hint: '前缀和模板题。',
    tags: buildTags([15, 11]),
    visibility: 'PUBLIC',
    solution: {
      solutionId: 2,
      content: '## 思路\n预处理前缀和数组，`sum[r] - sum[l-1]` 即为答案。',
      referenceCode: null,
      updateTime: '2026-10-15T09:30:00+08:00',
    },
  },
  {
    problemId: 102,
    title: '01 背包',
    ojProblemId: '1002',
    difficulty: 'MEDIUM',
    timeLimit: 1000,
    memoryLimit: 131072,
    description: '有 n 件物品和一个容量为 V 的背包，每件物品只能选一次，求最大价值。',
    inputFormat: '第一行 n、V；接下来 n 行每行两个整数 w、v。',
    outputFormat: '一个整数，表示最大总价值。',
    sampleInput: '4 5\n1 2\n2 4\n3 4\n4 5',
    sampleOutput: '8',
    hint: '经典 01 背包，注意一维数组需要倒序枚举。',
    tags: buildTags([22, 21]),
    visibility: 'PUBLIC',
    solution: {
      solutionId: 3,
      content: '## 思路\n定义 `dp[j]` 为容量 j 时的最大价值，倒序更新避免重复选取。',
      referenceCode: null,
      updateTime: '2026-10-20T14:00:00+08:00',
    },
  },
  {
    problemId: 103,
    title: '单源最短路',
    ojProblemId: '1003',
    difficulty: 'MEDIUM',
    timeLimit: 2000,
    memoryLimit: 262144,
    description: '给定一个 n 个点 m 条边的有向图，求从 1 号点到其他所有点的最短距离。',
    inputFormat: '第一行 n、m；接下来 m 行每行三个整数 u、v、w。',
    outputFormat: '一行 n 个整数，表示最短距离，不可达输出 -1。',
    sampleInput: '4 5\n1 2 1\n1 3 4\n2 3 1\n2 4 5\n3 4 1',
    sampleOutput: '0 1 2 3',
    hint: 'Dijkstra + 优先队列。',
    tags: buildTags([31, 42]),
    visibility: 'PUBLIC',
    solution: null,
  },
  {
    problemId: 104,
    title: '并查集合并',
    ojProblemId: '1004',
    difficulty: 'EASY',
    timeLimit: 1000,
    memoryLimit: 131072,
    description: '维护 n 个元素的集合，支持合并与查询是否属于同一集合。',
    inputFormat: '第一行 n、m；接下来 m 行每行一个操作。',
    outputFormat: '对每个查询输出 Yes 或 No。',
    sampleInput: '4 5\n1 1 2\n2 1 2\n1 3 4\n2 3 4\n2 1 4',
    sampleOutput: 'Yes\nYes\nNo',
    hint: '路径压缩模板。',
    tags: buildTags([41]),
    visibility: 'PUBLIC',
    solution: null,
  },
  {
    problemId: 105,
    title: '最长上升子序列',
    ojProblemId: '1005',
    difficulty: 'MEDIUM',
    timeLimit: 1000,
    memoryLimit: 131072,
    description: '给定长度为 n 的序列，求最长严格上升子序列的长度。',
    inputFormat: '第一行 n；第二行 n 个整数。',
    outputFormat: '一个整数。',
    sampleInput: '8\n10 9 2 5 3 7 101 18',
    sampleOutput: '4',
    hint: '可以尝试 O(n log n) 的贪心 + 二分做法。',
    tags: buildTags([21, 14]),
    visibility: 'PUBLIC',
    solution: null,
  },
  {
    problemId: 106,
    title: '线段树区间加',
    ojProblemId: '1006',
    difficulty: 'HARD',
    timeLimit: 2000,
    memoryLimit: 262144,
    description: '维护一个序列，支持区间加与区间求和。',
    inputFormat: '第一行 n、m；第二行 n 个整数；接下来 m 行操作。',
    outputFormat: '对每个求和操作输出结果。',
    sampleInput: '5 5\n1 2 3 4 5\n1 1 3 1\n2 1 3',
    sampleOutput: '9',
    hint: '懒标记下传是核心。',
    tags: buildTags([42, 21]),
    visibility: 'TRAINING_PRIVATE',
    solution: null,
  },
  {
    problemId: 107,
    title: '拓扑排序',
    ojProblemId: '1007',
    difficulty: 'MEDIUM',
    timeLimit: 1000,
    memoryLimit: 131072,
    description: '给定有向图，输出任意一个拓扑序，若存在环输出 -1。',
    inputFormat: '第一行 n、m；接下来 m 行每行一条边。',
    outputFormat: '一行拓扑序或 -1。',
    sampleInput: '4 4\n1 2\n1 3\n2 4\n3 4',
    sampleOutput: '1 3 2 4',
    hint: 'Kahn 算法。',
    tags: buildTags([33]),
    visibility: 'PUBLIC',
    solution: null,
  },
  {
    problemId: 108,
    title: '字符串哈希',
    ojProblemId: '1008',
    difficulty: 'MEDIUM',
    timeLimit: 1000,
    memoryLimit: 131072,
    description: '判断字符串的两个子串是否相同。',
    inputFormat: '第一行字符串 s；第二行 m；接下来 m 行每行四个整数 l1 r1 l2 r2。',
    outputFormat: '每次询问输出 Yes 或 No。',
    sampleInput: 'abcabc\n2\n1 3 4 6\n1 2 2 3',
    sampleOutput: 'Yes\nNo',
    hint: '双哈希更稳妥。',
    tags: buildTags([62]),
    visibility: 'PUBLIC',
    solution: null,
  },
  {
    problemId: 109,
    title: 'KMP 匹配',
    ojProblemId: '1009',
    difficulty: 'HARD',
    timeLimit: 1000,
    memoryLimit: 131072,
    description: '在文本串中查找模式串所有出现位置。',
    inputFormat: '第一行文本串；第二行模式串。',
    outputFormat: '所有出现位置，从 1 开始编号。',
    sampleInput: 'abababa\naba',
    sampleOutput: '1 3 5',
    hint: '注意 next 数组的构建。',
    tags: buildTags([61]),
    visibility: 'TRAINING_PRIVATE',
    solution: null,
  },
  {
    problemId: 110,
    title: '组合数取模',
    ojProblemId: '1010',
    difficulty: 'MEDIUM',
    timeLimit: 1000,
    memoryLimit: 131072,
    description: '求 C(n, m) mod 1e9+7。',
    inputFormat: '第一行 T；接下来 T 行每行两个整数 n、m。',
    outputFormat: '每行一个答案。',
    sampleInput: '3\n5 2\n10 5\n100 50',
    sampleOutput: '10\n252\n538992043',
    hint: '预处理阶乘与逆元。',
    tags: buildTags([52, 51]),
    visibility: 'PUBLIC',
    solution: null,
  },
  {
    problemId: 111,
    title: '树的重心',
    ojProblemId: '1011',
    difficulty: 'HARD',
    timeLimit: 2000,
    memoryLimit: 262144,
    description: '给定一棵树，求树的重心编号。',
    inputFormat: '第一行 n；接下来 n-1 行每行一条边。',
    outputFormat: '输出重心编号，多个输出最小的。',
    sampleInput: '5\n1 2\n2 3\n3 4\n4 5',
    sampleOutput: '3',
    hint: '一遍 DFS 统计子树大小。',
    tags: buildTags([24, 42]),
    visibility: 'TRAINING_PRIVATE',
    solution: null,
  },
]

/* ===================== 训练 ===================== */
const mockCreator: UserInfo = {
  userId: 2,
  studentId: '2022120321',
  name: '徐清越',
  roles: ['ASSISTANT_COACH'],
  avatar: null,
}

export const mockTrainings: TrainingInfo[] = [
  {
    trainingId: 1,
    name: '第十周队内训练赛',
    type: 'SOLO',
    startTime: '2026-11-04T19:00:00+08:00',
    endTime: '2026-11-04T22:00:00+08:00',
    leaderboardVisible: true,
    freezeTime: 30,
    problemIds: [100, 101, 102, 103, 104],
    status: 'ENDED',
    creator: mockCreator,
    createTime: '2026-10-30T10:00:00+08:00',
  },
  {
    trainingId: 2,
    name: 'DP 专项训练（一）',
    type: 'SOLO',
    startTime: '2026-11-06T19:00:00+08:00',
    endTime: '2026-11-06T22:00:00+08:00',
    leaderboardVisible: true,
    freezeTime: 30,
    problemIds: [102, 105, 106],
    status: 'PUBLISHED',
    creator: mockCreator,
    createTime: '2026-11-01T14:20:00+08:00',
  },
  {
    trainingId: 3,
    name: '图论专题训练',
    type: 'TEAM',
    startTime: '2026-11-08T19:00:00+08:00',
    endTime: '2026-11-08T22:00:00+08:00',
    leaderboardVisible: true,
    freezeTime: 0,
    problemIds: [103, 104, 107, 111],
    status: 'PUBLISHED',
    creator: mockCreator,
    createTime: '2026-11-02T09:40:00+08:00',
  },
  {
    trainingId: 4,
    name: '字符串与哈希',
    type: 'SOLO',
    startTime: '2026-11-11T19:00:00+08:00',
    endTime: '2026-11-11T22:00:00+08:00',
    leaderboardVisible: false,
    freezeTime: 20,
    problemIds: [108, 109],
    status: 'PUBLISHED',
    creator: mockCreator,
    createTime: '2026-11-03T11:10:00+08:00',
  },
  {
    trainingId: 5,
    name: '新生基础算法训练',
    type: 'SOLO',
    startTime: '2026-11-13T19:00:00+08:00',
    endTime: '2026-11-13T21:30:00+08:00',
    leaderboardVisible: true,
    freezeTime: 0,
    problemIds: [100, 101, 104],
    status: 'DRAFT',
    creator: mockCreator,
    createTime: '2026-11-04T16:00:00+08:00',
  },
  {
    trainingId: 6,
    name: '第九周队内训练赛',
    type: 'TEAM',
    startTime: '2026-10-28T19:00:00+08:00',
    endTime: '2026-10-28T22:00:00+08:00',
    leaderboardVisible: true,
    freezeTime: 30,
    problemIds: [101, 102, 103, 105, 107],
    status: 'ENDED',
    creator: mockCreator,
    createTime: '2026-10-24T09:00:00+08:00',
  },
]

export const mockSubmissions: SubmitRecord[] = [
  {
    submitId: 5001,
    trainingId: 1,
    problemId: 100,
    submitter: mockUser,
    language: 'C++17',
    result: 'AC',
    timeUsed: 12,
    memoryUsed: 3584,
    submitTime: '2026-11-04T19:05:00+08:00',
  },
  {
    submitId: 5002,
    trainingId: 1,
    problemId: 101,
    submitter: mockUser,
    language: 'C++17',
    result: 'AC',
    timeUsed: 45,
    memoryUsed: 5120,
    submitTime: '2026-11-04T19:18:00+08:00',
  },
  {
    submitId: 5003,
    trainingId: 1,
    problemId: 102,
    submitter: mockUser,
    language: 'C++17',
    result: 'WA',
    timeUsed: 34,
    memoryUsed: 4096,
    submitTime: '2026-11-04T19:32:00+08:00',
  },
  {
    submitId: 5004,
    trainingId: 1,
    problemId: 102,
    submitter: mockUser,
    language: 'C++17',
    result: 'AC',
    timeUsed: 52,
    memoryUsed: 6144,
    submitTime: '2026-11-04T19:47:00+08:00',
  },
  {
    submitId: 5005,
    trainingId: 1,
    problemId: 103,
    submitter: mockUser,
    language: 'C++17',
    result: 'TLE',
    timeUsed: 2002,
    memoryUsed: 8192,
    submitTime: '2026-11-04T20:15:00+08:00',
  },
  {
    submitId: 5006,
    trainingId: 1,
    problemId: 104,
    submitter: mockUser,
    language: 'C++17',
    result: 'AC',
    timeUsed: 28,
    memoryUsed: 4096,
    submitTime: '2026-11-04T20:41:00+08:00',
  },
  {
    submitId: 5007,
    trainingId: 2,
    problemId: 106,
    submitter: mockUser,
    language: 'Java 17',
    result: 'CE',
    timeUsed: 0,
    memoryUsed: 0,
    submitTime: '2026-11-06T19:12:00+08:00',
  },
  {
    submitId: 5008,
    trainingId: 2,
    problemId: 102,
    submitter: mockUser,
    language: 'C++17',
    result: 'JUDGING',
    timeUsed: 0,
    memoryUsed: 0,
    submitTime: '2026-11-06T19:20:00+08:00',
  },
]

export const mockLeaderboard: LeaderboardResult = {
  frozen: false,
  entries: [
    { rank: 1, userId: 2, userName: '李知远', teamId: null, teamName: null, solvedCount: 5, penalty: 126 },
    { rank: 2, userId: 1, userName: '陈昊', teamId: null, teamName: null, solvedCount: 4, penalty: 112 },
    { rank: 3, userId: 6, userName: '徐清越', teamId: null, teamName: null, solvedCount: 4, penalty: 168 },
    { rank: 4, userId: 4, userName: '周雨桐', teamId: null, teamName: null, solvedCount: 3, penalty: 96 },
    { rank: 5, userId: 3, userName: '王一鸣', teamId: null, teamName: null, solvedCount: 2, penalty: 58 },
    { rank: 6, userId: 11, userName: '黄启帆', teamId: null, teamName: null, solvedCount: 2, penalty: 77 },
  ],
}

export const mockAbility: AbilityResult = {
  userId: 1,
  userName: '陈昊',
  dimensions: [
    { name: '基础算法', value: 86 },
    { name: '动态规划', value: 72 },
    { name: '图论', value: 64 },
    { name: '数据结构', value: 78 },
    { name: '数学', value: 55 },
    { name: '字符串', value: 61 },
  ],
  report:
    '你在基础算法与数据结构维度表现突出，枚举、模拟类题目完成稳定；动态规划与图论仍有提升空间，建议本周重点完成「DP 专项训练（一）」与最短路专题；数学维度偏弱，可优先复习组合数学基础。',
}

/* ===================== 个性化训练推荐 ===================== */

const difficultyOrder: Record<string, number> = { EASY: 0, MEDIUM: 1, HARD: 2 }

/** 取能力评估中得分最低的 n 个维度（薄弱维度） */
function weakDimensions(ability: AbilityResult, n = 3): AbilityResult['dimensions'] {
  return [...ability.dimensions].sort((a, b) => a.value - b.value).slice(0, n)
}

/**
 * 基于能力评估推荐专项训练：
 * 仅推荐 PUBLISHED / ONGOING 状态训练，按训练内题目命中的薄弱维度数量排序。
 */
export function buildTrainingRecommendations(ability: AbilityResult): RecommendedTraining[] {
  const weak = weakDimensions(ability)
  const weakNames = new Set(weak.map((item) => item.name))
  return mockTrainings
    .filter((training) => training.status === 'PUBLISHED' || training.status === 'ONGOING')
    .map((training) => {
      const matched = new Set<string>()
      training.problemIds.forEach((problemId) => {
        const problem = mockProblems.find((item) => item.problemId === problemId)
        problem?.tags.forEach((tag) => {
          if (weakNames.has(tag.category)) matched.add(tag.category)
        })
      })
      const matchedDimensions = weak
        .map((item) => item.name)
        .filter((name) => matched.has(name))
      return { training, matchedDimensions }
    })
    .filter((item) => item.matchedDimensions.length > 0)
    .sort(
      (a, b) =>
        b.matchedDimensions.length - a.matchedDimensions.length ||
        a.training.startTime.localeCompare(b.training.startTime),
    )
    .slice(0, 3)
    .map(({ training, matchedDimensions }) => ({
      trainingId: training.trainingId,
      name: training.name,
      type: training.type,
      startTime: training.startTime,
      endTime: training.endTime,
      status: training.status,
      matchedDimensions,
      reason: `针对薄弱维度「${matchedDimensions.join('、')}」设计，建议优先完成`,
    }))
}

/**
 * 基于薄弱知识点推荐习题：
 * 按薄弱维度（知识点分类）分组，排除本人已 AC 的题目，组内按难度由易到难。
 */
export function buildProblemRecommendations(ability: AbilityResult): WeakTagProblemGroup[] {
  const weak = weakDimensions(ability)
  const acProblemIds = new Set(
    mockSubmissions
      .filter(
        (record) =>
          record.submitter.userId === ability.userId &&
          (record.result === 'AC' || record.result === 'JUDGING' || record.result === 'PENDING'),
      )
      .map((record) => record.problemId),
  )
  return weak
    .map((dimension) => {
      const problems = mockProblems
        .filter((problem) => !acProblemIds.has(problem.problemId))
        .filter((problem) => problem.tags.some((tag) => tag.category === dimension.name))
        .sort(
          (a, b) =>
            (difficultyOrder[a.difficulty] ?? 0) - (difficultyOrder[b.difficulty] ?? 0) ||
            a.problemId - b.problemId,
        )
        .slice(0, 3)
        .map<RecommendedProblem>((problem) => ({
          problemId: problem.problemId,
          title: problem.title,
          difficulty: problem.difficulty,
          tagNames: problem.tags.filter((tag) => tag.category === dimension.name).map((tag) => tag.name),
          reason: `${dimension.name}得分 ${dimension.value}，建议由易到难练习`,
        }))
      return {
        tagName: dimension.name,
        score: dimension.value,
        problems,
      }
    })
    .filter((group) => group.problems.length > 0)
}

export const mockMemberOverview: MemberOverview = {
  total: 156,
  byType: [
    { name: 'OBSERVE_MEMBER', value: 38 },
    { name: 'MEMBER', value: 118 },
  ],
  byGrade: [
    { name: '2022级', value: 21 },
    { name: '2023级', value: 47 },
    { name: '2024级', value: 58 },
    { name: '2025级', value: 30 },
  ],
  byDepartment: [
    { name: '竞赛部', value: 36 },
    { name: '组织部', value: 28 },
    { name: '宣传部', value: 24 },
    { name: '秘书部', value: 18 },
  ],
}

export const mockTrainingSummary: TrainingSummary = {
  totalTrainings: 42,
  totalSubmissions: 1860,
  acRate: 63.5,
  trend: [
    { date: '10-06', submissions: 86, ac: 52 },
    { date: '10-13', submissions: 112, ac: 70 },
    { date: '10-20', submissions: 98, ac: 61 },
    { date: '10-27', submissions: 134, ac: 88 },
    { date: '11-03', submissions: 156, ac: 101 },
    { date: '11-10', submissions: 142, ac: 92 },
  ],
}

/* ===================== 组队管理 ===================== */

/** 按学号取登录用户信息（队伍队长等场景使用），无演示账号时退化为会员信息 */
function userByStudentId(studentId: string): UserInfo {
  const account = mockAccounts[studentId]
  if (account) return account
  const member = mockMembers.find((item) => item.studentId === studentId)
  if (!member) throw new Error(`演示数据缺失：学号 ${studentId}`)
  const roles: RoleCode[] = []
  return {
    userId: member.memberId,
    studentId: member.studentId,
    name: member.name,
    roles,
    avatar: null,
  }
}

/** 依据会员学号生成队伍成员记录 */
function teamMemberOf(studentId: string, isLeader = false, joinTime = '2026-10-20T10:00:00+08:00'): TeamMember {
  const member = mockMembers.find((item) => item.studentId === studentId)
  if (!member) throw new Error(`演示数据缺失：学号 ${studentId}`)
  return {
    memberId: member.memberId,
    name: member.name,
    studentId: member.studentId,
    memberType: member.memberType,
    isLeader,
    joinTime,
  }
}

export const mockTeams: TeamInfo[] = [
  {
    teamId: 1,
    name: '深夜 AC 小分队',
    description: '备战 2026 校赛，主攻动态规划与图论，每周三、周六晚队内训练。',
    status: 'CONFIRMED',
    leader: userByStudentId('1004'),
    members: [
      teamMemberOf('1004', true, '2026-09-12T19:00:00+08:00'),
      teamMemberOf('1008', false, '2026-09-18T19:00:00+08:00'),
      teamMemberOf('1001', false, '2026-10-08T19:00:00+08:00'),
    ],
    dimensions: [
      { name: '基础算法', value: 88 },
      { name: '动态规划', value: 79 },
      { name: '图论', value: 74 },
      { name: '数据结构', value: 81 },
      { name: '数学', value: 62 },
      { name: '字符串', value: 58 },
    ],
    createTime: '2026-09-12T19:00:00+08:00',
  },
  {
    teamId: 2,
    name: '线性求导队',
    description: '实验室成员组建的训练队，侧重数据结构与数学专题。',
    status: 'CONFIRMED',
    leader: userByStudentId('1002'),
    members: [
      teamMemberOf('1002', true, '2026-09-20T19:00:00+08:00'),
      teamMemberOf('1003', false, '2026-09-25T19:00:00+08:00'),
    ],
    dimensions: [
      { name: '基础算法', value: 92 },
      { name: '动态规划', value: 85 },
      { name: '图论', value: 80 },
      { name: '数据结构', value: 94 },
      { name: '数学', value: 76 },
      { name: '字符串', value: 70 },
    ],
    createTime: '2026-09-20T19:00:00+08:00',
  },
  {
    teamId: 3,
    name: '新生冲锋队',
    description: '新生自发组队，等待助理教练确认后参加校赛选拔。',
    status: 'PENDING',
    leader: userByStudentId('1001'),
    members: [
      teamMemberOf('1001', true, '2026-10-28T20:00:00+08:00'),
      teamMemberOf('2024310211', false, '2026-10-29T20:00:00+08:00'),
    ],
    dimensions: [
      { name: '基础算法', value: 61 },
      { name: '动态规划', value: 44 },
      { name: '图论', value: 38 },
      { name: '数据结构', value: 52 },
      { name: '数学', value: 40 },
      { name: '字符串', value: 35 },
    ],
    createTime: '2026-10-28T20:00:00+08:00',
  },
]

/* ===================== 集训管理 ===================== */
export const mockCamps: CampInfo[] = [
  {
    campId: 1,
    name: '2027 年寒假集训',
    type: 'WINTER',
    startDate: '2027-01-10',
    endDate: '2027-01-24',
    registrationDeadline: '2027-01-05T23:59:59+08:00',
    location: '计算机学院实验楼 A301',
    description: '为期两周的寒假集中训练，上午专题讲解、下午自主训练、晚上队内赛，结营安排选拔赛。',
    creator: mockCreator,
    createTime: '2026-12-15T10:00:00+08:00',
  },
  {
    campId: 2,
    name: '2026 年暑假集训',
    type: 'SUMMER',
    startDate: '2026-07-12',
    endDate: '2026-07-26',
    registrationDeadline: '2026-07-06T23:59:59+08:00',
    location: '计算机学院实验楼 A305',
    description: '暑假集训已结营，集训期间完成动态规划、图论、数据结构三大专题训练与结营考核。',
    creator: mockCreator,
    createTime: '2026-06-20T10:00:00+08:00',
  },
]

export const mockCampTasks: CampTask[] = [
  {
    taskId: 1,
    campId: 1,
    date: '2027-01-10',
    title: 'Day1 开营与热身赛',
    content: '上午 9:00 开营宣讲并讲解集训安排；下午完成热身赛（5 题，3 小时）；晚上复盘并组队。',
    publisher: mockCreator,
    publishTime: '2027-01-09T20:00:00+08:00',
  },
  {
    taskId: 2,
    campId: 1,
    date: '2027-01-11',
    title: 'Day2 动态规划专题',
    content: '上午讲解线性 DP 与背包问题；下午完成 DP 专题 6 题；晚上队内赛（DP 专项）。',
    publisher: mockCreator,
    publishTime: '2027-01-10T20:30:00+08:00',
  },
  {
    taskId: 3,
    campId: 2,
    date: '2026-07-12',
    title: 'Day1 图论专题',
    content: '上午讲解最短路与最小生成树；下午完成图论专题 5 题；晚上队内赛复盘。',
    publisher: mockCreator,
    publishTime: '2026-07-11T20:00:00+08:00',
  },
]

export const mockCampRegistrations: CampRegistration[] = [
  {
    applyId: 1,
    campId: 1,
    memberId: 3,
    memberName: '王一鸣',
    studentId: '1001',
    grade: '2024级',
    applyTime: '2026-11-04T10:20:00+08:00',
    status: 'PENDING',
    remark: '希望利用寒假系统补习动态规划。',
  },
  {
    applyId: 2,
    campId: 1,
    memberId: 5,
    memberName: '赵子轩',
    studentId: '1003',
    grade: '2023级',
    applyTime: '2026-11-05T15:40:00+08:00',
    status: 'PENDING',
    remark: null,
  },
  {
    applyId: 3,
    campId: 1,
    memberId: 6,
    memberName: '徐清越',
    studentId: '1002',
    grade: '2022级',
    applyTime: '2026-11-03T09:10:00+08:00',
    status: 'APPROVED',
    remark: '已通过审核。',
  },
  {
    applyId: 4,
    campId: 2,
    memberId: 11,
    memberName: '黄启帆',
    studentId: '1004',
    grade: '2023级',
    applyTime: '2026-06-25T11:00:00+08:00',
    status: 'APPROVED',
    remark: null,
  },
  {
    applyId: 5,
    campId: 2,
    memberId: 6,
    memberName: '徐清越',
    studentId: '1002',
    grade: '2022级',
    applyTime: '2026-06-26T14:20:00+08:00',
    status: 'APPROVED',
    remark: null,
  },
  {
    applyId: 6,
    campId: 2,
    memberId: 5,
    memberName: '赵子轩',
    studentId: '1003',
    grade: '2023级',
    applyTime: '2026-06-28T16:35:00+08:00',
    status: 'APPROVED',
    remark: null,
  },
]

export const mockAttendance: AttendanceRecord[] = [
  {
    recordId: 1,
    campId: 2,
    memberId: 11,
    memberName: '黄启帆',
    studentId: '1004',
    date: '2026-07-12',
    status: 'PRESENT',
    checkinTime: '2026-07-12T09:02:00+08:00',
    remark: null,
  },
  {
    recordId: 2,
    campId: 2,
    memberId: 6,
    memberName: '徐清越',
    studentId: '1002',
    date: '2026-07-12',
    status: 'PRESENT',
    checkinTime: '2026-07-12T08:58:00+08:00',
    remark: null,
  },
  {
    recordId: 3,
    campId: 2,
    memberId: 5,
    memberName: '赵子轩',
    studentId: '1003',
    date: '2026-07-12',
    status: 'ABSENT',
    checkinTime: null,
    remark: '未签到',
  },
  {
    recordId: 4,
    campId: 2,
    memberId: 11,
    memberName: '黄启帆',
    studentId: '1004',
    date: '2026-07-13',
    status: 'PRESENT',
    checkinTime: '2026-07-13T09:05:00+08:00',
    remark: null,
  },
  {
    recordId: 5,
    campId: 2,
    memberId: 6,
    memberName: '徐清越',
    studentId: '1002',
    date: '2026-07-13',
    status: 'LEAVE',
    checkinTime: null,
    remark: '参加学院暑期实践，已提前请假',
  },
  {
    recordId: 6,
    campId: 2,
    memberId: 5,
    memberName: '赵子轩',
    studentId: '1003',
    date: '2026-07-13',
    status: 'ABSENT',
    checkinTime: null,
    remark: '未签到',
  },
  {
    recordId: 7,
    campId: 1,
    memberId: 6,
    memberName: '徐清越',
    studentId: '1002',
    date: '2027-01-10',
    status: 'PRESENT',
    checkinTime: '2027-01-10T08:50:00+08:00',
    remark: null,
  },
]

/* ===================== Mock 查询实现 ===================== */
export function queryMembers(query: MemberQuery) {
  let list = [...mockMembers]
  if (query.keyword) {
    const keyword = query.keyword.trim()
    list = list.filter((item) => item.name.includes(keyword) || item.studentId.includes(keyword))
  }
  if (query.memberType) list = list.filter((item) => item.memberType === query.memberType)
  if (query.grade) list = list.filter((item) => item.grade === query.grade)
  if (query.department) list = list.filter((item) => item.department === query.department)
  if (query.status) list = list.filter((item) => item.status === query.status)
  return paginate(list, query.page, query.size)
}

export function queryProblems(query: ProblemQuery) {
  let list = [...mockProblems]
  if (query.keyword) {
    const keyword = query.keyword.trim()
    list = list.filter((item) => item.title.includes(keyword) || item.ojProblemId.includes(keyword))
  }
  if (query.tagId) list = list.filter((item) => item.tags.some((tag) => tag.tagId === query.tagId))
  if (query.difficulty) list = list.filter((item) => item.difficulty === query.difficulty)
  if (query.visibility) list = list.filter((item) => item.visibility === query.visibility)
  return paginate(list, query.page, query.size)
}

export function queryTrainings(query: TrainingQuery) {
  let list = [...mockTrainings]
  if (query.keyword) list = list.filter((item) => item.name.includes(query.keyword!.trim()))
  if (query.status) list = list.filter((item) => item.status === query.status)
  if (query.type) list = list.filter((item) => item.type === query.type)
  list.sort((a, b) => b.startTime.localeCompare(a.startTime))
  return paginate(list, query.page, query.size)
}

export function queryTeams(query: TeamQuery) {
  let list = [...mockTeams]
  if (query.keyword) list = list.filter((item) => item.name.includes(query.keyword!.trim()))
  if (query.status) list = list.filter((item) => item.status === query.status)
  return paginate(list, query.page, query.size)
}

export function queryCamps(query: CampQuery) {
  let list = [...mockCamps]
  if (query.keyword) list = list.filter((item) => item.name.includes(query.keyword!.trim()))
  if (query.type) list = list.filter((item) => item.type === query.type)
  list.sort((a, b) => b.startDate.localeCompare(a.startDate))
  return paginate(list, query.page, query.size)
}

/* ===================== 赛事管理 ===================== */
export const mockContests: ContestInfo[] = [
  {
    contestId: 1,
    name: '中南民族大学第十届校内程序设计竞赛',
    category: 'SCHOLASTIC',
    startTime: '2026-11-01T13:00:00+08:00',
    endTime: '2026-11-01T18:00:00+08:00',
    registrationDeadline: '2026-10-29T23:59:59+08:00',
    description: '面向全校的年度校内程序设计竞赛，5 小时 8 题，赛后将评选一、二、三等奖并生成电子证书。',
    maxTeamSize: 3,
    problemIds: [100, 101, 102, 103, 104, 105, 107, 110],
    freezeMinutes: 60,
    status: 'ENDED',
    creator: mockCreator,
    createTime: '2026-10-15T10:00:00+08:00',
  },
  {
    contestId: 2,
    name: '2026 XCPC 网络预选赛',
    category: 'XCPC',
    startTime: '2026-11-07T09:00:00+08:00',
    endTime: '2026-11-07T21:00:00+08:00',
    registrationDeadline: '2026-11-05T23:59:59+08:00',
    description: 'XCPC 系列网络预选赛，队伍以 3 人为单位参赛，比赛结束前 60 分钟封榜，晋级队伍将代表学校参加区域赛。',
    maxTeamSize: 3,
    problemIds: [101, 102, 103, 105, 107, 108, 110, 111],
    freezeMinutes: 60,
    status: 'ONGOING',
    creator: mockCreator,
    createTime: '2026-10-20T09:00:00+08:00',
  },
  {
    contestId: 3,
    name: '2026 级新生程序设计选拔赛',
    category: 'SCHOLASTIC',
    startTime: '2026-11-20T14:00:00+08:00',
    endTime: '2026-11-20T19:00:00+08:00',
    registrationDeadline: '2026-11-18T23:59:59+08:00',
    description: '面向 2026 级新生的校内选拔赛，个人赛形式，用于选拔进入实验室的候选人。',
    maxTeamSize: 1,
    problemIds: [100, 101, 102, 104, 107],
    freezeMinutes: 30,
    status: 'PUBLISHED',
    creator: mockCreator,
    createTime: '2026-11-02T15:30:00+08:00',
  },
  {
    contestId: 4,
    name: 'CCPC 女生专场赛（校外）',
    category: 'NON_XCPC',
    startTime: '2026-11-28T09:00:00+08:00',
    endTime: '2026-11-28T14:00:00+08:00',
    registrationDeadline: '2026-11-25T23:59:59+08:00',
    description: '非 XCPC 系列的校外邀请赛，协会统一组织报名并由教练带队参赛。',
    maxTeamSize: 3,
    problemIds: [101, 103, 105, 110],
    freezeMinutes: 0,
    status: 'PUBLISHED',
    creator: mockCreator,
    createTime: '2026-11-04T11:00:00+08:00',
  },
  {
    contestId: 5,
    name: '2026 冬季校赛热身赛',
    category: 'SCHOLASTIC',
    startTime: '2026-12-05T19:00:00+08:00',
    endTime: '2026-12-05T22:00:00+08:00',
    registrationDeadline: '2026-12-03T23:59:59+08:00',
    description: '冬季校赛前的热身赛，题目难度中等偏易，用于帮助参赛队员调整状态。',
    maxTeamSize: 3,
    problemIds: [100, 101, 104, 107],
    freezeMinutes: 30,
    status: 'DRAFT',
    creator: mockCreator,
    createTime: '2026-11-06T16:40:00+08:00',
  },
]

export const mockContestRegistrations: ContestRegistration[] = [
  {
    applyId: 1,
    contestId: 1,
    applyType: 'TEAM',
    teamId: 1,
    teamName: '深夜 AC 小分队',
    memberId: 11,
    memberName: '黄启帆',
    studentId: '1004',
    grade: '2023级',
    memberCount: 3,
    applyTime: '2026-10-20T10:15:00+08:00',
    status: 'APPROVED',
    remark: null,
  },
  {
    applyId: 2,
    contestId: 1,
    applyType: 'SOLO',
    teamId: null,
    teamName: null,
    memberId: 2,
    memberName: '李知远',
    studentId: '1008',
    grade: '2023级',
    memberCount: 1,
    applyTime: '2026-10-21T14:02:00+08:00',
    status: 'APPROVED',
    remark: '个人报名，希望检验近期训练成果。',
  },
  {
    applyId: 3,
    contestId: 1,
    applyType: 'SOLO',
    teamId: null,
    teamName: null,
    memberId: 6,
    memberName: '徐清越',
    studentId: '1002',
    grade: '2022级',
    memberCount: 1,
    applyTime: '2026-10-22T09:30:00+08:00',
    status: 'APPROVED',
    remark: null,
  },
  {
    applyId: 4,
    contestId: 2,
    applyType: 'TEAM',
    teamId: 2,
    teamName: '线性求导队',
    memberId: 6,
    memberName: '徐清越',
    studentId: '1002',
    grade: '2022级',
    memberCount: 2,
    applyTime: '2026-11-01T18:20:00+08:00',
    status: 'APPROVED',
    remark: null,
  },
  {
    applyId: 5,
    contestId: 2,
    applyType: 'SOLO',
    teamId: null,
    teamName: null,
    memberId: 5,
    memberName: '赵子轩',
    studentId: '1003',
    grade: '2023级',
    memberCount: 1,
    applyTime: '2026-11-02T20:45:00+08:00',
    status: 'APPROVED',
    remark: null,
  },
  {
    applyId: 6,
    contestId: 3,
    applyType: 'TEAM',
    teamId: 3,
    teamName: '新生冲锋队',
    memberId: 3,
    memberName: '王一鸣',
    studentId: '1001',
    grade: '2024级',
    memberCount: 2,
    applyTime: '2026-11-05T11:10:00+08:00',
    status: 'PENDING',
    remark: '新生组队报名，希望参加选拔。',
  },
  {
    applyId: 7,
    contestId: 3,
    applyType: 'SOLO',
    teamId: null,
    teamName: null,
    memberId: 2,
    memberName: '李知远',
    studentId: '1008',
    grade: '2023级',
    memberCount: 1,
    applyTime: '2026-11-06T09:05:00+08:00',
    status: 'PENDING',
    remark: null,
  },
  {
    applyId: 8,
    contestId: 4,
    applyType: 'SOLO',
    teamId: null,
    teamName: null,
    memberId: 11,
    memberName: '黄启帆',
    studentId: '1004',
    grade: '2023级',
    memberCount: 1,
    applyTime: '2026-11-06T15:40:00+08:00',
    status: 'PENDING',
    remark: '希望随队参加校外邀请赛。',
  },
]

export const mockContestResults: ContestResult[] = [
  {
    resultId: 1,
    contestId: 1,
    memberId: 11,
    memberName: '黄启帆',
    studentId: '1004',
    teamName: '深夜 AC 小分队',
    rank: 1,
    award: '一等奖',
    score: 8,
    remark: '全场唯一 AK 队伍',
  },
  {
    resultId: 2,
    contestId: 1,
    memberId: 2,
    memberName: '李知远',
    studentId: '1008',
    teamName: null,
    rank: 2,
    award: '二等奖',
    score: 7,
    remark: null,
  },
  {
    resultId: 3,
    contestId: 1,
    memberId: 6,
    memberName: '徐清越',
    studentId: '1002',
    teamName: null,
    rank: 3,
    award: '二等奖',
    score: 6,
    remark: null,
  },
]

/* ===================== MBTI 管理 ===================== */
export const mockMbtiQuestions: MbtiQuestion[] = [
  {
    questionId: 1,
    dimension: 'EI',
    content: '在团队讨论中，你更倾向于哪种状态？',
    options: [
      { value: 'A', label: '主动发言，带动讨论气氛', pole: 'E' },
      { value: 'B', label: '先倾听，想清楚再表达', pole: 'I' },
    ],
  },
  {
    questionId: 2,
    dimension: 'EI',
    content: '连续参加几场社交活动后，你通常会：',
    options: [
      { value: 'A', label: '精力充沛，还想继续交流', pole: 'E' },
      { value: 'B', label: '感到疲惫，需要独处恢复', pole: 'I' },
    ],
  },
  {
    questionId: 3,
    dimension: 'SN',
    content: '面对一道全新的算法题，你更先关注：',
    options: [
      { value: 'A', label: '具体的样例数据与边界条件', pole: 'S' },
      { value: 'B', label: '题目背后可能存在的通用模型', pole: 'N' },
    ],
  },
  {
    questionId: 4,
    dimension: 'SN',
    content: '学习新知识时，你更信任：',
    options: [
      { value: 'A', label: '已被反复验证的经验做法', pole: 'S' },
      { value: 'B', label: '尚未尝试过的新思路与猜想', pole: 'N' },
    ],
  },
  {
    questionId: 5,
    dimension: 'TF',
    content: '当队友提交的代码出现问题时，你会：',
    options: [
      { value: 'A', label: '直接指出问题并说明原因', pole: 'T' },
      { value: 'B', label: '先安抚情绪再一起排查', pole: 'F' },
    ],
  },
  {
    questionId: 6,
    dimension: 'TF',
    content: '做决策时，你更看重：',
    options: [
      { value: 'A', label: '客观数据与逻辑推演', pole: 'T' },
      { value: 'B', label: '团队成员的感受与整体氛围', pole: 'F' },
    ],
  },
  {
    questionId: 7,
    dimension: 'JP',
    content: '备赛期间，你更习惯：',
    options: [
      { value: 'A', label: '提前列好计划并逐项完成', pole: 'J' },
      { value: 'B', label: '随机应变，按兴趣推进', pole: 'P' },
    ],
  },
  {
    questionId: 8,
    dimension: 'JP',
    content: '面对临近的比赛，你倾向于：',
    options: [
      { value: 'A', label: '尽早确定方案并完成收尾', pole: 'J' },
      { value: 'B', label: '保留调整空间，做到最后一刻', pole: 'P' },
    ],
  },
]

/** 各维度左右极字母（左极在前） */
const mbtiPolePair: Record<MbtiDimension, [MbtiPole, MbtiPole]> = {
  EI: ['E', 'I'],
  SN: ['S', 'N'],
  TF: ['T', 'F'],
  JP: ['J', 'P'],
}

const mbtiDimensionOrder: MbtiDimension[] = ['EI', 'SN', 'TF', 'JP']

/** 依据作答计算 MBTI 结果（Mock 判分；真实环境由后端计算） */
export function computeMbtiResult(answers: MbtiAnswer[]): MbtiResult {
  const user = getMockCurrentUser()
  const buckets: Record<MbtiDimension, { left: number; right: number }> = {
    EI: { left: 0, right: 0 },
    SN: { left: 0, right: 0 },
    TF: { left: 0, right: 0 },
    JP: { left: 0, right: 0 },
  }

  answers.forEach((answer) => {
    const question = mockMbtiQuestions.find((item) => item.questionId === answer.questionId)
    if (!question) return
    const option = question.options.find((item) => item.value === answer.value)
    if (!option) return
    if (option.pole === mbtiPolePair[question.dimension][0]) buckets[question.dimension].left += 1
    else buckets[question.dimension].right += 1
  })

  let type = ''
  const dimensions: MbtiDimensionScore[] = mbtiDimensionOrder.map((dimension) => {
    const { left, right } = buckets[dimension]
    const total = left + right || 1
    const leftPercent = Math.round((left / total) * 100)
    const preference: 'left' | 'right' = leftPercent >= 50 ? 'left' : 'right'
    type += preference === 'left' ? mbtiPolePair[dimension][0] : mbtiPolePair[dimension][1]
    const [leftLabel, rightLabel] = mbtiPoleLabelMap[dimension]
    return { dimension, leftLabel, rightLabel, leftPercent, preference }
  })

  const mbtiType = type as MbtiType
  return {
    userId: user.userId,
    userName: user.name,
    type: mbtiType,
    typeName: mbtiTypeNameMap[mbtiType],
    dimensions,
    description: `${user.name} 的测评结果为 ${mbtiType}（${mbtiTypeNameMap[mbtiType]}）。该类型在算法竞赛中通常表现为较强的逻辑组织能力与稳定的解题节奏。`,
    suggestions: [
      '发挥自身优势，在团队中承担与性格匹配的角色',
      '关注偏弱维度的协作方式，与不同性格的队友互补',
      '结合能力评估结果制定下一阶段训练计划',
    ],
    submitTime: new Date().toISOString(),
  }
}

function dimensionScore(
  dimension: MbtiDimension,
  leftPercent: number,
): MbtiDimensionScore {
  const [leftLabel, rightLabel] = mbtiPoleLabelMap[dimension]
  return {
    dimension,
    leftLabel,
    rightLabel,
    leftPercent,
    preference: leftPercent >= 50 ? 'left' : 'right',
  }
}

/* ===================== 奖惩管理 ===================== */
/**
 * 奖励 / 处分演示数据（接口手册第 12 章：/rewards 与 /punishments 两组接口）。
 * 权限表「奖惩管理」：干事、会长/副会长、指导老师可录入（REWARD_MANAGER_ROLES）。
 */
export const mockRewards: RewardRecord[] = [
  {
    rewardId: 1,
    memberId: 6,
    memberName: '徐清越',
    studentId: '1002',
    title: '2026 ICPC 区域赛银牌',
    category: 'COMPETITION',
    level: 'NATIONAL',
    amount: 500,
    description: '区域赛银牌，为协会争得荣誉',
    grantDate: '2026-11-01',
    issuerName: 'ICPC 协会',
    createTime: '2026-11-01T10:00:00+08:00',
  },
  {
    rewardId: 2,
    memberId: 5,
    memberName: '赵子轩',
    studentId: '1003',
    title: '2026 校赛金牌',
    category: 'COMPETITION',
    level: 'SCHOOL',
    amount: 200,
    description: '校赛个人赛第一名，训练态度积极',
    grantDate: '2026-09-30',
    issuerName: 'ICPC 协会',
    createTime: '2026-09-30T15:30:00+08:00',
  },
]

export const mockPunishments: PunishmentRecord[] = [
  {
    punishmentId: 1,
    memberId: 3,
    memberName: '林可欣',
    studentId: '1005',
    type: 'WARNING',
    reason: '连续两次无故缺席集训',
    description: '经提醒仍未改正，予以会内通报警告',
    effectiveDate: '2026-11-10',
    expireDate: '2027-02-10',
    issuerName: 'ICPC 协会组织部',
    createTime: '2026-11-10T09:00:00+08:00',
  },
]

/* ===================== 考核管理 ===================== */
/**
 * 考核 / 考核成绩演示数据（接口手册第 13 章）。
 * 权限表「考核管理」：助理教练、会长/副会长、指导老师可维护（CONTENT_MANAGER_ROLES）；
 * 成绩审核仅会长/副会长、指导老师。
 */
export const mockAssessmentList: AssessmentInfo[] = [
  {
    assessmentId: 1,
    name: '2026 春季观察期考核',
    type: 'OBSERVATION',
    description: '面向 2026 级观察会员的观察期综合考核',
    startTime: '2026-11-20T09:00:00',
    endTime: '2026-12-05T18:00:00',
    passScore: 60,
    status: 'PUBLISHED',
    createTime: '2026-11-05T10:00:00+08:00',
  },
  {
    assessmentId: 2,
    name: '2026 秋季正式会员月度考核',
    type: 'REGULAR',
    description: '十月训练出勤、题量与榜单名次综合评定',
    startTime: '2026-10-01T09:00:00',
    endTime: '2026-10-31T18:00:00',
    passScore: 60,
    status: 'PUBLISHED',
    createTime: '2026-09-25T10:00:00+08:00',
  },
]

export const mockAssessmentResults: AssessmentResultRecord[] = [
  {
    resultId: 1,
    assessmentId: 2,
    memberId: 5,
    memberName: '赵子轩',
    studentId: '1003',
    score: 92,
    remark: '出勤率 100%，训练投入度高',
    auditStatus: 'APPROVED',
    auditRemark: '成绩无误',
  },
  {
    resultId: 2,
    assessmentId: 2,
    memberId: 6,
    memberName: '徐清越',
    studentId: '1002',
    score: 85,
    remark: '月度完成 46 题，AC 率 72%',
    auditStatus: 'PENDING',
    auditRemark: null,
  },
  {
    resultId: 3,
    assessmentId: 2,
    memberId: 7,
    memberName: '王一鸣',
    studentId: '1001',
    score: 58,
    remark: '需加强算法基础',
    auditStatus: 'REJECTED',
    auditRemark: '分数录入有误，请复核',
  },
  {
    resultId: 4,
    assessmentId: 1,
    memberId: 7,
    memberName: '王一鸣',
    studentId: '1001',
    score: 78,
    remark: '基础题完成良好，需加强图论专题',
    auditStatus: 'PENDING',
    auditRemark: null,
  },
]

/* ===================== 系统管理 ===================== */
/**
 * 操作日志 / 系统配置 / 文件记录演示数据（接口手册第 14 章）。
 * 权限表「系统管理」：会长/副会长、指导老师可查看与维护（SYSTEM_MANAGER_ROLES）。
 */
export const mockSystemLogs: SystemLog[] = [
  {
    logId: 1,
    userId: 1,
    userName: '陈昊',
    action: 'UPDATE_MEMBER_ROLES',
    detail: '将会员「徐清越」角色调整为 [MEMBER, TEAM_LEADER]',
    ip: '10.12.30.21',
    createTime: '2026-11-15T10:20:00+08:00',
  },
  {
    logId: 2,
    userId: 9,
    userName: '顾明',
    action: 'UPDATE_SYSTEM_CONFIGS',
    detail: '批量更新系统配置 member.maxCount = 200',
    ip: '10.12.30.35',
    createTime: '2026-11-14T16:45:00+08:00',
  },
  {
    logId: 3,
    userId: 3,
    userName: '林可欣',
    action: 'AUDIT_JOIN_APPLY',
    detail: '通过入会申请 #1032（申请人：钱晓）',
    ip: '10.12.31.8',
    createTime: '2026-11-13T09:12:00+08:00',
  },
]

export const mockSystemConfigs: SystemConfig[] = [
  {
    configKey: 'member.maxCount',
    configValue: '200',
    description: '协会在册会员上限',
    updateTime: '2026-11-14T16:45:00+08:00',
  },
  {
    configKey: 'camp.registerDeadlineDays',
    configValue: '7',
    description: '集训报名截止天数（开营前几天）',
    updateTime: '2026-11-14T16:45:00+08:00',
  },
  {
    configKey: 'training.freezeMinutes',
    configValue: '30',
    description: '训练榜单默认封榜时长（分钟）',
    updateTime: '2026-10-02T11:00:00+08:00',
  },
]

export const mockSystemFiles: SystemFile[] = [
  {
    fileId: 1,
    filename: '入会动机说明-钱晓.pdf',
    url: '/uploads/2026/10/abc123.pdf',
    size: 245760,
    uploaderName: '钱晓',
    createTime: '2026-10-21T14:00:00+08:00',
  },
  {
    fileId: 2,
    filename: '2026校赛获奖证书.zip',
    url: '/uploads/2026/11/cert2026.zip',
    size: 1048576,
    uploaderName: '陈昊',
    createTime: '2026-11-02T18:30:00+08:00',
  },
]

export const mockMbtiResults: MbtiResult[] = [
  {
    userId: 1,
    userName: '陈昊',
    type: 'ENTP',
    typeName: mbtiTypeNameMap.ENTP,
    dimensions: [
      dimensionScore('EI', 62),
      dimensionScore('SN', 34),
      dimensionScore('TF', 58),
      dimensionScore('JP', 40),
    ],
    description: '陈昊 的测评结果为 ENTP（辩论家）。该类型思维活跃、乐于尝试新解法，适合在队伍中担任破题与思路拓展的角色。',
    suggestions: [
      '把发散的想法及时收敛为可执行的解题方案',
      '在训练中加强代码实现的稳定性',
      '与判断型队友配合，确保赛前准备按计划完成',
    ],
    submitTime: '2026-10-18T20:15:00+08:00',
  },
  {
    userId: 2,
    userName: '李知远',
    type: 'INTJ',
    typeName: mbtiTypeNameMap.INTJ,
    dimensions: [
      dimensionScore('EI', 28),
      dimensionScore('SN', 22),
      dimensionScore('TF', 74),
      dimensionScore('JP', 82),
    ],
    description: '李知远 的测评结果为 INTJ（建筑师）。该类型擅长整体规划与结构化思考，适合承担训练计划制定与知识体系搭建的工作。',
    suggestions: [
      '在团队协作中多同步阶段性思路，避免信息差',
      '适当参与讨论，帮助队友理解整体方案',
      '利用规划优势承担赛前拆题与分工工作',
    ],
    submitTime: '2026-10-20T19:40:00+08:00',
  },
]

export function queryContests(query: ContestQuery) {
  let list = [...mockContests]
  if (query.keyword) list = list.filter((item) => item.name.includes(query.keyword!.trim()))
  if (query.category) list = list.filter((item) => item.category === query.category)
  list.sort((a, b) => b.startTime.localeCompare(a.startTime))
  return paginate(list, query.page, query.size)
}
