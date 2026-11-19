-- =====================================================================
-- ICPC 协会管理系统 · 演示初始化数据
-- 数据来源：icpc_frontend/src/mock/index.ts（前端演示数据）
-- 执行时机：dev profile 启动时由 spring.sql.init 自动执行（可重复执行）
-- 说明：全部使用 INSERT IGNORE + 显式主键，空库首次启动即可得到与前端 Mock
--       一致的会员、题库、训练、集训、赛事、队伍与 MBTI 数据。
-- 演示账号（密码统一为 123456）：
--   1001 陈昊（会长）      1002 李知远（部长）    1003 林可欣（干事）
--   1004 周雨桐（助理教练） 1005 黄启帆（队长）    1006 徐清越（实验室成员）
--   1007 赵子轩（正式会员） 1008 王一鸣（观察会员） 1009 顾明（指导老师）
-- =====================================================================
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;
SET time_zone = '+08:00';

-- ==================== 角色字典 ====================
INSERT IGNORE INTO sys_role (role_code, role_name, description) VALUES
    ('OBSERVE_MEMBER', '观察会员', '观察会员'),
    ('MEMBER', '会员', '正式会员'),
    ('LAB_MEMBER', '实验室成员', '实验室成员'),
    ('TEAM_LEADER', '队长', '队伍队长'),
    ('OFFICER', '干事', '协会干事'),
    ('ASSISTANT_COACH', '助理教练', '助理教练'),
    ('MINISTER', '部长', '部门部长'),
    ('PRESIDENT', '会长', '协会会长'),
    ('ADVISOR', '指导老师', '指导老师');

-- ==================== 会员（与 mockMembers 顺序一致） ====================
-- 密码哈希对应明文 123456
INSERT IGNORE INTO member (member_id, student_id, password, name, major, grade, phone, email, avatar,
                           member_type, department, join_date, status,
                           create_time, update_time, deleted) VALUES
    (1, '1001', '$2a$10$J658/MW6soiM65MAWeWEKOW2R5b9qwGfSjsHzQIdVqcyH0jbsUUPa',
     '陈昊', '计算机科学与技术', '2023级', '13800000001', 'chenhao@example.com', NULL,
     'LAB_MEMBER', '竞赛部', '2023-09-20', 'ACTIVE', '2023-09-20 09:00:00', NOW(), 0),
    (2, '1002', '$2a$10$J658/MW6soiM65MAWeWEKOW2R5b9qwGfSjsHzQIdVqcyH0jbsUUPa',
     '李知远', '软件工程', '2023级', '13800000002', 'lizhiyuan@example.com', NULL,
     'MEMBER', '竞赛部', '2023-10-08', 'ACTIVE', '2023-10-08 09:00:00', NOW(), 0),
    (3, '1008', '$2a$10$J658/MW6soiM65MAWeWEKOW2R5b9qwGfSjsHzQIdVqcyH0jbsUUPa',
     '王一鸣', '计算机科学与技术', '2024级', '13800000003', 'wangyiming@example.com', NULL,
     'OBSERVE_MEMBER', NULL, '2026-10-02', 'ACTIVE', '2026-10-02 09:00:00', NOW(), 0),
    (4, '1004', '$2a$10$J658/MW6soiM65MAWeWEKOW2R5b9qwGfSjsHzQIdVqcyH0jbsUUPa',
     '周雨桐', '数据科学与大数据技术', '2024级', '13800000004', 'zhouyutong@example.com', NULL,
     'LAB_MEMBER', '竞赛部', '2024-09-15', 'ACTIVE', '2024-09-15 09:00:00', NOW(), 0),
    (5, '1007', '$2a$10$J658/MW6soiM65MAWeWEKOW2R5b9qwGfSjsHzQIdVqcyH0jbsUUPa',
     '赵子轩', '网络工程', '2023级', '13800000005', 'zhaozixuan@example.com', NULL,
     'MEMBER', '组织部', '2023-10-12', 'INACTIVE', '2023-10-12 09:00:00', NOW(), 0),
    (6, '1006', '$2a$10$J658/MW6soiM65MAWeWEKOW2R5b9qwGfSjsHzQIdVqcyH0jbsUUPa',
     '徐清越', '计算机科学与技术', '2022级', '13800000006', 'xuqingyue@example.com', NULL,
     'LAB_MEMBER', '竞赛部', '2022-09-18', 'ACTIVE', '2022-09-18 09:00:00', NOW(), 0),
    (7, '2024310211', '$2a$10$J658/MW6soiM65MAWeWEKOW2R5b9qwGfSjsHzQIdVqcyH0jbsUUPa',
     '孙嘉树', '人工智能', '2024级', '13800000007', 'sunjiaishu@example.com', NULL,
     'OBSERVE_MEMBER', NULL, '2026-10-09', 'ACTIVE', '2026-10-09 09:00:00', NOW(), 0),
    (8, '2022320118', '$2a$10$J658/MW6soiM65MAWeWEKOW2R5b9qwGfSjsHzQIdVqcyH0jbsUUPa',
     '何思远', '软件工程', '2022级', '13800000008', 'hesiyuan@example.com', NULL,
     'MEMBER', '宣传部', '2022-10-20', 'RESIGNED', '2022-10-20 09:00:00', NOW(), 0),
    (9, '1003', '$2a$10$J658/MW6soiM65MAWeWEKOW2R5b9qwGfSjsHzQIdVqcyH0jbsUUPa',
     '林可欣', '计算机科学与技术', '2023级', '13800000009', 'linkexin@example.com', NULL,
     'MEMBER', '秘书部', '2023-09-25', 'ACTIVE', '2023-09-25 09:00:00', NOW(), 0),
    (10, '2024220155', '$2a$10$J658/MW6soiM65MAWeWEKOW2R5b9qwGfSjsHzQIdVqcyH0jbsUUPa',
     '郑浩然', '数据科学与大数据技术', '2024级', '13800000010', 'zhenghaoran@example.com', NULL,
     'OBSERVE_MEMBER', NULL, '2026-10-15', 'ACTIVE', '2026-10-15 09:00:00', NOW(), 0),
    (11, '1005', '$2a$10$J658/MW6soiM65MAWeWEKOW2R5b9qwGfSjsHzQIdVqcyH0jbsUUPa',
     '黄启帆', '网络工程', '2023级', '13800000011', 'huangqifan@example.com', NULL,
     'LAB_MEMBER', '竞赛部', '2023-09-28', 'ACTIVE', '2023-09-28 09:00:00', NOW(), 0),
    (12, '2022120144', '$2a$10$J658/MW6soiM65MAWeWEKOW2R5b9qwGfSjsHzQIdVqcyH0jbsUUPa',
     '吴思琪', '软件工程', '2022级', '13800000012', 'wusiqi@example.com', NULL,
     'MEMBER', '组织部', '2022-10-05', 'REMOVED', '2022-10-05 09:00:00', NOW(), 0),
    (20, '1009', '$2a$10$J658/MW6soiM65MAWeWEKOW2R5b9qwGfSjsHzQIdVqcyH0jbsUUPa',
     '顾明', '计算机科学与技术', NULL, NULL, 'guming@example.com', NULL,
     'LAB_MEMBER', '指导老师', '2020-09-01', 'ACTIVE', '2020-09-01 09:00:00', NOW(), 0);

