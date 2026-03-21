package com.ruijie.supplysystem.auxiliary.service.impl;

import com.ruijie.supplysystem.auxiliary.dto.AreaCoordinatorDTO;
import com.ruijie.supplysystem.auxiliary.dto.LeaveAgentProductDTO;
import com.ruijie.supplysystem.auxiliary.dto.LeaveConfigDTO;
import com.ruijie.supplysystem.auxiliary.dto.MessagePushDTO;
import com.ruijie.supplysystem.auxiliary.dto.VirtualProductDTO;
import com.ruijie.supplysystem.auxiliary.service.AuxiliaryService;
import com.ruijie.supplysystem.common.BusinessException;
import com.ruijie.supplysystem.dto.PageResult;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AuxiliaryServiceImpl implements AuxiliaryService {

    private static final String CACHE_KEY_VIRTUAL = "aux:virtual:";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final StringRedisTemplate stringRedisTemplate;

    public AuxiliaryServiceImpl(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    private final List<LeaveConfigDTO> leaveConfigs = new CopyOnWriteArrayList<>();
    private final List<VirtualProductDTO> virtualProducts = new CopyOnWriteArrayList<>();
    private final List<MessagePushDTO> messagePushes = new CopyOnWriteArrayList<>();
    private final List<LeaveAgentProductDTO> leaveAgentProducts = new CopyOnWriteArrayList<>();
    private final List<AreaCoordinatorDTO> areaCoordinators = new CopyOnWriteArrayList<>();

    @PostConstruct
    public void init() {
        if (!leaveConfigs.isEmpty()) {
            return;
        }

        LeaveConfigDTO leave = new LeaveConfigDTO();
        leave.setId(UUID.randomUUID().toString());
        leave.setUserId("U1001");
        leave.setUserName("张三");
        leave.setLeaveStart("2026-01-01 09:00:00");
        leave.setLeaveEnd("2026-01-03 18:00:00");
        leave.setLeaveReason("年假");
        leave.setStatus(Boolean.TRUE);
        leaveConfigs.add(leave);

        VirtualProductDTO virtual = new VirtualProductDTO();
        virtual.setId(UUID.randomUUID().toString());
        virtual.setProductModel("VX-100");
        virtual.setAutoReplyContent("该型号属于虚拟产品，请走虚拟处理流程。");
        virtual.setStatus(Boolean.TRUE);
        virtual.setCreatedAt("2026-01-01 10:00:00");
        virtualProducts.add(virtual);

        MessagePushDTO push = new MessagePushDTO();
        push.setId(UUID.randomUUID().toString());
        push.setPushName("请假代理变更通知");
        push.setRouteCode("LEAVE_AGENT_CHANGE");
        push.setFeishuTemplateCode("TPL_LEAVE_AGENT");
        push.setIsEnabled(Boolean.TRUE);
        push.setPushRule("agentUserChanged");
        push.setCreatedAt("2026-01-01 11:00:00");
        messagePushes.add(push);

        LeaveAgentProductDTO agent = new LeaveAgentProductDTO();
        agent.setId(UUID.randomUUID().toString());
        agent.setProductModel("RG-AP180");
        agent.setOriginalUserId("U1001");
        agent.setOriginalUserName("张三");
        agent.setAgentUserId("U2001");
        agent.setAgentUserName("李四");
        agent.setStatus(Boolean.TRUE);
        leaveAgentProducts.add(agent);

        AreaCoordinatorDTO coordinator = new AreaCoordinatorDTO();
        coordinator.setId(UUID.randomUUID().toString());
        coordinator.setSaleDeptCode("SOUTH-A");
        coordinator.setProvinceCode("GD");
        coordinator.setDeptKeyword("深圳");
        coordinator.setProjectKeyword("教育");
        coordinator.setCoordinatorUserId("U3001");
        coordinator.setCoordinatorUserName("王五");
        coordinator.setPriorityNo(100);
        coordinator.setStatus(Boolean.TRUE);
        areaCoordinators.add(coordinator);
    }

    @Override
    public PageResult<LeaveConfigDTO> queryLeaveConfigs(Integer pageNo, Integer pageSize, String userId, String userName) {
        List<LeaveConfigDTO> filtered = leaveConfigs.stream()
                .filter(x -> contains(x.getUserId(), userId))
                .filter(x -> contains(x.getUserName(), userName))
                .collect(Collectors.toList());
        return page(filtered, pageNo, pageSize);
    }

    @Override
    public Boolean saveLeaveConfig(LeaveConfigDTO dto) {
        validateLeaveConfig(dto, null);
        fillId(dto);
        leaveConfigs.add(dto);
        return Boolean.TRUE;
    }

    @Override
    public Boolean updateLeaveConfig(String id, LeaveConfigDTO dto) {
        validateLeaveConfig(dto, id);
        return replaceById(leaveConfigs, id, dto);
    }

    @Override
    public Boolean deleteLeaveConfig(String id) {
        return removeById(leaveConfigs, id);
    }

    @Override
    public Boolean importLeaveConfigs(List<LeaveConfigDTO> list) {
        list.forEach(item -> validateLeaveConfig(item, null));
        list.forEach(this::fillId);
        leaveConfigs.addAll(list);
        return Boolean.TRUE;
    }

    @Override
    public Map<String, Object> matchLeaveByUser(String userId, String date) {
        boolean onLeave = leaveConfigs.stream().anyMatch(x -> x.getStatus() != null && x.getStatus() && userId != null && userId.equals(x.getUserId()));
        return Map.of("onLeave", onLeave, "date", date == null ? "" : date);
    }

    @Override
    public PageResult<VirtualProductDTO> queryVirtualProducts(Integer pageNo, Integer pageSize, String productModel) {
        List<VirtualProductDTO> filtered = virtualProducts.stream()
                .filter(x -> contains(x.getProductModel(), productModel))
                .collect(Collectors.toList());
        return page(filtered, pageNo, pageSize);
    }

    @Override
    public Boolean saveVirtualProduct(VirtualProductDTO dto) {
        validateVirtualProduct(dto, null);
        fillId(dto);
        virtualProducts.add(dto);
        evictVirtualCache(dto.getProductModel());
        return Boolean.TRUE;
    }

    @Override
    public Boolean updateVirtualProduct(String id, VirtualProductDTO dto) {
        validateVirtualProduct(dto, id);
        String oldModel = findVirtualModelById(id);
        if (oldModel != null && !Objects.equals(oldModel, dto.getProductModel())) {
            evictVirtualCache(oldModel);
        }
        evictVirtualCache(dto.getProductModel());
        return replaceById(virtualProducts, id, dto);
    }

    @Override
    public Boolean deleteVirtualProduct(String id) {
        String oldModel = findVirtualModelById(id);
        if (oldModel != null) {
            evictVirtualCache(oldModel);
        }
        return removeById(virtualProducts, id);
    }

    @Override
    public Boolean importVirtualProducts(List<VirtualProductDTO> list) {
        list.forEach(item -> validateVirtualProduct(item, null));
        list.forEach(this::fillId);
        virtualProducts.addAll(list);
        list.forEach(item -> evictVirtualCache(item.getProductModel()));
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

        return virtualProducts.stream()
                .filter(x -> x.getStatus() != null && x.getStatus())
                .filter(x -> contains(x.getProductModel(), productModel))
                .findFirst()
                .map(x -> {
                    String content = x.getAutoReplyContent() == null ? "" : x.getAutoReplyContent();
                    try {
                        stringRedisTemplate.opsForValue().set(cacheKey, content, Duration.ofMinutes(10));
                    } catch (Exception ex) {
                        log.warn("Write virtual product cache failed, key={}", cacheKey, ex);
                    }
                    return Map.<String, Object>of("autoReplyContent", content);
                })
                .orElse(Map.of("autoReplyContent", "未命中虚拟产品规则"));
    }

    @Override
    public PageResult<MessagePushDTO> queryMessagePushes(Integer pageNo, Integer pageSize, String pushName, String routeCode) {
        List<MessagePushDTO> filtered = messagePushes.stream()
                .filter(x -> contains(x.getPushName(), pushName))
                .filter(x -> contains(x.getRouteCode(), routeCode))
                .collect(Collectors.toList());
        return page(filtered, pageNo, pageSize);
    }

    @Override
    public Boolean saveMessagePush(MessagePushDTO dto) {
        validateMessagePush(dto, null);
        fillId(dto);
        messagePushes.add(dto);
        return Boolean.TRUE;
    }

    @Override
    public Boolean updateMessagePush(String id, MessagePushDTO dto) {
        validateMessagePush(dto, id);
        return replaceById(messagePushes, id, dto);
    }

    @Override
    public Boolean deleteMessagePush(String id) {
        return removeById(messagePushes, id);
    }

    @Override
    public Boolean importMessagePushes(List<MessagePushDTO> list) {
        list.forEach(item -> validateMessagePush(item, null));
        list.forEach(this::fillId);
        messagePushes.addAll(list);
        return Boolean.TRUE;
    }

    @Override
    public PageResult<LeaveAgentProductDTO> queryLeaveAgentProducts(Integer pageNo, Integer pageSize, String productModel, String originalUserName) {
        List<LeaveAgentProductDTO> filtered = leaveAgentProducts.stream()
                .filter(x -> contains(x.getProductModel(), productModel))
                .filter(x -> contains(x.getOriginalUserName(), originalUserName))
                .collect(Collectors.toList());
        return page(filtered, pageNo, pageSize);
    }

    @Override
    public Boolean saveLeaveAgentProduct(LeaveAgentProductDTO dto) {
        validateLeaveAgentProduct(dto, null);
        fillId(dto);
        leaveAgentProducts.add(dto);
        return Boolean.TRUE;
    }

    @Override
    public Boolean updateLeaveAgentProduct(String id, LeaveAgentProductDTO dto) {
        validateLeaveAgentProduct(dto, id);
        return replaceById(leaveAgentProducts, id, dto);
    }

    @Override
    public Boolean deleteLeaveAgentProduct(String id) {
        return removeById(leaveAgentProducts, id);
    }

    @Override
    public Boolean importLeaveAgentProducts(List<LeaveAgentProductDTO> list) {
        list.forEach(item -> validateLeaveAgentProduct(item, null));
        list.forEach(this::fillId);
        leaveAgentProducts.addAll(list);
        return Boolean.TRUE;
    }

    @Override
    public Map<String, Object> matchLeaveAgentProduct(String productModel, String originalUserId) {
        return leaveAgentProducts.stream()
                .filter(x -> x.getStatus() != null && x.getStatus())
                .filter(x -> contains(x.getProductModel(), productModel))
                .filter(x -> contains(x.getOriginalUserId(), originalUserId))
                .findFirst()
                .map(x -> Map.<String, Object>of("agentUserId", x.getAgentUserId(), "agentUserName", x.getAgentUserName()))
                .orElse(Map.of("agentUserId", "", "agentUserName", ""));
    }

    @Override
    public PageResult<AreaCoordinatorDTO> queryAreaCoordinators(Integer pageNo, Integer pageSize, String saleDeptCode, String provinceCode, String coordinatorUserName) {
        List<AreaCoordinatorDTO> filtered = areaCoordinators.stream()
                .filter(x -> contains(x.getSaleDeptCode(), saleDeptCode))
                .filter(x -> contains(x.getProvinceCode(), provinceCode))
                .filter(x -> contains(x.getCoordinatorUserName(), coordinatorUserName))
                .sorted(Comparator.comparing(AreaCoordinatorDTO::getPriorityNo, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
        return page(filtered, pageNo, pageSize);
    }

    @Override
    public Boolean saveAreaCoordinator(AreaCoordinatorDTO dto) {
        fillId(dto);
        areaCoordinators.add(dto);
        return Boolean.TRUE;
    }

    @Override
    public Boolean updateAreaCoordinator(String id, AreaCoordinatorDTO dto) {
        return replaceById(areaCoordinators, id, dto);
    }

    @Override
    public Boolean deleteAreaCoordinator(String id) {
        return removeById(areaCoordinators, id);
    }

    @Override
    public Boolean importAreaCoordinators(List<AreaCoordinatorDTO> list) {
        list.forEach(this::fillId);
        areaCoordinators.addAll(list);
        return Boolean.TRUE;
    }

    @Override
    public Map<String, Object> matchAreaCoordinator(String saleDeptCode, String provinceCode, String deptKeyword, String projectKeyword) {
        List<AreaCoordinatorDTO> activeRules = areaCoordinators.stream()
                .filter(x -> x.getStatus() != null && x.getStatus())
                .sorted(Comparator.comparing(AreaCoordinatorDTO::getPriorityNo, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        List<AreaCoordinatorDTO> precise = activeRules.stream()
                .filter(x -> isEqual(x.getSaleDeptCode(), saleDeptCode))
                .filter(x -> isEqual(x.getProvinceCode(), provinceCode))
                .toList();

        AreaCoordinatorDTO hit = precise.stream()
                .filter(x -> keywordMatch(x.getDeptKeyword(), deptKeyword) && keywordMatch(x.getProjectKeyword(), projectKeyword))
                .findFirst()
                .orElseGet(() -> precise.isEmpty() ? null : precise.get(0));

        if (hit == null) {
            hit = activeRules.stream()
                    .filter(x -> keywordMatch(x.getDeptKeyword(), deptKeyword) || keywordMatch(x.getProjectKeyword(), projectKeyword))
                    .findFirst()
                    .orElse(null);
        }

        if (hit == null) {
            return Map.of("coordinatorUserId", "", "coordinatorUserName", "");
        }
        return Map.of("coordinatorUserId", hit.getCoordinatorUserId(), "coordinatorUserName", hit.getCoordinatorUserName());
    }

    private String findVirtualModelById(String id) {
        return virtualProducts.stream()
                .filter(item -> id != null && id.equals(item.getId()))
                .map(VirtualProductDTO::getProductModel)
                .findFirst()
                .orElse(null);
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
        LocalDateTime start = parseTime(dto.getLeaveStart(), "请假开始时间格式错误，正确格式 yyyy-MM-dd HH:mm:ss");
        LocalDateTime end = parseTime(dto.getLeaveEnd(), "请假结束时间格式错误，正确格式 yyyy-MM-dd HH:mm:ss");
        if (start.isAfter(end)) {
            throw new BusinessException(2002, "请假开始时间不能晚于结束时间");
        }
        boolean overlap = leaveConfigs.stream()
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
        boolean exists = virtualProducts.stream()
                .filter(item -> !Objects.equals(item.getId(), currentId))
                .anyMatch(item -> isEqual(item.getProductModel(), dto.getProductModel()));
        if (exists) {
            throw new BusinessException(2012, "虚拟产品型号重复");
        }
    }

    private void validateMessagePush(MessagePushDTO dto, String currentId) {
        if (dto == null || dto.getRouteCode() == null || dto.getRouteCode().isBlank()) {
            throw new BusinessException(2021, "消息路由不能为空");
        }
        boolean exists = messagePushes.stream()
                .filter(item -> !Objects.equals(item.getId(), currentId))
                .anyMatch(item -> isEqual(item.getRouteCode(), dto.getRouteCode()));
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
        boolean exists = leaveAgentProducts.stream()
                .filter(item -> !Objects.equals(item.getId(), currentId))
                .anyMatch(item -> isEqual(item.getProductModel(), dto.getProductModel())
                        && isEqual(item.getOriginalUserId(), dto.getOriginalUserId()));
        if (exists) {
            throw new BusinessException(2034, "同一原统筹的产品代理规则重复");
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

    private <T> PageResult<T> page(List<T> source, Integer pageNo, Integer pageSize) {
        int no = pageNo == null || pageNo < 1 ? 1 : pageNo;
        int size = pageSize == null || pageSize < 1 ? 10 : pageSize;
        int from = (no - 1) * size;
        if (from >= source.size()) {
            return new PageResult<>(List.of(), (long) source.size());
        }
        int to = Math.min(from + size, source.size());
        return new PageResult<>(new ArrayList<>(source.subList(from, to)), (long) source.size());
    }

    private boolean contains(String source, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }
        return source != null && source.contains(keyword.trim());
    }

    private <T> boolean removeById(List<T> source, String id) {
        return source.removeIf(item -> id != null && id.equals(extractId(item)));
    }

    private <T> boolean replaceById(List<T> source, String id, T newValue) {
        for (int i = 0; i < source.size(); i++) {
            if (id != null && id.equals(extractId(source.get(i)))) {
                applyId(newValue, id);
                source.set(i, newValue);
                return Boolean.TRUE;
            }
        }
        return Boolean.FALSE;
    }

    private void fillId(Object dto) {
        if (extractId(dto) == null || extractId(dto).isBlank()) {
            applyId(dto, UUID.randomUUID().toString());
        }
    }

    private String extractId(Object dto) {
        if (dto instanceof LeaveConfigDTO val) {
            return val.getId();
        }
        if (dto instanceof VirtualProductDTO val) {
            return val.getId();
        }
        if (dto instanceof MessagePushDTO val) {
            return val.getId();
        }
        if (dto instanceof LeaveAgentProductDTO val) {
            return val.getId();
        }
        if (dto instanceof AreaCoordinatorDTO val) {
            return val.getId();
        }
        return null;
    }

    private void applyId(Object dto, String id) {
        if (dto instanceof LeaveConfigDTO val) {
            val.setId(id);
            return;
        }
        if (dto instanceof VirtualProductDTO val) {
            val.setId(id);
            return;
        }
        if (dto instanceof MessagePushDTO val) {
            val.setId(id);
            return;
        }
        if (dto instanceof LeaveAgentProductDTO val) {
            val.setId(id);
            return;
        }
        if (dto instanceof AreaCoordinatorDTO val) {
            val.setId(id);
        }
    }
}
