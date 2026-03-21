package com.ruijie.supplysystem.service;

import com.ruijie.supplysystem.dto.PageResult;
import com.ruijie.supplysystem.dto.SysOperationLogDTO;

public interface OperationLogService {
    PageResult<SysOperationLogDTO> query(Integer pageNo, Integer pageSize, String operUser,
                                         String operType, String operModule, String operIp,
                                         String keyword, String startTime, String endTime);
    Boolean clean(String startTime, String endTime);
    Boolean record(SysOperationLogDTO dto);
}
