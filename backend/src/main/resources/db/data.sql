-- =====================================================================
-- ICPC 协会管理系统 · 演示初始化数据
-- 数据来源：icpc_frontend/src/mock/index.ts（前端演示数据）
-- 执行时机：dev profile 启动时由 spring.sql.init 自动执行（可重复执行）
-- 说明：全部使用 INSERT IGNORE + 显式主键，空库首次启动即可得到与前端 Mock
--       一致的会员、题库、训练、集训、赛事、队伍与 MBTI 数据。
-- 演示账号（密码统一为 123456，学号连续编号）：
--   1001 陈昊（观察会员）   1002 李知远（正式会员）  1003 周雨桐（队长）
--   1004 黄启帆（干事）     1005 徐清越（助理教练）  1006 赵子轩（会长/副会长）
--   1007 顾明（指导老师）
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
-- 密码哈希对应明文 123456；演示身份（学号连续 1001-1007）：
--   1001 观察会员  1002 正式会员  1003 队长  1004 干事
--   1005 助理教练  1006 会长/副会长  1007 指导老师
INSERT IGNORE INTO member (member_id, student_id, password, name, major, grade, phone, email, avatar,
                           member_type, department, join_date, status,
                           create_time, update_time, deleted) VALUES
    (1, '1001', '$2a$10$J658/MW6soiM65MAWeWEKOW2R5b9qwGfSjsHzQIdVqcyH0jbsUUPa',
     '陈昊', '计算机科学与技术', '2023级', '13800000001', 'chenhao@example.com', NULL,
     'OBSERVE_MEMBER', '竞赛部', '2023-09-20', 'ACTIVE', '2023-09-20 09:00:00', NOW(), 0),
    (2, '1002', '$2a$10$J658/MW6soiM65MAWeWEKOW2R5b9qwGfSjsHzQIdVqcyH0jbsUUPa',
     '李知远', '软件工程', '2023级', '13800000002', 'lizhiyuan@example.com', NULL,
     'MEMBER', '竞赛部', '2023-10-08', 'ACTIVE', '2023-10-08 09:00:00', NOW(), 0),
    (3, '2024310108', '$2a$10$J658/MW6soiM65MAWeWEKOW2R5b9qwGfSjsHzQIdVqcyH0jbsUUPa',
     '王一鸣', '计算机科学与技术', '2024级', '13800000003', 'wangyiming@example.com', NULL,
     'MEMBER', NULL, '2026-10-02', 'ACTIVE', '2026-10-02 09:00:00', NOW(), 0),
    (4, '1003', '$2a$10$J658/MW6soiM65MAWeWEKOW2R5b9qwGfSjsHzQIdVqcyH0jbsUUPa',
     '周雨桐', '数据科学与大数据技术', '2024级', '13800000004', 'zhouyutong@example.com', NULL,
     'MEMBER', '竞赛部', '2024-09-15', 'ACTIVE', '2024-09-15 09:00:00', NOW(), 0),
    (5, '1006', '$2a$10$J658/MW6soiM65MAWeWEKOW2R5b9qwGfSjsHzQIdVqcyH0jbsUUPa',
     '赵子轩', '网络工程', '2023级', '13800000005', 'zhaozixuan@example.com', NULL,
     'MEMBER', '组织部', '2023-10-12', 'ACTIVE', '2023-10-12 09:00:00', NOW(), 0),
    (6, '1005', '$2a$10$J658/MW6soiM65MAWeWEKOW2R5b9qwGfSjsHzQIdVqcyH0jbsUUPa',
     '徐清越', '计算机科学与技术', '2022级', '13800000006', 'xuqingyue@example.com', NULL,
     'MEMBER', '竞赛部', '2022-09-18', 'ACTIVE', '2022-09-18 09:00:00', NOW(), 0),
    (7, '2024310211', '$2a$10$J658/MW6soiM65MAWeWEKOW2R5b9qwGfSjsHzQIdVqcyH0jbsUUPa',
     '孙嘉树', '人工智能', '2024级', '13800000007', 'sunjiaishu@example.com', NULL,
     'OBSERVE_MEMBER', NULL, '2026-10-09', 'ACTIVE', '2026-10-09 09:00:00', NOW(), 0),
    (8, '2022320118', '$2a$10$J658/MW6soiM65MAWeWEKOW2R5b9qwGfSjsHzQIdVqcyH0jbsUUPa',
     '何思远', '软件工程', '2022级', '13800000008', 'hesiyuan@example.com', NULL,
     'MEMBER', '宣传部', '2022-10-20', 'RESIGNED', '2022-10-20 09:00:00', NOW(), 0),
    (9, '2023310109', '$2a$10$J658/MW6soiM65MAWeWEKOW2R5b9qwGfSjsHzQIdVqcyH0jbsUUPa',
     '林可欣', '计算机科学与技术', '2023级', '13800000009', 'linkexin@example.com', NULL,
     'MEMBER', '秘书部', '2023-09-25', 'ACTIVE', '2023-09-25 09:00:00', NOW(), 0),
    (10, '2024220155', '$2a$10$J658/MW6soiM65MAWeWEKOW2R5b9qwGfSjsHzQIdVqcyH0jbsUUPa',
     '郑浩然', '数据科学与大数据技术', '2024级', '13800000010', 'zhenghaoran@example.com', NULL,
     'OBSERVE_MEMBER', NULL, '2026-10-15', 'ACTIVE', '2026-10-15 09:00:00', NOW(), 0),
    (11, '1004', '$2a$10$J658/MW6soiM65MAWeWEKOW2R5b9qwGfSjsHzQIdVqcyH0jbsUUPa',
     '黄启帆', '网络工程', '2023级', '13800000011', 'huangqifan@example.com', NULL,
     'MEMBER', '竞赛部', '2023-09-28', 'ACTIVE', '2023-09-28 09:00:00', NOW(), 0),
    (12, '2022120144', '$2a$10$J658/MW6soiM65MAWeWEKOW2R5b9qwGfSjsHzQIdVqcyH0jbsUUPa',
     '吴思琪', '软件工程', '2022级', '13800000012', 'wusiqi@example.com', NULL,
     'MEMBER', '组织部', '2022-10-05', 'REMOVED', '2022-10-05 09:00:00', NOW(), 0),
    (20, '1007', '$2a$10$J658/MW6soiM65MAWeWEKOW2R5b9qwGfSjsHzQIdVqcyH0jbsUUPa',
     '顾明', '计算机科学与技术', NULL, NULL, 'guming@example.com', NULL,
     'MEMBER', '指导老师', '2020-09-01', 'ACTIVE', '2020-09-01 09:00:00', NOW(), 0);

-- 会员角色：演示账号学号统一为连续的 1001-1007。
-- 兼容旧脚本已写入的数据库：先腾出目标学号，再按顺序迁移。
UPDATE member SET student_id = '2023310109' WHERE member_id = 9 AND student_id = '1003';
UPDATE member SET student_id = '2024310108' WHERE member_id = 3 AND student_id = '1008';
UPDATE member SET student_id = '1003' WHERE member_id = 4 AND student_id = '1004';
UPDATE member SET student_id = '1004' WHERE member_id = 11 AND student_id = '1005';
UPDATE member SET student_id = '1005' WHERE member_id = 6 AND student_id = '1006';
UPDATE member SET student_id = '1006' WHERE member_id = 5 AND student_id = '1007';
UPDATE member SET student_id = '1007' WHERE member_id = 20 AND student_id = '1009';

-- 已取消“实验室成员”类型：历史 LAB_MEMBER 统一合并为正式会员。
-- 仅当该会员没有其他角色时补一个 MEMBER 角色，避免给会长/教练叠加普通会员角色。
INSERT IGNORE INTO member_role (member_id, role_code)
SELECT member_id, 'MEMBER' FROM member
WHERE member_type = 'LAB_MEMBER'
  AND member_id NOT IN (SELECT member_id FROM member_role WHERE role_code <> 'LAB_MEMBER');
DELETE FROM member_role WHERE role_code = 'LAB_MEMBER';
UPDATE member SET member_type = 'MEMBER' WHERE member_type = 'LAB_MEMBER';

-- 清理旧版演示脚本可能写入的冲突角色，保证重新执行后身份与演示账号一致。
DELETE FROM member_role WHERE member_id IN (SELECT member_id FROM member WHERE student_id = '1001') AND role_code = 'PRESIDENT';
DELETE FROM member_role WHERE member_id IN (SELECT member_id FROM member WHERE student_id = '1002') AND role_code = 'MINISTER';
DELETE FROM member_role WHERE member_id IN (SELECT member_id FROM member WHERE student_id = '2023310109') AND role_code = 'OFFICER';
DELETE FROM member_role WHERE member_id IN (SELECT member_id FROM member WHERE student_id = '1003') AND role_code = 'ASSISTANT_COACH';
DELETE FROM member_role WHERE member_id IN (SELECT member_id FROM member WHERE student_id = '1004') AND role_code = 'TEAM_LEADER';
DELETE FROM member_role WHERE member_id IN (SELECT member_id FROM member WHERE student_id = '1005') AND role_code IN ('LAB_MEMBER', 'MEMBER');
DELETE FROM member_role WHERE member_id IN (SELECT member_id FROM member WHERE student_id = '1006') AND role_code = 'MEMBER';
DELETE FROM member_role WHERE member_id IN (SELECT member_id FROM member WHERE student_id = '2024310108') AND role_code IN ('OBSERVE_MEMBER', 'PRESIDENT');

