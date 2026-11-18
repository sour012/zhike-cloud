-- 生产环境二级索引（只需执行一次）
-- 表结构与数据由 db/schema.sql、db/data.sql 负责，这里只补索引：
-- schema.sql 里不写索引，是因为 MySQL 的 CREATE INDEX 不支持 IF NOT EXISTS，
-- 而 schema.sql 会被应用或脚手架重复执行，重复建索引会直接报错。

CREATE INDEX idx_user_role_user ON sys_user_role (user_id);
CREATE INDEX idx_role_menu_role ON sys_role_menu (role_id);
CREATE INDEX idx_offering_term ON edu_offering (term_id);
CREATE INDEX idx_offering_teacher ON edu_offering (teacher_id);
CREATE INDEX idx_offering_course ON edu_offering (course_id);
CREATE INDEX idx_enroll_student ON edu_enrollment (student_id);
CREATE INDEX idx_enroll_offering ON edu_enrollment (offering_id);
CREATE INDEX idx_score_student ON edu_score (student_id);
CREATE INDEX idx_notice_time ON sys_notice (published_at);
CREATE INDEX idx_operlog_time ON sys_oper_log (created_at);
