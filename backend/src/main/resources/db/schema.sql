-- ICPC 协会管理系统数据库初始化脚本（MySQL 8.0）
-- 文档编号：ICPC-PLAN-V1.0 / 数据库设计 M1 交付物
CREATE DATABASE IF NOT EXISTS icpc_system
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;
USE icpc_system;

-- 统一时区，保证脚本里的 NOW()/CURDATE() 与应用（Asia/Shanghai）一致
SET time_zone = '+08:00';

-- 显式声明连接字符集，避免 mysql 客户端默认 latin1 导致中文示例数据乱码
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

-- ==================== 角色与会员 ====================
CREATE TABLE IF NOT EXISTS sys_role (
    role_code   VARCHAR(32)  NOT NULL COMMENT '角色编码',
    role_name   VARCHAR(50)  NOT NULL COMMENT '角色名称',
    description VARCHAR(255) NULL,
    PRIMARY KEY (role_code)
) ENGINE = InnoDB COMMENT = '角色字典';

CREATE TABLE IF NOT EXISTS member (
    member_id       BIGINT       NOT NULL AUTO_INCREMENT,
    student_id      VARCHAR(32)  NOT NULL COMMENT '学号',
    password        VARCHAR(100) NOT NULL COMMENT 'BCrypt 密码',
    name            VARCHAR(50)  NOT NULL,
    major           VARCHAR(100) NULL,
    grade           VARCHAR(20)  NULL COMMENT '年级，如 2023级',
    phone           VARCHAR(20)  NULL,
    email           VARCHAR(100) NULL,
    avatar          VARCHAR(255) NULL,
    member_type     VARCHAR(30)  NOT NULL DEFAULT 'OBSERVE_MEMBER',
    department      VARCHAR(50)  NULL,
    join_date       DATE         NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    last_login_time DATETIME     NULL,
    create_time     DATETIME     NULL,
    update_time     DATETIME     NULL,
    deleted         TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (member_id),
    UNIQUE KEY uk_member_student_id (student_id),
    KEY idx_member_type (member_type),
    KEY idx_member_status (status),
    KEY idx_member_grade (grade)
) ENGINE = InnoDB COMMENT = '会员';

CREATE TABLE IF NOT EXISTS member_role (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    member_id  BIGINT      NOT NULL,
    role_code  VARCHAR(32) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_member_role (member_id, role_code),
    KEY idx_member_role_code (role_code)
) ENGINE = InnoDB COMMENT = '会员-角色关联';

-- 通用申请表：入会/退会/集训报名/赛事报名共用同一张表。
-- type 取值：JOIN_MEMBER / RESIGN / CAMP_REGISTER / CONTEST_REGISTER。
-- target_id 指向集训或赛事主键，入会/退会申请为空；team_id 仅赛事组队报名使用。
CREATE TABLE IF NOT EXISTS application (
    apply_id     BIGINT      NOT NULL AUTO_INCREMENT,
    type         VARCHAR(30) NOT NULL COMMENT 'JOIN_MEMBER/RESIGN/CAMP_REGISTER/CONTEST_REGISTER',
    target_id    BIGINT      NULL COMMENT '集训/赛事 ID，入会/退会为空',
    applicant_id BIGINT      NULL COMMENT '申请人，入会申请审核通过前可为空',
    team_id      BIGINT      NULL COMMENT '赛事组队报名时的队伍 ID',
    apply_data   JSON        NULL,
    status       VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    audit_remark VARCHAR(500) NULL,
    auditor_id   BIGINT      NULL,
    apply_time   DATETIME    NULL,
    audit_time   DATETIME    NULL,
    create_time  DATETIME    NULL,
    update_time  DATETIME    NULL,
    deleted      TINYINT     NOT NULL DEFAULT 0,
    PRIMARY KEY (apply_id),
    KEY idx_application_target (type, target_id, status),
    KEY idx_application_applicant (applicant_id),
    KEY idx_application_team (team_id)
) ENGINE = InnoDB COMMENT = '通用申请（入会/退会/集训报名/赛事报名）';

