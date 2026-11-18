package com.zhike;

import com.jayway.jsonpath.JsonPath;
import com.zhike.support.ApiTestSupport;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 选课四条规则的集成测试：重复、冲突、容量、已录成绩不可退。 */
@Transactional
class EnrollmentTest extends ApiTestSupport {

    private int enrolledOf(long offeringId) throws Exception {
        String body = mvc.perform(get("/offerings").param("termId", "2")
                        .header("Authorization", bearer(studentToken)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<Integer> values = JsonPath.read(body, "$.data[?(@.id==" + offeringId + ")].enrolled");
        return values.isEmpty() ? -1 : values.get(0);
    }

    @Test
    @DisplayName("同一时段重复选课被拒绝：2007 与已选的 2001 都是周一 1-2 节")
    void conflictIsRejected() throws Exception {
        mvc.perform(post("/enrollments").contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", bearer(studentToken))
                        .content("{\"offeringId\":2007}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.msg").value(org.hamcrest.Matchers.containsString("时间冲突")));
    }

    @Test
    @DisplayName("已选过的课不能再选")
    void duplicateIsRejected() throws Exception {
        mvc.perform(post("/enrollments").contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", bearer(studentToken))
                        .content("{\"offeringId\":2001}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.msg").value(org.hamcrest.Matchers.containsString("已经选过")));
    }

    @Test
    @DisplayName("最后一个名额被抢走后，下一个学生选课返回名额已满，且 enrolled 不超过 capacity")
    void capacityIsNotOversold() throws Exception {
        assertThat(enrolledOf(2008)).isEqualTo(1);

        mvc.perform(post("/enrollments").contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", bearer(login("student2", "student123")))
                        .content("{\"offeringId\":2008}"))
                .andExpect(status().isOk());
        assertThat(enrolledOf(2008)).isEqualTo(2);

        mvc.perform(post("/enrollments").contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", bearer(studentToken))
                        .content("{\"offeringId\":2008}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.msg").value(org.hamcrest.Matchers.containsString("名额已满")));

        // 关键断言：并发占座失败时不能把 enrolled 加上去
        assertThat(enrolledOf(2008)).isEqualTo(2);
    }

    @Test
    @DisplayName("退课释放名额；已录成绩的课程不允许退课")
    void dropReleasesSeatButBlockedWhenScored() throws Exception {
        assertThat(enrolledOf(2003)).isEqualTo(2);

        mvc.perform(delete("/enrollments/3003").header("Authorization", bearer(studentToken)))
                .andExpect(status().isOk());
        assertThat(enrolledOf(2003)).isEqualTo(1);

        // 3101 是上学期的选课记录，已经录入成绩
        mvc.perform(delete("/enrollments/3101").header("Authorization", bearer(studentToken)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.msg").value(org.hamcrest.Matchers.containsString("已录入成绩")));

        // 已退课的记录不能重复退
        mvc.perform(delete("/enrollments/3003").header("Authorization", bearer(studentToken)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.msg").value(org.hamcrest.Matchers.containsString("已退课")));
    }

    @Test
    @DisplayName("退课只能退自己的课")
    void cannotDropOthersEnrollment() throws Exception {
        // 3005 属于 student2
        mvc.perform(delete("/enrollments/3005").header("Authorization", bearer(studentToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.msg").value(org.hamcrest.Matchers.containsString("只能退自己")));
    }

    @Test
    @DisplayName("我的课表默认只返回当前学期，不传 termId 时不会把上学期已结课的记录混进来")
    void myCoursesDefaultToCurrentTerm() throws Exception {
        // student1 上学期 5 条 + 本学期 5 条，共 10 条
        String current = mvc.perform(get("/enrollments/my")
                        .header("Authorization", bearer(studentToken)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<Integer> currentIds = JsonPath.read(current, "$.data[*].offeringId");
        assertThat(currentIds).hasSize(5);
        assertThat(currentIds).allMatch(id -> id >= 2000 && id < 3000);

        // 显式指定上学期，应只拿到上学的 5 条
        String past = mvc.perform(get("/enrollments/my").param("termId", "1")
                        .header("Authorization", bearer(studentToken)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<Integer> pastIds = JsonPath.read(past, "$.data[*].offeringId");
        assertThat(pastIds).hasSize(5);
        assertThat(pastIds).allMatch(id -> id >= 2100 && id < 2200);
    }
}
