package com.ruijie.supplysystem.log;

import com.ruijie.supplysystem.dto.SysOperationLogDTO;
import com.ruijie.supplysystem.mapper.OperationLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SysLogService {

    private final OperationLogMapper operationLogMapper;

    public void save(OperationLogRecord record) {
        SysOperationLogDTO dto = new SysOperationLogDTO();
        dto.setOperUser(record.getOperUser());
        dto.setOperIp(record.getOperIp());
        dto.setOperType(record.getOperType());
        dto.setOperModule(record.getOperModule());
        dto.setOperContent(record.getOperContent());
        dto.setOperResult(record.getOperResult());
        dto.setRemark(record.getRemark());
        operationLogMapper.insert(dto);
    }
}
