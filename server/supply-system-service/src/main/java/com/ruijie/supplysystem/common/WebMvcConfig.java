package com.ruijie.supplysystem.common;

import com.ruijie.supplysystem.security.AuthPermissionInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final AuthPermissionInterceptor authPermissionInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authPermissionInterceptor)
                .addPathPatterns("/api/v1/supply/system/**")
                .excludePathPatterns("/api/v1/supply/system/health", "/swagger-ui/**", "/v3/api-docs/**");
    }
}
