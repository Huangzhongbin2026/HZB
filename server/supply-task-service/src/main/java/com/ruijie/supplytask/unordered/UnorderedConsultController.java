package com.ruijie.supplytask.unordered;

import com.ruijie.supplytask.common.ApiResponse;
import com.ruijie.supplytask.common.TraceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/supply/task/unordered-consult")
public class UnorderedConsultController {

    private final UnorderedConsultService unorderedConsultService;

    @PostMapping("/signature")
    public ApiResponse<Map<String, String>> signature(@RequestBody Map<String, String> body) {
        return ApiResponse.success(unorderedConsultService.generateSignature(body), TraceContext.getTraceId());
    }

    @PostMapping("/step1/analyze")
    public ApiResponse<Map<String, Object>> analyzeStep1(@RequestBody Map<String, Object> body,
                                                          @RequestHeader("sysId") String sysId,
                                                          @RequestHeader("sign-server-auth") String signServerAuth) {
        return ApiResponse.success(unorderedConsultService.analyzeStep1(body, sysId, signServerAuth), TraceContext.getTraceId());
    }

    @GetMapping("/step2/plan")
    public ApiResponse<Map<String, Object>> queryPlan(@RequestParam String crmNumber,
                                                      @RequestHeader("sysId") String sysId,
                                                      @RequestHeader("sign-server-auth") String signServerAuth) {
        return ApiResponse.success(unorderedConsultService.queryPlanByCrm(crmNumber, sysId, signServerAuth), TraceContext.getTraceId());
    }

    @GetMapping("/duplicate/today")
    public ApiResponse<Map<String, Object>> duplicateToday(@RequestParam String crmNumber) {
        return ApiResponse.success(unorderedConsultService.queryTodayDuplicate(crmNumber), TraceContext.getTraceId());
    }

    @PostMapping("/duplicate/today/join")
    public ApiResponse<Boolean> joinTodayDuplicate(@RequestBody Map<String, String> body) {
        return ApiResponse.success(unorderedConsultService.joinTodayDuplicate(body.get("crmNumber"), body.get("collaborator")), TraceContext.getTraceId());
    }

    @GetMapping("/duplicate/history")
    public ApiResponse<Map<String, Object>> duplicateHistory(@RequestParam String crmNumber) {
        return ApiResponse.success(unorderedConsultService.queryHistoryDuplicate(crmNumber), TraceContext.getTraceId());
    }

    @PostMapping("/submit")
    public ApiResponse<Map<String, Object>> submit(@RequestBody Map<String, Object> body,
                                                    @RequestHeader("sysId") String sysId,
                                                    @RequestHeader("sign-server-auth") String signServerAuth) {
        return ApiResponse.success(unorderedConsultService.submit(body, sysId, signServerAuth), TraceContext.getTraceId());
    }

    @GetMapping("/management")
    public ApiResponse<Map<String, Object>> queryManagement(@RequestParam(defaultValue = "1") Integer pageNo,
                                                            @RequestParam(defaultValue = "10") Integer pageSize,
                                                            @RequestParam(required = false) String taskNo,
                                                            @RequestParam(required = false) String crmNumber,
                                                            @RequestParam(required = false) String productModel,
                                                            @RequestParam(required = false) String createdAt) {
        return ApiResponse.success(unorderedConsultService.queryManagement(pageNo, pageSize, taskNo, crmNumber, productModel, createdAt),
                TraceContext.getTraceId());
    }

    @PostMapping("/management/{taskNo}/evaluation")
    public ApiResponse<Boolean> saveEvaluation(@PathVariable String taskNo, @RequestBody Map<String, Object> body) {
        String productModel = body.get("productModel") == null ? "" : String.valueOf(body.get("productModel"));
        String reply = body.get("reply") == null ? "" : String.valueOf(body.get("reply"));
        Integer quantity = body.get("quantity") instanceof Number n ? n.intValue() : null;
        return ApiResponse.success(unorderedConsultService.saveEvaluation(taskNo, productModel, reply, quantity), TraceContext.getTraceId());
    }

    @PostMapping("/management/{taskNo}/urge")
    public ApiResponse<Boolean> urge(@PathVariable String taskNo) {
        return ApiResponse.success(unorderedConsultService.urge(taskNo), TraceContext.getTraceId());
    }
}
