package com.zhs.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 统一阻止生成式 CRUD 外部访问，业务接口走专用 Controller。
 */
@Component
public class GeneratedCrudAccessInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();
        if (uri == null || !uri.startsWith("/basket/")) {
            return true;
        }
        // 业务接口白名单
        if (uri.startsWith("/basket/member/auth/")
                || uri.startsWith("/basket/account/")
                || uri.startsWith("/basket/player/certification/")
                || uri.startsWith("/basket/player/match-data/")
                || uri.startsWith("/basket/team/")) {
            return true;
        }

        boolean generatedCrud = uri.endsWith("/getByPage")
                || uri.endsWith("/getById")
                || uri.contains("/getById")
                || uri.endsWith("/addOrUpdate")
                || uri.contains("/delete/")
                || uri.endsWith("/deleteByIds");

        if (!generatedCrud) {
            return true;
        }

        response.setStatus(403);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":403,\"msg\":\"无权访问该接口\",\"data\":null}");
        return false;
    }
}
