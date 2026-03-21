package com.ruijie.supplysystem.dto;

import lombok.Data;

@Data
public class SysRoleDTO {
    private String id;
    private String roleName;
    private String roleCode;
    private String deptCode;
    private String description;
    private Boolean isEnabled;
}
