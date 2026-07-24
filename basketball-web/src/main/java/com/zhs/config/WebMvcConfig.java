package com.zhs.config;

import jakarta.annotation.Resource;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Resource
    private LoginInterceptor loginInterceptor;

    @Resource
    private GeneratedCrudAccessInterceptor generatedCrudAccessInterceptor;

    @Resource
    private AccountFileProperties accountFileProperties;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/basket/**", "/api/**")
                .excludePathPatterns(
                        "/error",
                        "/swagger-resources/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/v3/api-docs/**",
                        "/webjars/**",
                        "/doc.html",
                        "/favicon.ico",
                        "/files/public/**",
                        "/head/**",
                        "/player-avatar/**"
                );

        registry.addInterceptor(generatedCrudAccessInterceptor)
                .addPathPatterns("/basket/**");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String root = accountFileProperties.getLocalRoot();
        if (!root.endsWith("/") && !root.endsWith("\\")) {
            root = root + "/";
        }
        registry.addResourceHandler("/files/public/**")
                .addResourceLocations("file:" + root + "public/");
        registry.addResourceHandler("/head/**")
                .addResourceLocations("classpath:/head/");
        registry.addResourceHandler("/player-avatar/**")
                .addResourceLocations("classpath:/player/");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
