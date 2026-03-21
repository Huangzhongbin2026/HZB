package com.ruijie.supplysystem.auxiliary.dto;

import lombok.Data;

@Data
public class LeaveConfigDTO {
    private String id;
    private String userId;
    private String userName;
    private String leaveStart;
    private String leaveEnd;
    private String leaveReason;
    private Boolean status;
}
