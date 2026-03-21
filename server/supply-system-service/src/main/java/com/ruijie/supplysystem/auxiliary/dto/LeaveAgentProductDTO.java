package com.ruijie.supplysystem.auxiliary.dto;

import lombok.Data;

@Data
public class LeaveAgentProductDTO {
    private String id;
    private String productModel;
    private String originalUserId;
    private String originalUserName;
    private String agentUserId;
    private String agentUserName;
    private Boolean status;
}
