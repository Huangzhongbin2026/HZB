package com.ruijie.supplysystem.security;

import com.ruijie.supplysystem.common.BusinessException;
import com.ruijie.supplysystem.common.TraceContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class AuthPermissionInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String token = request.getHeader("Authorization");
        if (token == null || token.isBlank()) {
            throw new BusinessException(1002, "鉴权失败，请先登录");
        }

        String traceId = request.getHeader("X-Trace-Id");
        TraceContext.setTraceId(traceId == null || traceId.isBlank() ? TraceContext.getTraceId() : traceId);

        String permissionHeader = request.getHeader("X-Permissions");
        Set<String> codes = permissionHeader == null || permissionHeader.isBlank()
                ? Set.of("sys:menu:view", "sys:dict:view", "sys:role:view", "sys:user:view", "sys:log:view")
                : Arrays.stream(permissionHeader.split(",")).map(String::trim).collect(Collectors.toSet());
        PermissionContext.set(codes);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        PermissionContext.clear();
        TraceContext.clear();
    }
}
