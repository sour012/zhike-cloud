package com.zhike.edu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import lombok.Data;

@Data
@TableName("edu_course")
public class Course {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String code;
    private String name;
    private BigDecimal credit;
    private Integer hours;

    /** 专业必修 / 专业选修 / 通识 */
    private String category;

    private String dept;
    private String description;
}
