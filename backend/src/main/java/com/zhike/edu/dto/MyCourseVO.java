package com.zhike.edu.dto;

import com.zhike.edu.TimeSlot;
import java.math.BigDecimal;
import lombok.Data;

/** 学生课表 / 选课记录行。 */
@Data
public class MyCourseVO {

    private Long enrollmentId;
    private Long offeringId;
    private String courseCode;
    private String courseName;
    private BigDecimal credit;
    private String category;
    private String teacherName;
    private Integer weekday;
    private Integer startSection;
    private Integer endSection;
    private String location;
    /** 1 已选 0 已退 */
    private Integer status;
    private BigDecimal total;
    private BigDecimal gpa;

    public String getTimeText() {
        return TimeSlot.text(weekday, startSection, endSection);
    }
}
