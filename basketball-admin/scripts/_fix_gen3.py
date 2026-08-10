from pathlib import Path
p=Path("full_smoke_common.py")
t=p.read_text(encoding="utf-8")
start=t.find("def generate_and_publish")
end=t.find("\ndef ", start+1)
new='''def generate_and_publish(token: str, stage_id: str, init: bool = True, division_id: str | None = None) -> dict:
    # Prefer series presence for elimination stages; for RR use stage-scoped matches.
    series_existing = list_series(token, stage_id)
    if series_existing:
        pub = publish_schedule(token, stage_id)
        return {"generate": {"code": 200, "msg": "skip-existing-series", "count": len(series_existing)}, "publish": pub}

    stage_matches = []
    if division_id:
        stage_matches = [
            m for m in list_matches(token, division_id, stage_id)
            if as_id(m.get("stageId") or stage_id) == as_id(stage_id) and not m.get("seriesId")
        ]
    if stage_matches:
        pub = publish_schedule(token, stage_id)
        return {"generate": {"code": 200, "msg": "skip-existing-matches", "count": len(stage_matches)}, "publish": pub}

    if init:
        ir = init_stage_teams(token, stage_id)
        if ir.get("code") != 200:
            msg = str(ir.get("msg") or "")
            if "已" not in msg and "already" not in msg.lower():
                raise RuntimeError(f"initialize-teams failed stage {stage_id}: {ir}")
    gen = generate_schedule(token, stage_id)
    if gen.get("code") != 200:
        raise RuntimeError(f"generate-schedule failed stage {stage_id}: {gen}")
    pub = publish_schedule(token, stage_id)
    if pub.get("code") != 200:
        msg = str(pub.get("msg") or "")
        if "已" not in msg and "PUBLISH" not in msg.upper() and "发布" not in msg:
            raise RuntimeError(f"publish-schedule failed stage {stage_id}: {pub}")
    return {"generate": gen, "publish": pub}
'''
p.write_text(t[:start]+new+t[end:], encoding="utf-8", newline="\n")
# Fix main: only confirm st2 after series completed; raise if no series
mp=Path("full_smoke_main.py")
mt=mp.read_text(encoding="utf-8")
old='''        s2_list = list_series(admin_token, stages["st2"])
        champion_et = None
        s3_list = []
        if len(s2_list) >= 2:
            sa = finish_series_games(admin_token, web_by_event, s2_list[0]["id"], ["HOME", "HOME"])
            g3 = next((g for g in (sa.get("games") or []) if g.get("seriesGameNo") == 3), None)
            bag.pass_("bo3_2_0", "Game3 not finished", f"status={sa.get('seriesStatus')} g3={g3.get('skipped') if g3 else None}")
            sb = finish_series_games(admin_token, web_by_event, s2_list[1]["id"], ["HOME", "AWAY", "HOME"])
            bag.pass_("bo3_2_1", "COMPLETED 2-1", str(sb.get("homeWinCount")))
        confirm_stage_qualification(admin_token, stages["st2"])'''
newm='''        s2_list = list_series(admin_token, stages["st2"])
        champion_et = None
        s3_list = []
        if len(s2_list) < 2:
            raise RuntimeError(f"expected 2 BO3 series, got {len(s2_list)}")
        sa = finish_series_games(admin_token, web_by_event, s2_list[0]["id"], ["HOME", "HOME"])
        g3 = next((g for g in (sa.get("games") or []) if g.get("seriesGameNo") == 3), None)
        bag.pass_("bo3_2_0", "Game3 not finished", f"status={sa.get('seriesStatus')} g3={g3.get('skipped') if g3 else None}")
        sb = finish_series_games(admin_token, web_by_event, s2_list[1]["id"], ["HOME", "AWAY", "HOME"])
        bag.pass_("bo3_2_1", "COMPLETED 2-1", str(sb.get("homeWinCount")))
        confirm_stage_qualification(admin_token, stages["st2"])'''
if old not in mt:
    raise SystemExit("main bo3 block not found")
mp.write_text(mt.replace(old,newm), encoding="utf-8", newline="\n")
print("patched")
