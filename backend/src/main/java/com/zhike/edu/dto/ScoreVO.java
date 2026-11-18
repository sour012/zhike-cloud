package com.zhike.edu.dto;

import java.math.BigDecimal;
import lombok.Data;

/** 成绩单行。 */
@Data
public class ScoreVO {

    private Long id;
    private Long enrollmentId;
    private Long studentId;
    private String studentNo;
    private String studentName;
    private String courseCode;
    private String courseName;
    private BigDecimal credit;
    private String termName;
    private BigDecimal usualScore;
    private BigDecimal examScore;
    private BigDecimal total;
    private BigDecimal gpa;
}