-- 会员角色：按学号关联，避免依赖自增 ID
INSERT IGNORE INTO member_role (member_id, role_code)
SELECT member_id, 'PRESIDENT' FROM member WHERE student_id = '1001';
INSERT IGNORE INTO member_role (member_id, role_code)
SELECT member_id, 'MINISTER' FROM member WHERE student_id = '1002';
INSERT IGNORE INTO member_role (member_id, role_code)
SELECT member_id, 'OBSERVE_MEMBER' FROM member WHERE student_id = '1008';
INSERT IGNORE INTO member_role (member_id, role_code)
SELECT member_id, 'ASSISTANT_COACH' FROM member WHERE student_id = '1004';
INSERT IGNORE INTO member_role (member_id, role_code)
SELECT member_id, 'MEMBER' FROM member WHERE student_id = '1007';
INSERT IGNORE INTO member_role (member_id, role_code)
SELECT member_id, 'LAB_MEMBER' FROM member WHERE student_id = '1006';
INSERT IGNORE INTO member_role (member_id, role_code)
SELECT member_id, 'OBSERVE_MEMBER' FROM member WHERE student_id = '2024310211';
INSERT IGNORE INTO member_role (member_id, role_code)
SELECT member_id, 'MEMBER' FROM member WHERE student_id = '2022320118';
INSERT IGNORE INTO member_role (member_id, role_code)
SELECT member_id, 'OFFICER' FROM member WHERE student_id = '1003';
INSERT IGNORE INTO member_role (member_id, role_code)
SELECT member_id, 'OBSERVE_MEMBER' FROM member WHERE student_id = '2024220155';
INSERT IGNORE INTO member_role (member_id, role_code)
SELECT member_id, 'TEAM_LEADER' FROM member WHERE student_id = '1005';
INSERT IGNORE INTO member_role (member_id, role_code)
SELECT member_id, 'MEMBER' FROM member WHERE student_id = '2022120144';
INSERT IGNORE INTO member_role (member_id, role_code)
SELECT member_id, 'ADVISOR' FROM member WHERE student_id = '1009';

-- ==================== 知识点（与 mockTagTree 一致） ====================
INSERT IGNORE INTO tag (tag_id, name, category, parent_id, sort_order, create_time, update_time, deleted) VALUES
    (1, '基础算法', '算法', NULL, 1, NOW(), NOW(), 0),
    (2, '动态规划', '算法', NULL, 2, NOW(), NOW(), 0),
    (3, '图论', '算法', NULL, 3, NOW(), NOW(), 0),
    (4, '数据结构', '算法', NULL, 4, NOW(), NOW(), 0),
    (5, '数学', '算法', NULL, 5, NOW(), NOW(), 0),
    (6, '字符串', '算法', NULL, 6, NOW(), NOW(), 0),

    (11, '枚举', '基础算法', 1, 1, NOW(), NOW(), 0),
    (12, '模拟', '基础算法', 1, 2, NOW(), NOW(), 0),
    (13, '排序', '基础算法', 1, 3, NOW(), NOW(), 0),
    (14, '二分', '基础算法', 1, 4, NOW(), NOW(), 0),
    (15, '前缀和', '基础算法', 1, 5, NOW(), NOW(), 0),

    (21, '线性 DP', '动态规划', 2, 1, NOW(), NOW(), 0),
    (22, '背包问题', '动态规划', 2, 2, NOW(), NOW(), 0),
    (23, '区间 DP', '动态规划', 2, 3, NOW(), NOW(), 0),
    (24, '树形 DP', '动态规划', 2, 4, NOW(), NOW(), 0),

    (31, '最短路', '图论', 3, 1, NOW(), NOW(), 0),
    (32, '最小生成树', '图论', 3, 2, NOW(), NOW(), 0),
    (33, '拓扑排序', '图论', 3, 3, NOW(), NOW(), 0),
    (34, '网络流', '图论', 3, 4, NOW(), NOW(), 0),

    (41, '并查集', '数据结构', 4, 1, NOW(), NOW(), 0),
    (42, '线段树', '数据结构', 4, 2, NOW(), NOW(), 0),
    (43, '树状数组', '数据结构', 4, 3, NOW(), NOW(), 0),
    (44, '单调栈', '数据结构', 4, 4, NOW(), NOW(), 0),

    (51, '数论', '数学', 5, 1, NOW(), NOW(), 0),
    (52, '组合数学', '数学', 5, 2, NOW(), NOW(), 0),
    (53, '概率与期望', '数学', 5, 3, NOW(), NOW(), 0),

    (61, 'KMP', '字符串', 6, 1, NOW(), NOW(), 0),
    (62, '哈希', '字符串', 6, 2, NOW(), NOW(), 0),
    (63, '字典树', '字符串', 6, 3, NOW(), NOW(), 0);

