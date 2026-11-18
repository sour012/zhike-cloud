package com.zhike.edu.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class ScoreUpsertRequest {

    @NotNull(message = "选课记录不能为空")
    private Long enrollmentId;

    @NotNull(message = "平时分不能为空")
    @DecimalMin(value = "0", message = "不能小于 0")
    @DecimalMax(value = "100", message = "不能大于 100")
    private BigDecimal usualScore;

    @NotNull(message = "考试分不能为空")
    @DecimalMin(value = "0", message = "不能小于 0")
    @DecimalMax(value = "100", message = "不能大于 100")
    private BigDecimal examScore;
}
