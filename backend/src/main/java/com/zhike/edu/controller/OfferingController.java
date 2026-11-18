package com.zhike.edu.controller;

import com.zhike.common.Result;
import com.zhike.edu.dto.OfferingVO;
import com.zhike.edu.entity.Clazz;
import com.zhike.edu.entity.CourseOffering;
import com.zhike.edu.entity.Term;
import com.zhike.edu.service.OfferingService;
import com.zhike.security.AuthUserHolder;
import com.zhike.security.LoginUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
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

@Tag(name = "开课管理")
@RestController
@RequestMapping
public class OfferingController {

    private final OfferingService offeringService;

    public OfferingController(OfferingService offeringService) {
        this.offeringService = offeringService;
    }

    @Operation(summary = "学期列表")
    @GetMapping("/terms")
    @PreAuthorize("hasAuthority('edu:offering:list')")
    public Result<List<Term>> terms() {
        return Result.ok(offeringService.terms());
    }

    @Operation(summary = "当前学期")
    @GetMapping("/terms/current")
    @PreAuthorize("hasAuthority('edu:offering:list')")
    public Result<Term> currentTerm() {
        return Result.ok(offeringService.currentTerm());
    }

    @Operation(summary = "班级列表")
    @GetMapping("/clazzes")
    @PreAuthorize("hasAuthority('edu:offering:list')")
    public Result<List<Clazz>> clazzes() {
        return Result.ok(offeringService.clazzes());
    }

    @Operation(summary = "任课教师下拉选项")
    @GetMapping("/teachers")
    @PreAuthorize("hasAuthority('edu:offering:list')")
    public Result<List<OfferingService.TeacherOption>> teachers() {
        return Result.ok(offeringService.teachers());
    }

    @Operation(summary = "开课列表；学生视角会标注已选与时间冲突")
    @GetMapping("/offerings")
    @PreAuthorize("hasAuthority('edu:offering:list')")
    public Result<List<OfferingVO>> list(@RequestParam(required = false) Long termId,
                                         @RequestParam(required = false) String keyword) {
        LoginUser me = AuthUserHolder.current();
        // 教师只看自己的课；学生看全部并带冲突标记；管理员看全部
        Long teacherId = me.isTeacher() && !me.isAdmin() ? me.getId() : null;
        Long studentId = me.isStudent() ? me.getId() : null;
        return Result.ok(offeringService.list(termId, teacherId, keyword, studentId));
    }

    @Operation(summary = "新增开课")
    @PostMapping("/offerings")
    @PreAuthorize("hasAuthority('edu:offering:add')")
    public Result<Long> create(@RequestBody CourseOffering offering) {
        return Result.ok(offeringService.create(offering));
    }

    @Operation(summary = "编辑开课")
    @PutMapping("/offerings/{id}")
    @PreAuthorize("hasAuthority('edu:offering:edit')")
    public Result<Void> update(@PathVariable Long id, @RequestBody CourseOffering offering) {
        offeringService.update(id, offering);
        return Result.ok();
    }

    @Operation(summary = "删除开课（已有学生选课则拒绝）")
    @DeleteMapping("/offerings/{id}")
    @PreAuthorize("hasAuthority('edu:offering:edit')")
    public Result<Void> delete(@PathVariable Long id) {
        offeringService.delete(id);
        return Result.ok();
    }
}