-- ==================== 题库（与 mockProblems 一致） ====================
INSERT IGNORE INTO problem (problem_id, title, oj_problem_id, difficulty, time_limit, memory_limit,
                            description, input_format, output_format, sample_input, sample_output, hint,
                            visibility, creator_id, create_time, update_time, deleted) VALUES
    (100, 'A + B Problem', '1000', 'EASY', 1000, 131072,
     '输入两个整数 A 和 B，输出它们的和。\n\n这是评测系统的入门题目，用于检查代码提交链路是否正常。',
     '一行两个整数 A、B（-10^9 ≤ A, B ≤ 10^9）。', '一个整数，表示 A + B。', '1 2', '3',
     '注意使用 long long，避免溢出。', 'PUBLIC', 2, '2026-10-01 10:00:00', NOW(), 0),
    (101, '区间求和', '1001', 'EASY', 1000, 131072,
     '给定一个长度为 n 的数组，回答 m 次区间 [l, r] 的和。',
     '第一行两个整数 n、m；第二行 n 个整数；接下来 m 行每行两个整数 l、r。',
     '对每次询问输出一行，表示区间和。', '5 3\n1 2 3 4 5\n1 3\n2 4\n1 5', '6\n9\n15',
     '前缀和模板题。', 'PUBLIC', 2, '2026-10-02 10:00:00', NOW(), 0),
    (102, '01 背包', '1002', 'MEDIUM', 1000, 131072,
     '有 n 件物品和一个容量为 V 的背包，每件物品只能选一次，求最大价值。',
     '第一行 n、V；接下来 n 行每行两个整数 w、v。', '一个整数，表示最大总价值。',
     '4 5\n1 2\n2 4\n3 4\n4 5', '8', '经典 01 背包，注意一维数组需要倒序枚举。',
     'PUBLIC', 2, '2026-10-03 10:00:00', NOW(), 0),
    (103, '单源最短路', '1003', 'MEDIUM', 2000, 262144,
     '给定一个 n 个点 m 条边的有向图，求从 1 号点到其他所有点的最短距离。',
     '第一行 n、m；接下来 m 行每行三个整数 u、v、w。',
     '一行 n 个整数，表示最短距离，不可达输出 -1。',
     '4 5\n1 2 1\n1 3 4\n2 3 1\n2 4 5\n3 4 1', '0 1 2 3', 'Dijkstra + 优先队列。',
     'PUBLIC', 2, '2026-10-04 10:00:00', NOW(), 0),
    (104, '并查集合并', '1004', 'EASY', 1000, 131072,
     '维护 n 个元素的集合，支持合并与查询是否属于同一集合。',
     '第一行 n、m；接下来 m 行每行一个操作。', '对每个查询输出 Yes 或 No。',
     '4 5\n1 1 2\n2 1 2\n1 3 4\n2 3 4\n2 1 4', 'Yes\nYes\nNo', '路径压缩模板。',
     'PUBLIC', 2, '2026-10-05 10:00:00', NOW(), 0),
    (105, '最长上升子序列', '1005', 'MEDIUM', 1000, 131072,
     '给定长度为 n 的序列，求最长严格上升子序列的长度。',
     '第一行 n；第二行 n 个整数。', '一个整数。',
     '8\n10 9 2 5 3 7 101 18', '4', '可以尝试 O(n log n) 的贪心 + 二分做法。',
     'PUBLIC', 2, '2026-10-06 10:00:00', NOW(), 0),
    (106, '线段树区间加', '1006', 'HARD', 2000, 262144,
     '维护一个序列，支持区间加与区间求和。',
     '第一行 n、m；第二行 n 个整数；接下来 m 行操作。', '对每个求和操作输出结果。',
     '5 5\n1 2 3 4 5\n1 1 3 1\n2 1 3', '9', '懒标记下传是核心。',
     'TRAINING_PRIVATE', 2, '2026-10-07 10:00:00', NOW(), 0),
    (107, '拓扑排序', '1007', 'MEDIUM', 1000, 131072,
     '给定有向图，输出任意一个拓扑序，若存在环输出 -1。',
     '第一行 n、m；接下来 m 行每行一条边。', '一行拓扑序或 -1。',
     '4 4\n1 2\n1 3\n2 4\n3 4', '1 3 2 4', 'Kahn 算法。',
     'PUBLIC', 2, '2026-10-08 10:00:00', NOW(), 0),
    (108, '字符串哈希', '1008', 'MEDIUM', 1000, 131072,
     '判断字符串的两个子串是否相同。',
     '第一行字符串 s；第二行 m；接下来 m 行每行四个整数 l1 r1 l2 r2。', '每次询问输出 Yes 或 No。',
     'abcabc\n2\n1 3 4 6\n1 2 2 3', 'Yes\nNo', '双哈希更稳妥。',
     'PUBLIC', 2, '2026-10-09 10:00:00', NOW(), 0),
    (109, 'KMP 匹配', '1009', 'HARD', 1000, 131072,
     '在文本串中查找模式串所有出现位置。',
     '第一行文本串；第二行模式串。', '所有出现位置，从 1 开始编号。',
     'abababa\naba', '1 3 5', '注意 next 数组的构建。',
     'TRAINING_PRIVATE', 2, '2026-10-10 10:00:00', NOW(), 0),
    (110, '组合数取模', '1010', 'MEDIUM', 1000, 131072,
     '求 C(n, m) mod 1e9+7。',
     '第一行 T；接下来 T 行每行两个整数 n、m。', '每行一个答案。',
     '3\n5 2\n10 5\n100 50', '10\n252\n538992043', '预处理阶乘与逆元。',
     'PUBLIC', 2, '2026-10-11 10:00:00', NOW(), 0),
    (111, '树的重心', '1011', 'HARD', 2000, 262144,
     '给定一棵树，求树的重心编号。',
     '第一行 n；接下来 n-1 行每行一条边。', '输出重心编号，多个输出最小的。',
     '5\n1 2\n2 3\n3 4\n4 5', '3', '一遍 DFS 统计子树大小。',
     'TRAINING_PRIVATE', 2, '2026-10-12 10:00:00', NOW(), 0);

