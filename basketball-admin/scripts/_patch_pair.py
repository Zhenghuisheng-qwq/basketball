from pathlib import Path
p = Path(r"d:/code/dz/basketball/scaffolding-v2/basketball-admin/scripts/playoff_v1_e2e_smoke.py")
text = p.read_text(encoding="utf-8")
old = '''def pairing_units(token: str, division_id: str, stage_id: str) -> list[dict]:
    series = list_series(token, stage_id)
    if series:
        out = []
        for s in sorted(series, key=lambda x: (x.get("bracketSlot") is None, x.get("bracketSlot") or 0, x.get("id") or 0)):
            out.append({"kind": "SERIES", "id": as_id(s["id"]), "bracketSlot": s.get("bracketSlot"),
                        "homeEventTeamId": as_id(s.get("homeEventTeamId")), "awayEventTeamId": as_id(s.get("awayEventTeamId")),
                        "bestOf": s.get("bestOf"),
                        "winnerEventTeamId": as_id(s["winnerEventTeamId"]) if s.get("winnerEventTeamId") else None, "raw": s})
        return out
    matches = [m for m in list_matches(token, division_id, stage_id) if not m.get("seriesId")]
    out = []
    for m in sorted(matches, key=lambda x: (x.get("bracketSlot") is None, x.get("bracketSlot") or 0, x.get("id") or 0)):
        out.append({"kind": "MATCH", "id": as_id(m["id"]), "bracketSlot": m.get("bracketSlot"),
                    "homeEventTeamId": as_id(m.get("homeEventTeamId")), "awayEventTeamId": as_id(m.get("awayEventTeamId")),
                    "bestOf": 1, "winnerEventTeamId": as_id(m["winnerEventTeamId"]) if m.get("winnerEventTeamId") else None, "raw": m})
    return out'''
new = '''def pairing_units(token: str, division_id: str, stage_id: str) -> list[dict]:
    series = list_series(token, stage_id)
    if series:
        out = []
        for s in sorted(series, key=lambda x: (x.get("bracketSlot") is None, x.get("bracketSlot") or 0, x.get("id") or 0)):
            out.append({"kind": "SERIES", "id": as_id(s["id"]), "bracketSlot": s.get("bracketSlot"),
                        "homeEventTeamId": as_id(s.get("homeEventTeamId")), "awayEventTeamId": as_id(s.get("awayEventTeamId")),
                        "bestOf": s.get("bestOf"),
                        "winnerEventTeamId": as_id(s["winnerEventTeamId"]) if s.get("winnerEventTeamId") else None, "raw": s})
        return out
    # Prefer DB stage-scoped matches — admin list API may ignore stageId and return whole division
    rows = db_query(
        "SELECT id, bracket_slot AS bracketSlot, home_event_team_id AS homeEventTeamId, away_event_team_id AS awayEventTeamId, "
        "winner_event_team_id AS winnerEventTeamId, match_status AS matchStatus, series_id AS seriesId "
        "FROM basket_match WHERE stage_id=%s AND is_delete=0 AND series_id IS NULL "
        "ORDER BY round_no ASC, bracket_slot ASC, id ASC",
        (int(stage_id),),
    )
    out = []
    for m in rows:
        out.append({"kind": "MATCH", "id": as_id(m["id"]), "bracketSlot": m.get("bracketSlot"),
                    "homeEventTeamId": as_id(m.get("homeEventTeamId")), "awayEventTeamId": as_id(m.get("awayEventTeamId")),
                    "bestOf": 1, "winnerEventTeamId": as_id(m["winnerEventTeamId"]) if m.get("winnerEventTeamId") else None, "raw": m})
    return out'''
if old not in text:
    raise SystemExit("pairing_units not found")
text = text.replace(old, new)
# fix stage2 assertion to use original seeds
text = text.replace(
'''    seeds2 = seed_map_for_stage(st2)
    seed2_by_et = {as_id(r["eventTeamId"]): int(r["seedNo"]) for r in seeds2}
    units2 = pairing_units(admin_token, division_id, st2)
    annotate_seeds(units2, seed2_by_et, name_by_et)
    got2_ordered = [(u["homeSeed"], u["awaySeed"]) for u in units2]
    out["stage2"] = [{"slot": u["bracketSlot"], "homeSeed": u["homeSeed"], "awaySeed": u["awaySeed"]} for u in units2]
    path_ok = got2_ordered == [(1, 4), (5, 8), (6, 7), (2, 3)]''',
'''    units2 = pairing_units(admin_token, division_id, st2)
    annotate_seeds(units2, seed_by_et, name_by_et)  # original create-time seeds
    got2_ordered = [(u["homeSeed"], u["awaySeed"]) for u in units2]
    out["stage2"] = [{"slot": u["bracketSlot"], "homeSeed": u["homeSeed"], "awaySeed": u["awaySeed"]} for u in units2]
    path_ok = got2_ordered == [(1, 4), (5, 8), (6, 7), (2, 3)]''')
p.write_text(text, encoding="utf-8")
import py_compile; py_compile.compile(str(p), doraise=True)
print("fixed pairing_units + stage2 assert")
