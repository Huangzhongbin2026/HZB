package com.ruijie.supplytask.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TaskCreateRequest {

    @NotBlank(message = "任务类型不能为空")
    private String type;

    @NotBlank(message = "任务标题不能为空")
    private String title;

    private String orderNo;

    @NotBlank(message = "产品型号不能为空")
    private String productModel;

    @NotBlank(message = "提单人不能为空")
    private String requester;

    @NotBlank(message = "需求时间不能为空")
    private String requiredDate;

    @NotBlank(message = "任务原因不能为空")
    private String reason;

    @NotNull(message = "优先级不能为空")
    @Min(value = 1)
    @Max(value = 5)
    private Integer priority;
}
