package com.ruijie.supplytask.dto;

import lombok.Data;

import java.util.List;

@Data
public class LeaveConfigDTO {

    private String userId;
    private String startAt;
    private String endAt;
    private String agentUserId;
    private List<String> productModels;
}