-- ==================== 知识点与题库 ====================
CREATE TABLE IF NOT EXISTS tag (
    tag_id     BIGINT       NOT NULL AUTO_INCREMENT,
    name       VARCHAR(100) NOT NULL,
    category   VARCHAR(100) NULL COMMENT '知识点分类，如 DP/图论/数论',
    parent_id  BIGINT       NULL,
    sort_order INT          NOT NULL DEFAULT 0,
    create_time DATETIME    NULL,
    update_time DATETIME    NULL,
    deleted    TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (tag_id),
    KEY idx_tag_parent (parent_id)
) ENGINE = InnoDB COMMENT = '知识点';

CREATE TABLE IF NOT EXISTS problem (
    problem_id     BIGINT       NOT NULL AUTO_INCREMENT,
    title          VARCHAR(255) NOT NULL,
    oj_problem_id  VARCHAR(64)  NULL COMMENT '自建 OJ 题目编号',
    difficulty     VARCHAR(20)  NOT NULL DEFAULT 'MEDIUM',
    time_limit     INT          NOT NULL DEFAULT 1000 COMMENT '毫秒',
    memory_limit   INT          NOT NULL DEFAULT 262144 COMMENT 'KB',
    description    LONGTEXT     NULL,
    input_format   TEXT         NULL,
    output_format  TEXT         NULL,
    sample_input   TEXT         NULL,
    sample_output  TEXT         NULL,
    hint           TEXT         NULL,
    visibility     VARCHAR(30)  NOT NULL DEFAULT 'PUBLIC',
    creator_id     BIGINT       NULL,
    create_time    DATETIME     NULL,
    update_time    DATETIME     NULL,
    deleted        TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (problem_id),
    KEY idx_problem_oj (oj_problem_id),
    KEY idx_problem_visibility (visibility),
    KEY idx_problem_difficulty (difficulty)
) ENGINE = InnoDB COMMENT = '题目';

CREATE TABLE IF NOT EXISTS problem_tag (
    id         BIGINT NOT NULL AUTO_INCREMENT,
    problem_id BIGINT NOT NULL,
    tag_id     BIGINT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_problem_tag (problem_id, tag_id),
    KEY idx_problem_tag_tag (tag_id)
) ENGINE = InnoDB COMMENT = '题目-知识点关联';

-- 通用题目关联表：训练、集训任务、赛事共用。
-- target_type 取值：TRAINING / CAMP_TASK / CONTEST。
CREATE TABLE IF NOT EXISTS problem_link (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    target_type VARCHAR(20) NOT NULL COMMENT 'TRAINING/CAMP_TASK/CONTEST',
    target_id   BIGINT      NOT NULL COMMENT '训练/集训任务/赛事 ID',
    problem_id  BIGINT      NOT NULL,
    sort_order  INT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_problem_link (target_type, target_id, problem_id),
    KEY idx_problem_link_target (target_type, target_id),
    KEY idx_problem_link_problem (problem_id)
) ENGINE = InnoDB COMMENT = '通用题目关联（训练/集训任务/赛事）';

CREATE TABLE IF NOT EXISTS solution (
    solution_id    BIGINT   NOT NULL AUTO_INCREMENT,
    problem_id     BIGINT   NOT NULL,
    content        LONGTEXT NULL COMMENT 'Markdown 题解',
    reference_code LONGTEXT NULL,
    create_time    DATETIME NULL,
    update_time    DATETIME NULL,
    deleted        TINYINT  NOT NULL DEFAULT 0,
    PRIMARY KEY (solution_id),
    UNIQUE KEY uk_solution_problem (problem_id)
) ENGINE = InnoDB COMMENT = '题解';

