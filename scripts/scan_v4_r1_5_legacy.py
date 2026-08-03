# -*- coding: utf-8 -*-
"""Scan legacy event-domain assets for CNPA V4 round 1.5."""
from __future__ import annotations

import re
from pathlib import Path

root = Path(__file__).resolve().parents[1]

LEGACY_HINTS = re.compile(
    r"(Tournament|CompetitionSeason|MatchSchedule|ScheduleRule|ScheduleGenerate|"
    r"Standing|Ranking|Honor|Settlement|CaptainQualification|CaptainGrant|"
    r"Registration|Roster|OpenRegistration|MatchData|MatchStat|MatchScore|"
    r"MatchDispute|MatchException|MatchLineup|MatchRoster|StageStanding|"
    r"SeasonRoster|SeasonResult|EventTeam|HallOfFame|PointAdjustment|"
    r"StatSync|DataSettlement|ScheduleManage|SeasonStat|PlayerRank|TeamRank|"
    r"LocalDemo)",
    re.I,
)

KEEP_HINTS = re.compile(
    r"(PlayerCert|PlatformChange|AccountPlayer|Partner|Announcement|HomeBanner|"
    r"PrivateFile|Discipline|BusinessAudit|TeamAudit|TeamProfile|"
    r"Dashboard|PermanentArchive)",
    re.I,
)

REQ_MAP = re.compile(
    r'@(?:RequestMapping|GetMapping|PostMapping|PutMapping|DeleteMapping)\s*(?:\([^)]*?(?:value|path)\s*=\s*)?["\']([^"\']+)["\']',
    re.S,
)
CLASS = re.compile(r"public\s+(?:class|interface)\s+(\w+)")
PKG = re.compile(r"^package\s+([\w.]+);", re.M)


def rel(p: Path) -> str:
    return str(p.relative_to(root)).replace("\\", "/")


def scan_controllers(base: Path, label: str):
    rows = []
    for p in sorted(base.rglob("*Controller.java")):
        text = p.read_text(encoding="utf-8", errors="ignore")
        name = CLASS.search(text)
        pkg = PKG.search(text)
        cname = name.group(1) if name else p.stem
        if "v4" in str(p).replace("\\", "/"):
            continue
        # classify
        is_legacy = bool(LEGACY_HINTS.search(cname) or LEGACY_HINTS.search(text[:800]))
        is_keep = bool(KEEP_HINTS.search(cname))
        if label == "admin" and "controller/basket" not in rel(p) and "controller\\basket" not in str(p):
            # admin only basket controllers for this scan unless clearly event
            if not is_legacy:
                continue
        if label == "web":
            if not is_legacy and "publicapi" not in rel(p) and "AccountTournament" not in cname:
                # still include public standing/tournament
                if not any(k in cname for k in ("Match", "Tournament", "Standing", "Registration", "Honor", "Captain", "Season", "Schedule")):
                    continue
        maps = REQ_MAP.findall(text)
        class_maps = []
        # prefer first RequestMapping on class: crude: take mappings before first method-ish
        # just list unique paths
        paths = []
        for m in maps:
            if m.startswith("/"):
                paths.append(m)
            else:
                paths.append("/" + m)
        # dedupe preserve order
        seen = set()
        upaths = []
        for x in paths:
            if x not in seen:
                seen.add(x)
                upaths.append(x)
        rows.append(
            {
                "label": label,
                "class": cname,
                "pkg": pkg.group(1) if pkg else "",
                "path": rel(p),
                "routes": upaths[:20],
                "legacy": is_legacy and not is_keep,
                "keepish": is_keep,
            }
        )
    return rows


def scan_jobs():
    rows = []
    job_dir = root / "basketball-admin/src/main/java/com/zhs/basket/job"
    for p in sorted(job_dir.glob("*.java")) if job_dir.exists() else []:
        text = p.read_text(encoding="utf-8", errors="ignore")
        name = CLASS.search(text)
        rows.append({"class": name.group(1) if name else p.stem, "path": rel(p), "snippet": text[:1200]})
    # also @Scheduled in basket package
    sched = []
    for p in (root / "basketball-admin/src/main/java").rglob("*.java"):
        text = p.read_text(encoding="utf-8", errors="ignore")
        if "@Scheduled" in text or "ScheduleAutoGenerate" in text or "MatchStatusAutoFinish" in text:
            if "quartz" in rel(p) and "SysJob" in p.name:
                continue
            if any(k in p.name for k in ("ScheduleAuto", "MatchStatusAuto", "Job")) or "@Scheduled" in text:
                sched.append(rel(p))
    return rows, sorted(set(sched))


