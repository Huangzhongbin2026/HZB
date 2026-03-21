package com.ruijie.supplysystem.auxiliary.controller;

import com.ruijie.supplysystem.auxiliary.dto.AreaCoordinatorDTO;
import com.ruijie.supplysystem.auxiliary.dto.LeaveAgentProductDTO;
import com.ruijie.supplysystem.auxiliary.dto.LeaveConfigDTO;
import com.ruijie.supplysystem.auxiliary.dto.MessagePushDTO;
import com.ruijie.supplysystem.auxiliary.dto.VirtualProductDTO;
import com.ruijie.supplysystem.auxiliary.service.AuxiliaryService;
import com.ruijie.supplysystem.common.ApiResponse;
import com.ruijie.supplysystem.dto.PageResult;
import com.ruijie.supplysystem.log.SysLog;
import com.ruijie.supplysystem.security.RequirePermission;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/supply/system/aux")
public class AuxiliaryController {

    private final AuxiliaryService auxiliaryService;

    @GetMapping("/leave-configs")
    @RequirePermission("aux:leave:view")
    public ApiResponse<PageResult<LeaveConfigDTO>> queryLeaveConfigs(@RequestParam(defaultValue = "1") Integer pageNo,
                                                                     @RequestParam(defaultValue = "10") Integer pageSize,
                                                                     @RequestParam(required = false) String userId,
                                                                     @RequestParam(required = false) String userName) {
        return ApiResponse.success(auxiliaryService.queryLeaveConfigs(pageNo, pageSize, userId, userName));
    }

    @PostMapping("/leave-configs")
    @SysLog(module = "AUX", type = "CREATE", content = "新增请假配置")
    @RequirePermission("aux:leave:add")
    public ApiResponse<Boolean> saveLeaveConfig(@RequestBody LeaveConfigDTO dto) {
        return ApiResponse.success(auxiliaryService.saveLeaveConfig(dto));
    }

    @PutMapping("/leave-configs/{id}")
    @SysLog(module = "AUX", type = "UPDATE", content = "更新请假配置")
    @RequirePermission("aux:leave:edit")
    public ApiResponse<Boolean> updateLeaveConfig(@PathVariable String id, @RequestBody LeaveConfigDTO dto) {
        return ApiResponse.success(auxiliaryService.updateLeaveConfig(id, dto));
    }

    @DeleteMapping("/leave-configs/{id}")
    @SysLog(module = "AUX", type = "DELETE", content = "删除请假配置")
    @RequirePermission("aux:leave:delete")
    public ApiResponse<Boolean> deleteLeaveConfig(@PathVariable String id) {
        return ApiResponse.success(auxiliaryService.deleteLeaveConfig(id));
    }

    @PostMapping("/leave-configs/import")
    @SysLog(module = "AUX", type = "IMPORT", content = "导入请假配置")
    @RequirePermission("aux:leave:import")
    public ApiResponse<Boolean> importLeaveConfigs(@RequestBody List<LeaveConfigDTO> list) {
        return ApiResponse.success(auxiliaryService.importLeaveConfigs(list));
    }

    @GetMapping("/leave-configs/export")
    @SysLog(module = "AUX", type = "EXPORT", content = "导出请假配置")
    @RequirePermission("aux:leave:export")
    public ResponseEntity<byte[]> exportLeaveConfigs() {
        return exportCsv("leave-configs.csv", "userId,userName,leaveStart,leaveEnd\nU1001,张三,2026-01-01 09:00:00,2026-01-03 18:00:00\n");
    }

    @GetMapping("/leave-configs/match/by-user")
    @RequirePermission("aux:leave:match")
    public ApiResponse<Map<String, Object>> matchLeaveByUser(@RequestParam String userId,
                                                              @RequestParam(required = false) String date) {
        return ApiResponse.success(auxiliaryService.matchLeaveByUser(userId, date));
    }

