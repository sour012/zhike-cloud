package com.zhike.edu.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("edu_score")
public class Score {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long enrollmentId;
    private Long studentId;
    private Long offeringId;

    /** 平时分 */
    private BigDecimal usualScore;
    /** 考试分 */
    private BigDecimal examScore;
    /** 总评 = 平时 * 30% + 考试 * 70% */
    private BigDecimal total;
    /** 绩点，按学校 4.0 制换算 */
    private BigDecimal gpa;

    private LocalDateTime updatedAt;
}
