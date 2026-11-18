package com.zhike.system.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zhike.common.BizException;
import com.zhike.security.AuthUserHolder;
import com.zhike.security.JwtTokenProvider;
import com.zhike.security.LoginUser;
import com.zhike.system.dto.LoginRequest;
import com.zhike.system.dto.LoginResponse;
import com.zhike.system.dto.MenuNode;
import com.zhike.system.entity.SysMenu;
import com.zhike.system.entity.SysUser;
import com.zhike.system.mapper.SysMenuMapper;
import com.zhike.system.mapper.SysUserMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final SysUserMapper userMapper;
    private final SysMenuMapper menuMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    public AuthService(SysUserMapper userMapper, SysMenuMapper menuMapper,
                       PasswordEncoder passwordEncoder, JwtTokenProvider tokenProvider) {
        this.userMapper = userMapper;
        this.menuMapper = menuMapper;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest req) {
        SysUser user = userMapper.selectOne(
                Wrappers.<SysUser>lambdaQuery().eq(SysUser::getUsername, req.getUsername().trim()));

        // 用户不存在与密码错误返回同一句话，避免账号枚举
        if (user == null || !passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new BizException(401, "用户名或密码错误");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BizException(403, "账号已停用，请联系教务管理员");
        }

        int userType = user.getUserType() == null ? 3 : user.getUserType();
        List<String> roles = userMapper.selectRoleCodes(user.getId());
        List<String> perms = userMapper.selectPerms(user.getId());

        LoginUser principal = new LoginUser(user.getId(), user.getUsername(), user.getRealName(),
                userType, roles, perms);
        String token = tokenProvider.issue(principal);
        return new LoginResponse(token, tokenProvider.ttlSeconds(),
                new LoginResponse.UserProfile(user.getId(), user.getUsername(), user.getRealName(),
                        userType, user.getDept(), roles, perms));
    }

    /** 刷新页面时用令牌换回最新的用户画像 */
    @Transactional(readOnly = true)
    public LoginResponse.UserProfile profile() {
        LoginUser me = AuthUserHolder.current();
        SysUser user = userMapper.selectById(me.getId());
        if (user == null) {
            throw new BizException(401, "账号不存在或已被删除");
        }
        int userType = user.getUserType() == null ? 3 : user.getUserType();
        List<String> roles = userMapper.selectRoleCodes(user.getId());
        List<String> perms = userMapper.selectPerms(user.getId());
        return new LoginResponse.UserProfile(user.getId(), user.getUsername(), user.getRealName(),
                userType, user.getDept(), roles, perms);
    }

    /** 侧边栏：按角色可见的目录/菜单，组装成两级树 */
    @Transactional(readOnly = true)
    public List<MenuNode> navMenus() {
        List<SysMenu> all = menuMapper.selectNavByUserId(AuthUserHolder.currentId());
        Map<Long, MenuNode> index = new LinkedHashMap<>();
        List<MenuNode> roots = new ArrayList<>();
        for (SysMenu m : all) {
            index.put(m.getId(), new MenuNode(m.getId(), m.getTitle(), m.getPath(), m.getPerm(), m.getIcon()));
        }
        for (SysMenu m : all) {
            MenuNode node = index.get(m.getId());
            Long pid = m.getParentId();
            if (pid == null || pid == 0 || !index.containsKey(pid)) {
                roots.add(node);
            } else {
                index.get(pid).getChildren().add(node);
            }
        }
        // 目录下没有可见菜单时不展示该目录，避免点开是空的
        roots.removeIf(n -> n.getChildren().isEmpty() && n.getPath() == null);
        return roots;
    }
}
