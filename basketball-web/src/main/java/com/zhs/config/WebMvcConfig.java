package com.zhs.config;

import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Resource
    private LoginInterceptor loginInterceptor;

    @Resource
    private GeneratedCrudAccessInterceptor generatedCrudAccessInterceptor;

    @Resource
    private WebRateLimitInterceptor webRateLimitInterceptor;

    @Resource
    private AccountFileProperties accountFileProperties;

    @Resource
    private WebCorsProperties webCorsProperties;

    @Autowired(required = false)
    private ProductionDocsInterceptor productionDocsInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        if (productionDocsInterceptor != null) {
            registry.addInterceptor(productionDocsInterceptor)
                    .addPathPatterns(
                            "/swagger-ui/**",
                            "/swagger-ui.html",
                            "/swagger-resources/**",
                            "/v3/api-docs/**",
                            "/doc.html",
                            "/webjars/**",
                            "/basket/member/init/**"
                    );
        }

        registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/basket/**")
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

        registry.addInterceptor(webRateLimitInterceptor)
                .addPathPatterns("/basket/**");

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
        List<String> origins = webCorsProperties.allowedOriginList();
        if (origins.isEmpty()) {
            return;
        }
        registry.addMapping("/**")
                .allowedOrigins(origins.toArray(String[]::new))
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("Authorization", "Content-Type", "X-Terminal-Type")
                .exposedHeaders("Content-Type")
                .allowCredentials(webCorsProperties.isAllowCredentials())
                .maxAge(3600);
    }
}