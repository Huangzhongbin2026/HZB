package com.ruijie.supplysystem.service.impl;

import com.ruijie.supplysystem.common.BusinessException;
import com.ruijie.supplysystem.dto.SysMenuDTO;
import com.ruijie.supplysystem.mapper.MenuMapper;
import com.ruijie.supplysystem.service.MenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MenuServiceImpl implements MenuService {

    private final MenuMapper menuMapper;

    @Override
    public List<SysMenuDTO> tree() {
        return menuMapper.listTree();
    }

    @Override
    public Boolean create(SysMenuDTO dto) {
        return menuMapper.insert(dto) > 0;
    }

    @Override
    public Boolean update(String id, SysMenuDTO dto) {
        return menuMapper.updateById(parseId(id), dto) > 0;
    }

    @Override
    public Boolean remove(String id) {
        return menuMapper.softDelete(parseId(id)) > 0;
    }

    @Override
    public Boolean batchEnable(List<String> ids, Boolean enabled) {
        if (ids == null || ids.isEmpty()) {
            return Boolean.TRUE;
        }
        List<Long> longIds = ids.stream().map(this::parseId).collect(Collectors.toList());
        return menuMapper.batchEnable(longIds, enabled) >= 0;
    }

    private Long parseId(String id) {
        try {
            return Long.parseLong(id);
        } catch (Exception ex) {
            throw new BusinessException(3001, "非法ID: " + id);
        }
    }
}
