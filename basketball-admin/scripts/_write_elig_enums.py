from pathlib import Path

files = {}
files[r"d:\code\dz\basketball\scaffolding-v2\basketball-web\src\main\java\com\zhs\enums\v4\RegistrationWindowStatusEnum.java"] = r'''package com.zhs.enums.v4;

/** 报名时间窗口（与 JDBC serverTimezone=GMT+8 对齐）。 */
public enum RegistrationWindowStatusEnum {
    NOT_STARTED("NOT_STARTED", "报名未开始"),
    OPEN("OPEN", "报名中"),
    CLOSED("CLOSED", "报名已截止");

    private final String code;
    private final String label;

    RegistrationWindowStatusEnum(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() { return code; }
    public String getLabel() { return label; }
}
'''

files[r"d:\code\dz\basketball\scaffolding-v2\basketball-web\src\main\java\com\zhs\enums\v4\RegistrationEligibilityReasonEnum.java"] = r'''package com.zhs.enums.v4;

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
'''

files[r"d:\code\dz\basketball\scaffolding-v2\basketball-web\src\main\java\com\zhs\support\v4\TeamRegistrationEligibility.java"] = r'''package com.zhs.support.v4;

import com.zhs.enums.v4.RegistrationEligibilityReasonEnum;
import com.zhs.enums.v4.RegistrationWindowStatusEnum;

public class TeamRegistrationEligibility {
    private final boolean canRegister;
    private final boolean canViewRegistration;
    private final RegistrationWindowStatusEnum windowStatus;
    private final RegistrationEligibilityReasonEnum reason;
    private final String reasonMessage;

    private TeamRegistrationEligibility(boolean canRegister,
                                        boolean canViewRegistration,
                                        RegistrationWindowStatusEnum windowStatus,
                                        RegistrationEligibilityReasonEnum reason,
                                        String reasonMessage) {
        this.canRegister = canRegister;
        this.canViewRegistration = canViewRegistration;
        this.windowStatus = windowStatus;
        this.reason = reason;
        this.reasonMessage = reasonMessage;
    }

    public static TeamRegistrationEligibility allow(RegistrationWindowStatusEnum windowStatus) {
        return new TeamRegistrationEligibility(true, true, windowStatus, null, null);
    }

    public static TeamRegistrationEligibility deny(RegistrationWindowStatusEnum windowStatus,
                                                   RegistrationEligibilityReasonEnum reason,
                                                   boolean canViewRegistration) {
        return new TeamRegistrationEligibility(false, canViewRegistration, windowStatus, reason,
                reason == null ? null : reason.getMessage());
    }

    public boolean isCanRegister() { return canRegister; }
    public boolean isCanViewRegistration() { return canViewRegistration; }
    public RegistrationWindowStatusEnum getWindowStatus() { return windowStatus; }
    public RegistrationEligibilityReasonEnum getReason() { return reason; }
    public String getReasonMessage() { return reasonMessage; }
    public String getWindowStatusCode() { return windowStatus == null ? null : windowStatus.getCode(); }
    public String getWindowStatusLabel() { return windowStatus == null ? null : windowStatus.getLabel(); }
    public String getReasonCode() { return reason == null ? null : reason.getCode(); }
}
'''

for path, text in files.items():
    p = Path(path)
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(text, encoding="utf-8", newline="\n")
    b = p.read_bytes()
    print(p.name, "ok", len(b), "nulls", b.count(0))