-- ==================== 训练与提交 ====================
CREATE TABLE IF NOT EXISTS training (
    training_id        BIGINT       NOT NULL AUTO_INCREMENT,
    name               VARCHAR(255) NOT NULL,
    type               VARCHAR(20)  NOT NULL DEFAULT 'SOLO',
    description        TEXT         NULL,
    start_time         DATETIME     NULL,
    end_time           DATETIME     NULL,
    leaderboard_visible TINYINT     NOT NULL DEFAULT 1,
    freeze_time        INT          NOT NULL DEFAULT 0 COMMENT '结束前 N 分钟封榜',
    status             VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    creator_id         BIGINT       NOT NULL,
    create_time        DATETIME     NULL,
    update_time        DATETIME     NULL,
    deleted            TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (training_id),
    KEY idx_training_status (status),
    KEY idx_training_creator (creator_id)
) ENGINE = InnoDB COMMENT = '训练';

CREATE TABLE IF NOT EXISTS submission (
    submit_id     BIGINT      NOT NULL AUTO_INCREMENT,
    training_id   BIGINT      NULL,
    contest_id    BIGINT      NULL,
    problem_id    BIGINT      NOT NULL,
    submitter_id  BIGINT      NOT NULL,
    code          LONGTEXT    NOT NULL,
    language      VARCHAR(30) NOT NULL,
    result        VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    time_used     INT         NULL COMMENT '毫秒',
    memory_used   INT         NULL COMMENT 'KB',
    oj_submit_id  VARCHAR(64) NULL,
    submit_time   DATETIME    NULL,
    create_time   DATETIME    NULL,
    update_time   DATETIME    NULL,
    deleted       TINYINT     NOT NULL DEFAULT 0,
    PRIMARY KEY (submit_id),
    KEY idx_submission_training (training_id, submitter_id),
    KEY idx_submission_contest (contest_id, submitter_id),
    KEY idx_submission_problem (problem_id)
) ENGINE = InnoDB COMMENT = '代码提交记录';

-- ==================== 组队 ====================
CREATE TABLE IF NOT EXISTS team (
    team_id        BIGINT       NOT NULL AUTO_INCREMENT,
    name           VARCHAR(100) NOT NULL,
    leader_id      BIGINT       NOT NULL,
    contest_target VARCHAR(100) NULL,
    description    VARCHAR(500) NULL,
    status         VARCHAR(30)  NOT NULL DEFAULT 'DRAFT',
    create_time    DATETIME     NULL,
    update_time    DATETIME     NULL,
    deleted        TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (team_id),
    KEY idx_team_leader (leader_id)
) ENGINE = InnoDB COMMENT = '队伍';

CREATE TABLE IF NOT EXISTS team_member (
    id         BIGINT   NOT NULL AUTO_INCREMENT,
    team_id    BIGINT   NOT NULL,
    member_id  BIGINT   NOT NULL,
    join_time  DATETIME NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_team_member (team_id, member_id),
    KEY idx_team_member_member (member_id)
) ENGINE = InnoDB COMMENT = '队伍成员';

-- ==================== 寒假/暑假集训 ====================
CREATE TABLE IF NOT EXISTS camp (
    camp_id               BIGINT       NOT NULL AUTO_INCREMENT,
    type                  VARCHAR(20)  NOT NULL COMMENT 'WINTER/SUMMER',
    name                  VARCHAR(255) NOT NULL,
    start_time            DATETIME     NOT NULL COMMENT '集训开始时间',
    end_time              DATETIME     NOT NULL COMMENT '集训结束时间',
    location              VARCHAR(255) NULL COMMENT '集训地点',
    registration_deadline DATETIME     NULL COMMENT '报名截止时间',
    description           TEXT         NULL,
    requirements          TEXT         NULL COMMENT '报名要求',
    status                VARCHAR(20)  NOT NULL DEFAULT 'ANNOUNCED',
    creator_id            BIGINT       NULL,
    create_time           DATETIME     NULL,
    update_time           DATETIME     NULL,
    deleted               TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (camp_id),
    KEY idx_camp_type (type),
    KEY idx_camp_start_time (start_time)
) ENGINE = InnoDB COMMENT = '集训';

