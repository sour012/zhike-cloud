package com.zhike.notice;

import com.zhike.common.Result;
import com.zhike.notice.dto.NoticeRow;
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

@Tag(name = "通知公告")
@RestController
@RequestMapping("/notices")
public class NoticeController {

    private final NoticeService noticeService;

    public NoticeController(NoticeService noticeService) {
        this.noticeService = noticeService;
    }

    @Operation(summary = "公告列表")
    @GetMapping
    @PreAuthorize("hasAuthority('notice:list')")
    public Result<List<NoticeRow>> list(@RequestParam(required = false) String keyword) {
        return Result.ok(noticeService.list(keyword));
    }

    @Operation(summary = "发布公告")
    @PostMapping
    @PreAuthorize("hasAuthority('notice:add')")
    public Result<Long> publish(@RequestBody Notice notice) {
        return Result.ok(noticeService.publish(notice));
    }

    @Operation(summary = "编辑公告")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('notice:edit')")
    public Result<Void> update(@PathVariable Long id, @RequestBody Notice notice) {
        noticeService.update(id, notice);
        return Result.ok();
    }

    @Operation(summary = "删除公告")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('notice:delete')")
    public Result<Void> delete(@PathVariable Long id) {
        noticeService.delete(id);
        return Result.ok();
    }
}