INSERT IGNORE INTO member_role (member_id, role_code)
SELECT member_id, 'OBSERVE_MEMBER' FROM member WHERE student_id = '1001';
INSERT IGNORE INTO member_role (member_id, role_code)
SELECT member_id, 'MEMBER' FROM member WHERE student_id = '1002';
INSERT IGNORE INTO member_role (member_id, role_code)
SELECT member_id, 'TEAM_LEADER' FROM member WHERE student_id = '1003';
INSERT IGNORE INTO member_role (member_id, role_code)
SELECT member_id, 'OFFICER' FROM member WHERE student_id = '1004';
INSERT IGNORE INTO member_role (member_id, role_code)
SELECT member_id, 'ASSISTANT_COACH' FROM member WHERE student_id = '1005';
INSERT IGNORE INTO member_role (member_id, role_code)
SELECT member_id, 'PRESIDENT' FROM member WHERE student_id = '1006';
INSERT IGNORE INTO member_role (member_id, role_code)
SELECT member_id, 'ADVISOR' FROM member WHERE student_id = '1007';
INSERT IGNORE INTO member_role (member_id, role_code)
SELECT member_id, 'MEMBER' FROM member WHERE student_id = '2023310109';
INSERT IGNORE INTO member_role (member_id, role_code)
SELECT member_id, 'MEMBER' FROM member WHERE student_id = '2024310108';
INSERT IGNORE INTO member_role (member_id, role_code)
SELECT member_id, 'OBSERVE_MEMBER' FROM member WHERE student_id = '2024310211';
INSERT IGNORE INTO member_role (member_id, role_code)
SELECT member_id, 'MEMBER' FROM member WHERE student_id = '2022320118';
INSERT IGNORE INTO member_role (member_id, role_code)
SELECT member_id, 'OBSERVE_MEMBER' FROM member WHERE student_id = '2024220155';
INSERT IGNORE INTO member_role (member_id, role_code)
SELECT member_id, 'MEMBER' FROM member WHERE student_id = '2022120144';

-- 同步演示账号的会员类型（INSERT IGNORE 不会更新已存在的会员记录）
UPDATE member SET member_type = 'OBSERVE_MEMBER' WHERE student_id = '1001';
UPDATE member SET member_type = 'MEMBER' WHERE student_id IN ('1002', '1003', '1004');
UPDATE member SET member_type = 'MEMBER' WHERE student_id IN ('1005', '1006', '1007');
UPDATE member SET status = 'ACTIVE' WHERE student_id = '1006';

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

-- ==================== 奖惩记录 ====================
INSERT IGNORE INTO reward (reward_id, member_id, title, category, level, amount, description,
                           grant_date, issuer_name, creator_id, create_time, update_time, deleted) VALUES
    (1, 6, '2026 校内程序设计竞赛一等奖', 'COMPETITION', 'SCHOOL', 500.00,
     '校赛个人赛第一名，授予荣誉证书与奖金。', '2026-11-01', 'ICPC 协会', 1, NOW(), NOW(), 0),
    (2, 9, '2025-2026 学年优秀干事', 'SERVICE', 'ASSOCIATION', 300.00,
     '在协会日常组织与训练保障工作中表现突出。', '2026-10-20', 'ICPC 协会组织部', 1, NOW(), NOW(), 0),
    (3, 11, '省级程序设计竞赛二等奖', 'COMPETITION', 'PROVINCIAL', 800.00,
     '代表学校参加省级赛事并获得二等奖。', '2026-09-15', 'ICPC 协会', 1, NOW(), NOW(), 0);

INSERT IGNORE INTO punishment (punishment_id, member_id, type, reason, description,
                               effective_date, expire_date, issuer_name, creator_id,
                               create_time, update_time, deleted) VALUES
    (1, 5, 'WARNING', '连续两次无故缺席集训', '经提醒后仍未按时参加集训，给予警告处分。',
     '2026-11-10', '2027-02-10', 'ICPC 协会组织部', 1, NOW(), NOW(), 0),
    (2, 12, 'SERIOUS_WARNING', '训练期间多次未按规定提交代码', '影响队伍整体训练进度，给予严重警告处分。',
     '2026-10-25', '2027-04-25', 'ICPC 协会组织部', 1, NOW(), NOW(), 0);

-- ==================== 考核与考核成绩 ====================
INSERT IGNORE INTO assessment (assessment_id, name, type, description, start_time, end_time,
                               pass_score, status, creator_id, create_time, update_time, deleted) VALUES
    (1, '2026 春季观察期考核', 'OBSERVATION', '面向 2026 级观察会员的观察期综合考核',
     '2026-11-20 09:00:00', '2026-12-05 18:00:00', 60, 'PUBLISHED', 4, NOW(), NOW(), 0),
    (2, '2026 秋季正式会员月度考核', 'REGULAR', '正式会员算法与工程能力月度考核',
     '2026-11-25 09:00:00', '2026-12-10 18:00:00', 70, 'DRAFT', 4, NOW(), NOW(), 0);

INSERT IGNORE INTO assessment_result (result_id, assessment_id, member_id, score, remark,
                                      audit_status, audit_remark, auditor_id, audit_time,
                                      create_time, update_time, deleted) VALUES
    (1, 1, 3, 82.00, '表现良好，基础扎实', 'APPROVED', '成绩无误', 1, '2026-12-06 10:00:00', NOW(), NOW(), 0),
    (2, 1, 7, 58.00, '算法基础仍需加强', 'PENDING', NULL, NULL, NULL, NOW(), NOW(), 0),
    (3, 1, 10, 76.00, '综合表现稳定', 'PENDING', NULL, NULL, NULL, NOW(), NOW(), 0);

-- ==================== 系统配置 ====================
INSERT IGNORE INTO system_config (config_key, config_value, description, update_time) VALUES
    ('member.maxCount', '200', '协会会员人数上限', NOW()),
    ('camp.registerDeadlineDays', '7', '集训报名提前截止天数', NOW()),
    ('training.freezeMinutes', '30', '训练结束前默认封榜分钟数', NOW()),
    ('contest.maxTeamSize', '3', 'XCPC 赛事默认最大队伍人数', NOW()),
    ('mbti.enabled', 'true', '是否开启 MBTI 测评模块', NOW());

-- ==================== 操作日志 ====================
INSERT IGNORE INTO operation_log (log_id, member_id, username, method, path, action, ip, status, duration, create_time) VALUES
    (1, 1, '陈昊', 'POST', '/api/v1/members/approvals/1/audit', '审批会员申请', '127.0.0.1', 200, 35, '2026-11-03 09:00:00'),
    (2, 4, '周雨桐', 'POST', '/api/v1/trainings', '创建训练', '127.0.0.1', 200, 52, '2026-11-05 14:20:00'),
    (3, 1, '陈昊', 'PUT', '/api/v1/system/configs', '更新系统配置', '127.0.0.1', 200, 28, '2026-11-08 10:10:00');

-- ==================== 文件记录 ====================
INSERT IGNORE INTO file_record (file_id, filename, stored_name, url, size, content_type,
                                uploader_id, create_time, update_time, deleted) VALUES
    (1, '入会动机说明.pdf', 'demo-join-reason.pdf', '/uploads/demo/join-reason.pdf', 245760,
     'application/pdf', 9, '2026-10-10 16:30:00', NOW(), 0),
    (2, '2026 校赛获奖名单.xlsx', 'demo-award-list.xlsx', '/uploads/demo/award-list.xlsx', 51200,
     'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', 1, '2026-11-02 09:15:00', NOW(), 0);

