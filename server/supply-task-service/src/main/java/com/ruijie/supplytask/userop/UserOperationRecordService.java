package com.ruijie.supplytask.userop;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class UserOperationRecordService {

    private static final DateTimeFormatter DATETIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final List<Map<String, Object>> records = new CopyOnWriteArrayList<>();

    public Boolean record(Map<String, Object> body) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("opNo", "CZJL" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + (int) (Math.random() * 10));
        item.put("createdAt", LocalDateTime.now().format(DATETIME));
        item.put("requester", value(body, "requester"));
        item.put("feishuId", value(body, "feishuId"));
        item.put("flowType", value(body, "flowType"));
        item.put("stepName", value(body, "stepName"));
        item.put("action", value(body, "action"));
        item.put("status", value(body, "status"));
        item.put("payload", body.getOrDefault("payload", Map.of()));
        records.add(0, item);
        return Boolean.TRUE;
    }

    public Map<String, Object> list(Integer pageNo, Integer pageSize, String requester, String feishuId, String flowType, String status, String createdAt) {
        List<Map<String, Object>> filtered = records.stream()
                .filter(x -> contains(x.get("requester"), requester))
                .filter(x -> contains(x.get("feishuId"), feishuId))
                .filter(x -> contains(x.get("flowType"), flowType))
                .filter(x -> contains(x.get("status"), status))
                .filter(x -> contains(x.get("createdAt"), createdAt))
                .toList();

        int no = pageNo == null || pageNo < 1 ? 1 : pageNo;
        int size = pageSize == null || pageSize < 1 ? 10 : pageSize;
        int from = (no - 1) * size;
        List<Map<String, Object>> page = from >= filtered.size() ? List.of() : filtered.subList(from, Math.min(from + size, filtered.size()));
        return Map.of("list", page, "total", filtered.size());
    }

    private boolean contains(Object source, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }
        return String.valueOf(source == null ? "" : source).contains(keyword.trim());
    }

    private String value(Map<String, Object> body, String key) {
        if (body == null || body.get(key) == null) {
            return "";
        }
        return String.valueOf(body.get(key));
    }
}