CREATE TABLE IF NOT EXISTS camp_task (
    task_id     BIGINT       NOT NULL AUTO_INCREMENT,
    camp_id     BIGINT       NOT NULL,
    title       VARCHAR(255) NOT NULL,
    content     TEXT         NULL,
    task_date   DATE         NULL,
    creator_id  BIGINT       NULL,
    create_time DATETIME     NULL,
    update_time DATETIME     NULL,
    deleted     TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (task_id),
    KEY idx_camp_task_camp (camp_id)
) ENGINE = InnoDB COMMENT = '集训每日任务';

CREATE TABLE IF NOT EXISTS attendance (
    record_id     BIGINT      NOT NULL AUTO_INCREMENT,
    camp_id       BIGINT      NOT NULL,
    member_id     BIGINT      NOT NULL,
    check_in_date DATE        NOT NULL,
    check_in_time DATETIME    NULL,
    status        VARCHAR(20) NOT NULL DEFAULT 'PRESENT',
    remark        VARCHAR(500) NULL,
    create_time   DATETIME    NULL,
    update_time   DATETIME    NULL,
    deleted       TINYINT     NOT NULL DEFAULT 0,
    PRIMARY KEY (record_id),
    UNIQUE KEY uk_attendance (camp_id, member_id, check_in_date),
    KEY idx_attendance_member (member_id)
) ENGINE = InnoDB COMMENT = '集训考勤';

-- ==================== 赛事 ====================
CREATE TABLE IF NOT EXISTS contest (
    contest_id            BIGINT       NOT NULL AUTO_INCREMENT,
    name                  VARCHAR(255) NOT NULL,
    category              VARCHAR(20)  NOT NULL DEFAULT 'SCHOLASTIC',
    start_time            DATETIME     NULL,
    end_time              DATETIME     NULL,
    registration_deadline DATETIME     NULL,
    description           TEXT         NULL,
    requirements          TEXT         NULL COMMENT '报名要求',
    max_team_size         INT          NOT NULL DEFAULT 3,
    freeze_minutes        INT          NOT NULL DEFAULT 0,
    status                VARCHAR(20)  NOT NULL DEFAULT 'ANNOUNCED',
    oj_contest_id         VARCHAR(64)  NULL,
    creator_id            BIGINT       NULL,
    create_time           DATETIME     NULL,
    update_time           DATETIME     NULL,
    deleted               TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (contest_id),
    KEY idx_contest_category (category),
    KEY idx_contest_status (status)
) ENGINE = InnoDB COMMENT = '赛事（校赛/XCPC/非XCPC）';

CREATE TABLE IF NOT EXISTS contest_result (
    result_id     BIGINT       NOT NULL AUTO_INCREMENT,
    contest_id    BIGINT       NOT NULL,
    team_id       BIGINT       NULL,
    member_id     BIGINT       NULL,
    rank_no       INT          NULL,
    solved_count  INT          NULL,
    penalty       INT          NULL,
    score         DECIMAL(10,2) NULL COMMENT '得分（非 XCPC 赛事）',
    award         VARCHAR(100) NULL,
    remark        VARCHAR(500) NULL,
    create_time   DATETIME     NULL,
    update_time   DATETIME     NULL,
    deleted       TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (result_id),
    KEY idx_contest_result (contest_id, rank_no)
) ENGINE = InnoDB COMMENT = '赛事成绩与获奖';

