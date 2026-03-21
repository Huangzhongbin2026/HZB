package com.ruijie.supplysystem.dto;

import lombok.Data;

@Data
public class SysDictItemDTO {
    private String id;
    private String dictTypeId;
    private String itemName;
    private String itemCode;
    private String itemValue;
    private Integer sortNo;
    private Boolean isEnabled;
    private String remark;
}
