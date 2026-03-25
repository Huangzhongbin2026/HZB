package com.ruixiaomi.system.model;

import java.util.List;

public class RbacModels {

  public record RoleDTO(Long id, String roleName, String roleCode, String status, List<String> permissionCodes) {}

  public record UserDTO(Long id, String username, String nickname, String email, String phone, String status, List<String> roleCodes) {}

  public record FieldPolicyDTO(Long roleId, String moduleCode, String fieldKey, String mode) {}
}