INSERT IGNORE INTO problem_tag (id, problem_id, tag_id) VALUES
    (1, 100, 11), (2, 100, 12),
    (3, 101, 15), (4, 101, 11),
    (5, 102, 22), (6, 102, 21),
    (7, 103, 31), (8, 103, 42),
    (9, 104, 41),
    (10, 105, 21), (11, 105, 14),
    (12, 106, 42), (13, 106, 21),
    (14, 107, 33),
    (15, 108, 62),
    (16, 109, 61),
    (17, 110, 52), (18, 110, 51),
    (19, 111, 24), (20, 111, 42);

INSERT IGNORE INTO solution (solution_id, problem_id, content, reference_code,
                             create_time, update_time, deleted) VALUES
    (1, 100,
     '## 思路\n直接读入两个整数并输出它们的和即可。\n\n## 复杂度\n- 时间复杂度：O(1)\n- 空间复杂度：O(1)',
     '#include <iostream>\nusing namespace std;\nint main() {\n    long long a, b;\n    cin >> a >> b;\n    cout << a + b << endl;\n    return 0;\n}',
     '2026-10-12 10:00:00', '2026-10-12 10:00:00', 0),
    (2, 101,
     '## 思路\n预处理前缀和数组，`sum[r] - sum[l-1]` 即为答案。',
     NULL, '2026-10-15 09:30:00', '2026-10-15 09:30:00', 0),
    (3, 102,
     '## 思路\n定义 `dp[j]` 为容量 j 时的最大价值，倒序更新避免重复选取。',
     NULL, '2026-10-20 14:00:00', '2026-10-20 14:00:00', 0);

-- ==================== 训练与题目关联（与 mockTrainings 一致） ====================
-- creator_id = 6（徐清越，与 Mock 中展示的创建人姓名一致）
INSERT IGNORE INTO training (training_id, name, type, description, start_time, end_time,
                             leaderboard_visible, freeze_time, status, creator_id,
                             create_time, update_time, deleted) VALUES
    -- 时间为相对当前时间的示意值，保证启动后「已结束 / 未开始 / 草稿」状态可演示
    (1, '第十周队内训练赛', 'SOLO', '每周队内训练赛，覆盖基础算法与数据结构。',
     TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 3 DAY), '19:00:00'),
     TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 3 DAY), '22:00:00'), 1, 30, 'ENDED', 6,
     DATE_SUB(NOW(), INTERVAL 6 DAY), NOW(), 0),
    (2, 'DP 专项训练（一）', 'SOLO', '动态规划专项训练。',
     TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 1 DAY), '19:00:00'),
     TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 1 DAY), '22:00:00'), 1, 30, 'PUBLISHED', 6,
     DATE_SUB(NOW(), INTERVAL 2 DAY), NOW(), 0),
    (3, '图论专题训练', 'TEAM', '图论专题组队训练。',
     TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 2 DAY), '19:00:00'),
     TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 2 DAY), '22:00:00'), 1, 0, 'PUBLISHED', 6,
     DATE_SUB(NOW(), INTERVAL 1 DAY), NOW(), 0),
    (4, '字符串与哈希', 'SOLO', '字符串与哈希专题训练。',
     TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 4 DAY), '19:00:00'),
     TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 4 DAY), '22:00:00'), 0, 20, 'PUBLISHED', 6,
     NOW(), NOW(), 0),
    (5, '新生基础算法训练', 'SOLO', '面向新生的基础算法训练。',
     TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 6 DAY), '19:00:00'),
     TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 6 DAY), '21:30:00'), 1, 0, 'DRAFT', 6,
     NOW(), NOW(), 0),
    (6, '第九周队内训练赛', 'TEAM', '第九周队内组队训练赛。',
     TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 7 DAY), '19:00:00'),
     TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 7 DAY), '22:00:00'), 1, 30, 'ENDED', 6,
     DATE_SUB(NOW(), INTERVAL 10 DAY), NOW(), 0);

