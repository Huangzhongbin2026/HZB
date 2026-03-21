package com.ruijie.supplysystem.controller;

import com.ruijie.supplysystem.common.ApiResponse;
import com.ruijie.supplysystem.dto.SysMenuDTO;
import com.ruijie.supplysystem.log.SysLog;
import com.ruijie.supplysystem.security.RequirePermission;
import com.ruijie.supplysystem.service.MenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/supply/system/menus")
public class MenuController {

    private final MenuService menuService;

    @GetMapping("/tree")
    @RequirePermission("sys:menu:view")
    public ApiResponse<List<SysMenuDTO>> tree() {
        return ApiResponse.success(menuService.tree());
    }

    @PostMapping
    @SysLog(module = "MENU", type = "CREATE", content = "新增菜单")
    @RequirePermission("sys:menu:add")
    public ApiResponse<Boolean> create(@RequestBody SysMenuDTO dto) {
        return ApiResponse.success(menuService.create(dto));
    }

    @PutMapping("/{id}")
    @SysLog(module = "MENU", type = "UPDATE", content = "更新菜单")
    @RequirePermission("sys:menu:edit")
    public ApiResponse<Boolean> update(@PathVariable String id, @RequestBody SysMenuDTO dto) {
        return ApiResponse.success(menuService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @SysLog(module = "MENU", type = "DELETE", content = "删除菜单")
    @RequirePermission("sys:menu:delete")
    public ApiResponse<Boolean> remove(@PathVariable String id) {
        return ApiResponse.success(menuService.remove(id));
    }

    @PostMapping("/batch-enable")
    @RequirePermission("sys:menu:edit")
    public ApiResponse<Boolean> batchEnable(@RequestBody Map<String, Object> body) {
        List<String> ids = (List<String>) body.get("ids");
        Boolean enabled = (Boolean) body.get("enabled");
        return ApiResponse.success(menuService.batchEnable(ids, enabled));
    }
}
