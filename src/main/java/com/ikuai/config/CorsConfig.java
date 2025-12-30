package com.ikuai.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import java.util.Collections;

/**
 * @description:
 * @author: TangLei
 * @date: 2025/12/17 12:00
 */
@Configuration
public class CorsConfig {
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();

        // 低版本替代 addAllowedOriginPattern("*")：允许所有 Origin
        config.setAllowedOrigins(Collections.singletonList("*"));

        // 允许所有请求方法（GET/POST/PUT/DELETE 等）
        config.setAllowedMethods(Collections.singletonList("*"));

        // 允许所有请求头
        config.setAllowedHeaders(Collections.singletonList("*"));

        // 注意：低版本中，allowedOrigins设为*时，allowCredentials必须为false（否则报错）
        // 若需要Cookie/Token，改用方案2（动态匹配Origin）
        config.setAllowCredentials(false);

        // 预检请求缓存时间（减少OPTIONS请求）
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // 对所有接口生效
        source.registerCorsConfiguration("/api/**", config);
        return new CorsFilter(source);
    }
}