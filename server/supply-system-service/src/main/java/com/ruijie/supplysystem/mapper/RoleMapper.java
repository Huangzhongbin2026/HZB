package com.ruijie.supplysystem.mapper;

import com.ruijie.supplysystem.dto.SysRoleDTO;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface RoleMapper {

    @Select("""
            SELECT id, role_name AS roleName, role_code AS roleCode, dept_code AS deptCode,
                   description, is_enabled AS isEnabled
            FROM sys_role
            WHERE is_deleted = 0
            ORDER BY id DESC
            """)
    List<SysRoleDTO> list();

    @Insert("""
            INSERT INTO sys_role(role_name, role_code, dept_code, description, is_enabled, created_by, updated_by, is_deleted)
            VALUES(#{roleName}, #{roleCode}, #{deptCode}, #{description}, #{isEnabled}, 'system', 'system', 0)
            """)
    int insert(SysRoleDTO dto);

    @Update("""
            <script>
            UPDATE sys_role
            <set>
                <if test='roleName != null'>role_name = #{roleName},</if>
                <if test='roleCode != null'>role_code = #{roleCode},</if>
                <if test='deptCode != null'>dept_code = #{deptCode},</if>
                <if test='description != null'>description = #{description},</if>
                <if test='isEnabled != null'>is_enabled = #{isEnabled},</if>
                updated_by = 'system',
                updated_at = NOW()
            </set>
            WHERE id = #{id} AND is_deleted = 0
            </script>
            """)
    int updateById(@Param("id") Long id, SysRoleDTO dto);

    @Update("UPDATE sys_role SET is_deleted = 1, updated_at = NOW(), updated_by = 'system' WHERE id = #{id} AND is_deleted = 0")
    int softDelete(@Param("id") Long id);

    @Update("UPDATE sys_role_menu SET is_deleted = 1, updated_at = NOW(), updated_by = 'system' WHERE role_id = #{roleId} AND is_deleted = 0")
    int softDeleteRoleMenus(@Param("roleId") Long roleId);

    @Insert("""
            <script>
            INSERT INTO sys_role_menu(role_id, menu_id, created_by, updated_by, is_deleted)
            VALUES
            <foreach collection='menuIds' item='menuId' separator=','>
              (#{roleId}, #{menuId}, 'system', 'system', 0)
            </foreach>
            </script>
            """)
    int insertRoleMenus(@Param("roleId") Long roleId, @Param("menuIds") List<Long> menuIds);

    @Select("""
            SELECT m.permission_code
            FROM sys_role_menu rm
            INNER JOIN sys_menu m ON m.id = rm.menu_id AND m.is_deleted = 0
            WHERE rm.is_deleted = 0 AND rm.role_id = #{roleId}
            """)
    List<String> listMenuCodesByRoleId(@Param("roleId") Long roleId);

    @Select("""
            <script>
            SELECT id FROM sys_menu
            WHERE is_deleted = 0 AND permission_code IN
            <foreach collection='codes' item='code' open='(' separator=',' close=')'>
                #{code}
            </foreach>
            </script>
            """)
    List<Long> listMenuIdsByPermissionCodes(@Param("codes") List<String> codes);

    @Select("SELECT scope_type FROM sys_role_data_permission WHERE role_id = #{roleId} AND is_deleted = 0 LIMIT 1")
    String findDataScope(@Param("roleId") Long roleId);
}
