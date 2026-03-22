package com.ruijie.supplysystem.auxiliary.dto;

import lombok.Data;

@Data
public class AreaCoordinatorDTO {
    private String id;
    private String saleDeptCode;
    private String provinceCode;
    private String region;
    private String deptKeyword;
    private String projectKeyword;
    private String coordinatorUserId;
    private String coordinatorUserName;
    private String agentCoordinatorUserId;
    private String agentCoordinatorUserName;
    private String createdAt;
    private Integer priorityNo;
    private Boolean status;
}
