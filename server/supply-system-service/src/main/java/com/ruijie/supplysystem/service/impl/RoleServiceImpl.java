package com.ruijie.supplysystem.service.impl;

import com.ruijie.supplysystem.common.BusinessException;
import com.ruijie.supplysystem.dto.SysRoleDTO;
import com.ruijie.supplysystem.mapper.RoleMapper;
import com.ruijie.supplysystem.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleMapper roleMapper;

    @Override
    public List<SysRoleDTO> list() {
        return roleMapper.list();
    }

    @Override
    public Boolean save(SysRoleDTO dto) {
        return roleMapper.insert(dto) > 0;
    }

    @Override
    public Boolean update(String id, SysRoleDTO dto) {
        return roleMapper.updateById(parseId(id), dto) > 0;
    }

    @Override
    public Boolean remove(String id) {
        Long roleId = parseId(id);
        roleMapper.softDeleteRoleMenus(roleId);
        return roleMapper.softDelete(roleId) > 0;
    }

    @Override
    public Boolean savePermissions(String roleId, Map<String, Object> payload) {
        Long id = parseId(roleId);
        roleMapper.softDeleteRoleMenus(id);

        Object rawMenuIds = payload == null ? null : payload.get("menuIds");
        Object rawMenuCodes = payload == null ? null : payload.get("menuCodes");

        List<Long> menuIds = List.of();
        if (rawMenuIds instanceof List<?> list && !list.isEmpty()) {
            menuIds = list.stream().map(String::valueOf).map(this::parseId).collect(Collectors.toList());
        } else if (rawMenuCodes instanceof List<?> list && !list.isEmpty()) {
            List<String> codes = list.stream().map(String::valueOf).collect(Collectors.toList());
            menuIds = roleMapper.listMenuIdsByPermissionCodes(codes);
        }

        if (!menuIds.isEmpty()) {
            roleMapper.insertRoleMenus(id, menuIds);
        }
        return Boolean.TRUE;
    }

    @Override
    public Map<String, Object> preview(String roleId) {
        Long id = parseId(roleId);
        List<String> menuCodes = roleMapper.listMenuCodesByRoleId(id);
        List<String> buttonCodes = menuCodes.stream().filter(code -> code != null && code.contains(":")).collect(Collectors.toList());
        String dataScope = roleMapper.findDataScope(id);
        return Map.of(
                "menuCodes", menuCodes,
                "buttonCodes", buttonCodes,
                "dataScope", dataScope == null ? "ALL" : dataScope
        );
    }

    private Long parseId(String id) {
        try {
            return Long.parseLong(id);
        } catch (Exception ex) {
            throw new BusinessException(3201, "非法ID: " + id);
        }
    }
}
