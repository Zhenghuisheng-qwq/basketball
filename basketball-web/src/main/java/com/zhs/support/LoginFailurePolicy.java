package com.zhs.support;

import com.zhs.exception.BusinessReason;
import com.zhs.exception.ServiceException;

/**
 * 公开登录失败只返回同一原因码，避免通过错误详情判断账号是否存在。
 */
public final class LoginFailurePolicy {

    private LoginFailurePolicy() {
    }

    public static ServiceException invalidCredentials() {
        return BusinessReason.INVALID_CREDENTIALS.exception();
    }

    public static boolean revealsAccountExistence(ServiceException exception) {
        if (exception == null) {
            return false;
        }
        if ("ACCOUNT_NOT_FOUND".equals(exception.getReasonCode())
                || "EMAIL_ALREADY_REGISTERED".equals(exception.getReasonCode())) {
            return true;
        }
        String message = exception.getMessage() == null ? "" : exception.getMessage();
        return message.contains("不存在") || message.contains("未注册") || message.contains("已注册");
    }
}
