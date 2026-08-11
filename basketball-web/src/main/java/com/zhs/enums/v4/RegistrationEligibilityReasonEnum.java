package com.zhs.enums.v4;

/** 球队报名资格拒绝原因（列表 / 报名页 / save·submit 共用）。 */
public enum RegistrationEligibilityReasonEnum {
    NOT_STARTED("NOT_STARTED", "报名尚未开始"),
    REGISTRATION_CLOSED("REGISTRATION_CLOSED", "报名已截止"),
    DIVISION_NOT_OPEN("DIVISION_NOT_OPEN", "当前赛事未开放报名"),
    DIVISION_FINISHED("DIVISION_FINISHED", "赛事已结束，无法报名"),
    NO_CAPTAIN_GRANT("NO_CAPTAIN_GRANT", "无有效队长授权"),
    NO_TEAM("NO_TEAM", "尚未创建本届球队"),
    TEAM_NOT_CERTIFIED("TEAM_NOT_CERTIFIED", "球队尚未认证通过"),
    TEAM_ARCHIVED("TEAM_ARCHIVED", "球队已归档/解散，无法报名"),
    TEAM_DIVISION_MISMATCH("TEAM_DIVISION_MISMATCH", "球队所属赛事与报名 Division 不一致"),
    QUOTA_FULL("QUOTA_FULL", "名额已满"),
    ALREADY_PENDING("ALREADY_PENDING", "已有审核中的报名"),
    ALREADY_APPROVED("ALREADY_APPROVED", "已完成报名"),
    DIVISION_NOT_FOUND("DIVISION_NOT_FOUND", "赛事不存在或已失效");

    private final String code;
    private final String message;

    RegistrationEligibilityReasonEnum(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() { return code; }
    public String getMessage() { return message; }
}
