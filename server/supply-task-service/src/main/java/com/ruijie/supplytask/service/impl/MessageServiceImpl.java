package com.ruijie.supplytask.service.impl;

import com.ruijie.supplytask.service.MessageService;
import org.springframework.stereotype.Service;

@Service
public class MessageServiceImpl implements MessageService {

    @Override
    public Boolean pushTaskMessage(String taskId, String eventType) {
        return Boolean.TRUE;
    }
}
