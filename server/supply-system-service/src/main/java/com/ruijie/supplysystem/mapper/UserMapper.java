package com.ruijie.supplysystem.mapper;

import com.ruijie.supplysystem.dto.SysUserDTO;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface UserMapper {

    @Select("""
            SELECT id, user_name AS userName, account, mobile, feishu_id AS feishuId,
                   email, dept_code AS deptCode, status, DATE_FORMAT(last_login_at, '%Y-%m-%d %H:%i:%s') AS lastLoginAt
            FROM sys_user
            WHERE is_deleted = 0
            ORDER BY id DESC
            """)
    List<SysUserDTO> list();

    @Insert("""
            INSERT INTO sys_user(user_name, account, mobile, feishu_id, email, dept_code, password_hash, password_salt,
                                 status, created_by, updated_by, is_deleted)
            VALUES(#{userName}, #{account}, #{mobile}, #{feishuId}, #{email}, #{deptCode}, #{passwordHash}, #{passwordSalt},
                   #{status}, 'system', 'system', 0)
            """)
    int insert(@Param("userName") String userName,
               @Param("account") String account,
               @Param("mobile") String mobile,
               @Param("feishuId") String feishuId,
               @Param("email") String email,
               @Param("deptCode") String deptCode,
               @Param("passwordHash") String passwordHash,
               @Param("passwordSalt") String passwordSalt,
               @Param("status") Boolean status);

    @Update("""
            <script>
            UPDATE sys_user
            <set>
                <if test='dto.userName != null'>user_name = #{dto.userName},</if>
                <if test='dto.account != null'>account = #{dto.account},</if>
                <if test='dto.mobile != null'>mobile = #{dto.mobile},</if>
                <if test='dto.feishuId != null'>feishu_id = #{dto.feishuId},</if>
                <if test='dto.email != null'>email = #{dto.email},</if>
                <if test='dto.deptCode != null'>dept_code = #{dto.deptCode},</if>
                <if test='dto.status != null'>status = #{dto.status},</if>
                updated_by = 'system',
                updated_at = NOW()
            </set>
            WHERE id = #{id} AND is_deleted = 0
            </script>
            """)
    int updateById(@Param("id") Long id, @Param("dto") SysUserDTO dto);

    @Update("UPDATE sys_user SET is_deleted = 1, updated_at = NOW(), updated_by = 'system' WHERE id = #{id} AND is_deleted = 0")
    int softDelete(@Param("id") Long id);

    @Update("UPDATE sys_user SET password_hash = #{passwordHash}, password_salt = #{passwordSalt}, updated_at = NOW(), updated_by = 'system' WHERE id = #{id} AND is_deleted = 0")
    int resetPassword(@Param("id") Long id, @Param("passwordHash") String passwordHash, @Param("passwordSalt") String passwordSalt);

    @Update("UPDATE sys_user_role SET is_deleted = 1, updated_at = NOW(), updated_by = 'system' WHERE user_id = #{userId} AND is_deleted = 0")
    int clearUserRoles(@Param("userId") Long userId);

    @Insert("""
            INSERT INTO sys_user_role(user_id, role_id, created_by, updated_by, is_deleted)
            VALUES(#{userId}, #{roleId}, 'system', 'system', 0)
            """)
    int insertUserRole(@Param("userId") Long userId, @Param("roleId") Long roleId);

    @Select("""
            SELECT DISTINCT m.permission_code
            FROM sys_user_role ur
            INNER JOIN sys_role_menu rm ON rm.role_id = ur.role_id AND rm.is_deleted = 0
            INNER JOIN sys_menu m ON m.id = rm.menu_id AND m.is_deleted = 0
            WHERE ur.user_id = #{userId} AND ur.is_deleted = 0
            """)
    List<String> listUserMenuCodes(@Param("userId") Long userId);

    @Select("""
            SELECT biz_table AS bizTable, scope_type AS scopeType
            FROM sys_role_data_permission rdp
            INNER JOIN sys_user_role ur ON ur.role_id = rdp.role_id AND ur.is_deleted = 0
            WHERE ur.user_id = #{userId} AND rdp.is_deleted = 0
            """)
    List<Map<String, String>> listDataScopes(@Param("userId") Long userId);

    @Select("""
            SELECT biz_table AS bizTable, field_code AS fieldCode, permission_type AS permissionType
            FROM sys_role_field_permission rfp
            INNER JOIN sys_user_role ur ON ur.role_id = rfp.role_id AND ur.is_deleted = 0
            WHERE ur.user_id = #{userId} AND rfp.is_deleted = 0
            """)
    List<Map<String, String>> listFieldPermissions(@Param("userId") Long userId);
}
