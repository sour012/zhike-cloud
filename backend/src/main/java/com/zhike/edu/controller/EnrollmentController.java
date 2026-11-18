package com.zhike.edu.controller;

import com.zhike.common.Result;
import com.zhike.edu.dto.MyCourseVO;
import com.zhike.edu.service.EnrollmentService;
import com.zhike.log.LogOperation;
import com.zhike.security.AuthUserHolder;
import com.zhike.security.LoginUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "选课")
@RestController
@RequestMapping("/enrollments")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    public EnrollmentController(EnrollmentService enrollmentService) {
        this.enrollmentService = enrollmentService;
    }

    @Operation(summary = "我的课表（含已退记录，便于前端筛选）")
    @GetMapping("/my")
    @PreAuthorize("hasAuthority('edu:enroll:list')")
    public Result<List<MyCourseVO>> my(@RequestParam(required = false) Long termId,
                                       @RequestParam(defaultValue = "true") boolean onlyActive) {
        return Result.ok(enrollmentService.myCourses(AuthUserHolder.currentId(), termId, onlyActive));
    }

    @Operation(summary = "选课")
    @PostMapping
    @PreAuthorize("hasAuthority('edu:enroll:select')")
    @LogOperation(module = "选课", action = "选课")
    public Result<Long> select(@Valid @RequestBody SelectRequest req) {
        LoginUser me = AuthUserHolder.current();
        if (!me.isStudent()) {
            throw new com.zhike.common.BizException(403, "只有学生账号可以选课");
        }
        return Result.ok(enrollmentService.select(req.offeringId(), me.getId()));
    }

    @Operation(summary = "退课")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('edu:enroll:drop')")
    @LogOperation(module = "选课", action = "退课")
    public Result<Void> drop(@PathVariable Long id) {
        LoginUser me = AuthUserHolder.current();
        if (!me.isStudent()) {
            throw new com.zhike.common.BizException(403, "只有学生账号可以退课");
        }
        enrollmentService.drop(id, me.getId());
        return Result.ok();
    }

    public record SelectRequest(@NotNull(message = "开课 ID 不能为空") Long offeringId) {
    }
}
