package com.ruijie.supplysystem.service.impl;

import com.ruijie.supplysystem.common.BusinessException;
import com.ruijie.supplysystem.dto.SysUserDTO;
import com.ruijie.supplysystem.mapper.UserMapper;
import com.ruijie.supplysystem.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final BCryptPasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();

    private final UserMapper userMapper;

    @Override
    public List<SysUserDTO> list() {
        return userMapper.list();
    }

    @Override
    public Boolean save(SysUserDTO dto) {
        String salt = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        String passwordHash = PASSWORD_ENCODER.encode("123456" + salt);
        return userMapper.insert(dto.getUserName(), dto.getAccount(), dto.getMobile(), dto.getFeishuId(), dto.getEmail(),
                dto.getDeptCode(), passwordHash, salt, dto.getStatus() == null ? Boolean.TRUE : dto.getStatus()) > 0;
    }

    @Override
    public Boolean update(String id, SysUserDTO dto) {
        return userMapper.updateById(parseId(id), dto) > 0;
    }

    @Override
    public Boolean remove(String id) {
        Long userId = parseId(id);
        userMapper.clearUserRoles(userId);
        return userMapper.softDelete(userId) > 0;
    }

    @Override
    public Boolean assignRoles(String userId, List<String> roleIds) {
        Long uid = parseId(userId);
        userMapper.clearUserRoles(uid);
        if (roleIds != null) {
            for (String roleId : roleIds) {
                userMapper.insertUserRole(uid, parseId(roleId));
            }
        }
        return Boolean.TRUE;
    }

    @Override
    public Boolean resetPassword(String userId) {
        String salt = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        String passwordHash = PASSWORD_ENCODER.encode("123456" + salt);
        return userMapper.resetPassword(parseId(userId), passwordHash, salt) > 0;
    }

    @Override
    public Map<String, Object> permissionSnapshot() {
        Long userId = 1L;
        List<String> menuCodes = userMapper.listUserMenuCodes(userId);
        List<String> buttonCodes = menuCodes.stream().filter(code -> code != null && code.split(":").length >= 3).collect(Collectors.toList());

        Map<String, String> dataScopes = userMapper.listDataScopes(userId).stream()
                .collect(Collectors.toMap(x -> x.get("bizTable"), x -> x.get("scopeType"), (a, b) -> b));

        Map<String, Map<String, String>> fieldPermissions = userMapper.listFieldPermissions(userId).stream()
                .collect(Collectors.groupingBy(x -> x.get("bizTable"),
                        Collectors.toMap(x -> x.get("fieldCode"), x -> x.get("permissionType"), (a, b) -> b)));

        return Map.of(
                "menuCodes", menuCodes,
                "buttonCodes", buttonCodes,
                "dataScopes", dataScopes,
                "fieldPermissions", fieldPermissions
        );
    }

    private Long parseId(String id) {
        try {
            return Long.parseLong(id);
        } catch (Exception ex) {
            throw new BusinessException(3301, "非法ID: " + id);
        }
    }
}
