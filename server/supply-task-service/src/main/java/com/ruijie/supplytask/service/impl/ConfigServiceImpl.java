package com.ruijie.supplytask.service.impl;

import com.ruijie.supplytask.dto.LeaveConfigDTO;
import com.ruijie.supplytask.dto.MessageSwitchDTO;
import com.ruijie.supplytask.service.ConfigService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ConfigServiceImpl implements ConfigService {

    @Override
    public List<LeaveConfigDTO> getLeaveConfigs() {
        List<LeaveConfigDTO> list = new ArrayList<>();
        LeaveConfigDTO dto = new LeaveConfigDTO();
        dto.setUserId("U2001");
        dto.setAgentUserId("U3001");
        dto.setStartAt("2026-03-20T00:00:00+08:00");
        dto.setEndAt("2026-03-25T23:59:59+08:00");
        dto.setProductModels(List.of("RG-S6200-48XT8CQ", "RG-AP880"));
        list.add(dto);
        return list;
    }

    @Override
    public Boolean saveLeaveConfig(LeaveConfigDTO dto) {
        return Boolean.TRUE;
    }

    @Override
    public MessageSwitchDTO getMessageSwitch() {
        MessageSwitchDTO dto = new MessageSwitchDTO();
        dto.setTaskCreated(Boolean.TRUE);
        dto.setTaskTransfer(Boolean.TRUE);
        dto.setTaskOverdue(Boolean.TRUE);
        return dto;
    }

    @Override
    public Boolean saveMessageSwitch(MessageSwitchDTO dto) {
        return Boolean.TRUE;
    }
}
