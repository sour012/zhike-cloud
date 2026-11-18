package com.zhike.system.dto;

import java.util.List;

/** 登录返回：令牌 + 用户画像（前端据此渲染头像区与按钮级权限）。 */
public record LoginResponse(String token, long expiresIn, UserProfile user) {

    public record UserProfile(Long id, String username, String realName, int userType,
                              String dept, List<String> roles, List<String> perms) {
    }
}
