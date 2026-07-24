package com.zhs.config;

import com.zhs.common.NoLoginRequest;
import com.zhs.common.TokenUtil;
import com.zhs.common.UserContext;
import com.zhs.common.cachekey.member.MemberTokenCacheKey;
import com.zhs.common.cachekey.user.TokenCacheKey;
import com.zhs.util.MiniRedisUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 登录拦截：官网会员 Token 走 member:token；/api 小程序遗留 Token 兼容 mini:token。
 */
@Component
public class LoginInterceptor implements HandlerInterceptor {

    @Resource
    private MiniRedisUtil miniRedisUtil;

    @Resource
    private MemberAuthProperties memberAuthProperties;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        NoLoginRequest methodAnnotation = handlerMethod.getMethodAnnotation(NoLoginRequest.class);
        NoLoginRequest classAnnotation = handlerMethod.getBeanType().getAnnotation(NoLoginRequest.class);
        if (methodAnnotation != null || classAnnotation != null) {
            return true;
        }

        String token = TokenUtil.resolveFrom(request);
        if (!StringUtils.hasText(token)) {
            writeUnauthorized(response, "未登录");
            return false;
        }

        Long memberId = resolveMemberId(token, request.getRequestURI());
        if (memberId == null) {
            writeUnauthorized(response, "登录已过期");
            return false;
        }

        UserContext.setUserId(memberId);
        return true;
    }

    private Long resolveMemberId(String token, String uri) {
        Object cached = miniRedisUtil.get(new MemberTokenCacheKey(token, memberAuthProperties.getTokenExpireDays()));
        Long memberId = parseId(cached);
        if (memberId != null) {
            return memberId;
        }
        // 兼容小程序遗留 /api 会话，不与官网会员前缀混用
        if (uri != null && uri.startsWith("/api/")) {
            return parseId(miniRedisUtil.get(new TokenCacheKey(token)));
        }
        return null;
    }

    private Long parseId(Object cached) {
        if (cached == null) {
            return null;
        }
        if (cached instanceof Long value) {
            return value;
        }
        if (cached instanceof Integer value) {
            return value.longValue();
        }
        if (cached instanceof String value && StringUtils.hasText(value)) {
            try {
                return Long.parseLong(value.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }

    private void writeUnauthorized(HttpServletResponse response, String msg) throws Exception {
        response.setStatus(401);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":401,\"msg\":\"" + msg + "\",\"data\":null}");
    }
}
