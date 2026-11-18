package com.zhike.system.controller;

import com.zhike.common.Result;
import com.zhike.system.entity.SysMenu;
import com.zhike.system.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "角色与权限")
@RestController
@RequestMapping("/roles")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @Operation(summary = "角色列表，含每个角色已授权的菜单 ID")
    @GetMapping
    @PreAuthorize("hasAuthority('system:role:list')")
    public Result<List<RoleService.RoleVO>> list() {
        return Result.ok(roleService.list());
    }

    @Operation(summary = "全部菜单（供权限树渲染）")
    @GetMapping("/menus")
    @PreAuthorize("hasAuthority('system:role:list')")
    public Result<List<SysMenu>> menus() {
        return Result.ok(roleService.allMenus());
    }

    @Operation(summary = "为角色重新分配菜单/权限点")
    @PutMapping("/{id}/menus")
    @PreAuthorize("hasAuthority('system:role:list')")
    public Result<Void> assign(@PathVariable Long id, @RequestBody List<Long> menuIds) {
        roleService.assignMenus(id, menuIds);
        return Result.ok();
    }
}
