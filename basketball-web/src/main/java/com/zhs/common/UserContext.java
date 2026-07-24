package com.zhs.common;

import com.zhs.exception.ServiceException;

/**
 * 当前请求登录账号上下文。
 * {@link #getUserId()} 语义为 {@code basket_member_account.id}。
 */
public class UserContext {

    private static final ThreadLocal<Long> USER_ID_HOLDER = new ThreadLocal<>();

    public static void setUserId(Long userId) {
        USER_ID_HOLDER.set(userId);
    }

    public static Long getUserId() {
        return USER_ID_HOLDER.get();
    }

    /** 获取当前登录账号 ID，未登录抛 401 */
    public static Long getRequiredUserId() {
        return getRequiredMemberId();
    }

    /** 同 {@link #getRequiredUserId()}，兼容第一轮命名 */
    public static Long getRequiredMemberId() {
        Long memberId = USER_ID_HOLDER.get();
        if (memberId == null) {
            throw new ServiceException("未登录", 401);
        }
        return memberId;
    }

    public static void clear() {
        USER_ID_HOLDER.remove();
    }
}