-- ==================== MBTI / 文件 / 日志 ====================
CREATE TABLE IF NOT EXISTS mbti_question (
    question_id BIGINT       NOT NULL AUTO_INCREMENT,
    question_no INT          NOT NULL,
    dimension   VARCHAR(10)  NOT NULL COMMENT 'E_I/S_N/T_F/J_P',
    content     VARCHAR(500) NOT NULL,
    option_a    VARCHAR(255) NOT NULL,
    option_b    VARCHAR(255) NOT NULL,
    score_a     INT          NOT NULL DEFAULT 1,
    score_b     INT          NOT NULL DEFAULT 1,
    create_time DATETIME     NULL,
    update_time DATETIME     NULL,
    deleted     TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (question_id),
    UNIQUE KEY uk_mbti_question_no (question_no)
) ENGINE = InnoDB COMMENT = 'MBTI 测评题目';

CREATE TABLE IF NOT EXISTS mbti_result (
    result_id   BIGINT      NOT NULL AUTO_INCREMENT,
    member_id   BIGINT      NOT NULL,
    type        VARCHAR(10) NOT NULL,
    score_ei    DOUBLE      NULL,
    score_sn    DOUBLE      NULL,
    score_tf    DOUBLE      NULL,
    score_jp    DOUBLE      NULL,
    report      TEXT        NULL,
    test_time   DATETIME    NULL,
    create_time DATETIME    NULL,
    update_time DATETIME    NULL,
    deleted     TINYINT     NOT NULL DEFAULT 0,
    PRIMARY KEY (result_id),
    KEY idx_mbti_result_member (member_id)
) ENGINE = InnoDB COMMENT = 'MBTI 测评结果';

CREATE TABLE IF NOT EXISTS file_record (
    file_id      BIGINT       NOT NULL AUTO_INCREMENT,
    filename     VARCHAR(255) NOT NULL,
    stored_name  VARCHAR(255) NOT NULL,
    url          VARCHAR(500) NOT NULL,
    size         BIGINT       NOT NULL DEFAULT 0,
    content_type VARCHAR(100) NULL,
    uploader_id  BIGINT       NULL,
    create_time  DATETIME     NULL,
    update_time  DATETIME     NULL,
    deleted      TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (file_id),
    KEY idx_file_uploader (uploader_id)
) ENGINE = InnoDB COMMENT = '文件上传记录';

CREATE TABLE IF NOT EXISTS operation_log (
    log_id      BIGINT       NOT NULL AUTO_INCREMENT,
    member_id   BIGINT       NULL,
    username    VARCHAR(50)  NULL,
    method      VARCHAR(10)  NULL,
    path        VARCHAR(255) NULL,
    action      VARCHAR(100) NULL,
    ip          VARCHAR(64)  NULL,
    status      INT          NULL,
    duration    BIGINT       NULL COMMENT '耗时毫秒',
    create_time DATETIME     NULL,
    PRIMARY KEY (log_id),
    KEY idx_log_member (member_id),
    KEY idx_log_time (create_time)
) ENGINE = InnoDB COMMENT = '操作审计日志';

-- ==================== 示例数据 ====================
-- 仅用于本地开发联调；全部使用 INSERT IGNORE，可重复执行。
-- 内置测试账号（可直接登录）：
--   2023210100 / Admin@123456  会长
--   2023210101 / 123456        会员
--   2023210102 / 123456        实验室成员
--   2023210103 / 123456        队长
--   2023210104 / 123456        干事
--   2023210105 / 123456        助理教练
--   2023210106 / 123456        部长
-- 全新初始化时管理员 member_id = 1、测试会员 = 2，下文 creator_id / leader_id 按此编写。

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