-- ==================== 训练提交补充数据（多会员 / 近 7 天，供首页与统计趋势展示） ====================
INSERT IGNORE INTO submission (submit_id, training_id, contest_id, problem_id, submitter_id,
                               code, language, result, time_used, memory_used, submit_time,
                               create_time, update_time, deleted) VALUES
    (5101, 1, NULL, 100, 2,  '#include <bits/stdc++.h>\nint main(){return 0;}', 'C++17', 'AC',  18, 4096,
     TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 1 DAY), '19:05:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 1 DAY), '19:05:00'), NOW(), 0),
    (5102, 1, NULL, 101, 4,  '#include <bits/stdc++.h>\nint main(){return 0;}', 'C++17', 'WA',  41, 5120,
     TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 1 DAY), '19:22:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 1 DAY), '19:22:00'), NOW(), 0),
    (5103, 1, NULL, 101, 4,  '#include <bits/stdc++.h>\nint main(){return 0;}', 'C++17', 'AC',  36, 5120,
     TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 1 DAY), '19:35:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 1 DAY), '19:35:00'), NOW(), 0),
    (5104, 1, NULL, 102, 11, '#include <bits/stdc++.h>\nint main(){return 0;}', 'C++17', 'AC',  55, 6144,
     TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 2 DAY), '19:10:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 2 DAY), '19:10:00'), NOW(), 0),
    (5105, 1, NULL, 103, 1,  '#include <bits/stdc++.h>\nint main(){return 0;}', 'C++17', 'AC',  92, 7168,
     TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 2 DAY), '20:02:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 2 DAY), '20:02:00'), NOW(), 0),
    (5106, 1, NULL, 104, 5,  '#include <bits/stdc++.h>\nint main(){return 0;}', 'C++17', 'WA',  47, 4096,
     TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 2 DAY), '20:31:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 2 DAY), '20:31:00'), NOW(), 0),
    (5107, 1, NULL, 104, 4,  '#include <bits/stdc++.h>\nint main(){return 0;}', 'C++17', 'AC',  33, 4096,
     TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 3 DAY), '19:18:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 3 DAY), '19:18:00'), NOW(), 0),
    (5108, 1, NULL, 100, 2,  '#include <bits/stdc++.h>\nint main(){return 0;}', 'C++17', 'AC',  21, 4096,
     TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 3 DAY), '19:55:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 3 DAY), '19:55:00'), NOW(), 0),
    (5109, 6, NULL, 101, 6,  '#include <bits/stdc++.h>\nint main(){return 0;}', 'C++17', 'AC',  26, 4096,
     TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 4 DAY), '19:08:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 4 DAY), '19:08:00'), NOW(), 0),
    (5110, 6, NULL, 102, 9,  '#include <bits/stdc++.h>\nint main(){return 0;}', 'C++17', 'AC',  61, 6144,
     TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 4 DAY), '19:44:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 4 DAY), '19:44:00'), NOW(), 0),
    (5111, 6, NULL, 103, 11, '#include <bits/stdc++.h>\nint main(){return 0;}', 'C++17', 'TLE', 2001, 8192,
     TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 5 DAY), '20:03:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 5 DAY), '20:03:00'), NOW(), 0),
    (5112, 6, NULL, 105, 1,  '#include <bits/stdc++.h>\nint main(){return 0;}', 'C++17', 'AC',  44, 5120,
     TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 5 DAY), '20:27:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 5 DAY), '20:27:00'), NOW(), 0),
    (5113, 6, NULL, 107, 2,  '#include <bits/stdc++.h>\nint main(){return 0;}', 'C++17', 'AC',  39, 4096,
     TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 6 DAY), '19:16:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 6 DAY), '19:16:00'), NOW(), 0),
    (5114, 6, NULL, 101, 5,  '#include <bits/stdc++.h>\nint main(){return 0;}', 'C++17', 'AC',  30, 4096,
     TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 6 DAY), '19:52:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 6 DAY), '19:52:00'), NOW(), 0),
    (5115, 6, NULL, 102, 4,  '#include <bits/stdc++.h>\nint main(){return 0;}', 'C++17', 'AC',  58, 6144,
     TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 7 DAY), '19:30:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 7 DAY), '19:30:00'), NOW(), 0),
    (5116, 6, NULL, 103, 9,  '#include <bits/stdc++.h>\nint main(){return 0;}', 'C++17', 'WA',  63, 5120,
     TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 7 DAY), '20:12:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 7 DAY), '20:12:00'), NOW(), 0),
    (5117, 1, NULL, 105, 7,  '#include <bits/stdc++.h>\nint main(){return 0;}', 'C++17', 'AC',  24, 4096,
     TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 2 DAY), '19:40:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 2 DAY), '19:40:00'), NOW(), 0),
    (5118, 1, NULL, 106, 10, '#include <bits/stdc++.h>\nint main(){return 0;}', 'C++17', 'RE',  0, 0,
     TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 1 DAY), '20:15:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 1 DAY), '20:15:00'), NOW(), 0),
    (5119, 6, NULL, 107, 3,  '#include <bits/stdc++.h>\nint main(){return 0;}', 'C++17', 'AC',  71, 6144,
     TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 5 DAY), '19:38:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 5 DAY), '19:38:00'), NOW(), 0),
    (5120, 6, NULL, 105, 10, 'public class Main { public static void main(String[] a) {} }', 'Java 17', 'AC', 88, 10240,
     TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 4 DAY), '20:22:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 4 DAY), '20:22:00'), NOW(), 0);

-- ==================== 补充演示数据（每种 10 条，INSERT IGNORE 可重复执行） ====================

-- ==================== 补充演示数据：会员（10 名） ====================
INSERT IGNORE INTO member (member_id, student_id, password, name, major, grade, phone, email, avatar, member_type, department, join_date, status, last_login_time, create_time, update_time, deleted) VALUES
    (201, '2026201', '$2a$10$J658/MW6soiM65MAWeWEKOW2R5b9qwGfSjsHzQIdVqcyH0jbsUUPa', '林子航', '计算机科学与技术', '2026级', '13800000200', '2026201@scuec.edu.cn', NULL, 'MEMBER', '算法部', '2026-09-10', 'ACTIVE', NULL, NOW(), NOW(), 0),
    (202, '2026202', '$2a$10$J658/MW6soiM65MAWeWEKOW2R5b9qwGfSjsHzQIdVqcyH0jbsUUPa', '苏念', '软件工程', '2026级', '13800000201', '2026202@scuec.edu.cn', NULL, 'MEMBER', '开发部', '2026-09-11', 'ACTIVE', NULL, NOW(), NOW(), 0),
    (203, '2026203', '$2a$10$J658/MW6soiM65MAWeWEKOW2R5b9qwGfSjsHzQIdVqcyH0jbsUUPa', '何嘉树', '人工智能', '2026级', '13800000202', '2026203@scuec.edu.cn', NULL, 'MEMBER', '宣传部', '2026-09-12', 'ACTIVE', NULL, NOW(), NOW(), 0),
    (204, '2026204', '$2a$10$J658/MW6soiM65MAWeWEKOW2R5b9qwGfSjsHzQIdVqcyH0jbsUUPa', '唐可欣', '数据科学与大数据技术', '2026级', '13800000203', '2026204@scuec.edu.cn', NULL, 'MEMBER', '组织部', '2026-09-13', 'ACTIVE', NULL, NOW(), NOW(), 0),
    (205, '2026205', '$2a$10$J658/MW6soiM65MAWeWEKOW2R5b9qwGfSjsHzQIdVqcyH0jbsUUPa', '梁宇轩', '计算机科学与技术', '2026级', '13800000204', '2026205@scuec.edu.cn', NULL, 'MEMBER', '算法部', '2026-09-14', 'ACTIVE', NULL, NOW(), NOW(), 0),
    (206, '2026206', '$2a$10$J658/MW6soiM65MAWeWEKOW2R5b9qwGfSjsHzQIdVqcyH0jbsUUPa', '许安然', '软件工程', '2026级', '13800000205', '2026206@scuec.edu.cn', NULL, 'MEMBER', '开发部', '2026-09-15', 'ACTIVE', NULL, NOW(), NOW(), 0),
    (207, '2026207', '$2a$10$J658/MW6soiM65MAWeWEKOW2R5b9qwGfSjsHzQIdVqcyH0jbsUUPa', '邓皓', '人工智能', '2026级', '13800000206', '2026207@scuec.edu.cn', NULL, 'MEMBER', '宣传部', '2026-09-16', 'ACTIVE', NULL, NOW(), NOW(), 0),
    (208, '2026208', '$2a$10$J658/MW6soiM65MAWeWEKOW2R5b9qwGfSjsHzQIdVqcyH0jbsUUPa', '谢雨桐', '数据科学与大数据技术', '2026级', '13800000207', '2026208@scuec.edu.cn', NULL, 'MEMBER', '组织部', '2026-09-17', 'ACTIVE', NULL, NOW(), NOW(), 0),
    (209, '2026209', '$2a$10$J658/MW6soiM65MAWeWEKOW2R5b9qwGfSjsHzQIdVqcyH0jbsUUPa', '罗一鸣', '计算机科学与技术', '2026级', '13800000208', '2026209@scuec.edu.cn', NULL, 'MEMBER', '算法部', '2026-09-18', 'ACTIVE', NULL, NOW(), NOW(), 0),
    (210, '2026210', '$2a$10$J658/MW6soiM65MAWeWEKOW2R5b9qwGfSjsHzQIdVqcyH0jbsUUPa', '沈佳怡', '软件工程', '2026级', '13800000209', '2026210@scuec.edu.cn', NULL, 'MEMBER', '开发部', '2026-09-19', 'ACTIVE', NULL, NOW(), NOW(), 0);
INSERT IGNORE INTO member_role (member_id, role_code) VALUES
    (201, 'MEMBER'),
    (202, 'MEMBER'),
    (203, 'MEMBER'),
    (204, 'MEMBER'),
    (205, 'MEMBER'),
    (206, 'MEMBER'),
    (207, 'MEMBER'),
    (208, 'MEMBER'),
    (209, 'MEMBER'),
    (210, 'MEMBER'),
    (201, 'TEAM_LEADER'),
    (202, 'TEAM_LEADER'),
    (203, 'TEAM_LEADER'),
    (204, 'TEAM_LEADER'),
    (205, 'TEAM_LEADER');

-- ==================== 补充演示数据：队伍（10 支） ====================
INSERT IGNORE INTO team (team_id, name, leader_id, contest_target, description, status, create_time, update_time, deleted) VALUES
    (201, '晨星队', 201, '2026 秋季校赛', '由 2026 级正式会员组建，主攻算法竞赛与校内选拔。', 'CONFIRMED', '2026-10-01 19:00:00', NOW(), 0),
    (202, '越界者', 202, '2026 XCPC 网络赛', '由 2026 级正式会员组建，主攻算法竞赛与校内选拔。', 'CONFIRMED', '2026-10-04 19:00:00', NOW(), 0),
    (203, '二进制旅人', 203, '2026 CCPC 选拔', '由 2026 级正式会员组建，主攻算法竞赛与校内选拔。', 'CONFIRMED', '2026-10-07 19:00:00', NOW(), 0),
    (204, '递归森林', 204, '2027 寒假集训选拔', '由 2026 级正式会员组建，主攻算法竞赛与校内选拔。', 'CONFIRMED', '2026-10-10 19:00:00', NOW(), 0),
    (205, '贪心小队', 205, '2026 秋季校赛', '由 2026 级正式会员组建，主攻算法竞赛与校内选拔。', 'CONFIRMED', '2026-10-13 19:00:00', NOW(), 0),
    (206, '图论远征队', 206, '2026 XCPC 网络赛', '由 2026 级正式会员组建，主攻算法竞赛与校内选拔。', 'CONFIRMED', '2026-10-16 19:00:00', NOW(), 0),
    (207, '哈希风暴', 207, '2026 CCPC 选拔', '由 2026 级正式会员组建，主攻算法竞赛与校内选拔。', 'CONFIRMED', '2026-10-19 19:00:00', NOW(), 0),
    (208, '剪枝者', 208, '2027 寒假集训选拔', '由 2026 级正式会员组建，主攻算法竞赛与校内选拔。', 'PENDING_CONFIRM', '2026-10-22 19:00:00', NOW(), 0),
    (209, '动态规划局', 209, '2026 秋季校赛', '由 2026 级正式会员组建，主攻算法竞赛与校内选拔。', 'PENDING_CONFIRM', '2026-10-25 19:00:00', NOW(), 0),
    (210, '终局裁定', 210, '2026 XCPC 网络赛', '由 2026 级正式会员组建，主攻算法竞赛与校内选拔。', 'PENDING_CONFIRM', '2026-10-28 19:00:00', NOW(), 0);
