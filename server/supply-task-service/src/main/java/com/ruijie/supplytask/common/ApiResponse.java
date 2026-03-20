package com.ruijie.supplytask.common;

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

    public static <T> ApiResponse<T> success(T data, String traceId) {
        return new ApiResponse<>(0, data, "success", traceId);
    }

    public static <T> ApiResponse<T> fail(Integer code, String message, String traceId) {
        return new ApiResponse<>(code, null, message, traceId);
    }
}
