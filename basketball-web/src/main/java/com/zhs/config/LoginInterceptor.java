package com.zhs.config;

import com.zhs.common.NoLoginRequest;
import com.zhs.common.TokenUtil;
import com.zhs.common.UserContext;
import com.zhs.support.MemberSessionSupport;
import com.zhs.support.MemberSessionSupport.ParsedTokenSession;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 登录拦截：校验官网会员 Token（member:token）。
 */
@Component
public class LoginInterceptor implements HandlerInterceptor {

    @Resource
    private MemberSessionSupport memberSessionSupport;

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

        ParsedTokenSession session = memberSessionSupport.resolveTokenSession(token);
        if (session == null || session.memberId() == null) {
            writeUnauthorized(response, "登录已过期");
            return false;
        }
        if (!memberSessionSupport.isTokenAuthVersionValid(session.memberId(), session.authVersion())) {
            memberSessionSupport.revokeToken(token);
            writeUnauthorized(response, "登录已过期");
            return false;
        }

        UserContext.setUserId(session.memberId());
        return true;
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
