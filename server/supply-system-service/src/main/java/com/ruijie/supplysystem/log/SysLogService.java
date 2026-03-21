package com.ruijie.supplysystem.log;

import org.springframework.stereotype.Service;

@Service
public class SysLogService {

    public void save(OperationLogRecord record) {
        // 实际生产中应落库/消息队列
    }
}