-- 测试会员账号；密码为 BCrypt 哈希，按 student_id 唯一键去重
INSERT IGNORE INTO member (student_id, password, name, major, grade, phone, email, member_type,
                           department, join_date, status, create_time, update_time, deleted) VALUES
    ('2023210100', '$2a$10$iTKvA.t7c97ND6vWqyagde.gV5w4G8fgYu8NOUt4tejYPtIDS3mjK',
     '系统管理员', '计算机科学与技术', '2023级', '13800000000', 'admin@example.com',
     'LAB_MEMBER', '技术部', '2023-09-01', 'ACTIVE', NOW(), NOW(), 0),
    ('2023210101', '$2a$10$J658/MW6soiM65MAWeWEKOW2R5b9qwGfSjsHzQIdVqcyH0jbsUUPa',
     '测试会员', '计算机科学与技术', '2023级', '13800000001', 'member@example.com',
     'MEMBER', '技术部', '2023-09-01', 'ACTIVE', NOW(), NOW(), 0),
    ('2023210102', '$2a$10$J658/MW6soiM65MAWeWEKOW2R5b9qwGfSjsHzQIdVqcyH0jbsUUPa',
     '张明', '软件工程', '2023级', '13800000002', 'zhangming@example.com',
     'LAB_MEMBER', '技术部', '2023-09-01', 'ACTIVE', NOW(), NOW(), 0),
    ('2023210103', '$2a$10$J658/MW6soiM65MAWeWEKOW2R5b9qwGfSjsHzQIdVqcyH0jbsUUPa',
     '李华', '计算机科学与技术', '2022级', '13800000003', 'lihua@example.com',
     'MEMBER', '竞赛部', '2022-09-01', 'ACTIVE', NOW(), NOW(), 0),
    ('2023210104', '$2a$10$J658/MW6soiM65MAWeWEKOW2R5b9qwGfSjsHzQIdVqcyH0jbsUUPa',
     '王芳', '信息安全', '2022级', '13800000004', 'wangfang@example.com',
     'MEMBER', '组织部', '2022-09-01', 'ACTIVE', NOW(), NOW(), 0),
    ('2023210105', '$2a$10$J658/MW6soiM65MAWeWEKOW2R5b9qwGfSjsHzQIdVqcyH0jbsUUPa',
     '赵强', '计算机科学与技术', '2021级', '13800000005', 'zhaoqiang@example.com',
     'LAB_MEMBER', '竞赛部', '2021-09-01', 'ACTIVE', NOW(), NOW(), 0),
    ('2023210106', '$2a$10$J658/MW6soiM65MAWeWEKOW2R5b9qwGfSjsHzQIdVqcyH0jbsUUPa',
     '孙悦', '软件工程', '2022级', '13800000006', 'sunyue@example.com',
     'MEMBER', '宣传部', '2022-09-01', 'ACTIVE', NOW(), NOW(), 0);

-- 角色已由上面的 member 行创建，这里按 student_id 关联，避免依赖自增 ID
INSERT IGNORE INTO member_role (member_id, role_code)
SELECT member_id, 'PRESIDENT' FROM member WHERE student_id = '2023210100';
INSERT IGNORE INTO member_role (member_id, role_code)
SELECT member_id, 'MEMBER' FROM member WHERE student_id = '2023210101';
INSERT IGNORE INTO member_role (member_id, role_code)
SELECT member_id, 'LAB_MEMBER' FROM member WHERE student_id = '2023210102';
INSERT IGNORE INTO member_role (member_id, role_code)
SELECT member_id, 'TEAM_LEADER' FROM member WHERE student_id = '2023210103';
INSERT IGNORE INTO member_role (member_id, role_code)
SELECT member_id, 'OFFICER' FROM member WHERE student_id = '2023210104';
INSERT IGNORE INTO member_role (member_id, role_code)
SELECT member_id, 'ASSISTANT_COACH' FROM member WHERE student_id = '2023210105';
INSERT IGNORE INTO member_role (member_id, role_code)
SELECT member_id, 'MINISTER' FROM member WHERE student_id = '2023210106';

INSERT IGNORE INTO tag (tag_id, name, category, parent_id, sort_order, create_time, update_time, deleted) VALUES
    (1, '动态规划', 'DP', NULL, 1, NOW(), NOW(), 0),
    (2, '最短路', '图论', NULL, 2, NOW(), NOW(), 0),
    (3, '素数筛', '数论', NULL, 3, NOW(), NOW(), 0),
    (4, '贪心', '基础算法', NULL, 4, NOW(), NOW(), 0),
    (5, '二分答案', '基础算法', NULL, 5, NOW(), NOW(), 0),
    (6, '字符串哈希', '字符串', NULL, 6, NOW(), NOW(), 0);

