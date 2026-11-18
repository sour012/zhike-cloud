package com.zhike.edu.dto;

import com.zhike.edu.TimeSlot;
import java.math.BigDecimal;
import lombok.Data;

/** 开课列表行：一次 JOIN 出课程名、教师名、班级名，避免前端再发 N 个请求。 */
@Data
public class OfferingVO {

    private Long id;
    private Long termId;
    private String termName;
    private Long courseId;
    private String courseCode;
    private String courseName;
    private BigDecimal credit;
    private String category;
    private Long teacherId;
    private String teacherName;
    private String clazzName;
    private Integer weekday;
    private Integer startSection;
    private Integer endSection;
    private String location;
    private Integer capacity;
    private Integer enrolled;
    private Integer status;

    /** 当前登录学生是否已选 */
    private Boolean selected = false;
    /** 与已选课程是否时间冲突（前端置灰按钮用） */
    private Boolean conflict = false;
    private String conflictWith;

    /** "周一 1-2 节" */
    public String getTimeText() {
        return TimeSlot.text(weekday, startSection, endSection);
    }
}
