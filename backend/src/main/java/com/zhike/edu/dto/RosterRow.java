package com.zhike.edu.dto;

import java.math.BigDecimal;
import lombok.Data;

/** 某次开课的学生名单 + 已有成绩，教师录入成绩时用。 */
@Data
public class RosterRow {

    private Long enrollmentId;
    private Long studentId;
    private String studentNo;
    private String studentName;
    private String clazzName;
    private BigDecimal usualScore;
    private BigDecimal examScore;
    private BigDecimal total;
    private BigDecimal gpa;
}
