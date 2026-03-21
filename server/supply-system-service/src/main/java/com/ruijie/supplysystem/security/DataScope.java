package com.ruijie.supplysystem.security;

import java.lang.annotation.*;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataScope {
    String tableAlias() default "t";
    String ownerField() default "owner_user_id";
    String deptField() default "dept_code";
}
