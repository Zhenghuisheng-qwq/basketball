package com.zhs.support;

import org.springframework.util.StringUtils;

/**
 * 会员默认头像。
 */
public final class MemberAvatarSupport {

    public static final String DEFAULT_AVATAR_URL =
            "https://basketball-1453992653.cos.ap-shanghai.myqcloud.com/basketball/public/2026/08/01/b1f4d9a28beb48beb90fed28439e8e4a.png";

    private MemberAvatarSupport() {
    }

    public static String resolve(String avatarUrl) {
        return StringUtils.hasText(avatarUrl) ? avatarUrl.trim() : DEFAULT_AVATAR_URL;
    }

    /**
     * Session / Summary 用：仅返回正式 accessUrl；无文件时返回 null，不回落默认图。
     */
    public static String accessUrlOrNull(String avatarUrl) {
        return StringUtils.hasText(avatarUrl) ? avatarUrl.trim() : null;
    }
}
