package com.ruijie.supplytask.service;

import com.ruijie.supplytask.dto.PageResult;
import com.ruijie.supplytask.dto.TaskCreateRequest;
import com.ruijie.supplytask.dto.TaskVO;

public interface TaskService {

    String createTask(TaskCreateRequest request);

    PageResult<TaskVO> queryTasks(Integer pageNo, Integer pageSize, String keyword, String type, String status);

    TaskVO getTaskDetail(String taskId);

    Boolean transferTask(String taskId, String targetUserId);

    Boolean closeTask(String taskId, String reason);
}
