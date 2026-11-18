package com.zhike.security;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * 登录态主体。令牌里携带角色与权限点，所以每个请求不需要再查一次权限表；
 * 代价是权限变更要等令牌过期或重新登录才生效（TTL 8 小时，见 docs/DESIGN.md 的取舍说明）。
 */
@Getter
public class LoginUser implements UserDetails {

    private final Long id;
    private final String username;
    private final String realName;
    /** 1 管理员 2 教师 3 学生 */
    private final int userType;
    private final Set<String> roles;
    private final Set<String> perms;

    public LoginUser(Long id, String username, String realName, int userType,
                     Collection<String> roles, Collection<String> perms) {
        this.id = id;
        this.username = username;
        this.realName = realName;
        this.userType = userType;
        this.roles = new LinkedHashSet<>(roles);
        this.perms = new LinkedHashSet<>(perms);
    }

    public boolean isAdmin() {
        return roles.contains("admin");
    }

    public boolean isStudent() {
        return userType == 3;
    }

    public boolean isTeacher() {
        return userType == 2;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Set<GrantedAuthority> auths = perms.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        roles.forEach(r -> auths.add(new SimpleGrantedAuthority("ROLE_" + r)));
        return auths;
    }

    @Override
    public String getPassword() {
        return null; // 令牌认证不需要密码
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
