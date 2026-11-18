package com.zhike;

import com.jayway.jsonpath.JsonPath;
import com.zhike.support.ApiTestSupport;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 成绩录入：总评与绩点由后端算，教师只能操作自己的班，学生只能看自己。 */
@Transactional
class ScoreTest extends ApiTestSupport {

    @Test
    @DisplayName("录入成绩后总评 = 平时 30% + 考试 70%，绩点按 4.0 制换算")
    void saveComputesTotalAndGpa() throws Exception {
        // 3008 = 陈昊 选 数据结构（王爱国开的 2001），尚无成绩
        mvc.perform(post("/scores").contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", bearer(teacherToken))
                        .content("{\"enrollmentId\":3008,\"usualScore\":80,\"examScore\":90}"))
                .andExpect(status().isOk());

        String body = mvc.perform(get("/scores").header("Authorization", bearer(teacherToken)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<Map<String, Object>> rows = JsonPath.read(body, "$.data[?(@.enrollmentId==3008)]");
        assertThat(rows).hasSize(1);
        assertThat(((Number) rows.get(0).get("total")).doubleValue()).isEqualTo(87.0);
        assertThat(((Number) rows.get(0).get("gpa")).doubleValue()).isEqualTo(3.7);
    }

    @Test
    @DisplayName("教师不能给别人的教学班录成绩")
    void teacherCannotGradeOthersOffering() throws Exception {
        // 3003 = 李思远 选 计算机网络（李静开的 2003）
        mvc.perform(post("/scores").contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", bearer(teacherToken))
                        .content("{\"enrollmentId\":3003,\"usualScore\":80,\"examScore\":90}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("学生只能看到自己的成绩")
    void studentSeesOnlyOwnScores() throws Exception {
        String body = mvc.perform(get("/scores").header("Authorization", bearer(studentToken)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<String> names = JsonPath.read(body, "$.data[*].studentNo");
        assertThat(names).isNotEmpty().containsOnly("student1");
    }

    @Test
    @DisplayName("加权绩点按学分加权，不是简单平均")
    void gpaSummaryIsCreditWeighted() throws Exception {
        // 上学期 5 门：4.0*? 见 data.sql：89.2→3.7(4学分) 92.2→3.7(3.5) 83.1→3.3(3) 87.2→3.7(2.5) 81.1→3.0(2)
        mvc.perform(get("/scores/summary").header("Authorization", bearer(studentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.courseCount").value(5))
                .andExpect(jsonPath("$.data.credits").value(15.0))
                .andExpect(jsonPath("$.data.gpa").value(3.53));
    }

    @Test
    @DisplayName("分数超出 0~100 被参数校验拦下")
    void invalidScoreRejected() throws Exception {
        mvc.perform(post("/scores").contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", bearer(teacherToken))
                        .content("{\"enrollmentId\":3008,\"usualScore\":120,\"examScore\":90}"))
                .andExpect(status().isBadRequest());
    }
}
