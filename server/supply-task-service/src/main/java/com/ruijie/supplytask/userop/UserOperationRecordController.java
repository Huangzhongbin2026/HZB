package com.ruijie.supplytask.userop;

import com.ruijie.supplytask.common.ApiResponse;
import com.ruijie.supplytask.common.TraceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/supply/task/user-operation")
public class UserOperationRecordController {

    private final UserOperationRecordService userOperationRecordService;

    @PostMapping("/record")
    public ApiResponse<Boolean> record(@RequestBody Map<String, Object> body) {
        return ApiResponse.success(userOperationRecordService.record(body), TraceContext.getTraceId());
    }

    @GetMapping("/list")
    public ApiResponse<Map<String, Object>> list(@RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                                 @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                                                 @RequestParam(name = "requester", required = false) String requester,
                                                 @RequestParam(name = "feishuId", required = false) String feishuId,
                                                 @RequestParam(name = "flowType", required = false) String flowType,
                                                 @RequestParam(name = "status", required = false) String status,
                                                 @RequestParam(name = "createdAt", required = false) String createdAt) {
        return ApiResponse.success(userOperationRecordService.list(pageNo, pageSize, requester, feishuId, flowType, status, createdAt),
                TraceContext.getTraceId());
    }
}
