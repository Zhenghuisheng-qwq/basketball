package com.zhs.basket.enums;

import lombok.Getter;

@Getter
public enum CaptainGrantLogActionTypeEnum {
    GRANT("GRANT", "授予队长权限"),
    BIND_TEAM("BIND_TEAM", "绑定球队"),
    CHANGE_TEAM("CHANGE_TEAM", "变更绑定球队"),
    REVOKE("REVOKE", "收回权限"),
    EXPIRE("EXPIRE", "赛事结束自动失效"),
    ADJUST_VALID_TO("ADJUST_VALID_TO", "调整授权有效期"),
    SYNC_EVENT_TIME("SYNC_EVENT_TIME", "同步赛事结束时间"),
    UPDATE_REMARK("UPDATE_REMARK", "修改备注"),
    PAYMENT_CONFIRM("PAYMENT_CONFIRM", "确认缴费");

    private final String code;
    private final String description;

    CaptainGrantLogActionTypeEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public static CaptainGrantLogActionTypeEnum fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (CaptainGrantLogActionTypeEnum item : values()) {
            if (item.code.equalsIgnoreCase(code)) {
                return item;
            }
        }
        return null;
    }
}
