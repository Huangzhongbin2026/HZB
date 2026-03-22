package com.ruijie.supplysystem.auxiliary.service.impl;

import com.ruijie.supplysystem.auxiliary.dto.AreaCoordinatorDTO;
import com.ruijie.supplysystem.auxiliary.dto.LeaveAgentProductDTO;
import com.ruijie.supplysystem.auxiliary.dto.LeaveConfigDTO;
import com.ruijie.supplysystem.auxiliary.dto.MessagePushDTO;
import com.ruijie.supplysystem.auxiliary.dto.VirtualProductDTO;
import com.ruijie.supplysystem.auxiliary.service.AuxiliaryService;
import com.ruijie.supplysystem.common.BusinessException;
import com.ruijie.supplysystem.dto.PageResult;
import com.ruijie.supplysystem.mapper.AuxiliaryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuxiliaryServiceImpl implements AuxiliaryService {

    private static final String CACHE_KEY_VIRTUAL = "aux:virtual:";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AuxiliaryMapper auxiliaryMapper;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public PageResult<LeaveConfigDTO> queryLeaveConfigs(Integer pageNo, Integer pageSize, String userId, String userName) {
        int no = normalizePageNo(pageNo);
        int size = normalizePageSize(pageSize);
        Long total = auxiliaryMapper.countLeaveConfigs(userId, userName);
        List<LeaveConfigDTO> list = auxiliaryMapper.listLeaveConfigs((no - 1) * size, size, userId, userName);
        return new PageResult<>(list, total == null ? 0L : total);
    }

    @Override
    public Boolean saveLeaveConfig(LeaveConfigDTO dto) {
        validateLeaveConfig(dto, null);
        return auxiliaryMapper.insertLeaveConfig(dto) > 0;
    }

    @Override
    public Boolean updateLeaveConfig(String id, LeaveConfigDTO dto) {
        validateLeaveConfig(dto, id);
        return auxiliaryMapper.updateLeaveConfig(parseId(id), dto) > 0;
    }

    @Override
    public Boolean deleteLeaveConfig(String id) {
        return auxiliaryMapper.deleteLeaveConfig(parseId(id)) > 0;
    }

    @Override
    public Boolean importLeaveConfigs(List<LeaveConfigDTO> list) {
        if (list == null || list.isEmpty()) {
            return Boolean.TRUE;
        }
        for (LeaveConfigDTO item : list) {
            validateLeaveConfig(item, null);
            auxiliaryMapper.insertLeaveConfig(item);
        }
        return Boolean.TRUE;
    }

    @Override
    public Map<String, Object> matchLeaveByUser(String userId, String date) {
        LocalDateTime point = date == null || date.isBlank() ? LocalDateTime.now() : parseTime(date, "日期格式错误，正确格式 yyyy-MM-dd HH:mm:ss");
        boolean onLeave = auxiliaryMapper.listLeaveConfigsByUserId(userId).stream()
                .filter(x -> Boolean.TRUE.equals(x.getStatus()))
                .anyMatch(x -> {
                    LocalDateTime start = parseTime(x.getLeaveStart(), "请假开始时间格式错误");
                    LocalDateTime end = parseTime(x.getLeaveEnd(), "请假结束时间格式错误");
                    return !point.isBefore(start) && !point.isAfter(end);
                });
        return Map.of("onLeave", onLeave, "date", date == null ? "" : date);
    }

    @Override
    public PageResult<VirtualProductDTO> queryVirtualProducts(Integer pageNo, Integer pageSize, String productModel) {
        int no = normalizePageNo(pageNo);
        int size = normalizePageSize(pageSize);
        Long total = auxiliaryMapper.countVirtualProducts(productModel);
        List<VirtualProductDTO> list = auxiliaryMapper.listVirtualProducts((no - 1) * size, size, productModel);
        return new PageResult<>(list, total == null ? 0L : total);
    }

    @Override
    public Boolean saveVirtualProduct(VirtualProductDTO dto) {
        validateVirtualProduct(dto, null);
        boolean ok = auxiliaryMapper.insertVirtualProduct(dto) > 0;
        evictVirtualCache(dto.getProductModel());
        return ok;
    }

    @Override
    public Boolean updateVirtualProduct(String id, VirtualProductDTO dto) {
        validateVirtualProduct(dto, id);
        Long rowId = parseId(id);
        String oldModel = auxiliaryMapper.findVirtualProductModelById(rowId);
        if (oldModel != null && !Objects.equals(oldModel, dto.getProductModel())) {
            evictVirtualCache(oldModel);
        }
        evictVirtualCache(dto.getProductModel());
        return auxiliaryMapper.updateVirtualProduct(rowId, dto) > 0;
    }

    @Override
    public Boolean deleteVirtualProduct(String id) {
        Long rowId = parseId(id);
        String oldModel = auxiliaryMapper.findVirtualProductModelById(rowId);
        if (oldModel != null) {
            evictVirtualCache(oldModel);
        }
        return auxiliaryMapper.deleteVirtualProduct(rowId) > 0;
    }

    @Override
    public Boolean importVirtualProducts(List<VirtualProductDTO> list) {
        if (list == null || list.isEmpty()) {
            return Boolean.TRUE;
        }
        for (VirtualProductDTO item : list) {
            validateVirtualProduct(item, null);
            auxiliaryMapper.insertVirtualProduct(item);
            evictVirtualCache(item.getProductModel());
        }
        return Boolean.TRUE;
    }

    @Override
    public Map<String, Object> matchVirtualProduct(String productModel) {
        if (productModel == null || productModel.isBlank()) {
            return Map.of("autoReplyContent", "未命中虚拟产品规则");
        }

        String cacheKey = CACHE_KEY_VIRTUAL + productModel.trim();
        try {
            String cached = stringRedisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                return Map.of("autoReplyContent", cached);
            }
        } catch (Exception ex) {
            log.warn("Read virtual product cache failed, fallback to memory match, key={}", cacheKey, ex);
        }

        VirtualProductDTO hit = auxiliaryMapper.findVirtualProductByModel(productModel.trim());
        if (hit == null || !Boolean.TRUE.equals(hit.getStatus())) {
            return Map.of("autoReplyContent", "未命中虚拟产品规则");
        }
        String content = hit.getAutoReplyContent() == null ? "" : hit.getAutoReplyContent();
        try {
            stringRedisTemplate.opsForValue().set(cacheKey, content, Duration.ofMinutes(10));
        } catch (Exception ex) {
            log.warn("Write virtual product cache failed, key={}", cacheKey, ex);
        }
        return Map.of("autoReplyContent", content);
    }

    @Override
    public PageResult<MessagePushDTO> queryMessagePushes(Integer pageNo, Integer pageSize, String pushName, String routeCode) {
        int no = normalizePageNo(pageNo);
        int size = normalizePageSize(pageSize);
        Long total = auxiliaryMapper.countMessagePushes(pushName, routeCode);
        List<MessagePushDTO> list = auxiliaryMapper.listMessagePushes((no - 1) * size, size, pushName, routeCode);
        return new PageResult<>(list, total == null ? 0L : total);
    }

    @Override
    public Boolean saveMessagePush(MessagePushDTO dto) {
        validateMessagePush(dto, null);
        return auxiliaryMapper.insertMessagePush(dto) > 0;
    }

    @Override
    public Boolean updateMessagePush(String id, MessagePushDTO dto) {
        validateMessagePush(dto, id);
        return auxiliaryMapper.updateMessagePush(parseId(id), dto) > 0;
    }

    @Override
    public Boolean deleteMessagePush(String id) {
        return auxiliaryMapper.deleteMessagePush(parseId(id)) > 0;
    }

    @Override
    public Boolean importMessagePushes(List<MessagePushDTO> list) {
        if (list == null || list.isEmpty()) {
            return Boolean.TRUE;
        }
        for (MessagePushDTO item : list) {
            validateMessagePush(item, null);
            auxiliaryMapper.insertMessagePush(item);
        }
        return Boolean.TRUE;
    }

    @Override
    public PageResult<LeaveAgentProductDTO> queryLeaveAgentProducts(Integer pageNo, Integer pageSize, String productModel, String originalUserName) {
        int no = normalizePageNo(pageNo);
        int size = normalizePageSize(pageSize);
        Long total = auxiliaryMapper.countLeaveAgentProducts(productModel, originalUserName);
        List<LeaveAgentProductDTO> list = auxiliaryMapper.listLeaveAgentProducts((no - 1) * size, size, productModel, originalUserName);
        return new PageResult<>(list, total == null ? 0L : total);
    }

    @Override
    public Boolean saveLeaveAgentProduct(LeaveAgentProductDTO dto) {
        validateLeaveAgentProduct(dto, null);
        return auxiliaryMapper.insertLeaveAgentProduct(dto) > 0;
    }

    @Override
    public Boolean updateLeaveAgentProduct(String id, LeaveAgentProductDTO dto) {
        validateLeaveAgentProduct(dto, id);
        return auxiliaryMapper.updateLeaveAgentProduct(parseId(id), dto) > 0;
    }

    @Override
    public Boolean deleteLeaveAgentProduct(String id) {
        return auxiliaryMapper.deleteLeaveAgentProduct(parseId(id)) > 0;
    }

    @Override
    public Boolean importLeaveAgentProducts(List<LeaveAgentProductDTO> list) {
        if (list == null || list.isEmpty()) {
            return Boolean.TRUE;
        }
        for (LeaveAgentProductDTO item : list) {
            validateLeaveAgentProduct(item, null);
            auxiliaryMapper.insertLeaveAgentProduct(item);
        }
        return Boolean.TRUE;
    }

    @Override
    public Map<String, Object> matchLeaveAgentProduct(String productModel, String originalUserId) {
        LeaveAgentProductDTO hit = auxiliaryMapper.findLeaveAgentProduct(productModel, originalUserId);
        if (hit == null || !Boolean.TRUE.equals(hit.getStatus())) {
            return Map.of("agentUserId", "", "agentUserName", "");
        }
        return Map.of("agentUserId", hit.getAgentUserId(), "agentUserName", hit.getAgentUserName());
    }

    @Override
    public PageResult<AreaCoordinatorDTO> queryAreaCoordinators(Integer pageNo, Integer pageSize, String saleDeptCode, String provinceCode, String coordinatorUserName) {
        int no = normalizePageNo(pageNo);
        int size = normalizePageSize(pageSize);
        Long total = auxiliaryMapper.countAreaCoordinators(saleDeptCode, provinceCode, coordinatorUserName);
        List<AreaCoordinatorDTO> list = auxiliaryMapper.listAreaCoordinators((no - 1) * size, size, saleDeptCode, provinceCode, coordinatorUserName);
        return new PageResult<>(list, total == null ? 0L : total);
    }

    @Override
    public Boolean saveAreaCoordinator(AreaCoordinatorDTO dto) {
        validateAreaCoordinator(dto);
        return auxiliaryMapper.insertAreaCoordinator(dto) > 0;
    }

    @Override
    public Boolean updateAreaCoordinator(String id, AreaCoordinatorDTO dto) {
        validateAreaCoordinator(dto);
        return auxiliaryMapper.updateAreaCoordinator(parseId(id), dto) > 0;
    }

    @Override
    public Boolean deleteAreaCoordinator(String id) {
        return auxiliaryMapper.deleteAreaCoordinator(parseId(id)) > 0;
    }

    @Override
    public Boolean importAreaCoordinators(List<AreaCoordinatorDTO> list) {
        if (list == null || list.isEmpty()) {
            return Boolean.TRUE;
        }
        for (AreaCoordinatorDTO item : list) {
            auxiliaryMapper.insertAreaCoordinator(item);
        }
        return Boolean.TRUE;
    }

    @Override
    public Map<String, Object> matchAreaCoordinator(String saleDeptCode, String provinceCode, String deptKeyword, String projectKeyword) {
        List<AreaCoordinatorDTO> activeRules = auxiliaryMapper.listActiveAreaCoordinatorRules();
        AreaCoordinatorDTO hit = activeRules.stream()
            .filter(x -> isEqual(x.getSaleDeptCode(), saleDeptCode) && isEqual(x.getProvinceCode(), provinceCode))
            .filter(x -> keywordMatch(x.getDeptKeyword(), deptKeyword) && keywordMatch(x.getProjectKeyword(), projectKeyword))
            .findFirst()
            .orElseGet(() -> activeRules.stream()
                .filter(x -> keywordMatch(x.getDeptKeyword(), deptKeyword) || keywordMatch(x.getProjectKeyword(), projectKeyword))
                .findFirst().orElse(null));
        return hit == null
            ? Map.of("coordinatorUserId", "", "coordinatorUserName", "", "agentCoordinatorUserId", "", "agentCoordinatorUserName", "")
            : Map.of(
                "coordinatorUserId", hit.getCoordinatorUserId(),
                "coordinatorUserName", hit.getCoordinatorUserName(),
                "agentCoordinatorUserId", hit.getAgentCoordinatorUserId() == null ? "" : hit.getAgentCoordinatorUserId(),
                "agentCoordinatorUserName", hit.getAgentCoordinatorUserName() == null ? "" : hit.getAgentCoordinatorUserName()
            );
    }

    private void evictVirtualCache(String productModel) {
        if (productModel == null || productModel.isBlank()) {
            return;
        }
        try {
            stringRedisTemplate.delete(CACHE_KEY_VIRTUAL + productModel.trim());
        } catch (Exception ex) {
            log.warn("Delete virtual product cache failed, model={}", productModel, ex);
        }
    }

    private void validateLeaveConfig(LeaveConfigDTO dto, String currentId) {
        if (dto == null || dto.getUserId() == null || dto.getUserId().isBlank()) {
            throw new BusinessException(2001, "请假人员不能为空");
        }
        if (dto.getUserName() == null || dto.getUserName().isBlank()) {
            throw new BusinessException(2001, "请假人员名称不能为空");
        }
        Long sameUserCount = auxiliaryMapper.countLeaveConfigByUserId(dto.getUserId());
        if ((currentId == null || currentId.isBlank()) && sameUserCount != null && sameUserCount > 0) {
            throw new BusinessException(2004, "同一人员请假配置已存在");
        }
        LocalDateTime start = parseTime(dto.getLeaveStart(), "请假开始时间格式错误，正确格式 yyyy-MM-dd HH:mm:ss");
        LocalDateTime end = parseTime(dto.getLeaveEnd(), "请假结束时间格式错误，正确格式 yyyy-MM-dd HH:mm:ss");
        if (start.isAfter(end)) {
            throw new BusinessException(2002, "请假开始时间不能晚于结束时间");
        }
        boolean overlap = auxiliaryMapper.listLeaveConfigsByUserId(dto.getUserId()).stream()
                .filter(item -> !Objects.equals(item.getId(), currentId))
                .filter(item -> Objects.equals(item.getUserId(), dto.getUserId()))
                .anyMatch(item -> {
                    LocalDateTime existsStart = parseTime(item.getLeaveStart(), "历史请假时间格式错误");
                    LocalDateTime existsEnd = parseTime(item.getLeaveEnd(), "历史请假时间格式错误");
                    return !start.isAfter(existsEnd) && !end.isBefore(existsStart);
                });
        if (overlap) {
            throw new BusinessException(2003, "同一人员请假时间存在重叠");
        }
    }

    private void validateVirtualProduct(VirtualProductDTO dto, String currentId) {
        if (dto == null || dto.getProductModel() == null || dto.getProductModel().isBlank()) {
            throw new BusinessException(2011, "产品型号不能为空");
        }
        if (dto.getAutoReplyContent() == null || dto.getAutoReplyContent().isBlank()) {
            throw new BusinessException(2013, "自动回复内容不能为空");
        }
        List<VirtualProductDTO> all = auxiliaryMapper.listVirtualProducts(0, Integer.MAX_VALUE, dto.getProductModel());
        boolean exists = all.stream().anyMatch(item -> !Objects.equals(item.getId(), currentId) && isEqual(item.getProductModel(), dto.getProductModel()));
        if (exists) {
            throw new BusinessException(2012, "虚拟产品型号重复");
        }
    }

    private void validateMessagePush(MessagePushDTO dto, String currentId) {
        if (dto == null || dto.getPushName() == null || dto.getPushName().isBlank()) {
            throw new BusinessException(2020, "消息推送名称不能为空");
        }
        if (dto.getRouteCode() == null || dto.getRouteCode().isBlank()) {
            throw new BusinessException(2021, "消息路由不能为空");
        }
        Long sameName = auxiliaryMapper.countMessagePushByName(dto.getPushName());
        if ((currentId == null || currentId.isBlank()) && sameName != null && sameName > 0) {
            throw new BusinessException(2023, "消息推送名称重复");
        }
        List<MessagePushDTO> all = auxiliaryMapper.listMessagePushes(0, Integer.MAX_VALUE, "", dto.getRouteCode());
        boolean exists = all.stream().anyMatch(item -> !Objects.equals(item.getId(), currentId) && isEqual(item.getRouteCode(), dto.getRouteCode()));
        if (exists) {
            throw new BusinessException(2022, "消息路由编码重复");
        }
    }

    private void validateLeaveAgentProduct(LeaveAgentProductDTO dto, String currentId) {
        if (dto == null || dto.getProductModel() == null || dto.getProductModel().isBlank()) {
            throw new BusinessException(2031, "产品型号不能为空");
        }
        if (dto.getOriginalUserId() == null || dto.getOriginalUserId().isBlank()) {
            throw new BusinessException(2032, "原统筹ID不能为空");
        }
        if (dto.getAgentUserId() == null || dto.getAgentUserId().isBlank()) {
            throw new BusinessException(2033, "代理统筹ID不能为空");
        }
        if (dto.getOriginalUserName() == null || dto.getOriginalUserName().isBlank()) {
            throw new BusinessException(2035, "原产品统筹名称不能为空");
        }
        if (dto.getAgentUserName() == null || dto.getAgentUserName().isBlank()) {
            throw new BusinessException(2036, "代理产品统筹名称不能为空");
        }
        boolean exists = auxiliaryMapper.listLeaveAgentProducts(0, Integer.MAX_VALUE, dto.getProductModel(), "").stream()
                .filter(item -> !Objects.equals(item.getId(), currentId))
                .anyMatch(item -> isEqual(item.getProductModel(), dto.getProductModel())
                        && isEqual(item.getOriginalUserId(), dto.getOriginalUserId()));
        if (exists) {
            throw new BusinessException(2034, "同一原统筹的产品代理规则重复");
        }
    }

    private void validateAreaCoordinator(AreaCoordinatorDTO dto) {
        if (dto == null || dto.getSaleDeptCode() == null || dto.getSaleDeptCode().isBlank()) {
            throw new BusinessException(2041, "销售部门不能为空");
        }
        if (dto.getProvinceCode() == null || dto.getProvinceCode().isBlank()) {
            throw new BusinessException(2042, "省份不能为空");
        }
        if (dto.getCoordinatorUserId() == null || dto.getCoordinatorUserId().isBlank()) {
            throw new BusinessException(2043, "区域统筹不能为空");
        }
        if (dto.getAgentCoordinatorUserId() == null || dto.getAgentCoordinatorUserId().isBlank()) {
            throw new BusinessException(2044, "代理区域统筹不能为空");
        }
    }

    private LocalDateTime parseTime(String value, String errMsg) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(2000, errMsg);
        }
        try {
            return LocalDateTime.parse(value.trim(), DATE_TIME_FORMATTER);
        } catch (DateTimeParseException ex) {
            throw new BusinessException(2000, errMsg);
        }
    }

    private boolean isEqual(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        return a.trim().equalsIgnoreCase(b.trim());
    }

    private boolean keywordMatch(String ruleKeyword, String incomingKeyword) {
        if (ruleKeyword == null || ruleKeyword.isBlank()) {
            return true;
        }
        if (incomingKeyword == null || incomingKeyword.isBlank()) {
            return false;
        }
        return incomingKeyword.contains(ruleKeyword.trim());
    }

    private int normalizePageNo(Integer pageNo) {
        return pageNo == null || pageNo < 1 ? 1 : pageNo;
    }

    private int normalizePageSize(Integer pageSize) {
        return pageSize == null || pageSize < 1 ? 10 : pageSize;
    }

    private Long parseId(String id) {
        try {
            return Long.parseLong(id);
        } catch (Exception ex) {
            throw new BusinessException(2100, "非法ID: " + id);
        }
    }
}
