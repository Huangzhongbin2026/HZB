package com.ruijie.supplysystem.service;

import com.ruijie.supplysystem.dto.SysUserDTO;

import java.util.List;
import java.util.Map;

public interface UserService {
    List<SysUserDTO> list();
    Boolean save(SysUserDTO dto);
    Boolean update(String id, SysUserDTO dto);
    Boolean remove(String id);
    Boolean assignRoles(String userId, List<String> roleIds);
    Boolean resetPassword(String userId);
    Map<String, Object> permissionSnapshot();
}
