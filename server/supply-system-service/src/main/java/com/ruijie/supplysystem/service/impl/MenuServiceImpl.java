package com.ruijie.supplysystem.service.impl;

import com.ruijie.supplysystem.dto.SysMenuDTO;
import com.ruijie.supplysystem.service.MenuService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class MenuServiceImpl implements MenuService {

    @Override
    public List<SysMenuDTO> tree() {
        List<SysMenuDTO> list = new ArrayList<>();
        SysMenuDTO dto = new SysMenuDTO();
        dto.setId("1");
        dto.setMenuName("系统管理");
        dto.setMenuType("DIR");
        dto.setParentId("0");
        dto.setRoutePath("/system");
        dto.setPermissionCode("sys:root");
        dto.setSortNo(1);
        dto.setIsVisible(true);
        dto.setIsEnabled(true);
        list.add(dto);
        return list;
    }

    @Override
    public Boolean create(SysMenuDTO dto) { return Boolean.TRUE; }

    @Override
    public Boolean update(String id, SysMenuDTO dto) { return Boolean.TRUE; }

    @Override
    public Boolean remove(String id) { return Boolean.TRUE; }

    @Override
    public Boolean batchEnable(List<String> ids, Boolean enabled) { return Boolean.TRUE; }
}