    @GetMapping("/virtual-products")
    @RequirePermission("aux:virtual:view")
    public ApiResponse<PageResult<VirtualProductDTO>> queryVirtualProducts(@RequestParam(defaultValue = "1") Integer pageNo,
                                                                            @RequestParam(defaultValue = "10") Integer pageSize,
                                                                            @RequestParam(required = false) String productModel) {
        return ApiResponse.success(auxiliaryService.queryVirtualProducts(pageNo, pageSize, productModel));
    }

    @PostMapping("/virtual-products")
    @SysLog(module = "AUX", type = "CREATE", content = "新增虚拟产品")
    @RequirePermission("aux:virtual:add")
    public ApiResponse<Boolean> saveVirtualProduct(@RequestBody VirtualProductDTO dto) {
        return ApiResponse.success(auxiliaryService.saveVirtualProduct(dto));
    }

    @PutMapping("/virtual-products/{id}")
    @SysLog(module = "AUX", type = "UPDATE", content = "更新虚拟产品")
    @RequirePermission("aux:virtual:edit")
    public ApiResponse<Boolean> updateVirtualProduct(@PathVariable String id, @RequestBody VirtualProductDTO dto) {
        return ApiResponse.success(auxiliaryService.updateVirtualProduct(id, dto));
    }

    @DeleteMapping("/virtual-products/{id}")
    @SysLog(module = "AUX", type = "DELETE", content = "删除虚拟产品")
    @RequirePermission("aux:virtual:delete")
    public ApiResponse<Boolean> deleteVirtualProduct(@PathVariable String id) {
        return ApiResponse.success(auxiliaryService.deleteVirtualProduct(id));
    }

    @PostMapping("/virtual-products/import")
    @SysLog(module = "AUX", type = "IMPORT", content = "导入虚拟产品")
    @RequirePermission("aux:virtual:import")
    public ApiResponse<Boolean> importVirtualProducts(@RequestBody List<VirtualProductDTO> list) {
        return ApiResponse.success(auxiliaryService.importVirtualProducts(list));
    }

    @GetMapping("/virtual-products/export")
    @SysLog(module = "AUX", type = "EXPORT", content = "导出虚拟产品")
    @RequirePermission("aux:virtual:export")
    public ResponseEntity<byte[]> exportVirtualProducts() {
        return exportCsv("virtual-products.csv", "productModel,autoReplyContent\nVX-100,该型号属于虚拟产品\n");
    }

    @GetMapping("/virtual-products/match")
    @RequirePermission("aux:virtual:match")
    public ApiResponse<Map<String, Object>> matchVirtualProduct(@RequestParam String productModel) {
        return ApiResponse.success(auxiliaryService.matchVirtualProduct(productModel));
    }

    @GetMapping("/message-pushes")
    @RequirePermission("aux:message:view")
    public ApiResponse<PageResult<MessagePushDTO>> queryMessagePushes(@RequestParam(defaultValue = "1") Integer pageNo,
                                                                       @RequestParam(defaultValue = "10") Integer pageSize,
                                                                       @RequestParam(required = false) String pushName,
                                                                       @RequestParam(required = false) String routeCode) {
        return ApiResponse.success(auxiliaryService.queryMessagePushes(pageNo, pageSize, pushName, routeCode));
    }

    @PostMapping("/message-pushes")
    @SysLog(module = "AUX", type = "CREATE", content = "新增消息推送")
    @RequirePermission("aux:message:add")
    public ApiResponse<Boolean> saveMessagePush(@RequestBody MessagePushDTO dto) {
        return ApiResponse.success(auxiliaryService.saveMessagePush(dto));
    }

    @PutMapping("/message-pushes/{id}")
    @SysLog(module = "AUX", type = "UPDATE", content = "更新消息推送")
    @RequirePermission("aux:message:edit")
    public ApiResponse<Boolean> updateMessagePush(@PathVariable String id, @RequestBody MessagePushDTO dto) {
        return ApiResponse.success(auxiliaryService.updateMessagePush(id, dto));
    }

