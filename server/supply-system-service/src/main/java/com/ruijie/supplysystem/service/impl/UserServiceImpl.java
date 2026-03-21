package com.ruijie.supplysystem.service.impl;

import com.ruijie.supplysystem.dto.SysUserDTO;
import com.ruijie.supplysystem.service.UserService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class UserServiceImpl implements UserService {

    @Override
    public List<SysUserDTO> list() {
        List<SysUserDTO> list = new ArrayList<>();
        SysUserDTO dto = new SysUserDTO();
        dto.setId("1");
        dto.setUserName("管理员");
        dto.setAccount("admin");
        dto.setMobile("13800000000");
        dto.setFeishuId("ou_admin");
        dto.setDeptCode("IT");
        dto.setStatus(true);
        dto.setLastLoginAt("2026-03-21T09:30:00+08:00");
        list.add(dto);
        return list;
    }

    @Override
    public Boolean save(SysUserDTO dto) { return Boolean.TRUE; }

    @Override
    public Boolean update(String id, SysUserDTO dto) { return Boolean.TRUE; }

    @Override
    public Boolean remove(String id) { return Boolean.TRUE; }

    @Override
    public Boolean assignRoles(String userId, List<String> roleIds) { return Boolean.TRUE; }

    @Override
    public Boolean resetPassword(String userId) { return Boolean.TRUE; }

    @Override
    public Map<String, Object> permissionSnapshot() {
        Map<String, Object> result = new HashMap<>();
        result.put("menuCodes", List.of("sys:menu:view", "sys:dict:view", "sys:role:view", "sys:user:view", "sys:log:view"));
        result.put("buttonCodes", List.of("sys:user:assignRole", "sys:user:resetPwd"));
        result.put("dataScopes", Map.of("t_task_main", "ALL"));
        result.put("fieldPermissions", Map.of("t_task_main", Map.of("customer_price", "HIDDEN", "title", "EDITABLE")));
        return result;
    }
}
