#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Generate CNPA V4 Round3 backend: CaptainGrant / Registration / Audit / Snapshot."""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ADMIN_JAVA = ROOT / "basketball-admin" / "src" / "main" / "java" / "com" / "zhs"
ADMIN_RES = ROOT / "basketball-admin" / "src" / "main" / "resources" / "mapper" / "basket" / "v4"
WEB_JAVA = ROOT / "basketball-web" / "src" / "main" / "java" / "com" / "zhs"
WEB_RES = ROOT / "basketball-web" / "src" / "main" / "resources" / "mapper" / "v4"


def write(path: Path, content: str):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content.lstrip("\n"), encoding="utf-8")
    print("wrote", path.relative_to(ROOT))


# ---------- Perms ----------
write(ADMIN_JAVA / "basket/constant/BasketEventV4Perms.java", """
package com.zhs.basket.constant;

/**
 * CNPA 赛事域 V4 后台权限。
 */
public final class BasketEventV4Perms {

    private BasketEventV4Perms() {}

    public static final String SERIES_LIST = "basket:v4Series:list";
    public static final String SERIES_QUERY = "basket:v4Series:query";
    public static final String SERIES_EDIT = "basket:v4Series:edit";

    public static final String SEASON_LIST = "basket:v4Season:list";
    public static final String SEASON_QUERY = "basket:v4Season:query";
    public static final String SEASON_ADD = "basket:v4Season:add";
    public static final String SEASON_EDIT = "basket:v4Season:edit";
    public static final String SEASON_PUBLISH = "basket:v4Season:publish";
    public static final String SEASON_OFFLINE = "basket:v4Season:offline";
    public static final String SEASON_REMOVE = "basket:v4Season:remove";

    public static final String DIVISION_LIST = "basket:v4Division:list";
    public static final String DIVISION_QUERY = "basket:v4Division:query";
    public static final String DIVISION_ADD = "basket:v4Division:add";
    public static final String DIVISION_EDIT = "basket:v4Division:edit";
    public static final String DIVISION_OPEN = "basket:v4Division:openRegistration";
    public static final String DIVISION_CLOSE = "basket:v4Division:closeRegistration";
    public static final String DIVISION_REMOVE = "basket:v4Division:remove";

    public static final String CAPTAIN_GRANT_LIST = "basket:v4CaptainGrant:list";
    public static final String CAPTAIN_GRANT_QUERY = "basket:v4CaptainGrant:query";
    public static final String CAPTAIN_GRANT_ADD = "basket:v4CaptainGrant:add";
    public static final String CAPTAIN_GRANT_REVOKE = "basket:v4CaptainGrant:revoke";

    public static final String REGISTRATION_AUDIT_LIST = "basket:v4RegistrationAudit:list";
    public static final String REGISTRATION_AUDIT_QUERY = "basket:v4RegistrationAudit:query";
    public static final String REGISTRATION_AUDIT_APPROVE = "basket:v4RegistrationAudit:approve";
    public static final String REGISTRATION_AUDIT_REJECT = "basket:v4RegistrationAudit:reject";

    public static final String EVENT_TEAM_LIST = "basket:v4EventTeam:list";
    public static final String EVENT_TEAM_QUERY = "basket:v4EventTeam:query";

    public static final String EVENT_PLAYER_LIST = "basket:v4EventPlayer:list";
}
""")

print("Round3 backend generator part 1 done - run part2 for services")
