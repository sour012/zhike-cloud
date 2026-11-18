package com.zhike.edu.controller;

import com.zhike.common.Result;
import com.zhike.edu.dto.DashboardVO;
import com.zhike.edu.service.DashboardService;
import com.zhike.security.AuthUserHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "工作台")
@RestController
@RequestMapping("/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @Operation(summary = "首页统计：课程/开课/选课概况、排课分布、热门课程、最新公告")
    @GetMapping
    @PreAuthorize("hasAuthority('dashboard:view')")
    public Result<DashboardVO> load() {
        return Result.ok(dashboardService.load(AuthUserHolder.current()));
    }
}