def scan_v4_imports():
    bad = []
    patterns = [
        root / "basketball-admin/src/main/java/com/zhs/basket/domain/v4",
        root / "basketball-admin/src/main/java/com/zhs/basket/mapper/v4",
        root / "basketball-admin/src/main/java/com/zhs/basket/enums/v4",
        root / "basketball-web/src/main/java/com/zhs/model/v4",
        root / "basketball-web/src/main/java/com/zhs/dao/v4",
        root / "basketball-web/src/main/java/com/zhs/enums/v4",
        root / "basketball-admin/src/main/resources/mapper/basket/v4",
    ]
    import_re = re.compile(r"^import\s+([\w.]+);", re.M)
    for base in patterns:
        if not base.exists():
            continue
        for p in base.rglob("*"):
            if p.suffix not in {".java", ".xml"}:
                continue
            text = p.read_text(encoding="utf-8", errors="ignore")
            for imp in import_re.findall(text):
                if ".v4." in imp:
                    continue
                # old event domain packages
                if any(
                    x in imp
                    for x in (
                        "com.zhs.basket.service",
                        "com.zhs.basket.mapper",
                        "com.zhs.basket.domain",
                        "com.zhs.basket.enums",
                        "com.zhs.service",
                        "com.zhs.dao",
                        "com.zhs.model",
                        "com.zhs.enums",
                    )
                ):
                    # allow common utils? none of these should appear in pure entity/mapper/enum
                    bad.append((rel(p), imp))
            # xml namespace/type refs to non-v4
            if p.suffix == ".xml":
                for m in re.findall(r'type="(com\.zhs\.[^"]+)"', text):
                    if ".v4." not in m and "domain.v4" not in m:
                        bad.append((rel(p), "xml:" + m))
                for m in re.findall(r'namespace="(com\.zhs\.[^"]+)"', text):
                    if ".mapper.v4." not in m:
                        bad.append((rel(p), "ns:" + m))
    return bad


def list_by_glob(pattern: str, filter_re: re.Pattern | None = None):
    out = []
    for p in sorted(root.glob(pattern)):
        if "v4" in rel(p):
            continue
        name = p.name
        if filter_re and not filter_re.search(name):
            continue
        out.append(rel(p))
    return out


def main():
    admin_c = scan_controllers(root / "basketball-admin/src/main/java/com/zhs/web/controller/basket", "admin")
    web_c = scan_controllers(root / "basketball-web/src/main/java/com/zhs/controller", "web")
    jobs, sched = scan_jobs()
    bad = scan_v4_imports()

    print("===ADMIN CONTROLLERS===")
    for r in admin_c:
        flag = "LEGACY" if r["legacy"] else ("KEEP" if r["keepish"] else "OTHER")
        print(f"{flag}\t{r['class']}\t{','.join(r['routes'][:8])}\t{r['path']}")

    print("===WEB CONTROLLERS===")
    for r in web_c:
        flag = "LEGACY" if r["legacy"] else ("KEEP" if r["keepish"] else "OTHER")
        print(f"{flag}\t{r['class']}\t{','.join(r['routes'][:8])}\t{r['path']}")

    print("===JOBS===")
    for j in jobs:
        print(j["class"], j["path"])
    print("===SCHED_FILES===")
    for s in sched:
        print(s)

    print("===V4_BAD_IMPORTS count===", len(bad))
    for b in bad[:50]:
        print(b[0], "=>", b[1])

    # mappers
    print("===ADMIN MAPPERS===")
    for p in list_by_glob("basketball-admin/src/main/java/com/zhs/basket/mapper/*.java", LEGACY_HINTS):
        print(p)
    print("===ADMIN XML===")
    for p in list_by_glob("basketball-admin/src/main/resources/mapper/basket/*.xml", LEGACY_HINTS):
        print(p)
    print("===WEB DAO===")
    for p in list_by_glob("basketball-web/src/main/java/com/zhs/dao/*.java", LEGACY_HINTS):
        print(p)


if __name__ == "__main__":
    main()
