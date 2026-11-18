package com.zhike.edu.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("edu_enrollment")
public class Enrollment {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long offeringId;
    private Long studentId;

    /** 1 已选 0 已退（唯一索引保证同一门课只有一条记录，退课是改状态而不是删行） */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
