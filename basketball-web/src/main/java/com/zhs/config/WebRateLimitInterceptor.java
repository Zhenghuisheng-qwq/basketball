package com.zhs.config;

import com.zhs.common.UserContext;
import com.zhs.common.cachekey.web.WebRateLimitCacheKey;
import com.zhs.support.ClientIpResolver;
import com.zhs.util.MiniRedisUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
public class WebRateLimitInterceptor implements HandlerInterceptor {

    @Resource
    private WebRateLimitProperties properties;
    @Resource
    private MiniRedisUtil miniRedisUtil;
    @Resource
    private ClientIpResolver clientIpResolver;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!properties.isEnabled()) {
            return true;
        }
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String group = WebRateLimitClassifier.classify(request.getMethod(), request.getRequestURI());
        if (group == null) {
            return true;
        }
        String identity = resolveIdentity(group, request);
        WebRateLimitCacheKey key = new WebRateLimitCacheKey(group, identity, properties.getWindowSeconds());
        try {
            long count = miniRedisUtil.increment(key);
            if (count > properties.limitOf(group)) {
                writeTooManyRequests(response);
                return false;
            }
        } catch (Exception ex) {
            log.warn("web rate-limit redis unavailable, fail-open group={} path={} ex={}",
                    group, request.getRequestURI(), ex.getClass().getSimpleName());
        }
        return true;
    }

    private String resolveIdentity(String group, HttpServletRequest request) {
        if (WebRateLimitClassifier.USER_WRITE.equals(group) && UserContext.getUserId() != null) {
            return "m:" + UserContext.getUserId();
        }
        return "ip:" + clientIpResolver.resolve(request);
    }

    private void writeTooManyRequests(HttpServletResponse response) throws Exception {
        response.setStatus(429);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":429,\"msg\":\"Too Many Requests\",\"data\":null}");
    }
}