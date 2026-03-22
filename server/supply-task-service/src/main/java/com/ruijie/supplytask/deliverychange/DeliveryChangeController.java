package com.ruijie.supplytask.deliverychange;

import com.ruijie.supplytask.common.ApiResponse;
import com.ruijie.supplytask.common.TraceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/supply/task/delivery-change")
public class DeliveryChangeController {

    private final DeliveryChangeService deliveryChangeService;

    @PostMapping("/signature")
    public ApiResponse<Map<String, String>> signature(@RequestBody Map<String, String> body) {
        return ApiResponse.success(deliveryChangeService.generateSignature(body), TraceContext.getTraceId());
    }

    @PostMapping("/step1/analyze")
    public ApiResponse<Map<String, Object>> analyzeStep1(@RequestBody Map<String, Object> body,
                                                          @RequestHeader("sysId") String sysId,
                                                          @RequestHeader("sign-server-auth") String signServerAuth) {
        return ApiResponse.success(deliveryChangeService.analyzeStep1(body, sysId, signServerAuth), TraceContext.getTraceId());
    }

    @GetMapping("/duplicate-check")
    public ApiResponse<Map<String, Boolean>> duplicateCheck(@RequestParam String contractNo, @RequestParam String taskType) {
        return ApiResponse.success(Map.of("duplicated", deliveryChangeService.hasDuplicate(contractNo, taskType)), TraceContext.getTraceId());
    }

    @GetMapping("/step3/plan")
    public ApiResponse<Map<String, Object>> queryPlan(@RequestParam String crmNumber,
                                                      @RequestHeader("sysId") String sysId,
                                                      @RequestHeader("sign-server-auth") String signServerAuth) {
        return ApiResponse.success(deliveryChangeService.queryPlan(crmNumber, sysId, signServerAuth), TraceContext.getTraceId());
    }

    @PostMapping("/submit")
    public ApiResponse<Map<String, Object>> submit(@RequestBody Map<String, Object> body,
                                                    @RequestHeader("sysId") String sysId,
                                                    @RequestHeader("sign-server-auth") String signServerAuth) {
        return ApiResponse.success(deliveryChangeService.submit(body, sysId, signServerAuth), TraceContext.getTraceId());
    }

    @GetMapping("/management")
    public ApiResponse<Map<String, Object>> queryManagement(@RequestParam(defaultValue = "1") Integer pageNo,
                                                            @RequestParam(defaultValue = "10") Integer pageSize,
                                                            @RequestParam(required = false) String approvalNo,
                                                            @RequestParam(required = false) String contractNo,
                                                            @RequestParam(required = false) String taskType,
                                                            @RequestParam(required = false) String createdAt) {
        return ApiResponse.success(deliveryChangeService.queryManagement(pageNo, pageSize, approvalNo, contractNo, taskType, createdAt),
                TraceContext.getTraceId());
    }

    @PostMapping("/management/{approvalNo}/evaluation")
    public ApiResponse<Boolean> saveEvaluation(@PathVariable String approvalNo, @RequestBody Map<String, String> body) {
        String marketCode = body.getOrDefault("marketCode", "");
        String reply = body.getOrDefault("reply", "");
        return ApiResponse.success(deliveryChangeService.saveEvaluation(approvalNo, marketCode, reply), TraceContext.getTraceId());
    }

    @PostMapping("/management/{approvalNo}/urge-user")
    public ApiResponse<Boolean> urgeUser(@PathVariable String approvalNo) {
        return ApiResponse.success(deliveryChangeService.urgeUser(approvalNo), TraceContext.getTraceId());
    }

    @PostMapping("/management/{approvalNo}/urge-region")
    public ApiResponse<Boolean> urgeRegion(@PathVariable String approvalNo) {
        return ApiResponse.success(deliveryChangeService.urgeRegion(approvalNo), TraceContext.getTraceId());
    }
}