INSERT IGNORE INTO problem_link (id, target_type, target_id, problem_id, sort_order) VALUES
    (1, 'TRAINING', 1, 100, 0), (2, 'TRAINING', 1, 101, 1), (3, 'TRAINING', 1, 102, 2),
    (4, 'TRAINING', 1, 103, 3), (5, 'TRAINING', 1, 104, 4),
    (6, 'TRAINING', 2, 102, 0), (7, 'TRAINING', 2, 105, 1), (8, 'TRAINING', 2, 106, 2),
    (9, 'TRAINING', 3, 103, 0), (10, 'TRAINING', 3, 104, 1), (11, 'TRAINING', 3, 107, 2),
    (12, 'TRAINING', 3, 111, 3),
    (13, 'TRAINING', 4, 108, 0), (14, 'TRAINING', 4, 109, 1),
    (15, 'TRAINING', 5, 100, 0), (16, 'TRAINING', 5, 101, 1), (17, 'TRAINING', 5, 104, 2),
    (18, 'TRAINING', 6, 101, 0), (19, 'TRAINING', 6, 102, 1), (20, 'TRAINING', 6, 103, 2),
    (21, 'TRAINING', 6, 105, 3), (22, 'TRAINING', 6, 107, 4);

-- ==================== 训练提交记录（与 mockSubmissions 一致） ====================
INSERT IGNORE INTO submission (submit_id, training_id, contest_id, problem_id, submitter_id,
                               code, language, result, time_used, memory_used, submit_time,
                               create_time, update_time, deleted) VALUES
    (5001, 1, NULL, 100, 1, '#include <bits/stdc++.h>\nint main(){return 0;}', 'C++17', 'AC', 12, 3584,
     '2026-11-04 19:05:00', '2026-11-04 19:05:00', NOW(), 0),
    (5002, 1, NULL, 101, 1, '#include <bits/stdc++.h>\nint main(){return 0;}', 'C++17', 'AC', 45, 5120,
     '2026-11-04 19:18:00', '2026-11-04 19:18:00', NOW(), 0),
    (5003, 1, NULL, 102, 1, '#include <bits/stdc++.h>\nint main(){return 0;}', 'C++17', 'WA', 34, 4096,
     '2026-11-04 19:32:00', '2026-11-04 19:32:00', NOW(), 0),
    (5004, 1, NULL, 102, 1, '#include <bits/stdc++.h>\nint main(){return 0;}', 'C++17', 'AC', 52, 6144,
     '2026-11-04 19:47:00', '2026-11-04 19:47:00', NOW(), 0),
    (5005, 1, NULL, 103, 1, '#include <bits/stdc++.h>\nint main(){return 0;}', 'C++17', 'TLE', 2002, 8192,
     '2026-11-04 20:15:00', '2026-11-04 20:15:00', NOW(), 0),
    (5006, 1, NULL, 104, 1, '#include <bits/stdc++.h>\nint main(){return 0;}', 'C++17', 'AC', 28, 4096,
     '2026-11-04 20:41:00', '2026-11-04 20:41:00', NOW(), 0),
    (5007, 2, NULL, 106, 1, 'public class Main { public static void main(String[] a) {} }', 'Java 17', 'CE', 0, 0,
     '2026-11-06 19:12:00', '2026-11-06 19:12:00', NOW(), 0),
    (5008, 2, NULL, 102, 1, '#include <bits/stdc++.h>\nint main(){return 0;}', 'C++17', 'JUDGING', 0, 0,
     '2026-11-06 19:20:00', '2026-11-06 19:20:00', NOW(), 0);

-- ==================== 队伍（与 mockTeams 一致） ====================
INSERT IGNORE INTO team (team_id, name, leader_id, contest_target, description, status,
                         create_time, update_time, deleted) VALUES
    (1, '深夜 AC 小分队', 11, '2026 XCPC 校内选拔赛',
     '备战 2026 校赛，主攻动态规划与图论，每周三、周六晚队内训练。', 'CONFIRMED',
     '2026-09-12 19:00:00', NOW(), 0),
    (2, '线性求导队', 6, '2026 新生程序设计校赛',
     '实验室成员组建的训练队，侧重数据结构与数学专题。', 'CONFIRMED',
     '2026-09-20 19:00:00', NOW(), 0),
    (3, '新生冲锋队', 3, '2026 级新生程序设计选拔赛',
     '新生自发组队，等待助理教练确认后参加校赛选拔。', 'PENDING_CONFIRM',
     '2026-10-28 20:00:00', NOW(), 0);

INSERT IGNORE INTO team_member (id, team_id, member_id, join_time) VALUES
    (1, 1, 11, '2026-09-12 19:00:00'),
    (2, 1, 2, '2026-09-18 19:00:00'),
    (3, 1, 3, '2026-10-08 19:00:00'),
    (4, 2, 6, '2026-09-20 19:00:00'),
    (5, 2, 5, '2026-09-25 19:00:00'),
    (6, 3, 3, '2026-10-28 20:00:00'),
    (7, 3, 7, '2026-10-29 20:00:00');

-- ==================== 集训、任务与考勤（与 mockCamps 一致） ====================
INSERT IGNORE INTO camp (camp_id, type, name, start_time, end_time, location, registration_deadline,
                         description, requirements, status, creator_id,
                         create_time, update_time, deleted) VALUES
    (1, 'WINTER', '2027 年寒假集训', '2027-01-10 00:00:00', '2027-01-24 23:59:59',
     '计算机学院实验楼 A301', '2027-01-05 23:59:59',
     '为期两周的寒假集中训练，上午专题讲解、下午自主训练、晚上队内赛，结营安排选拔赛。',
     NULL, 'ANNOUNCED', 6, '2026-12-15 10:00:00', NOW(), 0),
    (2, 'SUMMER', '2026 年暑假集训', '2026-07-12 00:00:00', '2026-07-26 23:59:59',
     '计算机学院实验楼 A305', '2026-07-06 23:59:59',
     '暑假集训已结营，集训期间完成动态规划、图论、数据结构三大专题训练与结营考核。',
     NULL, 'ENDED', 6, '2026-06-20 10:00:00', NOW(), 0);

