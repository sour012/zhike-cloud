package com.zhike.edu.controller;

import com.zhike.common.PageResult;
import com.zhike.common.Result;
import com.zhike.edu.entity.Course;
import com.zhike.edu.service.CourseService;
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

@Tag(name = "课程库")
@RestController
@RequestMapping("/courses")
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @Operation(summary = "课程分页列表")
    @GetMapping
    @PreAuthorize("hasAuthority('edu:course:list')")
    public Result<PageResult<Course>> page(@RequestParam(required = false) String keyword,
                                           @RequestParam(required = false) String category,
                                           @RequestParam(defaultValue = "1") long page,
                                           @RequestParam(defaultValue = "10") long size) {
        return Result.ok(courseService.page(keyword, category, page, size));
    }

    @Operation(summary = "全部课程（下拉选择用）")
    @GetMapping("/all")
    @PreAuthorize("hasAuthority('edu:course:list')")
    public Result<List<Course>> all() {
        return Result.ok(courseService.listAll());
    }

    @Operation(summary = "新增课程")
    @PostMapping
    @PreAuthorize("hasAuthority('edu:course:add')")
    public Result<Long> create(@RequestBody Course course) {
        return Result.ok(courseService.create(course));
    }

    @Operation(summary = "编辑课程")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('edu:course:edit')")
    public Result<Void> update(@PathVariable Long id, @RequestBody Course course) {
        courseService.update(id, course);
        return Result.ok();
    }

    @Operation(summary = "删除课程（已有开课记录则拒绝）")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('edu:course:delete')")
    public Result<Void> delete(@PathVariable Long id) {
        courseService.delete(id);
        return Result.ok();
    }
}
