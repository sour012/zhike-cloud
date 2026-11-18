package com.zhike.edu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import lombok.Data;

@Data
@TableName("edu_term")
public class Term {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String name;
    private LocalDate startDate;
    private LocalDate endDate;

    /** 1 当前学期 */
    private Integer isCurrent;
}
