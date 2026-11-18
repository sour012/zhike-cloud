package com.zhike.system.controller;

import com.zhike.common.PageResult;
import com.zhike.common.Result;
import com.zhike.log.LogOperation;
import com.zhike.system.dto.UserUpsertRequest;
import com.zhike.system.dto.UserVO;
import com.zhike.system.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "用户管理")
@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "分页查询用户，支持按用户名/姓名模糊搜索")
    @GetMapping
    @PreAuthorize("hasAuthority('system:user:list')")
    public Result<PageResult<UserVO>> page(@RequestParam(required = false) String keyword,
                                           @RequestParam(required = false) Integer userType,
                                           @RequestParam(defaultValue = "1") long page,
                                           @RequestParam(defaultValue = "10") long size) {
        return Result.ok(userService.page(keyword, userType, page, size));
    }

    @Operation(summary = "新增用户")
    @PostMapping
    @PreAuthorize("hasAuthority('system:user:add')")
    @LogOperation(module = "用户", action = "新增用户")
    public Result<Long> create(@Valid @RequestBody UserUpsertRequest req) {
        return Result.ok(userService.create(req));
    }

    @Operation(summary = "编辑用户（password 留空表示不改密码）")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('system:user:edit')")
    @LogOperation(module = "用户", action = "编辑用户")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody UserUpsertRequest req) {
        userService.update(id, req);
        return Result.ok();
    }

    @Operation(summary = "删除用户")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:user:delete')")
    @LogOperation(module = "用户", action = "删除用户")
    public Result<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return Result.ok();
    }
}
