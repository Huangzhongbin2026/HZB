package com.ruijie.supplysystem.common;

import java.util.UUID;

public final class TraceContext {

    private static final ThreadLocal<String> TRACE_ID = new ThreadLocal<>();

    private TraceContext() {}

    public static String getTraceId() {
        String traceId = TRACE_ID.get();
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString().replace("-", "");
            TRACE_ID.set(traceId);
        }
        return traceId;
    }

    public static void setTraceId(String traceId) {
        TRACE_ID.set(traceId);
    }

    public static void clear() {
        TRACE_ID.remove();
    }
}
