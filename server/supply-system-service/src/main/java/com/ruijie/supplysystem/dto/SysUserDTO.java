package com.ruijie.supplysystem.dto;

import lombok.Data;

@Data
public class SysUserDTO {
    private String id;
    private String userName;
    private String account;
    private String mobile;
    private String feishuId;
    private String email;
    private String deptCode;
    private Boolean status;
    private String lastLoginAt;
}
