package com.ruijie.supplysystem.security;

import com.ruijie.supplysystem.common.BusinessException;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class PermissionAspect {

    @Before("@annotation(requirePermission)")
    public void checkPermission(RequirePermission requirePermission) {
        if (!PermissionContext.contains(requirePermission.value())) {
            throw new BusinessException(1003, "无权限访问");
        }
    }
}
