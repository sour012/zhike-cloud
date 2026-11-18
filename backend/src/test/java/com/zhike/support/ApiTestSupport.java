package com.zhike.support;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 集成测试基类：内嵌 H2（MySQL 模式）+ 完整 Spring 上下文 + MockMvc。
 * 每个测试类各自 @Transactional，方法结束回滚，互不影响。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class ApiTestSupport {

    @Autowired
    protected MockMvc mvc;

    protected String adminToken;
    protected String teacherToken;
    protected String studentToken;

    @BeforeEach
    void loginAll() throws Exception {
        adminToken = login("admin", "admin123");
        teacherToken = login("teacher1", "teacher123");
        studentToken = login("student1", "student123");
    }

    protected String login(String username, String password) throws Exception {
        String body = mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.data.token");
    }

    protected String bearer(String token) {
        return "Bearer " + token;
    }
}
