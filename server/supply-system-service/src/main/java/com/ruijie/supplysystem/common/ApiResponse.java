package com.ruijie.supplysystem.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {

    private Integer code;
    private T data;
    private String message;
    private String traceId;

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(0, data, "success", TraceContext.getTraceId());
    }

    public static <T> ApiResponse<T> fail(Integer code, String message) {
        return new ApiResponse<>(code, null, message, TraceContext.getTraceId());
    }
}