INSERT IGNORE INTO problem (problem_id, title, oj_problem_id, difficulty, time_limit, memory_limit,
                            description, input_format, output_format, sample_input, sample_output, hint,
                            visibility, creator_id, create_time, update_time, deleted) VALUES
    (1, 'A+B Problem', '1001', 'EASY', 1000, 262144,
     '输入两个整数，输出它们的和。', '两个整数 a 和 b', '一个整数，表示 a + b', '1 2', '3',
     '注意数据范围与负数。', 'PUBLIC', 1, NOW(), NOW(), 0),
    (2, '单源最短路', '1002', 'MEDIUM', 2000, 262144,
     '给定带权有向图，求源点到所有点的最短距离。', '第一行 n m s，随后 m 行 u v w',
     'n 个整数，表示最短距离，不可达输出 -1', '4 5 1\n1 2 2\n1 3 5\n2 3 1\n2 4 4\n3 4 1',
     '0 2 3 4', '注意重边与自环。', 'PUBLIC', 1, NOW(), NOW(), 0),
    (3, '最大子段和', '1003', 'MEDIUM', 1000, 262144,
     '给定长度为 n 的整数序列，求最大连续子段和。', '第一行 n，第二行 n 个整数',
     '一个整数，表示最大子段和', '5\n-2 1 -3 4 -1', '4', '子段不能为空。', 'PUBLIC', 1, NOW(), NOW(), 0),
    (4, '区间求和', '1004', 'EASY', 1000, 262144,
     '给定序列，多次查询区间和。', '第一行 n q，第二行 n 个整数，随后 q 行 l r',
     '每个查询输出一行区间和', '5 2\n1 2 3 4 5\n1 3\n2 5', '6\n14', '使用前缀和。', 'PUBLIC', 1, NOW(), NOW(), 0);

INSERT IGNORE INTO problem_tag (id, problem_id, tag_id) VALUES
    (1, 1, 4),
    (2, 2, 2),
    (3, 3, 1),
    (4, 4, 5);

INSERT IGNORE INTO solution (solution_id, problem_id, content, reference_code, create_time, update_time, deleted) VALUES
    (1, 1, '直接读入两个整数并输出它们的和。', 'int main(){int a,b;scanf("%d%d",&a,&b);printf("%d",a+b);return 0;}',
     NOW(), NOW(), 0);

INSERT IGNORE INTO training (training_id, name, type, description, start_time, end_time,
                             leaderboard_visible, freeze_time, status, creator_id,
                             create_time, update_time, deleted) VALUES
    (1, '2026 秋季算法训练（一）', 'SOLO', '基础算法与数据结构专项训练',
     DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_ADD(NOW(), INTERVAL 4 DAY), 1, 0, 'PUBLISHED', 1,
     NOW(), NOW(), 0),
    (2, '2026 秋季算法训练（二）', 'TEAM', '图论与动态规划综合训练',
     DATE_ADD(NOW(), INTERVAL 7 DAY), DATE_ADD(NOW(), INTERVAL 10 DAY), 1, 60, 'DRAFT', 1,
     NOW(), NOW(), 0);

INSERT IGNORE INTO team (team_id, name, leader_id, contest_target, description, status,
                         create_time, update_time, deleted) VALUES
    (1, 'SCUEC Alpha', 2, '2026 XCPC 校内选拔赛', '示例队伍', 'CONFIRMED', NOW(), NOW(), 0);

INSERT IGNORE INTO team_member (id, team_id, member_id, join_time) VALUES
    (1, 1, 2, NOW());