INSERT IGNORE INTO camp_task (task_id, camp_id, title, content, task_date, creator_id,
                              create_time, update_time, deleted) VALUES
    (1, 1, 'Day1 开营与热身赛',
     '上午 9:00 开营宣讲并讲解集训安排；下午完成热身赛（5 题，3 小时）；晚上复盘并组队。',
     '2027-01-10', 6, '2027-01-09 20:00:00', NOW(), 0),
    (2, 1, 'Day2 动态规划专题',
     '上午讲解线性 DP 与背包问题；下午完成 DP 专题 6 题；晚上队内赛（DP 专项）。',
     '2027-01-11', 6, '2027-01-10 20:30:00', NOW(), 0),
    (3, 2, 'Day1 图论专题',
     '上午讲解最短路与最小生成树；下午完成图论专题 5 题；晚上队内赛复盘。',
     '2026-07-12', 6, '2026-07-11 20:00:00', NOW(), 0);

INSERT IGNORE INTO attendance (record_id, camp_id, member_id, check_in_date, check_in_time,
                               status, remark, create_time, update_time, deleted) VALUES
    (1, 2, 11, '2026-07-12', '2026-07-12 09:02:00', 'PRESENT', NULL, NOW(), NOW(), 0),
    (2, 2, 6, '2026-07-12', '2026-07-12 08:58:00', 'PRESENT', NULL, NOW(), NOW(), 0),
    (3, 2, 5, '2026-07-12', NULL, 'ABSENT', '未签到', NOW(), NOW(), 0),
    (4, 2, 11, '2026-07-13', '2026-07-13 09:05:00', 'PRESENT', NULL, NOW(), NOW(), 0),
    (5, 2, 6, '2026-07-13', NULL, 'LEAVE', '参加学院暑期实践，已提前请假', NOW(), NOW(), 0),
    (6, 2, 5, '2026-07-13', NULL, 'ABSENT', '未签到', NOW(), NOW(), 0),
    (7, 1, 6, '2027-01-10', '2027-01-10 08:50:00', 'PRESENT', NULL, NOW(), NOW(), 0);

-- ==================== 赛事（与 mockContests 一致） ====================
-- 前端 PUBLISHED 对应后端 REGISTERING；DRAFT 对应 ANNOUNCED
INSERT IGNORE INTO contest (contest_id, name, category, start_time, end_time, registration_deadline,
                            description, requirements, max_team_size, freeze_minutes, status, creator_id,
                            create_time, update_time, deleted) VALUES
    -- 时间为相对当前时间的示意值，保证启动后「已结束 / 进行中 / 报名中」状态可演示
    (1, '中南民族大学第十届校内程序设计竞赛', 'SCHOLASTIC',
     TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 14 DAY), '13:00:00'),
     TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 14 DAY), '18:00:00'),
     TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 17 DAY), '23:59:59'),
     '面向全校的年度校内程序设计竞赛，5 小时 8 题，赛后将评选一、二、三等奖并生成电子证书。',
     NULL, 3, 60, 'ENDED', 6, DATE_SUB(NOW(), INTERVAL 30 DAY), NOW(), 0),
    (2, '2026 XCPC 网络预选赛', 'XCPC',
     DATE_SUB(NOW(), INTERVAL 2 HOUR), DATE_ADD(NOW(), INTERVAL 3 HOUR),
     DATE_SUB(NOW(), INTERVAL 1 DAY),
     'XCPC 系列网络预选赛，队伍以 3 人为单位参赛，比赛结束前 60 分钟封榜，晋级队伍将代表学校参加区域赛。',
     NULL, 3, 60, 'ONGOING', 6, DATE_SUB(NOW(), INTERVAL 15 DAY), NOW(), 0),
    (3, '2026 级新生程序设计选拔赛', 'SCHOLASTIC',
     TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 1 DAY), '14:00:00'),
     TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 1 DAY), '19:00:00'),
     DATE_ADD(NOW(), INTERVAL 12 HOUR),
     '面向 2026 级新生的校内选拔赛，个人赛形式，用于选拔进入实验室的候选人。',
     NULL, 1, 30, 'REGISTERING', 6, DATE_SUB(NOW(), INTERVAL 5 DAY), NOW(), 0),
    (4, 'CCPC 女生专场赛（校外）', 'NON_XCPC',
     TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 7 DAY), '09:00:00'),
     TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 7 DAY), '14:00:00'),
     TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 5 DAY), '23:59:59'),
     '非 XCPC 系列的校外邀请赛，协会统一组织报名并由教练带队参赛。',
     NULL, 3, 0, 'REGISTERING', 6, DATE_SUB(NOW(), INTERVAL 3 DAY), NOW(), 0),
    (5, '2026 冬季校赛热身赛', 'SCHOLASTIC',
     TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 14 DAY), '19:00:00'),
     TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 14 DAY), '22:00:00'),
     TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 7 DAY), '23:59:59'),
     '冬季校赛前的热身赛，题目难度中等偏易，用于帮助参赛队员调整状态。',
     NULL, 3, 30, 'ANNOUNCED', 6, DATE_SUB(NOW(), INTERVAL 1 DAY), NOW(), 0);

