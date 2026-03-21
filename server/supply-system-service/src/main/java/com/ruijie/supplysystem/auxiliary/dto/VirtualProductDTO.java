package com.ruijie.supplysystem.auxiliary.dto;

import lombok.Data;

@Data
public class VirtualProductDTO {
    private String id;
    private String productModel;
    private String autoReplyContent;
    private Boolean status;
    private String createdAt;
}
