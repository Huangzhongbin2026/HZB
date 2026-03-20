package com.ruijie.supplytask.controller;

import com.ruijie.supplytask.common.ApiResponse;
import com.ruijie.supplytask.common.TraceContext;
import com.ruijie.supplytask.dto.LeaveConfigDTO;
import com.ruijie.supplytask.dto.MessageSwitchDTO;
import com.ruijie.supplytask.service.ConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/supply/task/configs")
public class ConfigController {

    private final ConfigService configService;

    @GetMapping("/leave")
    public ApiResponse<List<LeaveConfigDTO>> getLeaveConfigs() {
        return ApiResponse.success(configService.getLeaveConfigs(), TraceContext.getTraceId());
    }

    @PutMapping("/leave")
    public ApiResponse<Boolean> saveLeaveConfig(@RequestBody LeaveConfigDTO dto) {
        return ApiResponse.success(configService.saveLeaveConfig(dto), TraceContext.getTraceId());
    }

    @GetMapping("/message-switch")
    public ApiResponse<MessageSwitchDTO> getMessageSwitch() {
        return ApiResponse.success(configService.getMessageSwitch(), TraceContext.getTraceId());
    }

    @PutMapping("/message-switch")
    public ApiResponse<Boolean> saveMessageSwitch(@RequestBody MessageSwitchDTO dto) {
        return ApiResponse.success(configService.saveMessageSwitch(dto), TraceContext.getTraceId());
    }
}
