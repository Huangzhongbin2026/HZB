package com.ruijie.supplysystem.dto;

import lombok.Data;

@Data
public class SysMenuDTO {
    private String id;
    private String menuName;
    private String menuType;
    private String parentId;
    private String routePath;
    private String componentPath;
    private String permissionCode;
    private String icon;
    private Integer sortNo;
    private Boolean isVisible;
    private Boolean isEnabled;
}
