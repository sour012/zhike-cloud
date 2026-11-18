package com.zhike.edu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("edu_clazz")
public class Clazz {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String name;
    private Integer grade;
    private String major;
    private Long headTeacherId;
}
