package com.ruijie.supplysystem.service;

import com.ruijie.supplysystem.dto.SysRoleDTO;

import java.util.List;
import java.util.Map;

public interface RoleService {
    List<SysRoleDTO> list();
    Boolean save(SysRoleDTO dto);
    Boolean update(String id, SysRoleDTO dto);
    Boolean remove(String id);
    Boolean savePermissions(String roleId, Map<String, Object> payload);
    Map<String, Object> preview(String roleId);
}
