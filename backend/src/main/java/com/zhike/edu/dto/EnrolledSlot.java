package com.zhike.edu.dto;

import lombok.Data;

/** 学生已占用的时段，用于选课冲突校验。 */
@Data
public class EnrolledSlot {

    private Long offeringId;
    private String courseName;
    private Integer weekday;
    private Integer startSection;
    private Integer endSection;
}
