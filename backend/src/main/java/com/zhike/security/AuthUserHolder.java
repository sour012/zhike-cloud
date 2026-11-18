package com.zhike.security;

import com.zhike.common.BizException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** 取当前登录用户。业务层不直接碰 SecurityContext，便于单测里手动注入。 */
public final class AuthUserHolder {

    private AuthUserHolder() {
    }

    public static LoginUser current() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof LoginUser user)) {
            throw new BizException(401, "未登录或登录已失效");
        }
        return user;
    }

    public static Long currentId() {
        return current().getId();
    }

    /** 管理员或指定用户本人才能操作 */
    public static void requireSelfOrAdmin(Long ownerId) {
        LoginUser me = current();
        if (!me.isAdmin() && !me.getId().equals(ownerId)) {
            throw new BizException(403, "只能操作自己的数据");
        }
    }
}
