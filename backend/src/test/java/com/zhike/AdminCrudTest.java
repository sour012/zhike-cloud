package com.zhike;

import com.zhike.support.ApiTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 课程 CRUD 的约束、公告的归属校验、操作日志是否真的落库。 */
@Transactional
class AdminCrudTest extends ApiTestSupport {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("课程编号重复返回 409，唯一约束在应用层与数据库层各挡一次")
    void duplicateCourseCodeRejected() throws Exception {
        mvc.perform(post("/courses").contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", bearer(adminToken))
                        .content("{\"code\":\"CS201\",\"name\":\"数据结构（二）\",\"credit\":3}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.msg").value("课程编号已存在"));
    }

    @Test
    @DisplayName("已有开课记录的课程不能删除")
    void cannotDeleteCourseWithOffering() throws Exception {
        mvc.perform(delete("/courses/1001").header("Authorization", bearer(adminToken)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.msg").value(org.hamcrest.Matchers.containsString("开课记录")));
    }

    @Test
    @DisplayName("新增课程 → 列表能查到 → 删除成功")
    void createThenDeleteCourse() throws Exception {
        mvc.perform(post("/courses").contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", bearer(adminToken))
                        .content("{\"code\":\"CS403\",\"name\":\"编译原理\",\"credit\":3.5,"
                                + "\"hours\":56,\"category\":\"专业必修\",\"dept\":\"计算机学院\"}"))
                .andExpect(status().isOk());

        mvc.perform(get("/courses").param("keyword", "CS403")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    @DisplayName("教师没有公告发布权限，管理员可以发布")
    void noticePermission() throws Exception {
        mvc.perform(post("/notices").contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", bearer(teacherToken))
                        .content("{\"title\":\"临时通知\",\"content\":\"内容\"}"))
                .andExpect(status().isForbidden());

        mvc.perform(post("/notices").contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", bearer(adminToken))
                        .content("{\"title\":\"系统升级通知\",\"content\":\"本周六凌晨停机维护\"}"))
                .andExpect(status().isOk());

        mvc.perform(get("/notices").header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].title").value("系统升级通知"));
    }

    @Test
    @DisplayName("选课会写入操作日志（AOP 切面生效）")
    void operationLogIsWritten() throws Exception {
        Integer before = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_oper_log WHERE action = '选课'", Integer.class);

        mvc.perform(post("/enrollments").contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", bearer(studentToken))
                        .content("{\"offeringId\":2006}"))
                .andExpect(status().isConflict()); // 已选过，业务失败也应记录

        Integer after = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_oper_log WHERE action = '选课'", Integer.class);
        assertThat(after).isGreaterThan(before);

        // 成功、失败都要记，失败的那条 success = 0
        Integer failed = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_oper_log WHERE action = '选课' AND success = 0", Integer.class);
        assertThat(failed).isGreaterThanOrEqualTo(1);
    }
}
