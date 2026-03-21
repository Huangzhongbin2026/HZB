package com.ruijie.supplysystem.log;

import java.lang.annotation.*;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SysLog {
    String module();
    String type();
    String content();
}
