# 系统管理模块后端接口（伪代码）

## 1. 核心 RBAC 模型

```sql
-- 用户表
sys_user(id, username, password_hash, nickname, email, phone, status, created_at)

-- 角色表
sys_role(id, role_name, role_code, status, remark)

-- 权限点表（页面/按钮/字段）
sys_permission(id, perm_code, perm_name, perm_type, route_path, component, action, field_key)
-- perm_type: PAGE | BUTTON | FIELD

-- 关系表
sys_user_role(user_id, role_id)
sys_role_permission(role_id, permission_id)

-- 字段权限策略表
sys_field_policy(id, role_id, module_code, field_key, mode)
-- mode: HIDDEN | READONLY | EDITABLE

-- 日志表
sys_oper_log(id, operator_id, module, action, request_uri, request_body, ip, result, created_at)
sys_login_log(id, user_id, username, ip, location, user_agent, status, created_at)

-- 字典表
sys_dict_type(id, name, code, status, remark)
sys_dict_data(id, dict_code, label, value, sort, status)
```

## 2. 权限拦截流程

```java
class AuthFilter {
  void doFilter(HttpServletRequest req) {
    String token = req.getHeader("Authorization");
    Long userId = JwtUtil.parse(token);
    List<String> permCodes = permissionService.queryUserPermissions(userId);
    SecurityContext.setUser(userId, permCodes);
  }
}

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@interface RequiresPermission {
  String value();
}

@Aspect
class PermissionAspect {
  @Before("@annotation(rp)")
  void check(RequiresPermission rp) {
    if (!SecurityContext.has(rp.value())) {
      throw new ForbiddenException("NO_PERMISSION");
    }
  }
}
```

## 3. 页面级权限接口（路由 + 菜单）

```java
@RestController
@RequestMapping("/api/auth")
class AuthController {

  @PostMapping("/login")
  LoginVO login(@RequestBody LoginDTO dto) {
    // 鉴权成功后返回 token
  }

  @GetMapping("/profile")
  ProfileVO profile() {
    // 返回用户信息 + 角色
  }

  @GetMapping("/permissions")
  PermissionVO permissions() {
    // 返回 permissionCodes + fieldPolicies
    // permissionCodes: ["page:system:user", "btn:user:add", ...]
    // fieldPolicies: [{module:"user",fieldKey:"phone",mode:"HIDDEN"}, ...]
  }

  @GetMapping("/menus")
  List<MenuVO> menus() {
    // 按 PAGE 权限过滤菜单
  }
}
```

## 4. 角色管理接口

```java
@RestController
@RequestMapping("/api/system/roles")
class RoleController {

  @GetMapping
  @RequiresPermission("btn:role:query")
  Page<RoleVO> page(RoleQuery query) {}

  @PostMapping
  @RequiresPermission("btn:role:add")
  void create(@RequestBody RoleCreateDTO dto) {}

  @PutMapping("/{id}")
  @RequiresPermission("btn:role:edit")
  void update(@PathVariable Long id, @RequestBody RoleUpdateDTO dto) {}

  @DeleteMapping("/{id}")
  @RequiresPermission("btn:role:delete")
  void delete(@PathVariable Long id) {}

  @PutMapping("/{id}/permissions")
  @RequiresPermission("btn:role:assign")
  void assignPermissions(@PathVariable Long id, @RequestBody List<String> permCodes) {}
}
```

## 5. 用户管理接口

```java
@RestController
@RequestMapping("/api/system/users")
class UserController {

  @GetMapping
  @RequiresPermission("btn:user:query")
  Page<UserVO> page(UserQuery query) {}

  @PostMapping
  @RequiresPermission("btn:user:add")
  void create(@RequestBody UserCreateDTO dto) {}

  @PutMapping("/{id}")
  @RequiresPermission("btn:user:edit")
  void update(@PathVariable Long id, @RequestBody UserUpdateDTO dto) {}

  @DeleteMapping("/{id}")
  @RequiresPermission("btn:user:delete")
  void delete(@PathVariable Long id) {}

  @PutMapping("/{id}/roles")
  @RequiresPermission("btn:user:edit")
  void assignRoles(@PathVariable Long id, @RequestBody List<Long> roleIds) {}

  @PutMapping("/{id}/status")
  @RequiresPermission("btn:user:edit")
  void changeStatus(@PathVariable Long id, @RequestBody StatusDTO dto) {}
}
```

## 6. 权限管理接口（含字段权限）

```java
@RestController
@RequestMapping("/api/system/permissions")
class PermissionController {

  @GetMapping
  @RequiresPermission("page:system:permission")
  PermissionTreeVO tree() {}

  @GetMapping("/codes")
  List<String> allCodes() {}

  @GetMapping("/field-policies")
  List<FieldPolicyVO> fieldPolicies(@RequestParam Long roleId) {}

  @PutMapping("/field-policies")
  @RequiresPermission("btn:permission:save")
  void saveFieldPolicies(@RequestBody List<FieldPolicyDTO> dtos) {}
}
```

## 7. 系统日志接口

```java
@RestController
@RequestMapping("/api/system/logs")
class LogController {

  @GetMapping("/operation")
  @RequiresPermission("btn:log:query")
  Page<OperationLogVO> operationLogs(LogQuery query) {}

  @GetMapping("/login")
  @RequiresPermission("btn:log:query")
  Page<LoginLogVO> loginLogs(LogQuery query) {}
}
```

## 8. 数据字典接口

```java
@RestController
@RequestMapping("/api/system/dict")
class DictController {

  @GetMapping("/types")
  @RequiresPermission("btn:dict:type:query")
  Page<DictTypeVO> typePage(DictTypeQuery query) {}

  @PostMapping("/types")
  @RequiresPermission("btn:dict:type:add")
  void createType(@RequestBody DictTypeDTO dto) {}

  @PutMapping("/types/{id}")
  @RequiresPermission("btn:dict:type:edit")
  void updateType(@PathVariable Long id, @RequestBody DictTypeDTO dto) {}

  @DeleteMapping("/types/{id}")
  @RequiresPermission("btn:dict:type:delete")
  void deleteType(@PathVariable Long id) {}

  @GetMapping("/data")
  @RequiresPermission("btn:dict:data:query")
  Page<DictDataVO> dataPage(DictDataQuery query) {}

  @PostMapping("/data")
  @RequiresPermission("btn:dict:data:add")
  void createData(@RequestBody DictDataDTO dto) {}

  @PutMapping("/data/{id}")
  @RequiresPermission("btn:dict:data:edit")
  void updateData(@PathVariable Long id, @RequestBody DictDataDTO dto) {}

  @DeleteMapping("/data/{id}")
  @RequiresPermission("btn:dict:data:delete")
  void deleteData(@PathVariable Long id) {}
}
```

## 9. 字段权限下发与前端联动

```json
{
  "permissionCodes": ["page:system:user", "btn:user:edit", "btn:log:query"],
  "fieldPolicies": [
    { "module": "user", "fieldKey": "username", "mode": "READONLY" },
    { "module": "user", "fieldKey": "phone", "mode": "HIDDEN" },
    { "module": "user", "fieldKey": "email", "mode": "EDITABLE" }
  ]
}
```
