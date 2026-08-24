# SMOKE DATA ONLY — S13 playoff placeholder pre-scheduling
import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(r"d:/code/dz/basketball/scaffolding-v2/basketball-admin/scripts")))
import full_smoke_common as fs

fs.ADMIN = "http://111.229.241.29:8080"
fs.DB = dict(host="111.229.241.29", port=3306, user="root", password="zlzcnpa@@qaq", database="basketball", charset="utf8mb4")
ADMIN = fs.ADMIN
STAGE = 646
DIV = 177

def need(r, name):
    if r.get("code") != 200:
        raise RuntimeError(f"{name} failed: {r}")
    return r

def series_rows():
    return fs.db_query("""
        SELECT id, series_code, round_no, best_of, home_event_team_id, away_event_team_id,
               home_source_type, away_source_type, source_series_id_home, source_series_id_away,
               scheduled_date, scheduled_start_time, schedule_status, schedule_source, series_status
        FROM basket_match_series
        WHERE stage_id=%s AND is_delete=0
        ORDER BY round_no, bracket_slot
    """, (STAGE,))

def main():
    out = {}
    token = fs.admin_login()
    units = need(fs.req(f"{ADMIN}/basket/v4/admin/stages/{STAGE}/schedule-units", token=token), "units")
    data = units.get("data") or {}
    out["beforeSummary"] = data.get("summary")
    out["beforeUnits"] = [
        {
            "unitId": u.get("unitId"),
            "roundNo": u.get("roundNo"),
            "bestOf": u.get("bestOf"),
            "home": u.get("homeParticipantId"),
            "away": u.get("awayParticipantId"),
            "homeSource": u.get("homeSourceType"),
            "awaySource": u.get("awaySourceType"),
            "resolution": u.get("participantResolutionStatus"),
            "date": u.get("scheduledDate"),
            "time": u.get("scheduledStartTime"),
            "canEdit": u.get("canEditSchedule"),
        }
        for u in (data.get("units") or [])
    ]

    need(fs.req(f"{ADMIN}/basket/v4/admin/stages/{STAGE}/schedule/assign", "POST", {
        "unitType": "MATCH_SERIES", "unitId": 66, "scheduledDate": "2026-09-06", "scheduledStartTime": "21:00"
    }, token=token), "assign-sf1")
    need(fs.req(f"{ADMIN}/basket/v4/admin/stages/{STAGE}/schedule/assign", "POST", {
        "unitType": "MATCH_SERIES", "unitId": 67, "scheduledDate": "2026-09-06", "scheduledStartTime": "21:00"
    }, token=token), "assign-sf2")
    need(fs.req(f"{ADMIN}/basket/v4/admin/stages/{STAGE}/schedule/assign", "POST", {
        "unitType": "MATCH_SERIES", "unitId": 68, "scheduledDate": "2026-09-12", "scheduledStartTime": "21:00"
    }, token=token), "assign-final")

    need(fs.req(f"{ADMIN}/basket/v4/admin/stages/{STAGE}/schedule/batch-assign", "POST", {
        "roundNo": 2, "scheduledDate": "2026-09-06", "scheduledStartTime": "21:00"
    }, token=token), "batch-sf")

    reload = need(fs.req(f"{ADMIN}/basket/v4/admin/stages/{STAGE}/schedule-units", token=token), "reload")
    out["afterAssign"] = [
        {
            "unitId": u.get("unitId"),
            "roundNo": u.get("roundNo"),
            "resolution": u.get("participantResolutionStatus"),
            "date": u.get("scheduledDate"),
            "time": u.get("scheduledStartTime"),
            "canEdit": u.get("canEditSchedule"),
            "home": u.get("homeParticipantId"),
            "away": u.get("awayParticipantId"),
        }
        for u in ((reload.get("data") or {}).get("units") or [])
    ]

    preview = need(fs.req(f"{ADMIN}/basket/v4/admin/stages/{STAGE}/publish-preview", token=token), "publish-preview")
    out["publishPreview"] = preview.get("data")

    forfeit = fs.req(f"{ADMIN}/basket/v4/admin/stage-exception/series-forfeit", "POST", {
        "seriesId": 66, "forfeitEventTeamId": 296, "reason": "SMOKE placeholder forfeit must fail"
    }, token=token)
    out["forfeitPlaceholder"] = {"code": forfeit.get("code"), "msg": forfeit.get("msg")}

    official = fs.req(f"{ADMIN}/basket/v4/admin/stage-exception/match-official-result", "POST", {
        "matchId": 558, "homeScore": 90, "awayScore": 80, "reason": "SMOKE placeholder result must fail"
    }, token=token)
    out["officialPlaceholder"] = {"code": official.get("code"), "msg": official.get("msg")}

    manual = fs.req(f"{ADMIN}/basket/v4/admin/stage-exception/series-manual-resolve", "POST", {
        "seriesId": 66, "winnerEventTeamId": 296, "decisionType": "OTHER", "reason": "SMOKE placeholder manual must fail"
    }, token=token)
    out["manualPlaceholder"] = {"code": manual.get("code"), "msg": manual.get("msg")}

    pub = need(fs.req(f"{ADMIN}/basket/v4/admin/stages/{STAGE}/publish-schedule", "POST", token=token), "publish")
    out["published"] = pub.get("data") or True

    before_rev = fs.db_query("SELECT schedule_revision FROM basket_schedule_batch WHERE id=55")[0]["schedule_revision"]
    before_sf = [r for r in series_rows() if r["id"] == 66][0]
    out["advanceBefore"] = {
        "seriesId": before_sf["id"],
        "date": str(before_sf["scheduled_date"]),
        "time": str(before_sf["scheduled_start_time"]),
        "home": before_sf["home_event_team_id"],
        "away": before_sf["away_event_team_id"],
        "revision": before_rev,
    }

    for mid in (546, 547):
        need(fs.req(f"{ADMIN}/basket/v4/admin/stage-exception/match-official-result", "POST", {
            "matchId": mid, "homeScore": 91, "awayScore": 71, "reason": "SMOKE QF1 winner advance schedule preserve"
        }, token=token), f"qf1-game-{mid}")

    after_rev = fs.db_query("SELECT schedule_revision FROM basket_schedule_batch WHERE id=55")[0]["schedule_revision"]
    after_sf = [r for r in series_rows() if r["id"] == 66][0]
    out["advanceAfter"] = {
        "seriesId": after_sf["id"],
        "date": str(after_sf["scheduled_date"]),
        "time": str(after_sf["scheduled_start_time"]),
        "home": after_sf["home_event_team_id"],
        "away": after_sf["away_event_team_id"],
        "revision": after_rev,
        "source": after_sf["schedule_source"],
    }
    out["seriesAfter"] = [
        {k: (str(v) if v is not None else None) for k, v in r.items()}
        for r in series_rows()
    ]

    public = fs.req(f"http://111.229.241.29:8881/basket/v4/public/schedule?divisionId={DIV}")
    out["public"] = {"code": public.get("code"), "n": len(public.get("data") or []) if isinstance(public.get("data"), list) else public.get("data")}

    path = Path(r"d:/code/dz/basketball/scaffolding-v2/scripts/cnpa_s13_placeholder_preschedule_smoke_result.json")
    path.write_text(json.dumps(out, ensure_ascii=False, indent=2, default=str), encoding="utf-8")
    print(json.dumps({
        "forfeit": out["forfeitPlaceholder"],
        "official": out["officialPlaceholder"],
        "manual": out["manualPlaceholder"],
        "previewCan": (out["publishPreview"] or {}).get("canPublish") if isinstance(out["publishPreview"], dict) else None,
        "advanceBefore": out["advanceBefore"],
        "advanceAfter": out["advanceAfter"],
        "public": out["public"],
    }, ensure_ascii=False, indent=2, default=str))

if __name__ == "__main__":
    main()
