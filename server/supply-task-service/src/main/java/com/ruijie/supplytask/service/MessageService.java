package com.ruijie.supplytask.service;

public interface MessageService {

    Boolean pushTaskMessage(String taskId, String eventType);
}
