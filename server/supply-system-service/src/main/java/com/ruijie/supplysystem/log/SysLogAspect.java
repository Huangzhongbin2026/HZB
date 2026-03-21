package com.ruijie.supplysystem.log;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class SysLogAspect {

    private final HttpServletRequest request;
    private final SysLogService sysLogService;

    @Around("@annotation(sysLog)")
    public Object record(ProceedingJoinPoint pjp, SysLog sysLog) throws Throwable {
        String result = "SUCCESS";
        try {
            return pjp.proceed();
        } catch (Throwable ex) {
            result = "FAIL";
            throw ex;
        } finally {
            OperationLogRecord record = OperationLogRecord.builder()
                    .operUser(request.getHeader("X-User") == null ? "unknown" : request.getHeader("X-User"))
                    .operIp(request.getRemoteAddr())
                    .operType(sysLog.type())
                    .operModule(sysLog.module())
                    .operContent(sysLog.content())
                    .operResult(result)
                    .remark("")
                    .build();
            asyncSave(record);
        }
    }

    @Async
    protected void asyncSave(OperationLogRecord record) {
        sysLogService.save(record);
    }
}
