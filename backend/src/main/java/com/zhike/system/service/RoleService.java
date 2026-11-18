package com.zhike.system.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zhike.common.BizException;
import com.zhike.system.entity.SysMenu;
import com.zhike.system.entity.SysRole;
import com.zhike.system.entity.SysRoleMenu;
import com.zhike.system.mapper.SysMenuMapper;
import com.zhike.system.mapper.SysRoleMapper;
import com.zhike.system.mapper.SysRoleMenuMapper;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoleService {

    private final SysRoleMapper roleMapper;
    private final SysMenuMapper menuMapper;
    private final SysRoleMenuMapper roleMenuMapper;

    public RoleService(SysRoleMapper roleMapper, SysMenuMapper menuMapper,
                       SysRoleMenuMapper roleMenuMapper) {
        this.roleMapper = roleMapper;
        this.menuMapper = menuMapper;
        this.roleMenuMapper = roleMenuMapper;
    }

    @Transactional(readOnly = true)
    public List<RoleVO> list() {
        List<SysRole> roles = roleMapper.selectList(Wrappers.<SysRole>lambdaQuery().orderByAsc(SysRole::getId));
        List<RoleVO> out = new ArrayList<>();
        for (SysRole r : roles) {
            RoleVO vo = new RoleVO();
            vo.setId(r.getId());
            vo.setCode(r.getCode());
            vo.setName(r.getName());
            vo.setRemark(r.getRemark());
            vo.setMenuIds(menuMapper.selectMenuIdsByRole(r.getId()));
            out.add(vo);
        }
        return out;
    }

    @Transactional(readOnly = true)
    public List<SysMenu> allMenus() {
        return menuMapper.selectList(Wrappers.<SysMenu>lambdaQuery().orderByAsc(SysMenu::getSortNo));
    }

    @Transactional
    public void assignMenus(Long roleId, List<Long> menuIds) {
        SysRole role = roleMapper.selectById(roleId);
        if (role == null) {
            throw new BizException(404, "角色不存在");
        }
        if ("admin".equals(role.getCode())) {
            throw new BizException("超级管理员权限固定为全部，不允许裁剪");
        }
        roleMenuMapper.delete(Wrappers.<SysRoleMenu>lambdaQuery().eq(SysRoleMenu::getRoleId, roleId));
        if (menuIds == null) {
            return;
        }
        for (Long mid : menuIds.stream().distinct().toList()) {
            SysRoleMenu link = new SysRoleMenu();
            link.setRoleId(roleId);
            link.setMenuId(mid);
            roleMenuMapper.insert(link);
        }
    }

    @Data
    public static class RoleVO {
        private Long id;
        private String code;
        private String name;
        private String remark;
        private List<Long> menuIds;
    }
}