INSERT IGNORE INTO problem_link (id, target_type, target_id, problem_id, sort_order) VALUES
    (30, 'CONTEST', 1, 100, 0), (31, 'CONTEST', 1, 101, 1), (32, 'CONTEST', 1, 102, 2),
    (33, 'CONTEST', 1, 103, 3), (34, 'CONTEST', 1, 104, 4), (35, 'CONTEST', 1, 105, 5),
    (36, 'CONTEST', 1, 107, 6), (37, 'CONTEST', 1, 110, 7),
    (38, 'CONTEST', 2, 101, 0), (39, 'CONTEST', 2, 102, 1), (40, 'CONTEST', 2, 103, 2),
    (41, 'CONTEST', 2, 105, 3), (42, 'CONTEST', 2, 107, 4), (43, 'CONTEST', 2, 108, 5),
    (44, 'CONTEST', 2, 110, 6), (45, 'CONTEST', 2, 111, 7),
    (46, 'CONTEST', 3, 100, 0), (47, 'CONTEST', 3, 101, 1), (48, 'CONTEST', 3, 102, 2),
    (49, 'CONTEST', 3, 104, 3), (50, 'CONTEST', 3, 107, 4),
    (51, 'CONTEST', 4, 101, 0), (52, 'CONTEST', 4, 103, 1), (53, 'CONTEST', 4, 105, 2),
    (54, 'CONTEST', 4, 110, 3),
    (55, 'CONTEST', 5, 100, 0), (56, 'CONTEST', 5, 101, 1), (57, 'CONTEST', 5, 104, 2),
    (58, 'CONTEST', 5, 107, 3);

-- ==================== 通用申请：入会/退会 + 集训报名 + 赛事报名 ====================
-- 入会/退会（与 mockApprovals 一致）；入会申请人尚未成为会员，applicant_id 为空
INSERT IGNORE INTO application (apply_id, type, target_id, applicant_id, team_id, apply_data,
                                status, audit_remark, auditor_id, apply_time, audit_time,
                                create_time, update_time, deleted) VALUES
    (1, 'JOIN_MEMBER', NULL, NULL, NULL,
     '{"name":"刘星宇","studentId":"2025210333","reason":"对算法竞赛有浓厚兴趣，已自学完成基础数据结构，希望加入协会接受系统训练。"}',
     'PENDING', NULL, NULL, '2026-11-03 10:22:00', NULL, '2026-11-03 10:22:00', NOW(), 0),
    (2, 'JOIN_MEMBER', NULL, NULL, NULL,
     '{"name":"马若彤","studentId":"2025220211","reason":"参加过校内程序设计新生赛并获得二等奖，希望加入协会继续提升。"}',
     'PENDING', NULL, NULL, '2026-11-02 16:40:00', NULL, '2026-11-02 16:40:00', NOW(), 0),
    (3, 'JOIN_MEMBER', NULL, NULL, NULL,
     '{"name":"高子恒","studentId":"2024310405","reason":"希望通过协会的训练体系备战明年的校赛。"}',
     'PENDING', NULL, NULL, '2026-11-01 20:05:00', NULL, '2026-11-01 20:05:00', NOW(), 0),
    (4, 'RESIGN', NULL, 8, NULL,
     '{"name":"何思远","studentId":"2022320118","reason":"课业压力较大，暂时无法参加日常训练，申请退会。"}',
     'PENDING', NULL, NULL, '2026-10-30 09:12:00', NULL, '2026-10-30 09:12:00', NOW(), 0),
    (5, 'JOIN_MEMBER', NULL, NULL, NULL,
     '{"name":"唐可","studentId":"2023310222","reason":"已完成观察期考核，申请转为正式会员。"}',
     'APPROVED', '已通过审核', 2, '2026-10-28 14:30:00', '2026-10-29 10:00:00',
     '2026-10-28 14:30:00', NOW(), 0);

-- 集训报名（与 mockCampRegistrations 一致）
INSERT IGNORE INTO application (apply_id, type, target_id, applicant_id, team_id, apply_data,
                                status, audit_remark, auditor_id, apply_time, audit_time,
                                create_time, update_time, deleted) VALUES
    (6, 'CAMP_REGISTER', 1, 3, NULL,
     '{"grade":"2024级","reason":"希望利用寒假系统补习动态规划。"}',
     'PENDING', NULL, NULL, '2026-11-04 10:20:00', NULL, '2026-11-04 10:20:00', NOW(), 0),
    (7, 'CAMP_REGISTER', 1, 5, NULL,
     '{"grade":"2023级"}',
     'PENDING', NULL, NULL, '2026-11-05 15:40:00', NULL, '2026-11-05 15:40:00', NOW(), 0),
    (8, 'CAMP_REGISTER', 1, 6, NULL,
     '{"grade":"2022级","reason":"已通过审核。"}',
     'APPROVED', '已通过审核', 4, '2026-11-03 09:10:00', '2026-11-03 15:00:00',
     '2026-11-03 09:10:00', NOW(), 0),
    (9, 'CAMP_REGISTER', 2, 11, NULL,
     '{"grade":"2023级"}',
     'APPROVED', NULL, 4, '2026-06-25 11:00:00', '2026-06-26 09:00:00',
     '2026-06-25 11:00:00', NOW(), 0),
    (10, 'CAMP_REGISTER', 2, 6, NULL,
     '{"grade":"2022级"}',
     'APPROVED', NULL, 4, '2026-06-26 14:20:00', '2026-06-27 09:00:00',
     '2026-06-26 14:20:00', NOW(), 0),
    (11, 'CAMP_REGISTER', 2, 5, NULL,
     '{"grade":"2023级"}',
     'APPROVED', NULL, 4, '2026-06-28 16:35:00', '2026-06-29 09:00:00',
     '2026-06-28 16:35:00', NOW(), 0);

