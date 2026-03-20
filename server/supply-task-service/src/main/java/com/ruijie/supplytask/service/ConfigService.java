package com.ruijie.supplytask.service;

import com.ruijie.supplytask.dto.LeaveConfigDTO;
import com.ruijie.supplytask.dto.MessageSwitchDTO;

import java.util.List;

public interface ConfigService {

    List<LeaveConfigDTO> getLeaveConfigs();

    Boolean saveLeaveConfig(LeaveConfigDTO dto);

    MessageSwitchDTO getMessageSwitch();

    Boolean saveMessageSwitch(MessageSwitchDTO dto);
}
