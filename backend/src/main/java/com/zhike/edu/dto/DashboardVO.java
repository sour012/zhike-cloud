package com.zhike.edu.dto;

import com.zhike.notice.dto.NoticeRow;
import java.math.BigDecimal;
import java.util.List;
import lombok.Data;

/** 首页看板数据。管理员/教师看全局，学生额外拿到自己的选课与绩点。 */
@Data
public class DashboardVO {

    private long courseCount;
    private long offeringCount;
    private long studentCount;
    private long teacherCount;
    private long enrollmentCount;
    private BigDecimal avgCapacityUsage;

    private String currentTermName;

    private List<WeekLoad> weekLoad;
    private List<HotCourse> hotCourses;
    private List<NoticeRow> notices;

    /** 仅学生有值 */
    private Long myCourseCount;
    private BigDecimal myCredits;
    private BigDecimal myGpa;

    /** 仅教师有值：本学期自己的教学班数与听课人次 */
    private Long myOfferingCount;
    private Long myStudentCount;

    public record WeekLoad(int weekday, String label, long count) {
    }

    public record HotCourse(String courseName, String teacherName, int enrolled, int capacity) {
    }
}
