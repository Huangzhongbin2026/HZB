package com.ruijie.supplysystem.mapper;

import com.ruijie.supplysystem.auxiliary.dto.*;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface AuxiliaryMapper {

    @Select("""
            <script>
            SELECT COUNT(1) FROM sys_aux_leave_config
            WHERE is_deleted = 0
              <if test='userId != null and userId != ""'>AND user_id = #{userId}</if>
              <if test='userName != null and userName != ""'>AND user_name LIKE CONCAT('%', #{userName}, '%')</if>
            </script>
            """)
    Long countLeaveConfigs(@Param("userId") String userId, @Param("userName") String userName);

    @Select("""
            <script>
            SELECT id, user_id AS userId, user_name AS userName,
                   DATE_FORMAT(leave_start, '%Y-%m-%d %H:%i:%s') AS leaveStart,
                   DATE_FORMAT(leave_end, '%Y-%m-%d %H:%i:%s') AS leaveEnd,
                     leave_reason AS leaveReason, remark, status
            FROM sys_aux_leave_config
            WHERE is_deleted = 0
              <if test='userId != null and userId != ""'>AND user_id = #{userId}</if>
              <if test='userName != null and userName != ""'>AND user_name LIKE CONCAT('%', #{userName}, '%')</if>
            ORDER BY id DESC
            LIMIT #{offset}, #{pageSize}
            </script>
            """)
    List<LeaveConfigDTO> listLeaveConfigs(@Param("offset") int offset, @Param("pageSize") int pageSize,
                                          @Param("userId") String userId, @Param("userName") String userName);

    @Insert("""
            INSERT INTO sys_aux_leave_config(user_id, user_name, leave_start, leave_end, leave_reason, remark, status, created_by, updated_by, is_deleted)
            VALUES(#{userId}, #{userName}, #{leaveStart}, #{leaveEnd}, #{leaveReason}, #{remark}, #{status}, 'system', 'system', 0)
            """)
    int insertLeaveConfig(LeaveConfigDTO dto);

    @Update("""
            <script>
            UPDATE sys_aux_leave_config
            <set>
                <if test='dto.userId != null'>user_id = #{dto.userId},</if>
                <if test='dto.userName != null'>user_name = #{dto.userName},</if>
                <if test='dto.leaveStart != null'>leave_start = #{dto.leaveStart},</if>
                <if test='dto.leaveEnd != null'>leave_end = #{dto.leaveEnd},</if>
                <if test='dto.leaveReason != null'>leave_reason = #{dto.leaveReason},</if>
                <if test='dto.remark != null'>remark = #{dto.remark},</if>
                <if test='dto.status != null'>status = #{dto.status},</if>
                updated_by = 'system',
                updated_at = NOW()
            </set>
            WHERE id = #{id} AND is_deleted = 0
            </script>
            """)
    int updateLeaveConfig(@Param("id") Long id, @Param("dto") LeaveConfigDTO dto);

    @Update("UPDATE sys_aux_leave_config SET is_deleted = 1, updated_at = NOW() WHERE id = #{id} AND is_deleted = 0")
    int deleteLeaveConfig(@Param("id") Long id);

    @Select("""
            SELECT id, user_id AS userId, user_name AS userName,
                   DATE_FORMAT(leave_start, '%Y-%m-%d %H:%i:%s') AS leaveStart,
                   DATE_FORMAT(leave_end, '%Y-%m-%d %H:%i:%s') AS leaveEnd,
                   leave_reason AS leaveReason, remark, status
            FROM sys_aux_leave_config
            WHERE user_id = #{userId} AND status = 1 AND is_deleted = 0
            """)
    List<LeaveConfigDTO> listLeaveConfigsByUserId(@Param("userId") String userId);

    @Select("SELECT COUNT(1) FROM sys_aux_leave_config WHERE is_deleted = 0 AND user_id = #{userId}")
    Long countLeaveConfigByUserId(@Param("userId") String userId);

    @Select("""
            <script>
            SELECT COUNT(1) FROM sys_aux_virtual_product
            WHERE is_deleted = 0
              <if test='productModel != null and productModel != ""'>AND product_model LIKE CONCAT('%', #{productModel}, '%')</if>
            </script>
            """)
    Long countVirtualProducts(@Param("productModel") String productModel);

    @Select("""
            <script>
            SELECT id, product_model AS productModel, auto_reply_content AS autoReplyContent,
                   status, DATE_FORMAT(created_at, '%Y-%m-%d %H:%i:%s') AS createdAt
            FROM sys_aux_virtual_product
            WHERE is_deleted = 0
              <if test='productModel != null and productModel != ""'>AND product_model LIKE CONCAT('%', #{productModel}, '%')</if>
            ORDER BY id DESC
            LIMIT #{offset}, #{pageSize}
            </script>
            """)
    List<VirtualProductDTO> listVirtualProducts(@Param("offset") int offset, @Param("pageSize") int pageSize,
                                                @Param("productModel") String productModel);

    @Insert("""
            INSERT INTO sys_aux_virtual_product(product_model, auto_reply_content, status, created_by, updated_by, is_deleted)
            VALUES(#{productModel}, #{autoReplyContent}, #{status}, 'system', 'system', 0)
            """)
    int insertVirtualProduct(VirtualProductDTO dto);

    @Update("""
            <script>
            UPDATE sys_aux_virtual_product
            <set>
                <if test='dto.productModel != null'>product_model = #{dto.productModel},</if>
                <if test='dto.autoReplyContent != null'>auto_reply_content = #{dto.autoReplyContent},</if>
                <if test='dto.status != null'>status = #{dto.status},</if>
                updated_by = 'system',
                updated_at = NOW()
            </set>
            WHERE id = #{id} AND is_deleted = 0
            </script>
            """)
    int updateVirtualProduct(@Param("id") Long id, @Param("dto") VirtualProductDTO dto);

    @Update("UPDATE sys_aux_virtual_product SET is_deleted = 1, updated_at = NOW() WHERE id = #{id} AND is_deleted = 0")
    int deleteVirtualProduct(@Param("id") Long id);

    @Select("SELECT product_model FROM sys_aux_virtual_product WHERE id = #{id} AND is_deleted = 0 LIMIT 1")
    String findVirtualProductModelById(@Param("id") Long id);

    @Select("""
            SELECT id, product_model AS productModel, auto_reply_content AS autoReplyContent,
                   status, DATE_FORMAT(created_at, '%Y-%m-%d %H:%i:%s') AS createdAt
            FROM sys_aux_virtual_product
            WHERE is_deleted = 0 AND status = 1 AND product_model = #{productModel}
            LIMIT 1
            """)
    VirtualProductDTO findVirtualProductByModel(@Param("productModel") String productModel);

    @Select("""
            <script>
            SELECT COUNT(1) FROM sys_aux_message_push
            WHERE is_deleted = 0
              <if test='pushName != null and pushName != ""'>AND push_name LIKE CONCAT('%', #{pushName}, '%')</if>
              <if test='routeCode != null and routeCode != ""'>AND route_code LIKE CONCAT('%', #{routeCode}, '%')</if>
            </script>
            """)
    Long countMessagePushes(@Param("pushName") String pushName, @Param("routeCode") String routeCode);

    @Select("""
            <script>
            SELECT id, push_name AS pushName, route_code AS routeCode,
                   feishu_template_code AS feishuTemplateCode, is_enabled AS isEnabled,
                   push_rule AS pushRule, DATE_FORMAT(created_at, '%Y-%m-%d %H:%i:%s') AS createdAt
            FROM sys_aux_message_push
            WHERE is_deleted = 0
              <if test='pushName != null and pushName != ""'>AND push_name LIKE CONCAT('%', #{pushName}, '%')</if>
              <if test='routeCode != null and routeCode != ""'>AND route_code LIKE CONCAT('%', #{routeCode}, '%')</if>
            ORDER BY id DESC
            LIMIT #{offset}, #{pageSize}
            </script>
            """)
    List<MessagePushDTO> listMessagePushes(@Param("offset") int offset, @Param("pageSize") int pageSize,
                                           @Param("pushName") String pushName, @Param("routeCode") String routeCode);

    @Insert("""
            INSERT INTO sys_aux_message_push(push_name, route_code, feishu_template_code, is_enabled, push_rule, created_by, updated_by, is_deleted)
            VALUES(#{pushName}, #{routeCode}, #{feishuTemplateCode}, #{isEnabled}, #{pushRule}, 'system', 'system', 0)
            """)
    int insertMessagePush(MessagePushDTO dto);

    @Update("""
            <script>
            UPDATE sys_aux_message_push
            <set>
                <if test='dto.pushName != null'>push_name = #{dto.pushName},</if>
                <if test='dto.routeCode != null'>route_code = #{dto.routeCode},</if>
                <if test='dto.feishuTemplateCode != null'>feishu_template_code = #{dto.feishuTemplateCode},</if>
                <if test='dto.isEnabled != null'>is_enabled = #{dto.isEnabled},</if>
                <if test='dto.pushRule != null'>push_rule = #{dto.pushRule},</if>
                updated_by = 'system',
                updated_at = NOW()
            </set>
            WHERE id = #{id} AND is_deleted = 0
            </script>
            """)
    int updateMessagePush(@Param("id") Long id, @Param("dto") MessagePushDTO dto);

    @Update("UPDATE sys_aux_message_push SET is_deleted = 1, updated_at = NOW() WHERE id = #{id} AND is_deleted = 0")
    int deleteMessagePush(@Param("id") Long id);

        @Select("SELECT COUNT(1) FROM sys_aux_message_push WHERE is_deleted = 0 AND push_name = #{pushName}")
        Long countMessagePushByName(@Param("pushName") String pushName);

    @Select("""
            <script>
            SELECT COUNT(1) FROM sys_aux_leave_agent_product
            WHERE is_deleted = 0
              <if test='productModel != null and productModel != ""'>AND product_model LIKE CONCAT('%', #{productModel}, '%')</if>
              <if test='originalUserName != null and originalUserName != ""'>AND original_user_name LIKE CONCAT('%', #{originalUserName}, '%')</if>
            </script>
            """)
    Long countLeaveAgentProducts(@Param("productModel") String productModel, @Param("originalUserName") String originalUserName);

    @Select("""
            <script>
            SELECT id, product_model AS productModel, original_user_id AS originalUserId,
                   original_user_name AS originalUserName, agent_user_id AS agentUserId,
                   agent_user_name AS agentUserName, status
            FROM sys_aux_leave_agent_product
            WHERE is_deleted = 0
              <if test='productModel != null and productModel != ""'>AND product_model LIKE CONCAT('%', #{productModel}, '%')</if>
              <if test='originalUserName != null and originalUserName != ""'>AND original_user_name LIKE CONCAT('%', #{originalUserName}, '%')</if>
            ORDER BY id DESC
            LIMIT #{offset}, #{pageSize}
            </script>
            """)
    List<LeaveAgentProductDTO> listLeaveAgentProducts(@Param("offset") int offset, @Param("pageSize") int pageSize,
                                                      @Param("productModel") String productModel, @Param("originalUserName") String originalUserName);

    @Insert("""
            INSERT INTO sys_aux_leave_agent_product(product_model, original_user_id, original_user_name,
                                                    agent_user_id, agent_user_name, status, created_by, updated_by, is_deleted)
            VALUES(#{productModel}, #{originalUserId}, #{originalUserName},
                   #{agentUserId}, #{agentUserName}, #{status}, 'system', 'system', 0)
            """)
    int insertLeaveAgentProduct(LeaveAgentProductDTO dto);

    @Update("""
            <script>
            UPDATE sys_aux_leave_agent_product
            <set>
                <if test='dto.productModel != null'>product_model = #{dto.productModel},</if>
                <if test='dto.originalUserId != null'>original_user_id = #{dto.originalUserId},</if>
                <if test='dto.originalUserName != null'>original_user_name = #{dto.originalUserName},</if>
                <if test='dto.agentUserId != null'>agent_user_id = #{dto.agentUserId},</if>
                <if test='dto.agentUserName != null'>agent_user_name = #{dto.agentUserName},</if>
                <if test='dto.status != null'>status = #{dto.status},</if>
                updated_by = 'system',
                updated_at = NOW()
            </set>
            WHERE id = #{id} AND is_deleted = 0
            </script>
            """)
    int updateLeaveAgentProduct(@Param("id") Long id, @Param("dto") LeaveAgentProductDTO dto);

    @Update("UPDATE sys_aux_leave_agent_product SET is_deleted = 1, updated_at = NOW() WHERE id = #{id} AND is_deleted = 0")
    int deleteLeaveAgentProduct(@Param("id") Long id);

    @Select("""
            SELECT id, product_model AS productModel, original_user_id AS originalUserId,
                   original_user_name AS originalUserName, agent_user_id AS agentUserId,
                   agent_user_name AS agentUserName, status
            FROM sys_aux_leave_agent_product
            WHERE is_deleted = 0 AND status = 1 AND product_model = #{productModel} AND original_user_id = #{originalUserId}
            LIMIT 1
            """)
    LeaveAgentProductDTO findLeaveAgentProduct(@Param("productModel") String productModel, @Param("originalUserId") String originalUserId);

    @Select("""
            <script>
            SELECT COUNT(1) FROM sys_aux_area_coordinator
            WHERE is_deleted = 0
              <if test='saleDeptCode != null and saleDeptCode != ""'>AND sale_dept_code LIKE CONCAT('%', #{saleDeptCode}, '%')</if>
              <if test='provinceCode != null and provinceCode != ""'>AND province_code LIKE CONCAT('%', #{provinceCode}, '%')</if>
              <if test='coordinatorUserName != null and coordinatorUserName != ""'>AND coordinator_user_name LIKE CONCAT('%', #{coordinatorUserName}, '%')</if>
            </script>
            """)
    Long countAreaCoordinators(@Param("saleDeptCode") String saleDeptCode,
                               @Param("provinceCode") String provinceCode,
                               @Param("coordinatorUserName") String coordinatorUserName);

    @Select("""
            <script>
             SELECT id, sale_dept_code AS saleDeptCode, province_code AS provinceCode, region,
                   dept_keyword AS deptKeyword, project_keyword AS projectKeyword,
                   coordinator_user_id AS coordinatorUserId, coordinator_user_name AS coordinatorUserName,
                     agent_coordinator_user_id AS agentCoordinatorUserId,
                     agent_coordinator_user_name AS agentCoordinatorUserName,
                     DATE_FORMAT(created_at, '%Y-%m-%d %H:%i:%s') AS createdAt,
                   priority_no AS priorityNo, status
            FROM sys_aux_area_coordinator
            WHERE is_deleted = 0
              <if test='saleDeptCode != null and saleDeptCode != ""'>AND sale_dept_code LIKE CONCAT('%', #{saleDeptCode}, '%')</if>
              <if test='provinceCode != null and provinceCode != ""'>AND province_code LIKE CONCAT('%', #{provinceCode}, '%')</if>
              <if test='coordinatorUserName != null and coordinatorUserName != ""'>AND coordinator_user_name LIKE CONCAT('%', #{coordinatorUserName}, '%')</if>
            ORDER BY priority_no DESC, id DESC
            LIMIT #{offset}, #{pageSize}
            </script>
            """)
    List<AreaCoordinatorDTO> listAreaCoordinators(@Param("offset") int offset, @Param("pageSize") int pageSize,
                                                  @Param("saleDeptCode") String saleDeptCode,
                                                  @Param("provinceCode") String provinceCode,
                                                  @Param("coordinatorUserName") String coordinatorUserName);

    @Insert("""
             INSERT INTO sys_aux_area_coordinator(sale_dept_code, province_code, region, dept_keyword, project_keyword,
                                                        coordinator_user_id, coordinator_user_name, agent_coordinator_user_id, agent_coordinator_user_name,
                                                        priority_no, status,
                                                 created_by, updated_by, is_deleted)
             VALUES(#{saleDeptCode}, #{provinceCode}, #{region}, #{deptKeyword}, #{projectKeyword},
                     #{coordinatorUserId}, #{coordinatorUserName}, #{agentCoordinatorUserId}, #{agentCoordinatorUserName}, #{priorityNo}, #{status},
                   'system', 'system', 0)
            """)
    int insertAreaCoordinator(AreaCoordinatorDTO dto);

    @Update("""
            <script>
            UPDATE sys_aux_area_coordinator
            <set>
                <if test='dto.saleDeptCode != null'>sale_dept_code = #{dto.saleDeptCode},</if>
                <if test='dto.provinceCode != null'>province_code = #{dto.provinceCode},</if>
                <if test='dto.region != null'>region = #{dto.region},</if>
                <if test='dto.deptKeyword != null'>dept_keyword = #{dto.deptKeyword},</if>
                <if test='dto.projectKeyword != null'>project_keyword = #{dto.projectKeyword},</if>
                <if test='dto.coordinatorUserId != null'>coordinator_user_id = #{dto.coordinatorUserId},</if>
                <if test='dto.coordinatorUserName != null'>coordinator_user_name = #{dto.coordinatorUserName},</if>
                <if test='dto.agentCoordinatorUserId != null'>agent_coordinator_user_id = #{dto.agentCoordinatorUserId},</if>
                <if test='dto.agentCoordinatorUserName != null'>agent_coordinator_user_name = #{dto.agentCoordinatorUserName},</if>
                <if test='dto.priorityNo != null'>priority_no = #{dto.priorityNo},</if>
                <if test='dto.status != null'>status = #{dto.status},</if>
                updated_by = 'system',
                updated_at = NOW()
            </set>
            WHERE id = #{id} AND is_deleted = 0
            </script>
            """)
    int updateAreaCoordinator(@Param("id") Long id, @Param("dto") AreaCoordinatorDTO dto);

    @Update("UPDATE sys_aux_area_coordinator SET is_deleted = 1, updated_at = NOW() WHERE id = #{id} AND is_deleted = 0")
    int deleteAreaCoordinator(@Param("id") Long id);

    @Select("""
             SELECT id, sale_dept_code AS saleDeptCode, province_code AS provinceCode, region,
                   dept_keyword AS deptKeyword, project_keyword AS projectKeyword,
                   coordinator_user_id AS coordinatorUserId, coordinator_user_name AS coordinatorUserName,
                     agent_coordinator_user_id AS agentCoordinatorUserId,
                     agent_coordinator_user_name AS agentCoordinatorUserName,
                     DATE_FORMAT(created_at, '%Y-%m-%d %H:%i:%s') AS createdAt,
                   priority_no AS priorityNo, status
            FROM sys_aux_area_coordinator
            WHERE is_deleted = 0 AND status = 1
            ORDER BY priority_no DESC, id DESC
            """)
    List<AreaCoordinatorDTO> listActiveAreaCoordinatorRules();
}
