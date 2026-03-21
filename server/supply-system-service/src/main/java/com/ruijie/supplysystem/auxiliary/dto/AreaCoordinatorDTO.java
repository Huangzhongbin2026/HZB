package com.ruijie.supplysystem.auxiliary.dto;

import lombok.Data;

@Data
public class AreaCoordinatorDTO {
    private String id;
    private String saleDeptCode;
    private String provinceCode;
    private String deptKeyword;
    private String projectKeyword;
    private String coordinatorUserId;
    private String coordinatorUserName;
    private Integer priorityNo;
    private Boolean status;
}