    @DeleteMapping("/message-pushes/{id}")
    @SysLog(module = "AUX", type = "DELETE", content = "删除消息推送")
    @RequirePermission("aux:message:delete")
    public ApiResponse<Boolean> deleteMessagePush(@PathVariable String id) {
        return ApiResponse.success(auxiliaryService.deleteMessagePush(id));
    }

    @PostMapping("/message-pushes/import")
    @SysLog(module = "AUX", type = "IMPORT", content = "导入消息推送")
    @RequirePermission("aux:message:import")
    public ApiResponse<Boolean> importMessagePushes(@RequestBody List<MessagePushDTO> list) {
        return ApiResponse.success(auxiliaryService.importMessagePushes(list));
    }

    @GetMapping("/message-pushes/export")
    @SysLog(module = "AUX", type = "EXPORT", content = "导出消息推送")
    @RequirePermission("aux:message:export")
    public ResponseEntity<byte[]> exportMessagePushes() {
        return exportCsv("message-pushes.csv", "pushName,routeCode,isEnabled\n请假代理变更通知,LEAVE_AGENT_CHANGE,true\n");
    }

    @GetMapping("/leave-agent-products")
    @RequirePermission("aux:agent:view")
    public ApiResponse<PageResult<LeaveAgentProductDTO>> queryLeaveAgentProducts(@RequestParam(defaultValue = "1") Integer pageNo,
                                                                                   @RequestParam(defaultValue = "10") Integer pageSize,
                                                                                   @RequestParam(required = false) String productModel,
                                                                                   @RequestParam(required = false) String originalUserName) {
        return ApiResponse.success(auxiliaryService.queryLeaveAgentProducts(pageNo, pageSize, productModel, originalUserName));
    }

    @PostMapping("/leave-agent-products")
    @SysLog(module = "AUX", type = "CREATE", content = "新增请假代理产品")
    @RequirePermission("aux:agent:add")
    public ApiResponse<Boolean> saveLeaveAgentProduct(@RequestBody LeaveAgentProductDTO dto) {
        return ApiResponse.success(auxiliaryService.saveLeaveAgentProduct(dto));
    }

    @PutMapping("/leave-agent-products/{id}")
    @SysLog(module = "AUX", type = "UPDATE", content = "更新请假代理产品")
    @RequirePermission("aux:agent:edit")
    public ApiResponse<Boolean> updateLeaveAgentProduct(@PathVariable String id, @RequestBody LeaveAgentProductDTO dto) {
        return ApiResponse.success(auxiliaryService.updateLeaveAgentProduct(id, dto));
    }

    @DeleteMapping("/leave-agent-products/{id}")
    @SysLog(module = "AUX", type = "DELETE", content = "删除请假代理产品")
    @RequirePermission("aux:agent:delete")
    public ApiResponse<Boolean> deleteLeaveAgentProduct(@PathVariable String id) {
        return ApiResponse.success(auxiliaryService.deleteLeaveAgentProduct(id));
    }

    @PostMapping("/leave-agent-products/import")
    @SysLog(module = "AUX", type = "IMPORT", content = "导入请假代理产品")
    @RequirePermission("aux:agent:import")
    public ApiResponse<Boolean> importLeaveAgentProducts(@RequestBody List<LeaveAgentProductDTO> list) {
        return ApiResponse.success(auxiliaryService.importLeaveAgentProducts(list));
    }

    @GetMapping("/leave-agent-products/export")
    @SysLog(module = "AUX", type = "EXPORT", content = "导出请假代理产品")
    @RequirePermission("aux:agent:export")
    public ResponseEntity<byte[]> exportLeaveAgentProducts() {
        return exportCsv("leave-agent-products.csv", "productModel,originalUserName,agentUserName\nRG-AP180,张三,李四\n");
    }

    @GetMapping("/leave-agent-products/match")
    @RequirePermission("aux:agent:match")
    public ApiResponse<Map<String, Object>> matchLeaveAgentProduct(@RequestParam String productModel,
                                                                    @RequestParam String originalUserId) {
        return ApiResponse.success(auxiliaryService.matchLeaveAgentProduct(productModel, originalUserId));
    }

