package com.ruijie.supplysystem.service.impl;

import com.ruijie.supplysystem.dto.SysRoleDTO;
import com.ruijie.supplysystem.service.RoleService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class RoleServiceImpl implements RoleService {

    @Override
    public List<SysRoleDTO> list() {
        List<SysRoleDTO> list = new ArrayList<>();
        SysRoleDTO dto = new SysRoleDTO();
        dto.setId("1");
        dto.setRoleName("系统管理员");
        dto.setRoleCode("SYS_ADMIN");
        dto.setDeptCode("IT");
        dto.setIsEnabled(true);
        list.add(dto);
        return list;
    }

    @Override
    public Boolean save(SysRoleDTO dto) { return Boolean.TRUE; }

    @Override
    public Boolean update(String id, SysRoleDTO dto) { return Boolean.TRUE; }

    @Override
    public Boolean remove(String id) { return Boolean.TRUE; }

    @Override
    public Boolean savePermissions(String roleId, Map<String, Object> payload) { return Boolean.TRUE; }

    @Override
    public Map<String, Object> preview(String roleId) {
        Map<String, Object> preview = new HashMap<>();
        preview.put("menuCodes", List.of("sys:menu:view", "sys:user:view"));
        preview.put("buttonCodes", List.of("sys:user:resetPwd"));
        preview.put("dataScope", "ALL");
        return preview;
    }
}
