package com.ruijie.supplysystem.auxiliary.dto;

import lombok.Data;

@Data
public class MessagePushDTO {
    private String id;
    private String pushName;
    private String routeCode;
    private String feishuTemplateCode;
    private Boolean isEnabled;
    private String pushRule;
    private String createdAt;
}
