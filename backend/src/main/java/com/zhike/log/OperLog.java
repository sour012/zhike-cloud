package com.zhike.log;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("sys_oper_log")
public class OperLog {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String username;
    private String module;
    private String action;
    private String method;

    /** 请求参数（截断到 900 字符，避免大字段撑爆日志表） */
    private String params;

    private String ip;
    private Integer costMs;
    private Integer success;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
