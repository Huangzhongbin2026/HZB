package com.ruijie.supplysystem.service.impl;

import com.ruijie.supplysystem.dto.PageResult;
import com.ruijie.supplysystem.dto.SysOperationLogDTO;
import com.ruijie.supplysystem.service.OperationLogService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class OperationLogServiceImpl implements OperationLogService {

    @Override
    public PageResult<SysOperationLogDTO> query(Integer pageNo, Integer pageSize, String operUser,
                                                String operType, String operModule, String operIp,
                                                String keyword, String startTime, String endTime) {
        List<SysOperationLogDTO> list = new ArrayList<>();
        SysOperationLogDTO dto = new SysOperationLogDTO();
        dto.setId("1");
        dto.setOperUser("admin");
        dto.setOperTime("2026-03-21T10:00:00+08:00");
        dto.setOperIp("127.0.0.1");
        dto.setOperType("UPDATE");
        dto.setOperModule("ROLE");
        dto.setOperContent("更新角色权限");
        dto.setOperResult("SUCCESS");
        dto.setRemark("");
        list.add(dto);
        return new PageResult<>(list, 1L);
    }

    @Override
    public Boolean clean(String startTime, String endTime) { return Boolean.TRUE; }

    @Override
    public Boolean record(SysOperationLogDTO dto) { return Boolean.TRUE; }
}
