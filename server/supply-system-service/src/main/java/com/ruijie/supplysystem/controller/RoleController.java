package com.ruijie.supplysystem.controller;

import com.ruijie.supplysystem.common.ApiResponse;
import com.ruijie.supplysystem.dto.SysRoleDTO;
import com.ruijie.supplysystem.log.SysLog;
import com.ruijie.supplysystem.security.RequirePermission;
import com.ruijie.supplysystem.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/supply/system/roles")
public class RoleController {

    private final RoleService roleService;

    @GetMapping
    @RequirePermission("sys:role:view")
    public ApiResponse<List<SysRoleDTO>> list() {
        return ApiResponse.success(roleService.list());
    }

    @PostMapping
    @SysLog(module = "ROLE", type = "CREATE", content = "新增角色")
    @RequirePermission("sys:role:add")
    public ApiResponse<Boolean> save(@RequestBody SysRoleDTO dto) {
        return ApiResponse.success(roleService.save(dto));
    }

    @PutMapping("/{id}")
    @SysLog(module = "ROLE", type = "UPDATE", content = "更新角色")
    @RequirePermission("sys:role:edit")
    public ApiResponse<Boolean> update(@PathVariable String id, @RequestBody SysRoleDTO dto) {
        return ApiResponse.success(roleService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @SysLog(module = "ROLE", type = "DELETE", content = "删除角色")
    @RequirePermission("sys:role:delete")
    public ApiResponse<Boolean> remove(@PathVariable String id) {
        return ApiResponse.success(roleService.remove(id));
    }

    @PostMapping("/{id}/permissions")
    @SysLog(module = "ROLE", type = "UPDATE", content = "配置角色权限")
    @RequirePermission("sys:role:permission")
    public ApiResponse<Boolean> savePermissions(@PathVariable String id, @RequestBody Map<String, Object> payload) {
        return ApiResponse.success(roleService.savePermissions(id, payload));
    }

    @GetMapping("/{id}/permissions/preview")
    @RequirePermission("sys:role:view")
    public ApiResponse<Map<String, Object>> preview(@PathVariable String id) {
        return ApiResponse.success(roleService.preview(id));
    }

    @PostMapping("/permissions/batch")
    @RequirePermission("sys:role:permission")
    public ApiResponse<Boolean> batch(@RequestBody Map<String, Object> payload) {
        return ApiResponse.success(Boolean.TRUE);
    }
}
