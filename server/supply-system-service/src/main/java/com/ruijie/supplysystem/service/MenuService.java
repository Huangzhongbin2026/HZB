package com.ruijie.supplysystem.service;

import com.ruijie.supplysystem.dto.SysMenuDTO;

import java.util.List;

public interface MenuService {
    List<SysMenuDTO> tree();
    Boolean create(SysMenuDTO dto);
    Boolean update(String id, SysMenuDTO dto);
    Boolean remove(String id);
    Boolean batchEnable(List<String> ids, Boolean enabled);
}
