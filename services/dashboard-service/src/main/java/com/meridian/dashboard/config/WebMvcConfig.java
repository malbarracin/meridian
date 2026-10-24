package com.meridian.dashboard.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.meridian.dashboard.security.JwtAuthInterceptor;
import com.meridian.dashboard.security.JwtConfig;
import com.meridian.dashboard.security.JwtValidator;

/** Limita la autenticación JWT a las rutas del dashboard. */
@Configuration
@EnableConfigurationProperties(JwtConfig.class)
public class WebMvcConfig implements WebMvcConfigurer {

    private final JwtAuthInterceptor interceptor;

    public WebMvcConfig(JwtConfig config) {
        interceptor = new JwtAuthInterceptor(new JwtValidator(config));
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(interceptor).addPathPatterns("/api/v1/dashboard/**");
    }
}
