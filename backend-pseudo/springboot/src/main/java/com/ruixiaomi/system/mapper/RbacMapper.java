package com.ruixiaomi.system.mapper;

import com.ruixiaomi.system.entity.SysDictItemEntity;
import com.ruixiaomi.system.entity.SysDictTypeEntity;
import com.ruixiaomi.system.entity.SysOperationLogEntity;
import com.ruixiaomi.system.entity.SysRoleEntity;
import com.ruixiaomi.system.entity.SysRoleFieldPolicyEntity;
import com.ruixiaomi.system.entity.SysRoleRowRuleEntity;
import com.ruixiaomi.system.entity.SysUserEntity;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RbacMapper {

  List<SysRoleEntity> selectRoleList();

  SysRoleEntity selectRoleById(@Param("id") Long id);

  SysRoleEntity selectRoleByCode(@Param("roleCode") String roleCode);

  SysRoleEntity selectRoleByName(@Param("roleName") String roleName);

  int insertRole(SysRoleEntity entity);

  int updateRole(SysRoleEntity entity);

  int deleteRole(@Param("id") Long id);

  List<String> selectPermissionCodesByRoleId(@Param("roleId") Long roleId);

  int deleteRolePermissions(@Param("roleId") Long roleId);

  int insertRolePermission(@Param("roleId") Long roleId, @Param("permissionId") Long permissionId);

  int insertPermissionIfAbsent(@Param("permissionCode") String permissionCode, @Param("permissionName") String permissionName,
                               @Param("resourceType") String resourceType, @Param("moduleCode") String moduleCode,
                               @Param("pageCode") String pageCode, @Param("buttonCode") String buttonCode,
                               @Param("fieldCode") String fieldCode);

  Long selectPermissionIdByCode(@Param("permissionCode") String permissionCode);

  List<SysRoleRowRuleEntity> selectRowRulesByRoleId(@Param("roleId") Long roleId);

  int deleteRowRules(@Param("roleId") Long roleId, @Param("pageCode") String pageCode);

  int insertRowRule(@Param("roleId") Long roleId, @Param("pageCode") String pageCode, @Param("matchField") String matchField);

  List<SysUserEntity> selectUserList();

  List<SysUserEntity> selectInternalUserList();

  SysUserEntity selectUserById(@Param("id") Long id);

  SysUserEntity selectUserByUsername(@Param("username") String username);

  SysUserEntity selectUserByDisplayName(@Param("displayName") String displayName);

  SysUserEntity selectUserByFeishuId(@Param("feishuId") String feishuId);

  int insertUser(SysUserEntity entity);

  int updateUser(SysUserEntity entity);

  int deleteUser(@Param("id") Long id);

  int deleteUserRoles(@Param("userId") Long userId);

  int insertUserRole(@Param("userId") Long userId, @Param("roleId") Long roleId);

  List<String> selectRoleCodesByUserId(@Param("userId") Long userId);

  List<SysRoleFieldPolicyEntity> selectFieldPoliciesByRoleId(@Param("roleId") Long roleId);

  int deleteFieldPolicies(@Param("roleId") Long roleId);

  int insertFieldPolicy(@Param("roleId") Long roleId, @Param("moduleCode") String moduleCode,
                        @Param("pageCode") String pageCode, @Param("fieldCode") String fieldCode,
                        @Param("policyMode") String policyMode);

  List<SysOperationLogEntity> selectOperationLogs();

  List<SysOperationLogEntity> selectOperationLogsLegacy();

  List<com.ruixiaomi.system.entity.SysLoginLogEntity> selectLoginLogs();

  List<com.ruixiaomi.system.entity.SysLoginLogEntity> selectLoginLogsLegacy();

  int insertOperationLog(SysOperationLogEntity entity);

  int insertOperationLogLegacy(SysOperationLogEntity entity);

  List<SysDictTypeEntity> selectDictTypes();

  int insertDictType(SysDictTypeEntity entity);

  int updateDictType(SysDictTypeEntity entity);

  int deleteDictType(@Param("id") Long id);

  List<SysDictItemEntity> selectDictItems();

  int insertDictItem(SysDictItemEntity entity);

  int updateDictItem(SysDictItemEntity entity);

  int deleteDictItem(@Param("id") Long id);
}