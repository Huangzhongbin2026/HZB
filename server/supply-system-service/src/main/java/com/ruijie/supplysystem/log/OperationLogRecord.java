package com.ruijie.supplysystem.log;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OperationLogRecord {
    private String operUser;
    private String operIp;
    private String operType;
    private String operModule;
    private String operContent;
    private String operResult;
    private String remark;
}
