# -*- coding: utf-8 -*-
"""Generate Round 4 CNPA Admin basket backend files."""
import os

ROOT = os.path.join(os.path.dirname(__file__), "..")
JAVA = os.path.join(ROOT, "src", "main", "java", "com", "zhs")
RES = os.path.join(ROOT, "src", "main", "resources")
SQL_DIR = os.path.join(os.path.dirname(__file__), "..", "..", "sql")
WEB_JAVA = os.path.join(os.path.dirname(__file__), "..", "..", "basketball-web", "src", "main", "java", "com", "zhs", "enums")


def w(rel_path, content, base=ROOT):
    path = os.path.join(base, rel_path.replace("/", os.sep))
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        f.write(content)
    print("W", path)


# ---------- enums ----------
ENUMS = {
"PlayerCertStatusEnum": '''package com.zhs.basket.enums;

import com.zhs.common.exception.ServiceException;

public enum PlayerCertStatusEnum {
    DRAFT(0, "草稿"),
    SUBMITTED(1, "已提交"),
    REVIEWING(2, "审核中"),
    APPROVED(3, "已通过"),
    REJECTED(4, "已驳回"),
    CANCELLED(5, "已取消");

    private final int code;
    private final String description;

    PlayerCertStatusEnum(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() { return code; }
    public String getDescription() { return description; }

    public static PlayerCertStatusEnum fromCode(Integer code) {
        if (code == null) return DRAFT;
        for (PlayerCertStatusEnum v : values()) {
            if (v.code == code) return v;
        }
        throw new ServiceException("未知认证状态");
    }
}
''',
"CaptainQualificationStatusEnum": '''package com.zhs.basket.enums;

import com.zhs.common.exception.ServiceException;

public enum CaptainQualificationStatusEnum {
    REVOKED(0, "已撤销"),
    ACTIVE(1, "有效"),
    EXPIRED(2, "已过期");

    private final int code;
    private final String description;

    CaptainQualificationStatusEnum(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() { return code; }
    public String getDescription() { return description; }

    public static CaptainQualificationStatusEnum fromCode(Integer code) {
        if (code == null) throw new ServiceException("未知资格状态");
        for (CaptainQualificationStatusEnum v : values()) {
            if (v.code == code) return v;
        }
        throw new ServiceException("未知资格状态");
    }
}
''',
"TeamAuditStatusEnum": '''package com.zhs.basket.enums;

import com.zhs.common.exception.ServiceException;

public enum TeamAuditStatusEnum {
    PENDING(0, "待审核"),
    APPROVED(1, "已通过"),
    REJECTED(2, "已驳回"),
    REVIEWING(3, "审核中");

    private final int code;
    private final String description;

    TeamAuditStatusEnum(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() { return code; }
    public String getDescription() { return description; }

    public static TeamAuditStatusEnum fromCode(Integer code) {
        if (code == null) throw new ServiceException("未知审核状态");
        for (TeamAuditStatusEnum v : values()) {
            if (v.code == code) return v;
        }
        throw new ServiceException("未知审核状态");
    }
}
''',
"TeamStatusEnum": '''package com.zhs.basket.enums;

import com.zhs.common.exception.ServiceException;

public enum TeamStatusEnum {
    DISABLED(0, "已禁用"),
    ACTIVE(1, "正常"),
    DISSOLVED(2, "已解散");

    private final int code;
    private final String description;

    TeamStatusEnum(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() { return code; }
    public String getDescription() { return description; }

    public static TeamStatusEnum fromCode(Integer code) {
        if (code == null) throw new ServiceException("未知球队状态");
        for (TeamStatusEnum v : values()) {
            if (v.code == code) return v;
        }
        throw new ServiceException("未知球队状态");
    }
}
''',
"TournamentStatusEnum": '''package com.zhs.basket.enums;

import com.zhs.common.exception.ServiceException;

public enum TournamentStatusEnum {
    DISABLED(0, "已禁用"),
    ACTIVE(1, "已发布"),
    ENDED(2, "已结束"),
    DRAFT(3, "草稿");

    private final int code;
    private final String description;

    TournamentStatusEnum(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() { return code; }
    public String getDescription() { return description; }

    public static TournamentStatusEnum fromCode(Integer code) {
        if (code == null) throw new ServiceException("未知赛事状态");
        for (TournamentStatusEnum v : values()) {
            if (v.code == code) return v;
        }
        throw new ServiceException("未知赛事状态");
    }
}
''',
"SeasonStatusEnum": '''package com.zhs.basket.enums;

import com.zhs.common.exception.ServiceException;

public enum SeasonStatusEnum {
    PREPARE(0, "筹备中"),
    REGISTRATION_OPEN(1, "报名中"),
    IN_PROGRESS(2, "进行中"),
    FINISHED(3, "已结束"),
    REGISTRATION_CLOSED(4, "报名截止"),
    CLOSED(5, "已关闭"),
    CANCELLED(6, "已取消");

    private final int code;
    private final String description;

    SeasonStatusEnum(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() { return code; }
    public String getDescription() { return description; }

    public static SeasonStatusEnum fromCode(Integer code) {
        if (code == null) throw new ServiceException("未知赛季状态");
        for (SeasonStatusEnum v : values()) {
            if (v.code == code) return v;
        }
        throw new ServiceException("未知赛季状态");
    }
}
''',
"RegistrationStatusEnum": '''package com.zhs.basket.enums;

import com.zhs.common.exception.ServiceException;

public enum RegistrationStatusEnum {
    DRAFT(0, "草稿"),
    SUBMITTED(1, "已提交"),
    REVIEWING(2, "审核中"),
    APPROVED(3, "已通过"),
    REJECTED(4, "已驳回"),
    WITHDRAWN(5, "已撤回"),
    CANCELLED(6, "已取消");

    private final int code;
    private final String description;

    RegistrationStatusEnum(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() { return code; }
    public String getDescription() { return description; }

    public static RegistrationStatusEnum fromCode(Integer code) {
        if (code == null) throw new ServiceException("未知报名状态");
        for (RegistrationStatusEnum v : values()) {
            if (v.code == code) return v;
        }
        throw new ServiceException("未知报名状态");
    }
}
''',
"TeamMemberRoleEnum": '''package com.zhs.basket.enums;

public enum TeamMemberRoleEnum {
    CAPTAIN("CAPTAIN"),
    MEMBER("MEMBER");

    private final String code;
    TeamMemberRoleEnum(String code) { this.code = code; }
    public String getCode() { return code; }
}
''',
"TeamMembershipStatusEnum": '''package com.zhs.basket.enums;

public enum TeamMembershipStatusEnum {
    INACTIVE(0), ACTIVE(1);
    private final int code;
    TeamMembershipStatusEnum(int code) { this.code = code; }
    public int getCode() { return code; }
}
''',
"RegistrationPlayerValidationStatusEnum": '''package com.zhs.basket.enums;

public enum RegistrationPlayerValidationStatusEnum {
    PENDING(0), PASS(1), FAIL(2);
    private final int code;
    RegistrationPlayerValidationStatusEnum(int code) { this.code = code; }
    public int getCode() { return code; }
}
''',
"DisciplineStatusEnum": '''package com.zhs.basket.enums;

public enum DisciplineStatusEnum {
    INACTIVE(0), ACTIVE(1);
    private final int code;
    DisciplineStatusEnum(int code) { this.code = code; }
    public int getCode() { return code; }
}
''',
}

for name, content in ENUMS.items():
    w(f"src/main/java/com/zhs/basket/enums/{name}.java", content)

print("Enums done")
