package com.ruixiaomi.system.service;

import com.ruixiaomi.system.model.RbacModels.FieldPolicyDTO;
import com.ruixiaomi.system.model.RbacModels.RoleDTO;
import com.ruixiaomi.system.model.RbacModels.UserDTO;
import java.util.List;

public interface SystemService {

  List<RoleDTO> listRoles();

  void saveRole(RoleDTO dto);

  List<UserDTO> listUsers();

  void saveUser(UserDTO dto);

  List<String> queryPermissionCodes(Long userId);

  List<FieldPolicyDTO> queryFieldPolicies(Long roleId);

  void saveFieldPolicies(List<FieldPolicyDTO> policies);
}