INSERT IGNORE INTO camp (camp_id, type, name, start_time, end_time, location, registration_deadline,
                         description, requirements, status, creator_id, create_time, update_time, deleted) VALUES
    (1, 'WINTER', '2026 寒假集训', DATE_ADD(NOW(), INTERVAL 14 DAY), DATE_ADD(NOW(), INTERVAL 21 DAY),
     '计算机学院 301', DATE_ADD(NOW(), INTERVAL 10 DAY), '寒假算法集训', '协会正式成员',
     'ANNOUNCED', 1, NOW(), NOW(), 0);

INSERT IGNORE INTO camp_task (task_id, camp_id, title, content, task_date, creator_id,
                              create_time, update_time, deleted) VALUES
    (1, 1, '第 1 天：基础算法', '完成指定题目并提交', DATE_ADD(CURDATE(), INTERVAL 14 DAY), 1, NOW(), NOW(), 0),
    (2, 1, '第 2 天：动态规划', '完成 DP 入门题', DATE_ADD(CURDATE(), INTERVAL 15 DAY), 1, NOW(), NOW(), 0);

INSERT IGNORE INTO contest (contest_id, name, category, start_time, end_time, registration_deadline,
                            description, requirements, max_team_size, freeze_minutes, status, creator_id,
                            create_time, update_time, deleted) VALUES
    (1, '2026 新生程序设计校赛', 'SCHOLASTIC',
     DATE_ADD(NOW(), INTERVAL 7 DAY), DATE_ADD(DATE_ADD(NOW(), INTERVAL 7 DAY), INTERVAL 5 HOUR),
     DATE_ADD(NOW(), INTERVAL 5 DAY), '面向新生的个人赛', '在校学生', 1, 0, 'ANNOUNCED',
     1, NOW(), NOW(), 0),
    (2, '2026 XCPC 校内选拔赛', 'XCPC',
     DATE_SUB(NOW(), INTERVAL 2 HOUR), DATE_ADD(NOW(), INTERVAL 2 HOUR),
     DATE_SUB(NOW(), INTERVAL 1 DAY), 'XCPC 赛制队伍赛，封榜 60 分钟', '每队 3 人', 3, 60, 'ANNOUNCED',
     1, NOW(), NOW(), 0),
    (3, '2025 非 XCPC 程序设计竞赛', 'NON_XCPC',
     DATE_SUB(NOW(), INTERVAL 10 DAY), DATE_ADD(DATE_SUB(NOW(), INTERVAL 10 DAY), INTERVAL 5 HOUR),
     DATE_SUB(NOW(), INTERVAL 15 DAY), '非 XCPC 赛制个人赛', '协会成员', 1, 0, 'ANNOUNCED',
     1, NOW(), NOW(), 0);

INSERT IGNORE INTO contest_result (result_id, contest_id, team_id, member_id, rank_no, solved_count,
                                   penalty, score, award, remark, create_time, update_time, deleted) VALUES
    (1, 3, 1, NULL, 1, 6, 720, NULL, '一等奖', '示例成绩', NOW(), NOW(), 0);

INSERT IGNORE INTO problem_link (id, target_type, target_id, problem_id, sort_order) VALUES
    (1, 'TRAINING', 1, 1, 0),
    (2, 'TRAINING', 1, 2, 1),
    (3, 'TRAINING', 2, 3, 0),
    (4, 'TRAINING', 2, 4, 1),
    (5, 'CAMP_TASK', 1, 1, 0),
    (6, 'CAMP_TASK', 1, 2, 1),
    (7, 'CAMP_TASK', 2, 3, 0),
    (8, 'CONTEST', 1, 1, 0),
    (9, 'CONTEST', 1, 2, 1),
    (10, 'CONTEST', 1, 3, 2),
    (11, 'CONTEST', 1, 4, 3),
    (12, 'CONTEST', 2, 1, 0),
    (13, 'CONTEST', 2, 2, 1),
    (14, 'CONTEST', 3, 3, 0),
    (15, 'CONTEST', 3, 4, 1);