-- 赛事报名（与 mockContestRegistrations 一致）
INSERT IGNORE INTO application (apply_id, type, target_id, applicant_id, team_id, apply_data,
                                status, audit_remark, auditor_id, apply_time, audit_time,
                                create_time, update_time, deleted) VALUES
    (12, 'CONTEST_REGISTER', 1, 11, 1,
     '{"grade":"2023级","memberCount":3}',
     'APPROVED', NULL, 4, '2026-10-20 10:15:00', '2026-10-21 09:00:00',
     '2026-10-20 10:15:00', NOW(), 0),
    (13, 'CONTEST_REGISTER', 1, 2, NULL,
     '{"grade":"2023级","memberCount":1,"remark":"个人报名，希望检验近期训练成果。"}',
     'APPROVED', NULL, 4, '2026-10-21 14:02:00', '2026-10-22 09:00:00',
     '2026-10-21 14:02:00', NOW(), 0),
    (14, 'CONTEST_REGISTER', 1, 6, NULL,
     '{"grade":"2022级","memberCount":1}',
     'APPROVED', NULL, 4, '2026-10-22 09:30:00', '2026-10-23 09:00:00',
     '2026-10-22 09:30:00', NOW(), 0),
    (15, 'CONTEST_REGISTER', 2, 6, 2,
     '{"grade":"2022级","memberCount":2}',
     'APPROVED', NULL, 4, '2026-11-01 18:20:00', '2026-11-02 09:00:00',
     '2026-11-01 18:20:00', NOW(), 0),
    (16, 'CONTEST_REGISTER', 2, 5, NULL,
     '{"grade":"2023级","memberCount":1}',
     'APPROVED', NULL, 4, '2026-11-02 20:45:00', '2026-11-03 09:00:00',
     '2026-11-02 20:45:00', NOW(), 0),
    (17, 'CONTEST_REGISTER', 3, 3, 3,
     '{"grade":"2024级","memberCount":2,"remark":"新生组队报名，希望参加选拔。"}',
     'PENDING', NULL, NULL, '2026-11-05 11:10:00', NULL, '2026-11-05 11:10:00', NOW(), 0),
    (18, 'CONTEST_REGISTER', 3, 2, NULL,
     '{"grade":"2023级","memberCount":1}',
     'PENDING', NULL, NULL, '2026-11-06 09:05:00', NULL, '2026-11-06 09:05:00', NOW(), 0),
    (19, 'CONTEST_REGISTER', 4, 11, NULL,
     '{"grade":"2023级","memberCount":1,"remark":"希望随队参加校外邀请赛。"}',
     'PENDING', NULL, NULL, '2026-11-06 15:40:00', NULL, '2026-11-06 15:40:00', NOW(), 0);

-- ==================== 赛事成绩（与 mockContestResults 一致） ====================
INSERT IGNORE INTO contest_result (result_id, contest_id, team_id, member_id, rank_no, solved_count,
                                   penalty, score, award, remark, create_time, update_time, deleted) VALUES
    (1, 1, 1, NULL, 1, 8, NULL, 8.00, '一等奖', '全场唯一 AK 队伍', NOW(), NOW(), 0),
    (2, 1, NULL, 2, 2, 7, NULL, 7.00, '二等奖', NULL, NOW(), NOW(), 0),
    (3, 1, NULL, 6, 3, 6, NULL, 6.00, '二等奖', NULL, NOW(), NOW(), 0);

-- ==================== MBTI 题目与结果（与 mockMbtiQuestions / mockMbtiResults 一致） ====================
INSERT IGNORE INTO mbti_question (question_id, question_no, dimension, content, option_a, option_b,
                                  score_a, score_b, create_time, update_time, deleted) VALUES
    (1, 1, 'E_I', '在团队讨论中，你更倾向于哪种状态？', '主动发言，带动讨论气氛', '先倾听，想清楚再表达', 1, 1, NOW(), NOW(), 0),
    (2, 2, 'E_I', '连续参加几场社交活动后，你通常会：', '精力充沛，还想继续交流', '感到疲惫，需要独处恢复', 1, 1, NOW(), NOW(), 0),
    (3, 3, 'S_N', '面对一道全新的算法题，你更先关注：', '具体的样例数据与边界条件', '题目背后可能存在的通用模型', 1, 1, NOW(), NOW(), 0),
    (4, 4, 'S_N', '学习新知识时，你更信任：', '已被反复验证的经验做法', '尚未尝试过的新思路与猜想', 1, 1, NOW(), NOW(), 0),
    (5, 5, 'T_F', '当队友提交的代码出现问题时，你会：', '直接指出问题并说明原因', '先安抚情绪再一起排查', 1, 1, NOW(), NOW(), 0),
    (6, 6, 'T_F', '做决策时，你更看重：', '客观数据与逻辑推演', '团队成员的感受与整体氛围', 1, 1, NOW(), NOW(), 0),
    (7, 7, 'J_P', '备赛期间，你更习惯：', '提前列好计划并逐项完成', '随机应变，按兴趣推进', 1, 1, NOW(), NOW(), 0),
    (8, 8, 'J_P', '面对临近的比赛，你倾向于：', '尽早确定方案并完成收尾', '保留调整空间，做到最后一刻', 1, 1, NOW(), NOW(), 0);

INSERT IGNORE INTO mbti_result (result_id, member_id, type, score_ei, score_sn, score_tf, score_jp,
                                report, test_time, create_time, update_time, deleted) VALUES
    (1, 1, 'ENTP', 62, 34, 58, 40,
     '陈昊 的测评结果为 ENTP（辩论家）。该类型思维活跃、乐于尝试新解法，适合在队伍中担任破题与思路拓展的角色。',
     '2026-10-18 20:15:00', NOW(), NOW(), 0),
    (2, 2, 'INTJ', 28, 22, 74, 82,
     '李知远 的测评结果为 INTJ（建筑师）。该类型擅长整体规划与结构化思考，适合承担训练计划制定与知识体系搭建的工作。',
     '2026-10-20 19:40:00', NOW(), NOW(), 0);
