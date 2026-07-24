import os

BASE = r"d:\code\dz\basketball\scaffolding-v2\basketball-web\src\main\java\com\zhs"


def write(path, content):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        f.write(content)


int_enums = {
    "CaptainQualificationStatusEnum": [
        ("ACTIVE", 1, "有效"),
        ("REVOKED", 0, "已撤销"),
        ("EXPIRED", 2, "已过期"),
    ],
    "TeamStatusEnum": [
        ("DISABLED", 0, "已禁用"),
        ("ACTIVE", 1, "正常"),
        ("DISSOLVED", 2, "已解散"),
    ],
    "TeamAuditStatusEnum": [
        ("PENDING", 0, "待审核"),
        ("APPROVED", 1, "已通过"),
        ("REJECTED", 2, "已驳回"),
    ],
    "TeamPublicStatusEnum": [
        ("PRIVATE", 0, "非公开"),
        ("PUBLIC", 1, "公开"),
    ],
    "TeamMembershipStatusEnum": [
        ("LEFT", 0, "已离队"),
        ("ACTIVE", 1, "在队"),
        ("REMOVED", 2, "被移除"),
    ],
    "TeamJoinApplicationStatusEnum": [
        ("PENDING", 0, "待处理"),
        ("APPROVED", 1, "已通过"),
        ("REJECTED", 2, "已驳回"),
        ("CANCELLED", 3, "已取消"),
    ],
    "TeamInvitationStatusEnum": [
        ("PENDING", 0, "待响应"),
        ("ACCEPTED", 1, "已接受"),
        ("REJECTED", 2, "已拒绝"),
        ("EXPIRED", 3, "已过期"),
        ("CANCELLED", 4, "已取消"),
    ],
    "TournamentStatusEnum": [
        ("DISABLED", 0, "已禁用"),
        ("ACTIVE", 1, "进行中"),
        ("ENDED", 2, "已结束"),
    ],
    "TournamentSeasonStatusEnum": [
        ("PREPARE", 0, "筹备中"),
        ("REGISTRATION_OPEN", 1, "报名中"),
        ("IN_PROGRESS", 2, "进行中"),
        ("FINISHED", 3, "已结束"),
    ],
    "TournamentRegistrationStatusEnum": [
        ("DRAFT", 0, "草稿"),
        ("SUBMITTED", 1, "已提交"),
        ("REVIEWING", 2, "审核中"),
        ("APPROVED", 3, "已通过"),
        ("REJECTED", 4, "已驳回"),
        ("WITHDRAWN", 5, "已撤回"),
        ("CANCELLED", 6, "已取消"),
    ],
    "RegistrationPlayerValidationStatusEnum": [
        ("PENDING", 0, "待校验"),
        ("PASS", 1, "通过"),
        ("FAIL", 2, "不通过"),
    ],
    "DisciplineStatusEnum": [
        ("ACTIVE", 1, "生效中"),
        ("REVOKED", 0, "已撤销"),
        ("EXPIRED", 2, "已过期"),
    ],
}

for name, values in int_enums.items():
    helpers = ""
    if name == "TeamJoinApplicationStatusEnum":
        helpers = """
    public boolean canProcess() {
        return this == PENDING;
    }

    public boolean canCancel() {
        return this == PENDING;
    }"""
    elif name == "TeamInvitationStatusEnum":
        helpers = """
    public boolean canRespond() {
        return this == PENDING;
    }"""
    elif name == "TournamentRegistrationStatusEnum":
        helpers = """
    public boolean canEdit() {
        return this == DRAFT || this == REJECTED;
    }

    public boolean canSubmit() {
        return this == DRAFT || this == REJECTED;
    }

    public boolean canWithdraw() {
        return this == SUBMITTED;
    }

    public boolean canDeleteDraft() {
        return this == DRAFT;
    }"""
    elif name in ("CaptainQualificationStatusEnum", "DisciplineStatusEnum"):
        helpers = """
    public boolean isActive() {
        return this == ACTIVE;
    }"""
    elif name == "TeamStatusEnum":
        helpers = """
    public boolean isActive() {
        return this == ACTIVE;
    }"""

    entries = ",\n    ".join(f'{n}({c}, "{d}")' for n, c, d in values)
    content = f"""package com.zhs.enums;

import com.zhs.exception.ServiceException;

public enum {name} {{

    {entries};

    private final int code;
    private final String description;

    {name}(int code, String description) {{
        this.code = code;
        this.description = description;
    }}

    public int getCode() {{
        return code;
    }}

    public String getDescription() {{
        return description;
    }}
{helpers}

    public static {name} fromCode(Integer code) {{
        if (code == null) {{
            throw new ServiceException("未知状态");
        }}
        for ({name} value : values()) {{
            if (value.code == code) {{
                return value;
            }}
        }}
        throw new ServiceException("未知状态");
    }}
}}
"""
    write(os.path.join(BASE, "enums", name + ".java"), content)

str_enums = {
    "TeamMemberRoleEnum": [("CAPTAIN", "CAPTAIN", "队长"), ("MEMBER", "MEMBER", "队员")],
    "TeamRosterTypeEnum": [("OFFICIAL", "OFFICIAL", "正式"), ("TRIAL", "TRIAL", "试训")],
    "TeamJoinSourceEnum": [("CREATE", "CREATE", "创建"), ("INVITE", "INVITE", "邀请"), ("APPLY", "APPLY", "申请")],
}
for name, values in str_enums.items():
    entries = ",\n    ".join(f'{n}("{c}", "{d}")' for n, c, d in values)
    content = f"""package com.zhs.enums;

import com.zhs.exception.ServiceException;
import org.springframework.util.StringUtils;

public enum {name} {{

    {entries};

    private final String code;
    private final String description;

    {name}(String code, String description) {{
        this.code = code;
        this.description = description;
    }}

    public String getCode() {{
        return code;
    }}

    public String getDescription() {{
        return description;
    }}

    public static {name} fromCode(String code) {{
        if (!StringUtils.hasText(code)) {{
            throw new ServiceException("未知类型");
        }}
        for ({name} value : values()) {{
            if (value.code.equalsIgnoreCase(code.trim())) {{
                return value;
            }}
        }}
        throw new ServiceException("未知类型");
    }}
}}
"""
    write(os.path.join(BASE, "enums", name + ".java"), content)

print("Enums created")
