package com.ruijie.supplysystem.mapper;

import com.ruijie.supplysystem.dto.SysOperationLogDTO;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface OperationLogMapper {

    @Select("""
            <script>
            SELECT COUNT(1)
            FROM sys_operation_log
            WHERE is_deleted = 0
              <if test='operUser != null and operUser != ""'>AND oper_user = #{operUser}</if>
              <if test='operType != null and operType != ""'>AND oper_type = #{operType}</if>
              <if test='operModule != null and operModule != ""'>AND oper_module = #{operModule}</if>
              <if test='operIp != null and operIp != ""'>AND oper_ip = #{operIp}</if>
              <if test='keyword != null and keyword != ""'>AND oper_content LIKE CONCAT('%', #{keyword}, '%')</if>
              <if test='startTime != null and startTime != ""'>AND oper_time &gt;= #{startTime}</if>
              <if test='endTime != null and endTime != ""'>AND oper_time &lt;= #{endTime}</if>
            </script>
            """)
    Long count(@Param("operUser") String operUser,
               @Param("operType") String operType,
               @Param("operModule") String operModule,
               @Param("operIp") String operIp,
               @Param("keyword") String keyword,
               @Param("startTime") String startTime,
               @Param("endTime") String endTime);

    @Select("""
            <script>
            SELECT id, oper_user AS operUser, DATE_FORMAT(oper_time, '%Y-%m-%d %H:%i:%s') AS operTime,
                   oper_ip AS operIp, oper_type AS operType, oper_module AS operModule,
                   oper_content AS operContent, oper_result AS operResult, remark
            FROM sys_operation_log
            WHERE is_deleted = 0
              <if test='operUser != null and operUser != ""'>AND oper_user = #{operUser}</if>
              <if test='operType != null and operType != ""'>AND oper_type = #{operType}</if>
              <if test='operModule != null and operModule != ""'>AND oper_module = #{operModule}</if>
              <if test='operIp != null and operIp != ""'>AND oper_ip = #{operIp}</if>
              <if test='keyword != null and keyword != ""'>AND oper_content LIKE CONCAT('%', #{keyword}, '%')</if>
              <if test='startTime != null and startTime != ""'>AND oper_time &gt;= #{startTime}</if>
              <if test='endTime != null and endTime != ""'>AND oper_time &lt;= #{endTime}</if>
            ORDER BY oper_time DESC, id DESC
            LIMIT #{offset}, #{pageSize}
            </script>
            """)
    List<SysOperationLogDTO> query(@Param("offset") int offset,
                                   @Param("pageSize") int pageSize,
                                   @Param("operUser") String operUser,
                                   @Param("operType") String operType,
                                   @Param("operModule") String operModule,
                                   @Param("operIp") String operIp,
                                   @Param("keyword") String keyword,
                                   @Param("startTime") String startTime,
                                   @Param("endTime") String endTime);

    @Insert("""
            INSERT INTO sys_operation_log(oper_user, oper_ip, oper_type, oper_module, oper_content,
                                          oper_result, oper_time, remark, request_uri, request_method, trace_id, is_deleted)
            VALUES(#{operUser}, #{operIp}, #{operType}, #{operModule}, #{operContent},
                   #{operResult}, NOW(), #{remark}, '', '', '', 0)
            """)
    int insert(SysOperationLogDTO dto);

    @Update("""
            <script>
            UPDATE sys_operation_log
            SET is_deleted = 1, updated_at = NOW()
            WHERE is_deleted = 0
              <if test='startTime != null and startTime != ""'>AND oper_time &gt;= #{startTime}</if>
              <if test='endTime != null and endTime != ""'>AND oper_time &lt;= #{endTime}</if>
            </script>
            """)
    int clean(@Param("startTime") String startTime, @Param("endTime") String endTime);
}