    @GetMapping("/area-coordinators")
    @RequirePermission("aux:area:view")
    public ApiResponse<PageResult<AreaCoordinatorDTO>> queryAreaCoordinators(@RequestParam(defaultValue = "1") Integer pageNo,
                                                                               @RequestParam(defaultValue = "10") Integer pageSize,
                                                                               @RequestParam(required = false) String saleDeptCode,
                                                                               @RequestParam(required = false) String provinceCode,
                                                                               @RequestParam(required = false) String coordinatorUserName) {
        return ApiResponse.success(auxiliaryService.queryAreaCoordinators(pageNo, pageSize, saleDeptCode, provinceCode, coordinatorUserName));
    }

    @PostMapping("/area-coordinators")
    @SysLog(module = "AUX", type = "CREATE", content = "新增区域统筹")
    @RequirePermission("aux:area:add")
    public ApiResponse<Boolean> saveAreaCoordinator(@RequestBody AreaCoordinatorDTO dto) {
        return ApiResponse.success(auxiliaryService.saveAreaCoordinator(dto));
    }

    @PutMapping("/area-coordinators/{id}")
    @SysLog(module = "AUX", type = "UPDATE", content = "更新区域统筹")
    @RequirePermission("aux:area:edit")
    public ApiResponse<Boolean> updateAreaCoordinator(@PathVariable String id, @RequestBody AreaCoordinatorDTO dto) {
        return ApiResponse.success(auxiliaryService.updateAreaCoordinator(id, dto));
    }

    @DeleteMapping("/area-coordinators/{id}")
    @SysLog(module = "AUX", type = "DELETE", content = "删除区域统筹")
    @RequirePermission("aux:area:delete")
    public ApiResponse<Boolean> deleteAreaCoordinator(@PathVariable String id) {
        return ApiResponse.success(auxiliaryService.deleteAreaCoordinator(id));
    }

    @PostMapping("/area-coordinators/import")
    @SysLog(module = "AUX", type = "IMPORT", content = "导入区域统筹")
    @RequirePermission("aux:area:import")
    public ApiResponse<Boolean> importAreaCoordinators(@RequestBody List<AreaCoordinatorDTO> list) {
        return ApiResponse.success(auxiliaryService.importAreaCoordinators(list));
    }

    @GetMapping("/area-coordinators/export")
    @SysLog(module = "AUX", type = "EXPORT", content = "导出区域统筹")
    @RequirePermission("aux:area:export")
    public ResponseEntity<byte[]> exportAreaCoordinators() {
        return exportCsv("area-coordinators.csv", "saleDeptCode,provinceCode,coordinatorUserName,priorityNo\nSOUTH-A,GD,王五,100\n");
    }

    @GetMapping("/area-coordinators/match")
    @RequirePermission("aux:area:match")
    public ApiResponse<Map<String, Object>> matchAreaCoordinator(@RequestParam(required = false) String saleDeptCode,
                                                                  @RequestParam(required = false) String provinceCode,
                                                                  @RequestParam(required = false) String deptKeyword,
                                                                  @RequestParam(required = false) String projectKeyword) {
        return ApiResponse.success(auxiliaryService.matchAreaCoordinator(saleDeptCode, provinceCode, deptKeyword, projectKeyword));
    }

    @GetMapping("/cache/refresh")
    @RequirePermission("aux:cache:refresh")
    public ApiResponse<Map<String, Object>> refreshCache() {
        return ApiResponse.success(Map.of("refreshedAt", LocalDateTime.now().toString(), "message", "缓存刷新成功"));
    }

    private ResponseEntity<byte[]> exportCsv(String fileName, String content) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDisposition(ContentDisposition.attachment().filename(fileName, StandardCharsets.UTF_8).build());
        return ResponseEntity.ok().headers(headers).body(content.getBytes(StandardCharsets.UTF_8));
    }
}
