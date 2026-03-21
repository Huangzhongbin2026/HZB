package com.ruijie.supplysystem.dto;

import lombok.Data;

@Data
public class SysOperationLogDTO {
    private String id;
    private String operUser;
    private String operTime;
    private String operIp;
    private String operType;
    private String operModule;
    private String operContent;
    private String operResult;
    private String remark;
}
