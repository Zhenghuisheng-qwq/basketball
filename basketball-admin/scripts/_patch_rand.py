from pathlib import Path
p = Path(r"d:/code/dz/basketball/scaffolding-v2/basketball-admin/scripts/playoff_v1_e2e_smoke.py")
text = p.read_text(encoding="utf-8")
old = '''    def _setup_r(token, did):
        wipe_stages(token, did)
        st1 = _create_stage(token, did, stage_payload("R1", "随机首轮", 1, "SINGLE_ELIMINATION", 1, "WINNER_ADVANCE", "R2", pairing="RANDOM_DRAW"))
        st2 = _create_stage(token, did, stage_payload("R2", "继承轮", 2, "SINGLE_ELIMINATION", 1, "NONE", pairing="PLAYOFF_BRACKET"))
        chain_update(token, did)
        ids_holder["st1"] = st1
        ids_holder["st2"] = st2'''
new = '''    def _setup_r(token, did):
        wipe_stages(token, did)
        # 8 -> 4 -> 2：后续继承轮保持单轮，避免 Final 多队触发 BO1 完整树空位插入
        st1 = _create_stage(token, did, stage_payload("R1", "随机首轮", 1, "SINGLE_ELIMINATION", 1, "WINNER_ADVANCE", "R2", pairing="RANDOM_DRAW"))
        st2 = _create_stage(token, did, stage_payload("R2", "继承轮", 2, "SINGLE_ELIMINATION", 1, "WINNER_ADVANCE", "FINAL", pairing="PLAYOFF_BRACKET"))
        st3 = _create_stage(token, did, stage_payload("FINAL", "总决赛", 3, "SINGLE_ELIMINATION", 1, "NONE", pairing="PLAYOFF_BRACKET"))
        chain_update(token, did, top_n=4)
        ids_holder["st1"] = st1
        ids_holder["st2"] = st2
        ids_holder["st3"] = st3'''
if old not in text:
    raise SystemExit("setup_r not found")
text = text.replace(old, new)
# stage2 check still uses st2 pairing rule - good. Don't generate final in this case.
text = text.replace(
'''    generate_and_publish(admin_token, st2, init=True, division_id=division_id)
    stage2 = next(s for s in get_format_stages(admin_token, division_id) if as_id(s["id"]) == st2)
    out["laterPairingRule"] = stage2.get("pairingRule")
    out["laterReRandomize"] = "NO" if stage2.get("pairingRule") == "PLAYOFF_BRACKET" else "YES"
    out["pass"] = bool(out["firstRoundStable"] and out["laterReRandomize"] == "NO")
    return out''',
'''    generate_and_publish(admin_token, st2, init=True, division_id=division_id)
    stage2 = next(s for s in get_format_stages(admin_token, division_id) if as_id(s["id"]) == st2)
    out["laterPairingRule"] = stage2.get("pairingRule")
    out["laterReRandomize"] = "NO" if stage2.get("pairingRule") == "PLAYOFF_BRACKET" else "YES"
    u2 = pairing_units(admin_token, division_id, st2)
    out["laterPairCount"] = len(u2)
    out["pass"] = bool(out["firstRoundStable"] and out["laterReRandomize"] == "NO" and len(u2) == 2)
    return out''')
p.write_text(text, encoding="utf-8")
import py_compile; py_compile.compile(str(p), doraise=True)
print("random case fixed")
