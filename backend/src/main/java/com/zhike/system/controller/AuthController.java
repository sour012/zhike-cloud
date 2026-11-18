package com.zhike.system.controller;

import com.zhike.common.Result;
import com.zhike.system.dto.LoginRequest;
import com.zhike.system.dto.LoginResponse;
import com.zhike.system.dto.MenuNode;
import com.zhike.system.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "认证")
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "账号密码登录，返回 JWT")
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
        return Result.ok(authService.login(req));
    }

    @Operation(summary = "当前登录用户画像与权限点")
    @GetMapping("/profile")
    public Result<LoginResponse.UserProfile> profile() {
        return Result.ok(authService.profile());
    }

    @Operation(summary = "当前用户可见的侧边栏菜单树")
    @GetMapping("/menus")
    public Result<List<MenuNode>> menus() {
        return Result.ok(authService.navMenus());
    }

    /** 无状态令牌，服务端不维护会话；前端删掉本地令牌即视为登出 */
    @Operation(summary = "登出（前端清理令牌即可，接口保留用于埋点）")
    @PostMapping("/logout")
    public Result<Void> logout() {
        return Result.ok();
    }
}
