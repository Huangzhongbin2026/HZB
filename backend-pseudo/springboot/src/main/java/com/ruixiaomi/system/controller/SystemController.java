package com.ruixiaomi.system.controller;

import com.ruixiaomi.system.model.RbacModels.FieldPolicyDTO;
import com.ruixiaomi.system.model.RbacModels.RoleDTO;
import com.ruixiaomi.system.model.RbacModels.UserDTO;
import com.ruixiaomi.system.service.SystemService;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/system")
public class SystemController {

  private final SystemService systemService;

  public SystemController(SystemService systemService) {
    this.systemService = systemService;
  }

  @GetMapping("/roles")
  public List<RoleDTO> roles() {
    return systemService.listRoles();
  }

  @PostMapping("/roles")
  public void saveRole(@RequestBody RoleDTO dto) {
    systemService.saveRole(dto);
  }

  @GetMapping("/users")
  public List<UserDTO> users() {
    return systemService.listUsers();
  }

  @PostMapping("/users")
  public void saveUser(@RequestBody UserDTO dto) {
    systemService.saveUser(dto);
  }

  @GetMapping("/permissions/codes")
  public List<String> permissionCodes(@RequestParam("userId") Long userId) {
    return systemService.queryPermissionCodes(userId);
  }

  @GetMapping("/permissions/field-policies")
  public List<FieldPolicyDTO> fieldPolicies(@RequestParam("roleId") Long roleId) {
    return systemService.queryFieldPolicies(roleId);
  }

  @PutMapping("/permissions/field-policies")
  public void saveFieldPolicies(@RequestBody List<FieldPolicyDTO> body) {
    systemService.saveFieldPolicies(body);
  }
}
