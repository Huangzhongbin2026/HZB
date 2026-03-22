package com.ruijie.supplysystem.mapper;

import com.ruijie.supplysystem.dto.SysMenuDTO;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface MenuMapper {

    @Select("""
            SELECT id, menu_name AS menuName, menu_type AS menuType, parent_id AS parentId,
                   route_path AS routePath, component_path AS componentPath, permission_code AS permissionCode,
                   icon, sort_no AS sortNo, is_visible AS isVisible, is_enabled AS isEnabled
            FROM sys_menu
            WHERE is_deleted = 0
            ORDER BY parent_id ASC, sort_no ASC, id ASC
            """)
    List<SysMenuDTO> listTree();

    @Insert("""
            INSERT INTO sys_menu(menu_name, menu_type, parent_id, route_path, component_path, permission_code,
                                 icon, sort_no, is_visible, is_enabled, created_by, updated_by, is_deleted)
            VALUES(#{menuName}, #{menuType}, #{parentId}, #{routePath}, #{componentPath}, #{permissionCode},
                   #{icon}, #{sortNo}, #{isVisible}, #{isEnabled}, 'system', 'system', 0)
            """)
    int insert(SysMenuDTO dto);

    @Update("""
            <script>
            UPDATE sys_menu
            <set>
                <if test='menuName != null'>menu_name = #{menuName},</if>
                <if test='menuType != null'>menu_type = #{menuType},</if>
                <if test='parentId != null'>parent_id = #{parentId},</if>
                <if test='routePath != null'>route_path = #{routePath},</if>
                <if test='componentPath != null'>component_path = #{componentPath},</if>
                <if test='permissionCode != null'>permission_code = #{permissionCode},</if>
                <if test='icon != null'>icon = #{icon},</if>
                <if test='sortNo != null'>sort_no = #{sortNo},</if>
                <if test='isVisible != null'>is_visible = #{isVisible},</if>
                <if test='isEnabled != null'>is_enabled = #{isEnabled},</if>
                updated_by = 'system',
                updated_at = NOW()
            </set>
            WHERE id = #{id} AND is_deleted = 0
            </script>
            """)
    int updateById(@Param("id") Long id, SysMenuDTO dto);

    @Update("UPDATE sys_menu SET is_deleted = 1, updated_at = NOW(), updated_by = 'system' WHERE id = #{id} AND is_deleted = 0")
    int softDelete(@Param("id") Long id);

    @Update("""
            <script>
            UPDATE sys_menu
            SET is_enabled = #{enabled}, updated_at = NOW(), updated_by = 'system'
            WHERE is_deleted = 0
              AND id IN
              <foreach collection='ids' item='id' open='(' separator=',' close=')'>
                #{id}
              </foreach>
            </script>
            """)
    int batchEnable(@Param("ids") List<Long> ids, @Param("enabled") Boolean enabled);
}