INSERT IGNORE INTO team_member (team_id, member_id, join_time) VALUES
    (201, 201, '2026-10-01 19:00:00'),
    (201, 202, '2026-10-01 19:00:00'),
    (201, 203, '2026-10-01 19:00:00'),
    (202, 202, '2026-10-04 19:00:00'),
    (202, 203, '2026-10-04 19:00:00'),
    (202, 204, '2026-10-04 19:00:00'),
    (203, 203, '2026-10-07 19:00:00'),
    (203, 204, '2026-10-07 19:00:00'),
    (203, 205, '2026-10-07 19:00:00'),
    (204, 204, '2026-10-10 19:00:00'),
    (204, 205, '2026-10-10 19:00:00'),
    (204, 206, '2026-10-10 19:00:00'),
    (205, 205, '2026-10-13 19:00:00'),
    (205, 206, '2026-10-13 19:00:00'),
    (205, 207, '2026-10-13 19:00:00'),
    (206, 206, '2026-10-16 19:00:00'),
    (206, 207, '2026-10-16 19:00:00'),
    (206, 208, '2026-10-16 19:00:00'),
    (207, 207, '2026-10-19 19:00:00'),
    (207, 208, '2026-10-19 19:00:00'),
    (207, 209, '2026-10-19 19:00:00'),
    (208, 208, '2026-10-22 19:00:00'),
    (208, 209, '2026-10-22 19:00:00'),
    (208, 210, '2026-10-22 19:00:00'),
    (209, 209, '2026-10-25 19:00:00'),
    (209, 210, '2026-10-25 19:00:00'),
    (209, 201, '2026-10-25 19:00:00'),
    (210, 210, '2026-10-28 19:00:00'),
    (210, 201, '2026-10-28 19:00:00'),
    (210, 202, '2026-10-28 19:00:00');

-- ==================== 补充演示数据：训练（10 场） ====================
INSERT IGNORE INTO training (training_id, name, type, description, start_time, end_time, leaderboard_visible, freeze_time, status, creator_id, create_time, update_time, deleted) VALUES
    (201, '基础算法巩固训练', 'SOLO', '面向正式会员的专题训练，包含 3-5 道配套题目。', '2026-11-05 19:00:00', '2026-11-05 22:00:00', 1, 30, 'ENDED', 6, NOW(), NOW(), 0),
    (202, '图论专题训练', 'TEAM', '面向正式会员的专题训练，包含 3-5 道配套题目。', '2026-11-09 19:00:00', '2026-11-09 22:00:00', 1, 30, 'ENDED', 6, NOW(), NOW(), 0),
    (203, '动态规划进阶', 'SOLO', '面向正式会员的专题训练，包含 3-5 道配套题目。', '2026-11-13 19:00:00', '2026-11-13 22:00:00', 1, 30, 'ENDED', 6, NOW(), NOW(), 0),
    (204, '数据结构综合训练', 'TEAM', '面向正式会员的专题训练，包含 3-5 道配套题目。', '2026-11-17 19:00:00', '2026-11-17 22:00:00', 1, 30, 'ENDED', 6, NOW(), NOW(), 0),
    (205, '字符串专题训练', 'SOLO', '面向正式会员的专题训练，包含 3-5 道配套题目。', '2026-11-21 19:00:00', '2026-11-21 22:00:00', 1, 30, 'PUBLISHED', 6, NOW(), NOW(), 0),
    (206, '数论与组合数学', 'TEAM', '面向正式会员的专题训练，包含 3-5 道配套题目。', '2026-11-25 19:00:00', '2026-11-25 22:00:00', 1, 30, 'PUBLISHED', 6, NOW(), NOW(), 0),
    (207, '搜索与剪枝训练', 'SOLO', '面向正式会员的专题训练，包含 3-5 道配套题目。', '2026-11-29 19:00:00', '2026-11-29 22:00:00', 1, 30, 'PUBLISHED', 6, NOW(), NOW(), 0),
    (208, '计算几何入门', 'TEAM', '面向正式会员的专题训练，包含 3-5 道配套题目。', '2026-12-03 19:00:00', '2026-12-03 22:00:00', 1, 30, 'PUBLISHED', 6, NOW(), NOW(), 0),
    (209, '模拟赛（一）', 'SOLO', '面向正式会员的专题训练，包含 3-5 道配套题目。', '2026-12-07 19:00:00', '2026-12-07 22:00:00', 1, 30, 'DRAFT', 6, NOW(), NOW(), 0),
    (210, '模拟赛（二）', 'TEAM', '面向正式会员的专题训练，包含 3-5 道配套题目。', '2026-12-11 19:00:00', '2026-12-11 22:00:00', 1, 30, 'DRAFT', 6, NOW(), NOW(), 0);
INSERT IGNORE INTO problem_link (target_type, target_id, problem_id, sort_order) VALUES
    ('TRAINING', 201, 301, 1),
    ('TRAINING', 202, 302, 2),
    ('TRAINING', 203, 303, 3),
    ('TRAINING', 204, 304, 4),
    ('TRAINING', 205, 305, 5);

-- ==================== 补充演示数据：赛事（10 场） ====================
INSERT IGNORE INTO contest (contest_id, name, category, start_time, end_time, registration_deadline, description, requirements, max_team_size, freeze_minutes, status, oj_contest_id, creator_id, create_time, update_time, deleted) VALUES
    (201, '2026 秋季校赛热身赛', 'SCHOLASTIC', '2026-10-10 13:00:00', '2026-10-10 18:00:00', '2026-10-07 13:00:00', '面向协会成员的算法竞赛，赛后将评选奖项并生成电子证书。', NULL, 3, 60, 'ENDED', NULL, 6, NOW(), NOW(), 0),
    (202, '2026 XCPC 校内选拔（二）', 'XCPC', '2026-10-17 13:00:00', '2026-10-17 18:00:00', '2026-10-14 13:00:00', '面向协会成员的算法竞赛，赛后将评选奖项并生成电子证书。', NULL, 3, 60, 'ENDED', NULL, 6, NOW(), NOW(), 0),
    (203, '2026 CCPC 校内模拟赛', 'SCHOLASTIC', '2026-10-24 13:00:00', '2026-10-24 18:00:00', '2026-10-21 13:00:00', '面向协会成员的算法竞赛，赛后将评选奖项并生成电子证书。', NULL, 3, 60, 'ENDED', NULL, 6, NOW(), NOW(), 0),
    (204, '2026 新生邀请赛', 'SCHOLASTIC', '2026-10-31 13:00:00', '2026-10-31 18:00:00', '2026-10-28 13:00:00', '面向协会成员的算法竞赛，赛后将评选奖项并生成电子证书。', NULL, 3, 60, 'ENDED', NULL, 6, NOW(), NOW(), 0),
    (205, '2026 XCPC 网络赛（三）', 'XCPC', '2026-11-19 19:00:00', '2026-11-20 00:00:00', '2026-11-16 19:00:00', '面向协会成员的算法竞赛，赛后将评选奖项并生成电子证书。', NULL, 3, 60, 'ONGOING', NULL, 6, NOW(), NOW(), 0),
    (206, '2026 冬季校赛', 'SCHOLASTIC', '2026-12-05 13:00:00', '2026-12-05 18:00:00', '2026-12-02 13:00:00', '面向协会成员的算法竞赛，赛后将评选奖项并生成电子证书。', NULL, 3, 60, 'REGISTERING', NULL, 6, NOW(), NOW(), 0),
    (207, '2027 XCPC 选拔赛', 'XCPC', '2026-12-12 13:00:00', '2026-12-12 18:00:00', '2026-12-09 13:00:00', '面向协会成员的算法竞赛，赛后将评选奖项并生成电子证书。', NULL, 3, 60, 'REGISTERING', NULL, 6, NOW(), NOW(), 0),
    (208, '2027 寒假训练赛', 'SCHOLASTIC', '2026-12-20 13:00:00', '2026-12-20 18:00:00', '2026-12-17 13:00:00', '面向协会成员的算法竞赛，赛后将评选奖项并生成电子证书。', NULL, 3, 60, 'ANNOUNCED', NULL, 6, NOW(), NOW(), 0),
    (209, '2027 春季校赛', 'SCHOLASTIC', '2027-01-10 13:00:00', '2027-01-10 18:00:00', '2027-01-07 13:00:00', '面向协会成员的算法竞赛，赛后将评选奖项并生成电子证书。', NULL, 3, 60, 'ANNOUNCED', NULL, 6, NOW(), NOW(), 0),
    (210, '2027 CCPC 预选赛', 'XCPC', '2027-03-15 13:00:00', '2027-03-15 18:00:00', '2027-03-12 13:00:00', '面向协会成员的算法竞赛，赛后将评选奖项并生成电子证书。', NULL, 3, 60, 'ANNOUNCED', NULL, 6, NOW(), NOW(), 0);
INSERT IGNORE INTO problem_link (target_type, target_id, problem_id, sort_order) VALUES
    ('CONTEST', 201, 306, 1),
    ('CONTEST', 202, 307, 2),
    ('CONTEST', 203, 308, 3),
    ('CONTEST', 204, 309, 4),
    ('CONTEST', 205, 310, 5);

