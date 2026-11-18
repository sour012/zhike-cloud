-- 智课云教学管理系统 · 表结构
-- 说明：
--   1) 不写 ENGINE / CHARSET / COMMENT，保证同一份 DDL 既能跑 MySQL 8，也能跑 H2(MySQL 模式) 做集成测试；
--      库级别字符集由 sql/init-mysql.sql 统一设置。
--   2) 主键统一 BIGINT + 应用侧雪花 ID（MyBatis-Plus ASSIGN_ID），不依赖数据库自增，
--      这样 seed 数据里可以显式指定 ID，且测试库与生产库行为一致。
--   3) 不用外键约束：删除/迁移更灵活，一致性由 Service 层保证（这一点与 RuoYi 的做法一致）。

-- ---------------- 权限体系 ----------------

CREATE TABLE IF NOT EXISTS sys_user (
    id          BIGINT       NOT NULL PRIMARY KEY,
    username    VARCHAR(50)  NOT NULL,
    password    VARCHAR(100) NOT NULL,
    real_name   VARCHAR(50)  NOT NULL,
    user_type   TINYINT      NOT NULL,          -- 1 管理员 2 教师 3 学生
    dept        VARCHAR(100),
    phone       VARCHAR(20),
    email       VARCHAR(100),
    status      TINYINT      DEFAULT 1,         -- 1 正常 0 停用
    created_at  DATETIME,
    updated_at  DATETIME,
    CONSTRAINT uk_user_username UNIQUE (username)
);

CREATE TABLE IF NOT EXISTS sys_role (
    id          BIGINT      NOT NULL PRIMARY KEY,
    code        VARCHAR(50) NOT NULL,
    name        VARCHAR(50) NOT NULL,
    remark      VARCHAR(255),
    created_at  DATETIME,
    CONSTRAINT uk_role_code UNIQUE (code)
);

CREATE TABLE IF NOT EXISTS sys_menu (
    id          BIGINT      NOT NULL PRIMARY KEY,
    parent_id   BIGINT      DEFAULT 0,
    title       VARCHAR(50) NOT NULL,
    path        VARCHAR(120),
    perm        VARCHAR(80),
    menu_type   TINYINT     NOT NULL,           -- 1 目录 2 菜单 3 按钮
    icon        VARCHAR(50),
    sort_no     INT         DEFAULT 0,
    created_at  DATETIME
);

CREATE TABLE IF NOT EXISTS sys_user_role (
    id       BIGINT NOT NULL PRIMARY KEY,
    user_id  BIGINT NOT NULL,
    role_id  BIGINT NOT NULL,
    CONSTRAINT uk_user_role UNIQUE (user_id, role_id)
);

CREATE TABLE IF NOT EXISTS sys_role_menu (
    id       BIGINT NOT NULL PRIMARY KEY,
    role_id  BIGINT NOT NULL,
    menu_id  BIGINT NOT NULL,
    CONSTRAINT uk_role_menu UNIQUE (role_id, menu_id)
);

CREATE TABLE IF NOT EXISTS sys_notice (
    id           BIGINT       NOT NULL PRIMARY KEY,
    title        VARCHAR(200) NOT NULL,
    content      VARCHAR(2000),
    publisher_id BIGINT,
    published_at DATETIME,
    created_at   DATETIME
);

CREATE TABLE IF NOT EXISTS sys_oper_log (
    id         BIGINT       NOT NULL PRIMARY KEY,
    username   VARCHAR(50),
    module     VARCHAR(50),
    action     VARCHAR(50),
    method     VARCHAR(200),
    ip         VARCHAR(50),
    params     VARCHAR(1000),
    cost_ms    INT,
    success    TINYINT,
    created_at DATETIME
);

-- ---------------- 教务业务 ----------------

CREATE TABLE IF NOT EXISTS edu_term (
    id         BIGINT      NOT NULL PRIMARY KEY,
    name       VARCHAR(50) NOT NULL,
    start_date DATE,
    end_date   DATE,
    is_current TINYINT     DEFAULT 0           -- 1 当前学期
);

CREATE TABLE IF NOT EXISTS edu_course (
    id          BIGINT       NOT NULL PRIMARY KEY,
    code        VARCHAR(30)  NOT NULL,
    name        VARCHAR(100) NOT NULL,
    credit      DECIMAL(3,1) NOT NULL,
    hours       INT,
    category    VARCHAR(30),                    -- 专业必修 / 专业选修 / 通识
    dept        VARCHAR(100),
    description VARCHAR(500),
    CONSTRAINT uk_course_code UNIQUE (code)
);

CREATE TABLE IF NOT EXISTS edu_clazz (
    id              BIGINT      NOT NULL PRIMARY KEY,
    name            VARCHAR(60) NOT NULL,
    grade           INT,
    major           VARCHAR(80),
    head_teacher_id BIGINT
);

CREATE TABLE IF NOT EXISTS edu_offering (
    id            BIGINT      NOT NULL PRIMARY KEY,
    term_id       BIGINT      NOT NULL,
    course_id     BIGINT      NOT NULL,
    teacher_id    BIGINT      NOT NULL,
    clazz_id      BIGINT,
    weekday       TINYINT     NOT NULL,          -- 1~7
    start_section TINYINT     NOT NULL,          -- 第几节开始
    end_section   TINYINT     NOT NULL,
    location      VARCHAR(80),
    capacity      INT         NOT NULL,
    enrolled      INT         DEFAULT 0,         -- 冗余计数，选课用条件 UPDATE 原子扣减
    status        TINYINT     DEFAULT 1,         -- 1 开放选课 0 关闭
    created_at    DATETIME
);

CREATE TABLE IF NOT EXISTS edu_enrollment (
    id          BIGINT   NOT NULL PRIMARY KEY,
    offering_id BIGINT   NOT NULL,
    student_id  BIGINT   NOT NULL,
    status      TINYINT  DEFAULT 1,              -- 1 已选 0 已退
    created_at  DATETIME,
    CONSTRAINT uk_enrollment UNIQUE (offering_id, student_id)
);

CREATE TABLE IF NOT EXISTS edu_score (
    id           BIGINT       NOT NULL PRIMARY KEY,
    enrollment_id BIGINT      NOT NULL,
    student_id   BIGINT       NOT NULL,
    offering_id  BIGINT       NOT NULL,
    usual_score  DECIMAL(5,2),                   -- 平时分
    exam_score   DECIMAL(5,2),                   -- 考试分
    total        DECIMAL(5,2),
    gpa          DECIMAL(3,2),
    updated_at   DATETIME,
    CONSTRAINT uk_score_enrollment UNIQUE (enrollment_id)
);

-- 二级索引不写在这里：MySQL 的 CREATE INDEX 不支持 IF NOT EXISTS，重复执行会报错；
-- 索引统一放在 sql/init-mysql.sql，由 DBA 执行一次（测试库数据量小，无需索引）。

