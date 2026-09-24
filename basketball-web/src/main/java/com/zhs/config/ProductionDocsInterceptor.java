package com.zhs.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Profile("prod")
@Component
public class ProductionDocsInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();
        if (uri == null) {
            return true;
        }
        if (isDocsPath(uri)) {
            response.setStatus(404);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":404,\"msg\":\"Not Found\",\"data\":null}");
            return false;
        }
        return true;
    }

    static boolean isDocsPath(String uri) {
        String path = GeneratedCrudPaths.normalizePath(uri);
        if (path == null) {
            return false;
        }
        return path.startsWith("/swagger-ui")
                || path.equals("/swagger-ui.html")
                || path.startsWith("/swagger-resources")
                || path.startsWith("/v3/api-docs")
                || path.equals("/doc.html")
                || path.startsWith("/webjars/")
                || path.startsWith("/basket/member/init");
    }
}