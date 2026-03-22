package com.ruijie.supplysystem.mapper;

import com.ruijie.supplysystem.dto.SysDictItemDTO;
import com.ruijie.supplysystem.dto.SysDictTypeDTO;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface DictMapper {

    @Select("""
            SELECT id, dict_name AS dictName, dict_code AS dictCode, sort_no AS sortNo,
                   is_enabled AS isEnabled, remark
            FROM sys_dict_type
            WHERE is_deleted = 0
            ORDER BY sort_no ASC, id ASC
            """)
    List<SysDictTypeDTO> listTypes();

    @Insert("""
            INSERT INTO sys_dict_type(dict_name, dict_code, sort_no, is_enabled, remark, created_by, updated_by, is_deleted)
            VALUES(#{dictName}, #{dictCode}, #{sortNo}, #{isEnabled}, #{remark}, 'system', 'system', 0)
            """)
    int insertType(SysDictTypeDTO dto);

    @Select("SELECT id FROM sys_dict_type WHERE dict_code = #{typeCode} AND is_deleted = 0 LIMIT 1")
    Long findTypeIdByCode(@Param("typeCode") String typeCode);

    @Select("""
            <script>
            SELECT i.id, i.dict_type_id AS dictTypeId, i.item_name AS itemName, i.item_code AS itemCode,
                   i.item_value AS itemValue, i.sort_no AS sortNo, i.is_enabled AS isEnabled, i.remark
            FROM sys_dict_item i
            INNER JOIN sys_dict_type t ON t.id = i.dict_type_id AND t.is_deleted = 0
            WHERE i.is_deleted = 0
              AND t.dict_code = #{typeCode}
              <if test='keyword != null and keyword != ""'>
                AND (i.item_name LIKE CONCAT('%', #{keyword}, '%') OR i.item_code LIKE CONCAT('%', #{keyword}, '%'))
              </if>
            ORDER BY i.sort_no ASC, i.id ASC
            </script>
            """)
    List<SysDictItemDTO> listItems(@Param("typeCode") String typeCode, @Param("keyword") String keyword);

    @Insert("""
            INSERT INTO sys_dict_item(dict_type_id, item_name, item_code, item_value, sort_no, is_enabled, remark, created_by, updated_by, is_deleted)
            VALUES(#{dictTypeId}, #{itemName}, #{itemCode}, #{itemValue}, #{sortNo}, #{isEnabled}, #{remark}, 'system', 'system', 0)
            """)
    int insertItem(SysDictItemDTO dto);
}
