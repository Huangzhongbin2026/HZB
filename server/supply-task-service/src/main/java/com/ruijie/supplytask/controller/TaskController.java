package com.ruijie.supplytask.controller;

import com.ruijie.supplytask.common.ApiResponse;
import com.ruijie.supplytask.common.TraceContext;
import com.ruijie.supplytask.dto.PageResult;
import com.ruijie.supplytask.dto.TaskCreateRequest;
import com.ruijie.supplytask.dto.TaskVO;
import com.ruijie.supplytask.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/supply/task/tasks")
public class TaskController {

    private final TaskService taskService;

    @PostMapping
    public ApiResponse<Map<String, String>> createTask(@Valid @RequestBody TaskCreateRequest request) {
        String taskId = taskService.createTask(request);
        return ApiResponse.success(Map.of("taskId", taskId), TraceContext.getTraceId());
    }

    @GetMapping
    public ApiResponse<PageResult<TaskVO>> queryTasks(@RequestParam(defaultValue = "1") Integer pageNo,
                                                       @RequestParam(defaultValue = "10") Integer pageSize,
                                                       @RequestParam(required = false) String keyword,
                                                       @RequestParam(required = false) String type,
                                                       @RequestParam(required = false) String status) {
        return ApiResponse.success(taskService.queryTasks(pageNo, pageSize, keyword, type, status), TraceContext.getTraceId());
    }

    @GetMapping("/{taskId}")
    public ApiResponse<TaskVO> getTaskDetail(@PathVariable String taskId) {
        return ApiResponse.success(taskService.getTaskDetail(taskId), TraceContext.getTraceId());
    }

    @PostMapping("/{taskId}/transfer")
    public ApiResponse<Boolean> transferTask(@PathVariable String taskId, @RequestBody Map<String, String> body) {
        return ApiResponse.success(taskService.transferTask(taskId, body.get("targetUserId")), TraceContext.getTraceId());
    }

    @PostMapping("/{taskId}/close")
    public ApiResponse<Boolean> closeTask(@PathVariable String taskId, @RequestBody Map<String, String> body) {
        return ApiResponse.success(taskService.closeTask(taskId, body.get("reason")), TraceContext.getTraceId());
    }
}
