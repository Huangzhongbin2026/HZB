package com.ruijie.supplysystem.service.impl;

import com.ruijie.supplysystem.dto.PageResult;
import com.ruijie.supplysystem.dto.SysOperationLogDTO;
import com.ruijie.supplysystem.mapper.OperationLogMapper;
import com.ruijie.supplysystem.service.OperationLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OperationLogServiceImpl implements OperationLogService {

    private final OperationLogMapper operationLogMapper;

    @Override
    public PageResult<SysOperationLogDTO> query(Integer pageNo, Integer pageSize, String operUser,
                                                String operType, String operModule, String operIp,
                                                String keyword, String startTime, String endTime) {
        int no = pageNo == null || pageNo < 1 ? 1 : pageNo;
        int size = pageSize == null || pageSize < 1 ? 10 : pageSize;
        int offset = (no - 1) * size;
        Long total = operationLogMapper.count(operUser, operType, operModule, operIp, keyword, startTime, endTime);
        List<SysOperationLogDTO> list = operationLogMapper.query(offset, size, operUser, operType, operModule, operIp, keyword, startTime, endTime);
        return new PageResult<>(list, total == null ? 0L : total);
    }

    @Override
    public Boolean clean(String startTime, String endTime) {
        operationLogMapper.clean(startTime, endTime);
        return Boolean.TRUE;
    }

    @Override
    public Boolean record(SysOperationLogDTO dto) {
        return operationLogMapper.insert(dto) > 0;
    }
}
