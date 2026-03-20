package com.ruijie.supplytask.common;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private RequestTraceInterceptor requestTraceInterceptor;

    @Autowired
    private AuthInterceptor authInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(requestTraceInterceptor).addPathPatterns("/**");
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/v1/supply/task/**")
                .excludePathPatterns("/api/v1/supply/task/health", "/swagger-ui/**", "/v3/api-docs/**");
    }
}
