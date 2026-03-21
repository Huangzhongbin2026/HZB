package com.ruijie.supplysystem.controller;

import com.ruijie.supplysystem.common.ApiResponse;
import com.ruijie.supplysystem.dto.SysUserDTO;
import com.ruijie.supplysystem.log.SysLog;
import com.ruijie.supplysystem.security.RequirePermission;
import com.ruijie.supplysystem.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/supply/system/users")
public class UserController {

    private final UserService userService;

    @GetMapping
    @RequirePermission("sys:user:view")
    public ApiResponse<List<SysUserDTO>> list() {
        return ApiResponse.success(userService.list());
    }

    @PostMapping
    @SysLog(module = "USER", type = "CREATE", content = "新增用户")
    @RequirePermission("sys:user:add")
    public ApiResponse<Boolean> save(@RequestBody SysUserDTO dto) {
        return ApiResponse.success(userService.save(dto));
    }

    @PutMapping("/{id}")
    @SysLog(module = "USER", type = "UPDATE", content = "更新用户")
    @RequirePermission("sys:user:edit")
    public ApiResponse<Boolean> update(@PathVariable String id, @RequestBody SysUserDTO dto) {
        return ApiResponse.success(userService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @SysLog(module = "USER", type = "DELETE", content = "删除用户")
    @RequirePermission("sys:user:delete")
    public ApiResponse<Boolean> remove(@PathVariable String id) {
        return ApiResponse.success(userService.remove(id));
    }

    @PostMapping("/{id}/roles")
    @RequirePermission("sys:user:assignRole")
    public ApiResponse<Boolean> assignRoles(@PathVariable String id, @RequestBody Map<String, List<String>> payload) {
        return ApiResponse.success(userService.assignRoles(id, payload.get("roleIds")));
    }

    @PostMapping("/{id}/reset-password")
    @RequirePermission("sys:user:resetPwd")
    public ApiResponse<Boolean> resetPassword(@PathVariable String id) {
        return ApiResponse.success(userService.resetPassword(id));
    }

    @GetMapping("/me/permissions")
    public ApiResponse<Map<String, Object>> permissionSnapshot() {
        return ApiResponse.success(userService.permissionSnapshot());
    }

    @PostMapping("/sync/feishu")
    public ApiResponse<Boolean> syncFeishu() {
        return ApiResponse.success(Boolean.TRUE);
    }
}
