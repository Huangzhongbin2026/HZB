package com.ruijie.supplytask.dto;

import lombok.Data;

@Data
public class MessageSwitchDTO {

    private Boolean taskCreated;
    private Boolean taskTransfer;
    private Boolean taskOverdue;
}
