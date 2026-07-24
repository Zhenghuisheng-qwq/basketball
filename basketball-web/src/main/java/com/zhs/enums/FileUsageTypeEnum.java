package com.zhs.enums;

import com.zhs.exception.ServiceException;

public enum FileUsageTypeEnum {

    ACCOUNT_AVATAR("ACCOUNT_AVATAR", true),
    PLAYER_CERT_ID_FRONT("PLAYER_CERT_ID_FRONT", false),
    PLAYER_CERT_ID_BACK("PLAYER_CERT_ID_BACK", false),
    PLAYER_CERT_GAME_SCREENSHOT("PLAYER_CERT_GAME_SCREENSHOT", false),
    PLAYER_CERT_OTHER("PLAYER_CERT_OTHER", false),
    TEAM_LOGO("TEAM_LOGO", true);

    private final String code;
    private final boolean publicAccess;

    FileUsageTypeEnum(String code, boolean publicAccess) {
        this.code = code;
        this.publicAccess = publicAccess;
    }

    public String getCode() {
        return code;
    }

    public boolean isPublicAccess() {
        return publicAccess;
    }

    public boolean isCertMaterial() {
        return !publicAccess;
    }

    public static FileUsageTypeEnum fromCode(String code) {
        if (code == null || code.isBlank()) {
            throw new ServiceException("文件用途不能为空");
        }
        for (FileUsageTypeEnum value : values()) {
            if (value.code.equalsIgnoreCase(code.trim())) {
                return value;
            }
        }
        throw new ServiceException("不支持的文件用途");
    }
}
