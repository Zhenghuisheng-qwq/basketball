from pathlib import Path
p=Path("full_smoke_common.py")
t=p.read_text(encoding="utf-8")
old='''    stage_matches = []
    if division_id:
        stage_matches = [
            m for m in list_matches(token, division_id, stage_id)
            if as_id(m.get("stageId") or stage_id) == as_id(stage_id) and not m.get("seriesId")
        ]
    if stage_matches:
        pub = publish_schedule(token, stage_id)
        return {"generate": {"code": 200, "msg": "skip-existing-matches", "count": len(stage_matches)}, "publish": pub}'''
new='''    # Prefer DB count — admin match list may omit stageId and over-match.
    db_cnt = count_stage_matches(stage_id)
    if db_cnt > 0:
        pub = publish_schedule(token, stage_id)
        return {"generate": {"code": 200, "msg": "skip-existing-matches-db", "count": db_cnt}, "publish": pub}'''
if old not in t:
    raise SystemExit("skip block missing")
p.write_text(t.replace(old,new), encoding="utf-8", newline="\n")
print("fixed skip using db count")

# Reset stage 560 execution to allow regenerate if possible via API, else new season
from full_smoke_common import *
# check if we can reset - probably not via API. Create new season ADV_V3 and run carefully.
