package com.ruijie.supplytask.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DashboardVO {

    private Integer total;
    private Integer pending;
    private Integer overdue;
    private Double onTimeRate;
}
