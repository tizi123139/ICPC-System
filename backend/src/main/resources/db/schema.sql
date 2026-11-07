-- ICPC 协会管理系统数据库初始化脚本（MySQL 8.0）
-- 文档编号：ICPC-PLAN-V1.0 / 数据库设计 M1 交付物
CREATE DATABASE IF NOT EXISTS icpc_system
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;
USE icpc_system;

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
