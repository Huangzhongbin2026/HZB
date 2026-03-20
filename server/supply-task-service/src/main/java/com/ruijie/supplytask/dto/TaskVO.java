package com.ruijie.supplytask.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TaskVO {

    private String id;
    private String type;
    private String status;
    private String title;
    private String productModel;
    private String requester;
    private String coordinator;
    private String dueAt;
    private Integer priority;
    private String createdAt;
}
