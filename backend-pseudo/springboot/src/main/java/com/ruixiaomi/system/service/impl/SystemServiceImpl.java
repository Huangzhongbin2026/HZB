package com.ruixiaomi.system.service.impl;

import com.ruixiaomi.system.mapper.RoleMapper;
import com.ruixiaomi.system.model.RbacModels.FieldPolicyDTO;
import com.ruixiaomi.system.model.RbacModels.RoleDTO;
import com.ruixiaomi.system.model.RbacModels.UserDTO;
import com.ruixiaomi.system.model.RoleEntity;
import com.ruixiaomi.system.service.SystemService;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SystemServiceImpl implements SystemService {

  private final RoleMapper roleMapper;

  public SystemServiceImpl(RoleMapper roleMapper) {
    this.roleMapper = roleMapper;
  }

  @Override
  public List<RoleDTO> listRoles() {
    return roleMapper.selectRoleList().stream()
      .map(role -> new RoleDTO(role.getId(), role.getRoleName(), role.getRoleCode(), String.valueOf(role.getStatus()), new ArrayList<>()))
      .collect(Collectors.toList());
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void saveRole(RoleDTO dto) {
    RoleEntity entity = new RoleEntity();
    entity.setRoleName(dto.roleName());
    entity.setRoleCode(dto.roleCode());
    entity.setStatus(parseStatus(dto.status()));
    roleMapper.insertRole(entity);
  }

  private Integer parseStatus(String status) {
    if (status == null || status.isBlank()) {
      return 1;
    }
    if ("ENABLED".equalsIgnoreCase(status)) {
      return 1;
    }
    if ("DISABLED".equalsIgnoreCase(status)) {
      return 0;
    }
    try {
      return Integer.parseInt(status);
    } catch (NumberFormatException ex) {
      return 1;
    }
  }

  @Override
  public List<UserDTO> listUsers() {
    return new ArrayList<>();
  }

  @Override
  public void saveUser(UserDTO dto) {
    // TODO: use MyBatis + MySQL persistence instead of in-memory pseudo logic.
  }

  @Override
  public List<String> queryPermissionCodes(Long userId) {
    return new ArrayList<>();
  }

  @Override
  public List<FieldPolicyDTO> queryFieldPolicies(Long roleId) {
    return new ArrayList<>();
  }

  @Override
  public void saveFieldPolicies(List<FieldPolicyDTO> policies) {
    // TODO: use MyBatis + MySQL persistence instead of in-memory pseudo logic.
  }
}