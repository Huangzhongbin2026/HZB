package com.ruijie.supplysystem.auxiliary.service;

import com.ruijie.supplysystem.auxiliary.dto.AreaCoordinatorDTO;
import com.ruijie.supplysystem.auxiliary.dto.LeaveAgentProductDTO;
import com.ruijie.supplysystem.auxiliary.dto.LeaveConfigDTO;
import com.ruijie.supplysystem.auxiliary.dto.MessagePushDTO;
import com.ruijie.supplysystem.auxiliary.dto.VirtualProductDTO;
import com.ruijie.supplysystem.dto.PageResult;

import java.util.List;
import java.util.Map;

public interface AuxiliaryService {
    PageResult<LeaveConfigDTO> queryLeaveConfigs(Integer pageNo, Integer pageSize, String userId, String userName);
    Boolean saveLeaveConfig(LeaveConfigDTO dto);
    Boolean updateLeaveConfig(String id, LeaveConfigDTO dto);
    Boolean deleteLeaveConfig(String id);
    Boolean importLeaveConfigs(List<LeaveConfigDTO> list);
    Map<String, Object> matchLeaveByUser(String userId, String date);

    PageResult<VirtualProductDTO> queryVirtualProducts(Integer pageNo, Integer pageSize, String productModel);
    Boolean saveVirtualProduct(VirtualProductDTO dto);
    Boolean updateVirtualProduct(String id, VirtualProductDTO dto);
    Boolean deleteVirtualProduct(String id);
    Boolean importVirtualProducts(List<VirtualProductDTO> list);
    Map<String, Object> matchVirtualProduct(String productModel);

    PageResult<MessagePushDTO> queryMessagePushes(Integer pageNo, Integer pageSize, String pushName, String routeCode);
    Boolean saveMessagePush(MessagePushDTO dto);
    Boolean updateMessagePush(String id, MessagePushDTO dto);
    Boolean deleteMessagePush(String id);
    Boolean importMessagePushes(List<MessagePushDTO> list);

    PageResult<LeaveAgentProductDTO> queryLeaveAgentProducts(Integer pageNo, Integer pageSize, String productModel, String originalUserName);
    Boolean saveLeaveAgentProduct(LeaveAgentProductDTO dto);
    Boolean updateLeaveAgentProduct(String id, LeaveAgentProductDTO dto);
    Boolean deleteLeaveAgentProduct(String id);
    Boolean importLeaveAgentProducts(List<LeaveAgentProductDTO> list);
    Map<String, Object> matchLeaveAgentProduct(String productModel, String originalUserId);

    PageResult<AreaCoordinatorDTO> queryAreaCoordinators(Integer pageNo, Integer pageSize, String saleDeptCode, String provinceCode, String coordinatorUserName);
    Boolean saveAreaCoordinator(AreaCoordinatorDTO dto);
    Boolean updateAreaCoordinator(String id, AreaCoordinatorDTO dto);
    Boolean deleteAreaCoordinator(String id);
    Boolean importAreaCoordinators(List<AreaCoordinatorDTO> list);
    Map<String, Object> matchAreaCoordinator(String saleDeptCode, String provinceCode, String deptKeyword, String projectKeyword);
}
