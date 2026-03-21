package com.ruijie.supplysystem.dto;

import lombok.Data;

@Data
public class SysDictTypeDTO {
    private String id;
    private String dictName;
    private String dictCode;
    private Integer sortNo;
    private Boolean isEnabled;
    private String remark;
}
