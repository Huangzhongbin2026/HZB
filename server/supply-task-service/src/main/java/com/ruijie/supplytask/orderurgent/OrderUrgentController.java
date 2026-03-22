package com.ruijie.supplytask.orderurgent;

import com.ruijie.supplytask.common.ApiResponse;
import com.ruijie.supplytask.common.TraceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/supply/task/order-urgent")
public class OrderUrgentController {

    private final OrderUrgentService orderUrgentService;

    @PostMapping("/signature")
    public ApiResponse<Map<String, String>> signature(@RequestBody Map<String, String> body) {
        return ApiResponse.success(orderUrgentService.generateSignature(body), TraceContext.getTraceId());
    }

    @PostMapping("/step1/analyze")
    public ApiResponse<Map<String, Object>> analyzeStep1(@RequestBody Map<String, Object> body,
                                                          @RequestHeader("sysId") String sysId,
                                                          @RequestHeader("sign-server-auth") String signServerAuth) {
        return ApiResponse.success(orderUrgentService.analyzeStep1(body, sysId, signServerAuth), TraceContext.getTraceId());
    }

    @GetMapping("/duplicate-check")
    public ApiResponse<Map<String, Boolean>> duplicateCheck(@RequestParam String contractNo) {
        return ApiResponse.success(Map.of("duplicated", orderUrgentService.hasDuplicate(contractNo)), TraceContext.getTraceId());
    }

    @PostMapping("/step3/plan")
    public ApiResponse<Map<String, Object>> queryPlan(@RequestBody Map<String, String> body,
                                                      @RequestHeader("sysId") String sysId,
                                                      @RequestHeader("sign-server-auth") String signServerAuth) {
        return ApiResponse.success(orderUrgentService.queryPlan(body.get("crmNumber"), sysId, signServerAuth), TraceContext.getTraceId());
    }

    @PostMapping("/submit")
    public ApiResponse<Map<String, String>> submit(@RequestBody Map<String, Object> body,
                                                   @RequestHeader("sysId") String sysId,
                                                   @RequestHeader("sign-server-auth") String signServerAuth,
                                                   @RequestHeader(value = "Feishu-Authorization", required = false) String feishuAuthorization) {
        return ApiResponse.success(orderUrgentService.submit(body, sysId, signServerAuth, feishuAuthorization), TraceContext.getTraceId());
    }

    @GetMapping("/management")
    public ApiResponse<Map<String, Object>> queryManagement(@RequestParam(defaultValue = "1") Integer pageNo,
                                                            @RequestParam(defaultValue = "10") Integer pageSize,
                                                            @RequestParam(required = false) String contractNo,
                                                            @RequestParam(required = false) String orderNo,
                                                            @RequestParam(required = false) String projectName,
                                                            @RequestParam(required = false) String createdAt) {
        return ApiResponse.success(orderUrgentService.queryManagement(pageNo, pageSize, contractNo, orderNo, projectName, createdAt),
                TraceContext.getTraceId());
    }

    @PostMapping("/management/{taskNo}/evaluation")
    public ApiResponse<Boolean> saveEvaluation(@PathVariable String taskNo, @RequestBody Map<String, String> body) {
        return ApiResponse.success(orderUrgentService.saveEvaluation(taskNo, body.get("reply")), TraceContext.getTraceId());
    }

    @PostMapping("/management/{taskNo}/urge-user")
    public ApiResponse<Boolean> urgeUser(@PathVariable String taskNo) {
        return ApiResponse.success(Boolean.TRUE, TraceContext.getTraceId());
    }

    @PostMapping("/management/{taskNo}/urge-region")
    public ApiResponse<Boolean> urgeRegion(@PathVariable String taskNo) {
        return ApiResponse.success(Boolean.TRUE, TraceContext.getTraceId());
    }
}
