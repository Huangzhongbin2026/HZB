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
            ? Set.of("sys:menu:view", "sys:dict:view", "sys:role:view", "sys:user:view", "sys:log:view",
            "aux:leave:view", "aux:leave:add", "aux:leave:edit", "aux:leave:delete", "aux:leave:import", "aux:leave:export", "aux:leave:match",
            "aux:virtual:view", "aux:virtual:add", "aux:virtual:edit", "aux:virtual:delete", "aux:virtual:import", "aux:virtual:export", "aux:virtual:match",
            "aux:message:view", "aux:message:add", "aux:message:edit", "aux:message:delete", "aux:message:import", "aux:message:export",
            "aux:agent:view", "aux:agent:add", "aux:agent:edit", "aux:agent:delete", "aux:agent:import", "aux:agent:export", "aux:agent:match",
            "aux:area:view", "aux:area:add", "aux:area:edit", "aux:area:delete", "aux:area:import", "aux:area:export", "aux:area:match",
            "aux:cache:refresh")
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
