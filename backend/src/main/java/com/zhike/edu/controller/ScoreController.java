package com.zhike.edu.controller;

import com.zhike.common.Result;
import com.zhike.edu.dto.RosterRow;
import com.zhike.edu.dto.ScoreUpsertRequest;
import com.zhike.edu.dto.ScoreVO;
import com.zhike.edu.service.ScoreService;
import com.zhike.log.LogOperation;
import com.zhike.security.AuthUserHolder;
import com.zhike.security.LoginUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "成绩")
@RestController
@RequestMapping("/scores")
public class ScoreController {

    private final ScoreService scoreService;

    public ScoreController(ScoreService scoreService) {
        this.scoreService = scoreService;
    }

    @Operation(summary = "成绩列表（学生只看自己，教师只看自己任课，管理员看全部）")
    @GetMapping
    @PreAuthorize("hasAuthority('edu:score:list')")
    public Result<List<ScoreVO>> list(@RequestParam(required = false) Long termId,
                                      @RequestParam(required = false) String keyword) {
        return Result.ok(scoreService.list(AuthUserHolder.current(), termId, keyword));
    }

    @Operation(summary = "我的加权绩点与已修学分")
    @GetMapping("/summary")
    @PreAuthorize("hasAuthority('edu:score:list')")
    public Result<ScoreService.Summary> summary() {
        LoginUser me = AuthUserHolder.current();
        return Result.ok(scoreService.summary(me.getId()));
    }

    @Operation(summary = "某次开课的学生名单（教师录入成绩用）")
    @GetMapping("/roster")
    @PreAuthorize("hasAuthority('edu:score:add')")
    public Result<List<RosterRow>> roster(@RequestParam Long offeringId) {
        return Result.ok(scoreService.roster(AuthUserHolder.current(), offeringId));
    }

    @Operation(summary = "录入/修改成绩（总评与绩点由后端计算）")
    @PostMapping
    @PreAuthorize("hasAuthority('edu:score:add')")
    @LogOperation(module = "成绩", action = "录入成绩")
    public Result<Void> save(@Valid @RequestBody ScoreUpsertRequest req) {
        scoreService.save(AuthUserHolder.current(), req);
        return Result.ok();
    }
}
