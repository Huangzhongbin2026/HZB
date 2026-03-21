package com.ruijie.supplysystem.controller;

import com.ruijie.supplysystem.common.ApiResponse;
import com.ruijie.supplysystem.dto.SysDictItemDTO;
import com.ruijie.supplysystem.dto.SysDictTypeDTO;
import com.ruijie.supplysystem.log.SysLog;
import com.ruijie.supplysystem.security.RequirePermission;
import com.ruijie.supplysystem.service.DictService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/supply/system/dicts")
public class DictController {

    private final DictService dictService;

    @GetMapping("/types")
    @RequirePermission("sys:dict:view")
    public ApiResponse<List<SysDictTypeDTO>> listTypes() {
        return ApiResponse.success(dictService.listTypes());
    }

    @PostMapping("/types")
    @SysLog(module = "DICT", type = "CREATE", content = "新增字典分类")
    @RequirePermission("sys:dict:add")
    public ApiResponse<Boolean> saveType(@RequestBody SysDictTypeDTO dto) {
        return ApiResponse.success(dictService.saveType(dto));
    }

    @GetMapping("/items")
    @RequirePermission("sys:dict:view")
    public ApiResponse<List<SysDictItemDTO>> listItems(@RequestParam String typeCode,
                                                       @RequestParam(required = false, defaultValue = "") String keyword) {
        return ApiResponse.success(dictService.listItems(typeCode, keyword));
    }

    @PostMapping("/items")
    @SysLog(module = "DICT", type = "CREATE", content = "新增字典项")
    @RequirePermission("sys:dict:add")
    public ApiResponse<Boolean> saveItem(@RequestBody SysDictItemDTO dto) {
        return ApiResponse.success(dictService.saveItem(dto));
    }
}