-- ==================== 补充演示数据：集训（10 期） ====================
INSERT IGNORE INTO camp (camp_id, type, name, start_time, end_time, location, registration_deadline, description, requirements, status, creator_id, create_time, update_time, deleted) VALUES
    (201, 'WINTER', '2027 年寒假集训（一）', '2027-01-10 00:00:00', '2027-01-24 00:00:00', '计算机学院实验楼 A301', '2027-01-05 00:00:00', '集中训练与专题讲解，结营安排选拔赛。', NULL, 'ANNOUNCED', 6, NOW(), NOW(), 0),
    (202, 'SUMMER', '2026 年暑假集训（二）', '2026-07-10 00:00:00', '2026-07-24 00:00:00', '计算机学院实验楼 A302', '2026-07-05 00:00:00', '集中训练与专题讲解，结营安排选拔赛。', NULL, 'ENDED', 6, NOW(), NOW(), 0),
    (203, 'WINTER', '2027 年寒假集训（二）', '2027-02-01 00:00:00', '2027-02-15 00:00:00', '计算机学院实验楼 A303', '2027-01-27 00:00:00', '集中训练与专题讲解，结营安排选拔赛。', NULL, 'ANNOUNCED', 6, NOW(), NOW(), 0),
    (204, 'SUMMER', '2027 年暑假集训（一）', '2027-07-10 00:00:00', '2027-07-24 00:00:00', '计算机学院实验楼 A304', '2027-07-05 00:00:00', '集中训练与专题讲解，结营安排选拔赛。', NULL, 'ANNOUNCED', 6, NOW(), NOW(), 0),
    (205, 'WINTER', '2026 年冬季集训', '2026-12-20 00:00:00', '2027-01-03 00:00:00', '计算机学院实验楼 A305', '2026-12-15 00:00:00', '集中训练与专题讲解，结营安排选拔赛。', NULL, 'ANNOUNCED', 6, NOW(), NOW(), 0),
    (206, 'SUMMER', '2026 年暑假集训（三）', '2026-08-01 00:00:00', '2026-08-15 00:00:00', '计算机学院实验楼 A301', '2026-07-27 00:00:00', '集中训练与专题讲解，结营安排选拔赛。', NULL, 'ENDED', 6, NOW(), NOW(), 0),
    (207, 'WINTER', '2027 年寒假集训（三）', '2027-01-15 00:00:00', '2027-01-29 00:00:00', '计算机学院实验楼 A302', '2027-01-10 00:00:00', '集中训练与专题讲解，结营安排选拔赛。', NULL, 'ANNOUNCED', 6, NOW(), NOW(), 0),
    (208, 'SUMMER', '2027 年暑假集训（二）', '2027-08-01 00:00:00', '2027-08-15 00:00:00', '计算机学院实验楼 A303', '2027-07-27 00:00:00', '集中训练与专题讲解，结营安排选拔赛。', NULL, 'ANNOUNCED', 6, NOW(), NOW(), 0),
    (209, 'WINTER', '2026 年冬季集训（二）', '2026-12-25 00:00:00', '2027-01-08 00:00:00', '计算机学院实验楼 A304', '2026-12-20 00:00:00', '集中训练与专题讲解，结营安排选拔赛。', NULL, 'ANNOUNCED', 6, NOW(), NOW(), 0),
    (210, 'SUMMER', '2028 年暑假集训', '2028-07-10 00:00:00', '2028-07-24 00:00:00', '计算机学院实验楼 A305', '2028-07-05 00:00:00', '集中训练与专题讲解，结营安排选拔赛。', NULL, 'ANNOUNCED', 6, NOW(), NOW(), 0);
INSERT IGNORE INTO camp_task (task_id, camp_id, title, content, task_date, creator_id, create_time, update_time, deleted) VALUES
    (201, 201, 'Day 1 专题训练', '完成配套题目并提交题解。', '2027-01-10', 6, NOW(), NOW(), 0),
    (202, 201, 'Day 2 专题训练', '完成配套题目并提交题解。', '2027-01-11', 6, NOW(), NOW(), 0),
    (203, 201, 'Day 3 专题训练', '完成配套题目并提交题解。', '2027-01-12', 6, NOW(), NOW(), 0),
    (204, 201, 'Day 4 专题训练', '完成配套题目并提交题解。', '2027-01-13', 6, NOW(), NOW(), 0),
    (205, 201, 'Day 5 专题训练', '完成配套题目并提交题解。', '2027-01-14', 6, NOW(), NOW(), 0),
    (206, 201, 'Day 6 专题训练', '完成配套题目并提交题解。', '2027-01-15', 6, NOW(), NOW(), 0),
    (207, 201, 'Day 7 专题训练', '完成配套题目并提交题解。', '2027-01-16', 6, NOW(), NOW(), 0),
    (208, 201, 'Day 8 专题训练', '完成配套题目并提交题解。', '2027-01-17', 6, NOW(), NOW(), 0),
    (209, 201, 'Day 9 专题训练', '完成配套题目并提交题解。', '2027-01-18', 6, NOW(), NOW(), 0),
    (210, 201, 'Day 10 专题训练', '完成配套题目并提交题解。', '2027-01-19', 6, NOW(), NOW(), 0);

