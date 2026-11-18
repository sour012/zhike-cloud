package com.zhike.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhike.common.BizException;
import com.zhike.common.PageResult;
import com.zhike.security.AuthUserHolder;
import com.zhike.system.dto.RoleCodeRow;
import com.zhike.system.dto.UserUpsertRequest;
import com.zhike.system.dto.UserVO;
import com.zhike.system.entity.SysRole;
import com.zhike.system.entity.SysUser;
import com.zhike.system.entity.SysUserRole;
import com.zhike.system.mapper.SysRoleMapper;
import com.zhike.system.mapper.SysUserMapper;
import com.zhike.system.mapper.SysUserRoleMapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class UserService {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;

    public UserService(SysUserMapper userMapper, SysRoleMapper roleMapper,
                       SysUserRoleMapper userRoleMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public PageResult<UserVO> page(String keyword, Integer userType, long pageNo, long pageSize) {
        IPage<SysUser> page = userMapper.selectPage(new Page<>(pageNo, pageSize),
                Wrappers.<SysUser>lambdaQuery()
                        .and(StringUtils.hasText(keyword), w -> w
                                .like(SysUser::getUsername, keyword)
                                .or().like(SysUser::getRealName, keyword))
                        .eq(userType != null, SysUser::getUserType, userType)
                        .orderByAsc(SysUser::getUserType)
                        .orderByAsc(SysUser::getId));

        List<SysUser> rows = page.getRecords();
        Map<Long, List<String>> roleMap = new HashMap<>();
        if (!rows.isEmpty()) {
            List<Long> ids = rows.stream().map(SysUser::getId).toList();
            for (RoleCodeRow r : userMapper.selectRoleRows(ids)) {
                roleMap.computeIfAbsent(r.getUserId(), k -> new java.util.ArrayList<>()).add(r.getCode());
            }
        }
        List<UserVO> vos = rows.stream().map(u -> {
            UserVO vo = new UserVO();
            vo.setId(u.getId());
            vo.setUsername(u.getUsername());
            vo.setRealName(u.getRealName());
            vo.setUserType(u.getUserType());
            vo.setDept(u.getDept());
            vo.setPhone(u.getPhone());
            vo.setEmail(u.getEmail());
            vo.setStatus(u.getStatus());
            vo.setCreatedAt(u.getCreatedAt());
            vo.setRoles(roleMap.getOrDefault(u.getId(), List.of()));
            return vo;
        }).toList();
        return PageResult.of(page.getTotal(), vos);
    }

    @Transactional
    public Long create(UserUpsertRequest req) {
        if (!StringUtils.hasText(req.getPassword())) {
            throw new BizException("新增用户必须设置初始密码");
        }
        Long exists = userMapper.selectCount(
                Wrappers.<SysUser>lambdaQuery().eq(SysUser::getUsername, req.getUsername()));
        if (exists != null && exists > 0) {
            throw new BizException(409, "用户名已存在");
        }
        SysUser user = new SysUser();
        user.setUsername(req.getUsername().trim());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setRealName(req.getRealName());
        user.setUserType(req.getUserType());
        user.setDept(req.getDept());
        user.setPhone(req.getPhone());
        user.setEmail(req.getEmail());
        user.setStatus(Optional.ofNullable(req.getStatus()).orElse(1));
        userMapper.insert(user);
        bindRoles(user.getId(), req.getRoleCodes());
        return user.getId();
    }

    @Transactional
    public void update(Long id, UserUpsertRequest req) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BizException(404, "用户不存在");
        }
        Long dup = userMapper.selectCount(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getUsername, req.getUsername()).ne(SysUser::getId, id));
        if (dup != null && dup > 0) {
            throw new BizException(409, "用户名已被占用");
        }
        user.setUsername(req.getUsername().trim());
        user.setRealName(req.getRealName());
        user.setUserType(req.getUserType());
        user.setDept(req.getDept());
        user.setPhone(req.getPhone());
        user.setEmail(req.getEmail());
        if (req.getStatus() != null) {
            user.setStatus(req.getStatus());
        }
        if (StringUtils.hasText(req.getPassword())) {
            user.setPassword(passwordEncoder.encode(req.getPassword()));
        }
        userMapper.updateById(user);
        if (req.getRoleCodes() != null) {
            userRoleMapper.delete(Wrappers.<SysUserRole>lambdaQuery().eq(SysUserRole::getUserId, id));
            bindRoles(id, req.getRoleCodes());
        }
    }

    @Transactional
    public void delete(Long id) {
        if (id.equals(AuthUserHolder.currentId())) {
            throw new BizException("不能删除当前登录账号");
        }
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BizException(404, "用户不存在");
        }
        if (Integer.valueOf(1).equals(user.getUserType())) {
            throw new BizException("管理员账号不允许删除，请先调整其角色");
        }
        // 已选课的学生不允许直接删除，否则成绩单会出现孤儿数据
        userMapper.deleteById(id);
        userRoleMapper.delete(Wrappers.<SysUserRole>lambdaQuery().eq(SysUserRole::getUserId, id));
    }

    private void bindRoles(Long userId, List<String> roleCodes) {
        if (roleCodes == null || roleCodes.isEmpty()) {
            return;
        }
        List<SysRole> roles = roleMapper.selectList(
                Wrappers.<SysRole>lambdaQuery().in(SysRole::getCode, roleCodes));
        if (roles.size() != roleCodes.stream().distinct().count()) {
            List<String> found = roles.stream().map(SysRole::getCode).collect(Collectors.toList());
            throw new BizException("存在不合法的角色：" + roleCodes.stream()
                    .filter(c -> !found.contains(c)).toList());
        }
        for (SysRole role : roles) {
            SysUserRole link = new SysUserRole();
            link.setUserId(userId);
            link.setRoleId(role.getId());
            userRoleMapper.insert(link);
        }
    }
}
