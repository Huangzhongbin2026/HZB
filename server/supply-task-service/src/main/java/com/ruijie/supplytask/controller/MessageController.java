package com.ruijie.supplytask.controller;

import com.ruijie.supplytask.common.ApiResponse;
import com.ruijie.supplytask.common.TraceContext;
import com.ruijie.supplytask.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/supply/task/messages")
public class MessageController {

    private final MessageService messageService;

    @PostMapping("/push")
    public ApiResponse<Boolean> push(@RequestBody Map<String, String> body) {
        String taskId = body.getOrDefault("taskId", "");
        String eventType = body.getOrDefault("eventType", "TASK_CREATED");
        return ApiResponse.success(messageService.pushTaskMessage(taskId, eventType), TraceContext.getTraceId());
    }
}
