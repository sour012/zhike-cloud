package com.zhike;

import com.zhike.support.ApiTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class AuthAndPermissionTest extends ApiTestSupport {

    @Test
    @DisplayName("登录成功返回令牌与权限点，菜单树按角色下发")
    void loginReturnsProfileAndMenus() throws Exception {
        assertThat(studentToken).isNotBlank();

        mvc.perform(get("/auth/profile").header("Authorization", bearer(studentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("student1"))
                .andExpect(jsonPath("$.data.userType").value(3))
                .andExpect(jsonPath("$.data.perms").isArray());

        // 学生没有系统管理菜单
        mvc.perform(get("/auth/menus").header("Authorization", bearer(studentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.title=='系统管理')]").isEmpty())
                .andExpect(jsonPath("$.data[?(@.title=='教务管理')]").isNotEmpty());
    }

    @Test
    @DisplayName("密码错误返回 401，且不区分「用户不存在」与「密码错误」")
    void wrongPasswordReturns401() throws Exception {
        mvc.perform(post("/auth/login").contentType("application/json")
                        .content("{\"username\":\"student1\",\"password\":\"bad-pass\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.msg").value("用户名或密码错误"));

        mvc.perform(post("/auth/login").contentType("application/json")
                        .content("{\"username\":\"no-such-user\",\"password\":\"x\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.msg").value("用户名或密码错误"));
    }

    @Test
    @DisplayName("不带令牌 401；学生访问用户管理 403；管理员 200")
    void permissionIsEnforcedOnServerSide() throws Exception {
        mvc.perform(get("/users")).andExpect(status().isUnauthorized());

        mvc.perform(get("/users").header("Authorization", bearer(studentToken)))
                .andExpect(status().isForbidden());

        mvc.perform(get("/users").header("Authorization", bearer(teacherToken)))
                .andExpect(status().isForbidden());

        mvc.perform(get("/users").header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(6));
    }

    @Test
    @DisplayName("伪造令牌被拒绝")
    void forgedTokenRejected() throws Exception {
        String forged = studentToken.substring(0, studentToken.length() - 3) + "abc";
        mvc.perform(get("/auth/profile").header("Authorization", bearer(forged)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("请求体格式错误与动词用错分别返回 400 / 405，不能都算 500")
    void malformedRequestIsNotServerError() throws Exception {
        // 授权接口要求裸数组 [1,2]，传成对象应被识别为调用方错误
        mvc.perform(put("/roles/1/menus").contentType("application/json")
                        .header("Authorization", bearer(adminToken))
                        .content("{\"menuIds\":[1]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.msg").value("请求体格式不正确"));

        // 该接口只支持 PUT
        mvc.perform(post("/roles/1/menus").contentType("application/json")
                        .header("Authorization", bearer(adminToken))
                        .content("[1]"))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    @DisplayName("教师没有角色管理权限，即使请求体合法也返回 403 而不是 500")
    void teacherCannotAssignRoleMenus() throws Exception {
        mvc.perform(put("/roles/2/menus").contentType("application/json")
                        .header("Authorization", bearer(teacherToken))
                        .content("[1]"))
                .andExpect(status().isForbidden());
    }
}
