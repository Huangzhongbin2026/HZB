package com.ruijie.supplytask.service.impl;

import com.ruijie.supplytask.common.BusinessException;
import com.ruijie.supplytask.dto.PageResult;
import com.ruijie.supplytask.dto.TaskCreateRequest;
import com.ruijie.supplytask.dto.TaskVO;
import com.ruijie.supplytask.service.TaskService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class TaskServiceImpl implements TaskService {

    @Override
    public String createTask(TaskCreateRequest request) {
        return "TK" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }

    @Override
    public PageResult<TaskVO> queryTasks(Integer pageNo, Integer pageSize, String keyword, String type, String status) {
        List<TaskVO> list = new ArrayList<>();
        list.add(TaskVO.builder()
                .id("TK202603200001")
                .type("ORDER_URGENT")
                .status("PENDING")
                .title("客户A订单加急")
                .productModel("RG-S6200-48XT8CQ")
                .requester("U1001")
                .coordinator("U2001")
                .dueAt("2026-03-22T18:00:00+08:00")
                .priority(5)
                .createdAt(LocalDateTime.now().toString())
                .build());
        return new PageResult<>(list, 1L);
    }

    @Override
    public TaskVO getTaskDetail(String taskId) {
        return TaskVO.builder()
                .id(taskId)
                .type("DELIVERY_CHANGE")
                .status("PROCESSING")
                .title("客期变更评估")
                .productModel("RG-RSR30-XA")
                .requester("U1002")
                .coordinator("U2002")
                .dueAt("2026-03-21T12:00:00+08:00")
                .priority(4)
                .createdAt(LocalDateTime.now().toString())
                .build();
    }

    @Override
    public Boolean transferTask(String taskId, String targetUserId) {
        if (targetUserId == null || targetUserId.isBlank()) {
            throw new BusinessException(1001, "目标处理人不能为空");
        }
        return Boolean.TRUE;
    }

    @Override
    public Boolean closeTask(String taskId, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new BusinessException(1001, "关闭原因不能为空");
        }
        return Boolean.TRUE;
    }
}