-- ==================== 补充演示数据：题库与题解（10 题） ====================
INSERT IGNORE INTO problem (problem_id, title, oj_problem_id, difficulty, time_limit, memory_limit, description, input_format, output_format, sample_input, sample_output, hint, visibility, creator_id, create_time, update_time, deleted) VALUES
    (301, '线段树区间加', '3001', 'EASY', 1000, 262144, '给定规模为 n 的数据，完成题目要求的计算并输出答案。', '第一行一个整数 n，接下来 n 行描述输入数据。', '输出一个整数表示答案。', '5
1 2 3 4 5', '15', '注意数据范围与边界情况。', 'PUBLIC', 6, NOW(), NOW(), 0),
    (302, '并查集连通块', '3002', 'EASY', 1000, 262144, '给定规模为 n 的数据，完成题目要求的计算并输出答案。', '第一行一个整数 n，接下来 n 行描述输入数据。', '输出一个整数表示答案。', '5
1 2 3 4 5', '15', '注意数据范围与边界情况。', 'PUBLIC', 6, NOW(), NOW(), 0),
    (303, '最短路计数', '3003', 'MEDIUM', 1000, 262144, '给定规模为 n 的数据，完成题目要求的计算并输出答案。', '第一行一个整数 n，接下来 n 行描述输入数据。', '输出一个整数表示答案。', '5
1 2 3 4 5', '15', '注意数据范围与边界情况。', 'PUBLIC', 6, NOW(), NOW(), 0),
    (304, '字符串哈希', '3004', 'MEDIUM', 1000, 262144, '给定规模为 n 的数据，完成题目要求的计算并输出答案。', '第一行一个整数 n，接下来 n 行描述输入数据。', '输出一个整数表示答案。', '5
1 2 3 4 5', '15', '注意数据范围与边界情况。', 'PUBLIC', 6, NOW(), NOW(), 0),
    (305, '二分答案', '3005', 'HARD', 1000, 262144, '给定规模为 n 的数据，完成题目要求的计算并输出答案。', '第一行一个整数 n，接下来 n 行描述输入数据。', '输出一个整数表示答案。', '5
1 2 3 4 5', '15', '注意数据范围与边界情况。', 'TRAINING_PRIVATE', 6, NOW(), NOW(), 0),
    (306, '贪心调度', '3006', 'MEDIUM', 1000, 262144, '给定规模为 n 的数据，完成题目要求的计算并输出答案。', '第一行一个整数 n，接下来 n 行描述输入数据。', '输出一个整数表示答案。', '5
1 2 3 4 5', '15', '注意数据范围与边界情况。', 'PUBLIC', 6, NOW(), NOW(), 0),
    (307, '树形 DP', '3007', 'HARD', 1000, 262144, '给定规模为 n 的数据，完成题目要求的计算并输出答案。', '第一行一个整数 n，接下来 n 行描述输入数据。', '输出一个整数表示答案。', '5
1 2 3 4 5', '15', '注意数据范围与边界情况。', 'PUBLIC', 6, NOW(), NOW(), 0),
    (308, '计算几何入门', '3008', 'HARD', 1000, 262144, '给定规模为 n 的数据，完成题目要求的计算并输出答案。', '第一行一个整数 n，接下来 n 行描述输入数据。', '输出一个整数表示答案。', '5
1 2 3 4 5', '15', '注意数据范围与边界情况。', 'PUBLIC', 6, NOW(), NOW(), 0),
    (309, '搜索剪枝', '3009', 'MEDIUM', 1000, 262144, '给定规模为 n 的数据，完成题目要求的计算并输出答案。', '第一行一个整数 n，接下来 n 行描述输入数据。', '输出一个整数表示答案。', '5
1 2 3 4 5', '15', '注意数据范围与边界情况。', 'PUBLIC', 6, NOW(), NOW(), 0),
    (310, '数论综合', '3010', 'HARD', 1000, 262144, '给定规模为 n 的数据，完成题目要求的计算并输出答案。', '第一行一个整数 n，接下来 n 行描述输入数据。', '输出一个整数表示答案。', '5
1 2 3 4 5', '15', '注意数据范围与边界情况。', 'TRAINING_PRIVATE', 6, NOW(), NOW(), 0);
INSERT IGNORE INTO tag (tag_id, name, category, parent_id, sort_order, create_time, update_time, deleted) VALUES
    (201, '线段树', '算法', NULL, 201, NOW(), NOW(), 0),
    (202, '并查集', '算法', NULL, 202, NOW(), NOW(), 0),
    (203, '最短路', '算法', NULL, 203, NOW(), NOW(), 0),
    (204, '字符串哈希', '算法', NULL, 204, NOW(), NOW(), 0),
    (205, '二分答案', '算法', NULL, 205, NOW(), NOW(), 0),
    (206, '贪心', '算法', NULL, 206, NOW(), NOW(), 0),
    (207, '树形 DP', '算法', NULL, 207, NOW(), NOW(), 0),
    (208, '计算几何', '算法', NULL, 208, NOW(), NOW(), 0),
    (209, '搜索剪枝', '算法', NULL, 209, NOW(), NOW(), 0),
    (210, '数论', '算法', NULL, 210, NOW(), NOW(), 0);
INSERT IGNORE INTO problem_tag (problem_id, tag_id) VALUES
    (301, 201),
    (302, 202),
    (303, 203),
    (304, 204),
    (305, 205),
    (306, 206),
    (307, 207),
    (308, 208),
    (309, 209),
    (310, 210);
INSERT IGNORE INTO solution (solution_id, problem_id, content, reference_code, create_time, update_time, deleted) VALUES
    (201, 301, '## 思路
使用标准算法求解，注意边界与复杂度。', '#include <bits/stdc++.h>
int main(){return 0;}', NOW(), NOW(), 0),
    (202, 302, '## 思路
使用标准算法求解，注意边界与复杂度。', '#include <bits/stdc++.h>
int main(){return 0;}', NOW(), NOW(), 0),
    (203, 303, '## 思路
使用标准算法求解，注意边界与复杂度。', '#include <bits/stdc++.h>
int main(){return 0;}', NOW(), NOW(), 0),
    (204, 304, '## 思路
使用标准算法求解，注意边界与复杂度。', '#include <bits/stdc++.h>
int main(){return 0;}', NOW(), NOW(), 0),
    (205, 305, '## 思路
使用标准算法求解，注意边界与复杂度。', '#include <bits/stdc++.h>
int main(){return 0;}', NOW(), NOW(), 0),
    (206, 306, '## 思路
使用标准算法求解，注意边界与复杂度。', '#include <bits/stdc++.h>
int main(){return 0;}', NOW(), NOW(), 0),
    (207, 307, '## 思路
使用标准算法求解，注意边界与复杂度。', '#include <bits/stdc++.h>
int main(){return 0;}', NOW(), NOW(), 0),
    (208, 308, '## 思路
使用标准算法求解，注意边界与复杂度。', '#include <bits/stdc++.h>
int main(){return 0;}', NOW(), NOW(), 0),
    (209, 309, '## 思路
使用标准算法求解，注意边界与复杂度。', '#include <bits/stdc++.h>
int main(){return 0;}', NOW(), NOW(), 0),
    (210, 310, '## 思路
使用标准算法求解，注意边界与复杂度。', '#include <bits/stdc++.h>
int main(){return 0;}', NOW(), NOW(), 0);

-- ==================== 补充演示数据：提交记录（10 条） ====================
INSERT IGNORE INTO submission (submit_id, training_id, contest_id, problem_id, submitter_id, code, language, result, time_used, memory_used, oj_submit_id, submit_time, create_time, update_time, deleted) VALUES
    (6001, 201, NULL, 301, 206, 'print(1)', 'Python 3', 'AC', 30, 4096, NULL, '2026-11-19 19:05:00', '2026-11-19 19:05:00', NOW(), 0),
    (6002, 202, NULL, 302, 207, '#include <bits/stdc++.h> int main(){return 0;}', 'C++17', 'WA', 37, 4608, NULL, '2026-11-18 20:06:00', '2026-11-18 20:06:00', NOW(), 0),
    (6003, 201, NULL, 301, 208, '#include <bits/stdc++.h> int main(){return 0;}', 'C++17', 'AC', 44, 5120, NULL, '2026-11-17 21:07:00', '2026-11-17 21:07:00', NOW(), 0),
    (6004, 202, NULL, 302, 209, 'print(1)', 'Python 3', 'TLE', 51, 5632, NULL, '2026-11-16 19:08:00', '2026-11-16 19:08:00', NOW(), 0),
    (6005, 201, NULL, 301, 210, '#include <bits/stdc++.h> int main(){return 0;}', 'C++17', 'AC', 58, 6144, NULL, '2026-11-15 20:09:00', '2026-11-15 20:09:00', NOW(), 0),
    (6006, NULL, 205, 307, 201, '#include <bits/stdc++.h> int main(){return 0;}', 'C++17', 'AC', 65, 6656, NULL, '2026-11-18 21:10:00', '2026-11-18 21:10:00', NOW(), 0),
    (6007, NULL, 205, 306, 202, 'print(1)', 'Python 3', 'WA', 72, 7168, NULL, '2026-11-17 19:11:00', '2026-11-17 19:11:00', NOW(), 0),
    (6008, NULL, 205, 307, 203, '#include <bits/stdc++.h> int main(){return 0;}', 'C++17', 'AC', 79, 7680, NULL, '2026-11-16 20:12:00', '2026-11-16 20:12:00', NOW(), 0),
    (6009, NULL, 205, 306, 204, '#include <bits/stdc++.h> int main(){return 0;}', 'C++17', 'RE', 86, 8192, NULL, '2026-11-15 21:13:00', '2026-11-15 21:13:00', NOW(), 0),
    (6010, NULL, 205, 307, 205, 'print(1)', 'Python 3', 'AC', 93, 8704, NULL, '2026-11-14 19:14:00', '2026-11-14 19:14:00', NOW(), 0);

-- ==================== 补充演示数据：奖惩（各 10 条） ====================
INSERT IGNORE INTO reward (reward_id, member_id, title, category, level, amount, description, grant_date, issuer_name, creator_id, create_time, update_time, deleted) VALUES
    (201, 201, '校赛一等奖', 'COMPETITION', 'SCHOOL', 200.00, '在训练与竞赛中表现突出，予以表彰。', '2026-09-20', 'ICPC 协会', 6, NOW(), NOW(), 0),
    (202, 202, '校赛二等奖', 'COMPETITION', 'SCHOOL', 300.00, '在训练与竞赛中表现突出，予以表彰。', '2026-09-25', 'ICPC 协会', 6, NOW(), NOW(), 0),
    (203, 203, '省级竞赛三等奖', 'COMPETITION', 'PROVINCIAL', 400.00, '在训练与竞赛中表现突出，予以表彰。', '2026-09-30', 'ICPC 协会', 6, NOW(), NOW(), 0),
    (204, 204, '优秀训练个人', 'TRAINING', 'ASSOCIATION', 500.00, '在训练与竞赛中表现突出，予以表彰。', '2026-10-05', 'ICPC 协会', 6, NOW(), NOW(), 0),
    (205, 205, '月度进步奖', 'TRAINING', 'ASSOCIATION', 600.00, '在训练与竞赛中表现突出，予以表彰。', '2026-10-10', 'ICPC 协会', 6, NOW(), NOW(), 0),
    (206, 206, '优秀志愿者', 'SERVICE', 'ASSOCIATION', 700.00, '在训练与竞赛中表现突出，予以表彰。', '2026-10-15', 'ICPC 协会', 6, NOW(), NOW(), 0),
    (207, 207, '学术创新奖', 'ACADEMIC', 'SCHOOL', 800.00, '在训练与竞赛中表现突出，予以表彰。', '2026-10-20', 'ICPC 协会', 6, NOW(), NOW(), 0),
    (208, 208, '最佳团队奖', 'COMPETITION', 'SCHOOL', 900.00, '在训练与竞赛中表现突出，予以表彰。', '2026-10-25', 'ICPC 协会', 6, NOW(), NOW(), 0),
    (209, 209, '训练全勤奖', 'TRAINING', 'ASSOCIATION', 1000.00, '在训练与竞赛中表现突出，予以表彰。', '2026-10-30', 'ICPC 协会', 6, NOW(), NOW(), 0),
    (210, 210, '年度新星', 'OTHER', 'ASSOCIATION', 1100.00, '在训练与竞赛中表现突出，予以表彰。', '2026-11-04', 'ICPC 协会', 6, NOW(), NOW(), 0);
INSERT IGNORE INTO punishment (punishment_id, member_id, type, reason, description, effective_date, expire_date, issuer_name, creator_id, create_time, update_time, deleted) VALUES
    (201, 201, 'WARNING', '未按规定完成训练任务（示例 1）', '经提醒后仍未改进，给予相应处分。', '2026-10-01', '2026-12-30', 'ICPC 协会组织部', 6, NOW(), NOW(), 0),
    (202, 202, 'WARNING', '未按规定完成训练任务（示例 2）', '经提醒后仍未改进，给予相应处分。', '2026-10-05', '2027-01-03', 'ICPC 协会组织部', 6, NOW(), NOW(), 0),
    (203, 203, 'SERIOUS_WARNING', '未按规定完成训练任务（示例 3）', '经提醒后仍未改进，给予相应处分。', '2026-10-09', '2027-01-07', 'ICPC 协会组织部', 6, NOW(), NOW(), 0),
    (204, 204, 'WARNING', '未按规定完成训练任务（示例 4）', '经提醒后仍未改进，给予相应处分。', '2026-10-13', '2027-01-11', 'ICPC 协会组织部', 6, NOW(), NOW(), 0),
    (205, 205, 'SERIOUS_WARNING', '未按规定完成训练任务（示例 5）', '经提醒后仍未改进，给予相应处分。', '2026-10-17', '2027-01-15', 'ICPC 协会组织部', 6, NOW(), NOW(), 0),
    (206, 206, 'WARNING', '未按规定完成训练任务（示例 6）', '经提醒后仍未改进，给予相应处分。', '2026-10-21', '2027-01-19', 'ICPC 协会组织部', 6, NOW(), NOW(), 0),
    (207, 207, 'WARNING', '未按规定完成训练任务（示例 7）', '经提醒后仍未改进，给予相应处分。', '2026-10-25', '2027-01-23', 'ICPC 协会组织部', 6, NOW(), NOW(), 0),
    (208, 208, 'SERIOUS_WARNING', '未按规定完成训练任务（示例 8）', '经提醒后仍未改进，给予相应处分。', '2026-10-29', '2027-01-27', 'ICPC 协会组织部', 6, NOW(), NOW(), 0),
    (209, 209, 'WARNING', '未按规定完成训练任务（示例 9）', '经提醒后仍未改进，给予相应处分。', '2026-11-02', '2027-01-31', 'ICPC 协会组织部', 6, NOW(), NOW(), 0),
    (210, 210, 'WARNING', '未按规定完成训练任务（示例 10）', '经提醒后仍未改进，给予相应处分。', '2026-11-06', '2027-02-04', 'ICPC 协会组织部', 6, NOW(), NOW(), 0);

-- ==================== 补充演示数据：考核与成绩（各 10 条） ====================
INSERT IGNORE INTO assessment (assessment_id, name, type, description, start_time, end_time, pass_score, status, creator_id, create_time, update_time, deleted) VALUES
    (201, '2026 秋季观察期考核（二）', 'OBSERVATION', '检验阶段学习成果，包含机试与综合评定。', '2026-11-25 09:00:00', '2026-12-05 09:00:00', 60, 'PUBLISHED', 6, NOW(), NOW(), 0),
    (202, '2026 秋季正式会员考核（二）', 'REGULAR', '检验阶段学习成果，包含机试与综合评定。', '2026-11-28 09:00:00', '2026-12-08 09:00:00', 70, 'PUBLISHED', 6, NOW(), NOW(), 0),
    (203, '2026 算法能力测评', 'REGULAR', '检验阶段学习成果，包含机试与综合评定。', '2026-12-01 09:00:00', '2026-12-11 09:00:00', 80, 'PUBLISHED', 6, NOW(), NOW(), 0),
    (204, '2026 工程能力测评', 'REGULAR', '检验阶段学习成果，包含机试与综合评定。', '2026-12-04 09:00:00', '2026-12-14 09:00:00', 60, 'PUBLISHED', 6, NOW(), NOW(), 0),
    (205, '2026 团队协作考核', 'REGULAR', '检验阶段学习成果，包含机试与综合评定。', '2026-12-07 09:00:00', '2026-12-17 09:00:00', 70, 'PUBLISHED', 6, NOW(), NOW(), 0),
    (206, '2027 春季晋升考核', 'PROMOTION', '检验阶段学习成果，包含机试与综合评定。', '2026-12-10 09:00:00', '2026-12-20 09:00:00', 80, 'DRAFT', 6, NOW(), NOW(), 0),
    (207, '2026 数据结构专项考核', 'REGULAR', '检验阶段学习成果，包含机试与综合评定。', '2026-12-13 09:00:00', '2026-12-23 09:00:00', 60, 'DRAFT', 6, NOW(), NOW(), 0),
    (208, '2026 图论专项考核', 'REGULAR', '检验阶段学习成果，包含机试与综合评定。', '2026-12-16 09:00:00', '2026-12-26 09:00:00', 70, 'DRAFT', 6, NOW(), NOW(), 0),
    (209, '2026 模拟赛综合考核', 'REGULAR', '检验阶段学习成果，包含机试与综合评定。', '2026-12-19 09:00:00', '2026-12-29 09:00:00', 80, 'DRAFT', 6, NOW(), NOW(), 0),
    (210, '2026 年度总结考核', 'OBSERVATION', '检验阶段学习成果，包含机试与综合评定。', '2026-12-22 09:00:00', '2027-01-01 09:00:00', 60, 'DRAFT', 6, NOW(), NOW(), 0);
INSERT IGNORE INTO assessment_result (result_id, assessment_id, member_id, score, remark, audit_status, audit_remark, auditor_id, audit_time, create_time, update_time, deleted) VALUES
    (201, 201, 201, 55.00, '阶段考核成绩 55 分', 'APPROVED', '成绩已核对', 6, '2026-12-06 10:00:00', NOW(), NOW(), 0),
    (202, 201, 202, 59.00, '阶段考核成绩 59 分', 'APPROVED', '成绩已核对', 6, '2026-12-06 10:00:00', NOW(), NOW(), 0),
    (203, 202, 203, 63.00, '阶段考核成绩 63 分', 'APPROVED', '成绩已核对', 6, '2026-12-06 10:00:00', NOW(), NOW(), 0),
    (204, 202, 204, 67.00, '阶段考核成绩 67 分', 'APPROVED', '成绩已核对', 6, '2026-12-06 10:00:00', NOW(), NOW(), 0),
    (205, 203, 205, 71.00, '阶段考核成绩 71 分', 'APPROVED', '成绩已核对', 6, '2026-12-06 10:00:00', NOW(), NOW(), 0),
    (206, 203, 206, 75.00, '阶段考核成绩 75 分', 'PENDING', NULL, NULL, NULL, NOW(), NOW(), 0),
    (207, 204, 207, 79.00, '阶段考核成绩 79 分', 'PENDING', NULL, NULL, NULL, NOW(), NOW(), 0),
    (208, 204, 208, 83.00, '阶段考核成绩 83 分', 'PENDING', NULL, NULL, NULL, NOW(), NOW(), 0),
    (209, 205, 209, 87.00, '阶段考核成绩 87 分', 'REJECTED', '材料需补充', 6, '2026-12-06 10:00:00', NOW(), NOW(), 0),
    (210, 205, 210, 91.00, '阶段考核成绩 91 分', 'REJECTED', '材料需补充', 6, '2026-12-06 10:00:00', NOW(), NOW(), 0);

-- ==================== 补充演示数据：入会申请（10 条） ====================
INSERT IGNORE INTO application (apply_id, type, target_id, applicant_id, team_id, apply_data, status, audit_remark, auditor_id, apply_time, audit_time, create_time, update_time, deleted) VALUES
    (201, 'JOIN_MEMBER', NULL, NULL, NULL, '{"name": "顾星河", "studentId": "2026301", "major": "计算机科学与技术", "grade": "2026级", "phone": "13900000300"}', 'PENDING', NULL, NULL, '2026-11-10 10:00:00', NULL, NOW(), NOW(), 0),
    (202, 'JOIN_MEMBER', NULL, NULL, NULL, '{"name": "温以宁", "studentId": "2026302", "major": "计算机科学与技术", "grade": "2026级", "phone": "13900000301"}', 'PENDING', NULL, NULL, '2026-11-11 10:00:00', NULL, NOW(), NOW(), 0),
    (203, 'JOIN_MEMBER', NULL, NULL, NULL, '{"name": "程亦辰", "studentId": "2026303", "major": "计算机科学与技术", "grade": "2026级", "phone": "13900000302"}', 'PENDING', NULL, NULL, '2026-11-12 10:00:00', NULL, NOW(), NOW(), 0),
    (204, 'JOIN_MEMBER', NULL, NULL, NULL, '{"name": "白若溪", "studentId": "2026304", "major": "计算机科学与技术", "grade": "2026级", "phone": "13900000303"}', 'PENDING', NULL, NULL, '2026-11-13 10:00:00', NULL, NOW(), NOW(), 0),
    (205, 'JOIN_MEMBER', NULL, NULL, NULL, '{"name": "江临川", "studentId": "2026305", "major": "计算机科学与技术", "grade": "2026级", "phone": "13900000304"}', 'PENDING', NULL, NULL, '2026-11-14 10:00:00', NULL, NOW(), NOW(), 0),
    (206, 'JOIN_MEMBER', NULL, NULL, NULL, '{"name": "夏知微", "studentId": "2026306", "major": "计算机科学与技术", "grade": "2026级", "phone": "13900000305"}', 'PENDING', NULL, NULL, '2026-11-15 10:00:00', NULL, NOW(), NOW(), 0),
    (207, 'JOIN_MEMBER', NULL, NULL, NULL, '{"name": "陆时安", "studentId": "2026307", "major": "计算机科学与技术", "grade": "2026级", "phone": "13900000306"}', 'PENDING', NULL, NULL, '2026-11-16 10:00:00', NULL, NOW(), NOW(), 0),
    (208, 'JOIN_MEMBER', NULL, NULL, NULL, '{"name": "叶清和", "studentId": "2026308", "major": "计算机科学与技术", "grade": "2026级", "phone": "13900000307"}', 'PENDING', NULL, NULL, '2026-11-17 10:00:00', NULL, NOW(), NOW(), 0),
    (209, 'JOIN_MEMBER', NULL, NULL, NULL, '{"name": "秦予安", "studentId": "2026309", "major": "计算机科学与技术", "grade": "2026级", "phone": "13900000308"}', 'REJECTED', '材料不完整，请补充后重新提交', 6, '2026-11-18 10:00:00', '2026-11-18 10:00:00', NOW(), NOW(), 0),
    (210, 'JOIN_MEMBER', NULL, NULL, NULL, '{"name": "宋知夏", "studentId": "2026310", "major": "计算机科学与技术", "grade": "2026级", "phone": "13900000309"}', 'REJECTED', '材料不完整，请补充后重新提交', 6, '2026-11-19 10:00:00', '2026-11-18 10:00:00', NOW(), NOW(), 0);

-- ==================== 补充演示数据：考勤（10 条） ====================
INSERT IGNORE INTO attendance (record_id, camp_id, member_id, check_in_date, check_in_time, status, remark, create_time, update_time, deleted) VALUES
    (201, 201, 201, '2027-01-10', '2027-01-10 08:50:00', 'PRESENT', NULL, NOW(), NOW(), 0),
    (202, 201, 202, '2027-01-10', '2027-01-10 08:51:00', 'PRESENT', NULL, NOW(), NOW(), 0),
    (203, 201, 203, '2027-01-10', '2027-01-10 08:52:00', 'PRESENT', NULL, NOW(), NOW(), 0),
    (204, 201, 204, '2027-01-10', '2027-01-10 08:53:00', 'PRESENT', NULL, NOW(), NOW(), 0),
    (205, 201, 205, '2027-01-10', NULL, 'ABSENT', '未签到', NOW(), NOW(), 0),
    (206, 201, 206, '2027-01-10', '2027-01-10 08:55:00', 'PRESENT', NULL, NOW(), NOW(), 0),
    (207, 201, 207, '2027-01-10', '2027-01-10 08:56:00', 'PRESENT', NULL, NOW(), NOW(), 0),
    (208, 201, 208, '2027-01-10', '2027-01-10 08:57:00', 'PRESENT', NULL, NOW(), NOW(), 0),
    (209, 201, 209, '2027-01-10', '2027-01-10 08:58:00', 'PRESENT', NULL, NOW(), NOW(), 0),
    (210, 201, 210, '2027-01-10', NULL, 'ABSENT', '未签到', NOW(), NOW(), 0);

-- ==================== 补充演示数据：MBTI 结果（10 条） ====================
INSERT IGNORE INTO mbti_result (result_id, member_id, type, score_ei, score_sn, score_tf, score_jp, report, test_time, create_time, update_time, deleted) VALUES
    (201, 201, 'INTJ', 28, 22, 74, 82, '林子航 的测评结果为 INTJ（建筑师）。擅长整体规划与结构化思考，适合承担训练计划制定与知识体系搭建的工作。', '2026-10-20 19:00:00', NOW(), NOW(), 0),
    (202, 202, 'ENFP', 72, 68, 38, 45, '苏念 的测评结果为 ENFP（竞选者）。思维活跃、善于沟通，适合在队伍中承担破题与氛围带动的工作。', '2026-10-22 19:00:00', NOW(), NOW(), 0),
    (203, 203, 'ISTJ', 35, 25, 70, 78, '何嘉树 的测评结果为 ISTJ（物流师）。踏实严谨、执行力强，适合负责训练记录与赛前检查。', '2026-10-24 19:00:00', NOW(), NOW(), 0),
    (204, 204, 'ESFJ', 78, 32, 42, 72, '唐可欣 的测评结果为 ESFJ（执政官）。乐于协作、关注团队感受，适合承担队内协调与后勤保障。', '2026-10-26 19:00:00', NOW(), NOW(), 0),
    (205, 205, 'INTP', 30, 75, 82, 40, '梁宇轩 的测评结果为 INTP（逻辑学家）。善于抽象建模与推演，适合研究复杂算法与数学问题。', '2026-10-28 19:00:00', NOW(), NOW(), 0),
    (206, 206, 'ENTJ', 76, 62, 80, 84, '许安然 的测评结果为 ENTJ（指挥官）。目标导向、决策果断，适合担任队长与赛事统筹。', '2026-10-30 19:00:00', NOW(), NOW(), 0),
    (207, 207, 'ISFP', 40, 35, 45, 30, '邓皓 的测评结果为 ISFP（探险家）。注重体验与细节，适合在实现与调试环节发挥优势。', '2026-11-01 19:00:00', NOW(), NOW(), 0),
    (208, 208, 'ESTJ', 70, 28, 75, 80, '谢雨桐 的测评结果为 ESTJ（总经理）。组织能力强、重视规则，适合负责训练纪律与进度管理。', '2026-11-03 19:00:00', NOW(), NOW(), 0),
    (209, 209, 'INFJ', 32, 70, 40, 76, '罗一鸣 的测评结果为 INFJ（提倡者）。洞察力强、善于规划，适合承担长期培养与知识沉淀。', '2026-11-05 19:00:00', NOW(), NOW(), 0),
    (210, 210, 'ENTP', 80, 72, 68, 38, '沈佳怡 的测评结果为 ENTP（辩论家）。思维灵活、乐于尝试新解法，适合在队伍中担任破题角色。', '2026-11-07 19:00:00', NOW(), NOW(), 0);

-- ==================== 补充演示数据：赛事成绩（10 条） ====================
INSERT IGNORE INTO contest_result (result_id, contest_id, team_id, member_id, rank_no, solved_count, penalty, score, award, remark, create_time, update_time, deleted) VALUES
    (201, 201, 201, NULL, 1, 7, 600, 7.00, '一等奖', '队伍整体表现稳定', NOW(), NOW(), 0),
    (202, 201, 202, NULL, 2, 6, 620, 6.00, '二等奖', '队伍整体表现稳定', NOW(), NOW(), 0),
    (203, 202, 203, NULL, 1, 7, 640, 7.00, '一等奖', '队伍整体表现稳定', NOW(), NOW(), 0),
    (204, 202, 204, NULL, 2, 6, 660, 6.00, '二等奖', '队伍整体表现稳定', NOW(), NOW(), 0),
    (205, 203, 205, NULL, 1, 7, 680, 7.00, '一等奖', '队伍整体表现稳定', NOW(), NOW(), 0),
    (206, 203, 206, NULL, 2, 6, 700, 6.00, '二等奖', '队伍整体表现稳定', NOW(), NOW(), 0),
    (207, 204, 207, NULL, 1, 7, 720, 7.00, '一等奖', '队伍整体表现稳定', NOW(), NOW(), 0),
    (208, 204, 208, NULL, 2, 6, 740, 6.00, '二等奖', '队伍整体表现稳定', NOW(), NOW(), 0),
    (209, 205, 209, NULL, 1, 7, 760, 7.00, '一等奖', '队伍整体表现稳定', NOW(), NOW(), 0),
    (210, 205, 210, NULL, 2, 6, 780, 6.00, '二等奖', '队伍整体表现稳定', NOW(), NOW(), 0);

-- ==================== 补充演示数据：操作日志（10 条） ====================
INSERT IGNORE INTO operation_log (log_id, member_id, username, method, path, action, ip, status, duration, create_time) VALUES
    (201, 6, '徐清越', 'POST', '/api/v1/trainings', '创建训练', '127.0.0.1', 200, 25, '2026-11-10 09:30:00'),
    (202, 5, '赵子轩', 'PUT', '/api/v1/trainings/1', '修改训练', '127.0.0.1', 200, 28, '2026-11-11 10:30:00'),
    (203, 6, '徐清越', 'POST', '/api/v1/contests', '创建赛事', '127.0.0.1', 200, 31, '2026-11-12 11:30:00'),
    (204, 5, '赵子轩', 'POST', '/api/v1/camps', '创建集训', '127.0.0.1', 200, 34, '2026-11-13 12:30:00'),
    (205, 6, '徐清越', 'POST', '/api/v1/problems', '新增题目', '127.0.0.1', 200, 37, '2026-11-14 13:30:00'),
    (206, 5, '赵子轩', 'POST', '/api/v1/members/approvals/201/audit', '审批会员申请', '127.0.0.1', 200, 40, '2026-11-15 14:30:00'),
    (207, 6, '徐清越', 'POST', '/api/v1/contests/1/certificates', '生成赛事证书', '127.0.0.1', 200, 43, '2026-11-16 15:30:00'),
    (208, 5, '赵子轩', 'GET', '/api/v1/statistics/training/summary', '查看训练统计', '127.0.0.1', 200, 46, '2026-11-17 16:30:00'),
    (209, 6, '徐清越', 'POST', '/api/v1/files/upload', '上传文件', '127.0.0.1', 500, 49, '2026-11-18 17:30:00'),
    (210, 5, '赵子轩', 'DELETE', '/api/v1/problems/305', '删除题目', '127.0.0.1', 200, 52, '2026-11-19 18:30:00');

-- ==================== 补充演示数据：文件记录（10 条） ====================
INSERT IGNORE INTO file_record (file_id, filename, stored_name, url, size, content_type, uploader_id, create_time, update_time, deleted) VALUES
    (201, '2026 秋季训练计划.pdf', 'demo-extra-01.pdf', '/uploads/demo/demo-extra-01.pdf', 186320, 'application/pdf', 6, '2026-10-15 10:00:00', NOW(), 0),
    (202, '校赛报名表.xlsx', 'demo-extra-02.xlsx', '/uploads/demo/demo-extra-02.xlsx', 48210, 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', 6, '2026-10-18 10:00:00', NOW(), 0),
    (203, '寒假集训签到表.xlsx', 'demo-extra-03.xlsx', '/uploads/demo/demo-extra-03.xlsx', 53120, 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', 6, '2026-10-21 10:00:00', NOW(), 0),
    (204, '图论专题题解.md', 'demo-extra-04.md', '/uploads/demo/demo-extra-04.md', 12800, 'text/markdown', 6, '2026-10-24 10:00:00', NOW(), 0),
    (205, '赛事获奖名单.pdf', 'demo-extra-05.pdf', '/uploads/demo/demo-extra-05.pdf', 220400, 'application/pdf', 6, '2026-10-27 10:00:00', NOW(), 0),
    (206, '会员信息导入模板.xlsx', 'demo-extra-06.xlsx', '/uploads/demo/demo-extra-06.xlsx', 30210, 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', 6, '2026-10-30 10:00:00', NOW(), 0),
    (207, '训练成绩汇总.csv', 'demo-extra-07.csv', '/uploads/demo/demo-extra-07.csv', 9600, 'text/csv', 6, '2026-11-02 10:00:00', NOW(), 0),
    (208, '集训安全须知.pdf', 'demo-extra-08.pdf', '/uploads/demo/demo-extra-08.pdf', 158400, 'application/pdf', 6, '2026-11-05 10:00:00', NOW(), 0),
    (209, '题目数据包.zip', 'demo-extra-09.zip', '/uploads/demo/demo-extra-09.zip', 1048576, 'application/zip', 6, '2026-11-08 10:00:00', NOW(), 0),
    (210, '协会章程（修订版）.pdf', 'demo-extra-10.pdf', '/uploads/demo/demo-extra-10.pdf', 332800, 'application/pdf', 6, '2026-11-11 10:00:00', NOW(), 0);

