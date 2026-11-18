package com.zhike.edu.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("edu_offering")
public class CourseOffering {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long termId;
    private Long courseId;
    private Long teacherId;
    private Long clazzId;

    /** 1 周一 ~ 7 周日 */
    private Integer weekday;
    private Integer startSection;
    private Integer endSection;

    private String location;
    private Integer capacity;

    /** 冗余已选人数：选课时用条件 UPDATE 原子累加，避免超卖 */
    private Integer enrolled;

    /** 1 开放选课 0 关闭 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
