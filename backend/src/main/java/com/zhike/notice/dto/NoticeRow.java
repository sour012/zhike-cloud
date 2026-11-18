package com.zhike.notice.dto;

import java.time.LocalDateTime;
import lombok.Data;

/** 公告 + 发布人姓名（列表页一次 JOIN 拿到，不用前端再查用户表）。 */
@Data
public class NoticeRow {

    private Long id;
    private String title;
    private String content;
    private Long publisherId;
    private String publisherName;
    private LocalDateTime publishedAt;
}
