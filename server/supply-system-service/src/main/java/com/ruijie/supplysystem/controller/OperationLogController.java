package com.ruijie.supplysystem.controller;

import com.ruijie.supplysystem.common.ApiResponse;
import com.ruijie.supplysystem.dto.PageResult;
import com.ruijie.supplysystem.dto.SysOperationLogDTO;
import com.ruijie.supplysystem.security.RequirePermission;
import com.ruijie.supplysystem.service.OperationLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/supply/system/logs")
public class OperationLogController {

    private final OperationLogService operationLogService;

    @GetMapping
    @RequirePermission("sys:log:view")
    public ApiResponse<PageResult<SysOperationLogDTO>> query(@RequestParam(defaultValue = "1") Integer pageNo,
                                                             @RequestParam(defaultValue = "10") Integer pageSize,
                                                             @RequestParam(required = false) String operUser,
                                                             @RequestParam(required = false) String operType,
                                                             @RequestParam(required = false) String operModule,
                                                             @RequestParam(required = false) String operIp,
                                                             @RequestParam(required = false) String keyword,
                                                             @RequestParam(required = false) String startTime,
                                                             @RequestParam(required = false) String endTime) {
        return ApiResponse.success(operationLogService.query(pageNo, pageSize, operUser, operType, operModule, operIp, keyword, startTime, endTime));
    }

    @GetMapping("/export")
    @RequirePermission("sys:log:export")
    public ApiResponse<Boolean> export() {
        return ApiResponse.success(Boolean.TRUE);
    }

    @PostMapping("/clean")
    @RequirePermission("sys:log:clean")
    public ApiResponse<Boolean> clean(@RequestBody SysOperationLogDTO dto) {
        return ApiResponse.success(operationLogService.clean(dto.getOperTime(), dto.getRemark()));
    }

    @PostMapping("/record")
    public ApiResponse<Boolean> record(@RequestBody SysOperationLogDTO dto) {
        return ApiResponse.success(operationLogService.record(dto));
    }

    @PostMapping("/push/third-party")
    public ApiResponse<Boolean> pushThirdParty() {
        return ApiResponse.success(Boolean.TRUE);
    }
}
